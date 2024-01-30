package com.sushi.jsonmodelvalidator.dataclass;

import com.fasterxml.jackson.databind.JsonNode;
import com.sushi.JsonRuleManager;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class SubExpression extends AbstractExpression{
    private String jsonNodePath;

    @Override
    public boolean evaluate(JsonRuleManager<? extends Enum<?>> michaelScott, JsonNode node) {
        return michaelScott.evaluteSubExpression(jsonNodePath, node, expr);
    }
    
}
