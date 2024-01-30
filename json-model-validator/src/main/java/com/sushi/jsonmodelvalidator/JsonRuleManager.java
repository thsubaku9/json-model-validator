package com.sushi.jsonmodelvalidator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import com.sushi.jsonmodelvalidator.dataclass.AbstractExpression;
import com.sushi.jsonmodelvalidator.dataclass.EvalResult;
import com.sushi.jsonmodelvalidator.dataclass.MainExpression;
import com.sushi.jsonmodelvalidator.dataclass.SubExpression;

import io.appform.jsonrules.Expression;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.var;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonRuleManager<E extends Enum<E>> {
    private final Map<String, List<AbstractExpression>> expressionMap;

    private final Map<E, Set<String>> actionRuleMap;

    private final Map<E, Set<String>> actionBypassRuleMap;

    private final Map<String, Expression> standaloneJsonModelExpr;

    private final Supplier<Set<String>> activeRuleSet;
    private final ObjectMapper mapper;

    private static final String MAINRULEPREFIX = "$";
    private static final String MAINRULEPREFIXREGEX = "\\$";
    private static final String SUBRULEPREFIX = "&";

    @Builder
    public JsonRuleManager(Map<E, Set<String>> actionRuleMap, Map<E, Set<String>> actionBypassRuleMap,
            Supplier<Set<String>> activeRuleSet, ObjectMapper mapper) {
        this.expressionMap = new HashMap<>();
        this.standaloneJsonModelExpr = new HashMap<>();
        this.actionRuleMap = actionRuleMap;
        this.actionBypassRuleMap = actionBypassRuleMap;
        this.activeRuleSet = activeRuleSet;
        this.mapper = mapper;
    }

    /**
     * loadStandaloneExpressionMap is to be used for base json rules which are being
     * utilized by reference
     * 
     * @param ruleAlias
     * @param expression
     * @return
     */
    public JsonRuleManager<E> loadStandaloneExpressionMap(String ruleAlias, Object expression) {
        if (standaloneJsonModelExpr.containsKey(ruleAlias))
            log.warn("{} is being overwritten in {} !", ruleAlias, JsonRuleManager.class);

        Expression expr = mapper.convertValue(expression, new TypeReference<Expression>() {
        });
        standaloneJsonModelExpr.put(ruleAlias, expr);
        return this;
    }

    /**
     * loadExpressionMap requires enum and corresponding jsonpath : json_object as
     * input
     * <p>
     * </p>
     * if "$.path.to.field" : expr_object -> path binding happens
     * <p>
     * </p>
     * if "$.path.to.field" : alias_ref ->
     * <p>
     * </p>
     * if "&.path.to.subnode" : (expr_objet| alias_ref) -> traversal happens to that
     * node followed by evaluation
     * 
     * @param expressionKey
     * @param jsonPathMap
     * @return
     */
    public JsonRuleManager<E> loadExpressionMap(String expressionKey, Map<String, Object> jsonPathMap) {
        if (expressionMap.containsKey(expressionKey))
            log.warn("{} is being rewritten", expressionKey);

        log.info("{} is being loaded", expressionKey);

        Map<String, String> symbolicRefMap = mapper.convertValue(
                jsonPathMap.entrySet().stream().filter(it -> it.getValue() instanceof String)
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue)),
                new TypeReference<Map<String, String>>() {
                });

        Map<String, Expression> hardRefMap = mapper.convertValue(
                jsonPathMap.entrySet().stream().filter(it -> !(it.getValue() instanceof String))
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue)),
                new TypeReference<Map<String, Expression>>() {
                });

        expressionMap.put(expressionKey,
                Stream.concat(loadExpressionSymbolic(symbolicRefMap), loadExpressionHard(hardRefMap))
                        .collect(Collectors.toList()));
        return this;
    }

    private Stream<AbstractExpression> loadExpressionSymbolic(Map<String, String> symbolicRules) {
        if (symbolicRules == null)
            return Stream.empty();

        return symbolicRules.entrySet().stream().map(it -> {
            String evalPath = it.getKey().startsWith(SUBRULEPREFIX)
                    ? it.getKey().replaceFirst(SUBRULEPREFIX, MAINRULEPREFIXREGEX)
                    : it.getKey();
            String modelAlias = it.getValue();

            Expression expr = standaloneJsonModelExpr.get(modelAlias);
            if (expr == null)
                return null;

            return (AbstractExpression) SubExpression.builder().jsonNodePath(evalPath).expr(expr).build();
        }).filter(Objects::nonNull);

    }

    private Stream<AbstractExpression> loadExpressionHard(Map<String, Expression> jsonPathMap) {
        if (jsonPathMap == null)
            return Stream.empty();

        Stream<AbstractExpression> mainRule = jsonPathMap.entrySet().stream()
                .filter(it -> it.getKey().startsWith(MAINRULEPREFIX)).map(this::toExpression);

        Stream<AbstractExpression> subRule = jsonPathMap.entrySet().stream()
                .filter(it -> it.getKey().startsWith(SUBRULEPREFIX)).map(this::toSubExpression);

        return Stream.concat(mainRule, subRule);
    }

    @SneakyThrows
    private MainExpression toExpression(Entry<String, Expression> entry) {
        ObjectNode node = mapper.valueToTree(entry.getValue());
        node.put("path", entry.getKey());
        var expr = mapper.treeToValue(node, Expression.class);
        return MainExpression.builder().expr(expr).build();
    }

    private SubExpression toSubExpression(Entry<String, Expression> entry) {
        return SubExpression.builder()
                .jsonNodePath(entry.getKey().replaceFirst(SUBRULEPREFIX, MAINRULEPREFIXREGEX))
                .expr(entry.getValue())
                .build();
    }

    /// passthrough rule section ///

    public <T> boolean avoidModelEvaluation(E enumKey, T data) {
        return avoidModelEvaluationJsonNode(enumKey, mapper.valueToTree(data));
    }

    public boolean avoidModelEvaluationJsonNode(E enumKey, JsonNode jsonData) {
        var bypassRules = Optional.ofNullable(actionBypassRuleMap.get(enumKey)).orElse(Set.of());
        var activeRules = activeRuleSet.get();

        return bypassRules.stream()
                .filter(activeRules::contains)
                .anyMatch(expressionKey -> {
                    return expressionMap.getOrDefault(expressionKey, List.of())
                            .stream()
                            .allMatch(it -> it.evaluate(this, jsonData));
                });
    }

    /// evaluation section ///

    /**
     * Perform oneshot evaluation
     * 
     * @param <T>
     * @param enumKey
     * @param data
     * @return
     */
    public <T> boolean evaluate(E enumKey, T data) {
        return evaluteJsonNode(enumKey, mapper.valueToTree(data));
    }

    private boolean evaluteJsonNode(E enumKey, JsonNode jsonNode) {
        var linkedRules = Optional.ofNullable(actionRuleMap.get(enumKey)).orElse(Set.of());
        var activeRules = activeRuleSet.get();

        return linkedRules.stream()
                .filter(activeRules::contains)
                .allMatch(expressionKey -> {
                    return expressionMap.getOrDefault(expressionKey, List.of())
                            .stream()
                            .allMatch(it -> it.evaluate(this, jsonNode));
                });
    }

    public <T> Iterator<Callable<EvalResult>> evaluationIterator(E enumKey, T data) {
        return evaluationIteratorJsonNode(enumKey, mapper.valueToTree(data));
    }

    public Iterator<Callable<EvalResult>> evaluationIteratorJsonNode(E enumKey, JsonNode jsonNode) {
        return null;
    }

    // coupled with MainExpression
    public boolean evaluateMainExpression(@Nonnull JsonNode node, @Nonnull Expression exprRule) {
        return exprRule.evaluate(node);
    }

    // coupled with SubExpression
    @SneakyThrows
    public boolean evaluteSubExpression(@Nonnull String path, @Nonnull JsonNode node, @Nonnull Expression exprRule) {
        var object = JsonPath.parse(mapper.writeValueAsString(node)).read(path);
        var terminalNode = mapper.valueToTree(object);

        if (terminalNode.isArray()) {
            ArrayNode terminalArrayNode = (ArrayNode) terminalNode;
            AtomicBoolean result = new AtomicBoolean(!terminalArrayNode.isEmpty());

            terminalArrayNode.forEach(it -> result.set(result.get() && exprRule.evaluate(it)));
        }

        return exprRule.evaluate(terminalNode);
    }
}
