package com.sushi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sushi.jsonmodelvalidator.dataclass.AbstractExpression;

import io.appform.jsonrules.Expression;
import lombok.Builder;
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

    public JsonRuleManager<E> loadStandaloneExpressionMap(String ruleAlias, Object expression) {
        if (standaloneJsonModelExpr.containsKey(ruleAlias))
            log.warn("{} is being overwritten in {} !", ruleAlias, JsonRuleManager.class);

        Expression expr = mapper.convertValue(expression, new TypeReference<Expression>() {
        });
        standaloneJsonModelExpr.put(ruleAlias, expr);
        return this;
    }

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

        expressionMap.put(expressionKey, Stream.concat(loadExpressionSymbolic(symbolicRefMap), loadExpressionHard(hardRefMap)).collect(Collectors.toList()));
        return this;
    }

    private Stream<AbstractExpression> loadExpressionSymbolic(Map<String,String> symbolicRules) {
        return Stream.of();
    }

    private Stream<AbstractExpression> loadExpressionHard(Map<String,Expression> hardRules) {
        return Stream.of();
    }

}
