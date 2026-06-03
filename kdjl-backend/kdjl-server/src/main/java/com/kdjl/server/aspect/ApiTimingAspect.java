package com.kdjl.server.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ApiTimingAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiTimingAspect.class);
    private static final long SLOW_THRESHOLD_MS = 200;

    @Pointcut("within(com.kdjl.server.controller.*)")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logTiming(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("{} {}ms", pjp.getSignature().toShortString(), elapsed);
        }
    }
}
