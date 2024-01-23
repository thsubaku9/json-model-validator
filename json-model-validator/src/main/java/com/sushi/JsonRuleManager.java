package com.sushi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sushi.jsonmodelvalidator.dataclass.AbstractExpression;

import io.appform.jsonrules.Expression;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonRuleManager<E extends Enum<E>> {
    private final Map<String, List<AbstractExpression>> exprMap;

    private final Map<E, Set<String>> actionRuleMap;

    private final Map<E, Set<String>> actionBypassRuleMap;

    private final Map<String, Expression> standaloneJsonModelExpr;

    private final Supplier<Set<String>> activeRuleSet;
    private final ObjectMapper mapper;

    private static final String MAINRULEPREFIX = "$";
    private static final String SUBRULEPREFIX = "&";

    @Builder
    public JsonRuleManager(Map<E, Set<String>> actionRuleMap, Map<E, Set<String>> actionBypassRuleMap,
            Supplier<Set<String>> activeRuleSet, ObjectMapper mapper) {
                this.exprMap = new HashMap<>();
                this.standaloneJsonModelExpr = new HashMap<>();
                this.actionRuleMap = actionRuleMap;
                this.actionBypassRuleMap = actionBypassRuleMap;
                this.activeRuleSet = activeRuleSet;
                this.mapper = mapper;
    }

    public JsonRuleManager<E> loadStandaloneExpressionMap(String alias, Object exoression) {
        if (standaloneJsonModelExpr.containsKey(alias))
            log.warn("{} is being overwritten in {} !", alias, JsonRuleManager.class);

        Expression expr = mapper.convertValue(exoression, new TypeReference<Expression>() { });
        standaloneJsonModelExpr.put(alias, expr);
        return this;
    }

}
