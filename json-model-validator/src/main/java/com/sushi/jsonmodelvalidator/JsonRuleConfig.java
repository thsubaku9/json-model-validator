package com.sushi.jsonmodelvalidator;

import java.util.Map;
import java.util.Set;

import io.appform.jsonrules.Expression;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Jacksonized
@Builder
/**
 * All Object classes map to Expression class of json-rule
 */
public class JsonRuleConfig<E extends Enum<E>> {
    @Builder.Default
    private Map<String, Map<String, Object>> expressionJsonPathMap = Map.of();
    @Builder.Default
    private Map<String, Expression> fixedJsonModelRule = Map.of();
    @Builder.Default
    private Map<E, Set<String>> expressionsAgainstAction = Map.of();
    @Builder.Default
    private Map<E, Set<String>> passThroughRule = Map.of();
    @Builder.Default
    private Set<String> activeExpressionKeys = Set.of();
}
