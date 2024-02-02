package com.sushi.jsonmodelvalidator;


import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Provider;
import com.sushi.jsonmodelvalidator.dataclass.EvalResult;
import com.sushi.jsonmodelvalidator.interfaces.JsonRuleArgument;
import com.sushi.jsonmodelvalidator.interfaces.JsonRuleStamp;

import lombok.AllArgsConstructor;
import lombok.var;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class JsonRuleMethodInterceptor<E extends Enum<E>> implements MethodInterceptor {

    private Provider<JsonRuleManager<E>> ruleManagerProvider;
    private Consumer<EvalResult> reportErr;
    private Object defaultReturnObject;

    @Override
    public Object invoke(MethodInvocation proceedingJoinPoint) throws Throwable {
        JsonRuleStamp stamp = proceedingJoinPoint.getMethod().getAnnotation(JsonRuleStamp.class);
        Class<E> enumClazz = (Class<E>) stamp.enumClass();
        E enumVal = Enum.valueOf(enumClazz, stamp.enumKey());

        if (jsonRuleApplicationSuccess(proceedingJoinPoint, enumVal)) {
            proceedingJoinPoint.proceed();
        }

        return defaultReturnObject;
    }

    private boolean jsonRuleApplicationSuccess(MethodInvocation pjp, E enumVal) {
        var res = obtainRuleResult(pjp, enumVal);
        return res.allMatch(Boolean.TRUE::equals);
    }

    private Stream<Boolean> obtainRuleResult(MethodInvocation pjp, E enumVal) {
        return obtainIndices(pjp).boxed().flatMap(it -> {
            Object obj = pjp.getArguments()[it];

            var callableIterator = this.ruleManagerProvider.get().evaluationIterator(enumVal, obj);
            Stream<Boolean> resultStream = Stream.empty();

            while (callableIterator.hasNext()) {
                try {
                    var res = callableIterator.next().call();
                    if (Boolean.FALSE.equals(res.getIsSuccess())) {
                        reportErr.accept(res);
                    }
                    resultStream = Stream.concat(resultStream, Stream.of(res.getIsSuccess()));
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
            return resultStream;
        });
    }
    
    private IntStream obtainIndices(final MethodInvocation pjp) {
        return IntStream.range(0, pjp.getMethod().getParameterCount())
            .filter(it -> pjp.getMethod().getParameters()[it].isAnnotationPresent(JsonRuleArgument.class));
    }
}
