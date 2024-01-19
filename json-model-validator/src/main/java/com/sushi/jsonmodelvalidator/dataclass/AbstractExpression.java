package com.sushi.jsonmodelvalidator.dataclass;

import com.fasterxml.jackson.databind.JsonNode;
import com.sushi.JsonRuleManager;

import io.appform.jsonrules.Expression;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class AbstractExpression {
    Expression expr;

    public abstract boolean evaluate(JsonRuleManager<? extends Enum<?>> michaelScott, JsonNode node);
}
