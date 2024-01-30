package com.sushi.jsonmodelvalidator.dataclass;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class EvalResult {
    private Boolean isSuccess;
    private String ruleName;
}
