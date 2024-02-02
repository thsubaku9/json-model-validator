package com.sushi.jsonmodelvalidator;


import java.util.function.Consumer;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Provider;
import com.sushi.jsonmodelvalidator.dataclass.EvalResult;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class JsonRuleMethodInterceptor<E extends Enum<E>> implements MethodInterceptor {

    private Provider<JsonRuleManager<E>> ruleManagerProvider;
    private Consumer<EvalResult> reportErr;
    private Object defaultReturnObject;

    @Override
    public Object invoke(MethodInvocation proceedingJoinPoint) throws Throwable {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'invoke'");
    }
    
}
