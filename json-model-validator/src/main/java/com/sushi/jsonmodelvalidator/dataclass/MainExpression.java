package com.sushi.jsonmodelvalidator.dataclass;

import com.fasterxml.jackson.databind.JsonNode;
import com.sushi.jsonmodelvalidator.JsonRuleManager;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class MainExpression extends AbstractExpression{
    
    @Override
    public boolean evaluate(JsonRuleManager<? extends Enum<?>> michaelScott, JsonNode node) {
        return michaelScott.evaluateMainExpression(node, expr);
    }
    
}
