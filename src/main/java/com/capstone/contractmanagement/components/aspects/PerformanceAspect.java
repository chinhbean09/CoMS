package com.capstone.contractmanagement.components.aspects;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Aspect
@Component
public class PerformanceAspect {
    private Logger logger = Logger.getLogger(getClass().getName());

    private String getMethodName(JoinPoint joinPoint) {
        return joinPoint.getSignature().getName();
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void  controllerMethods() {};

    @Before("controllerMethods()")
    public void beforeMethodExecution(JoinPoint joinPoint) {
        String remoteAddress = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getRemoteAddr();
        logger.info("Starting execution of " + this.getMethodName(joinPoint) + ", IP address: " + remoteAddress);
    }

    @After("controllerMethods()")
    public void afterMethodExecution(JoinPoint joinPoint) {
        String remoteAddress = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getRemoteAddr();
        logger.info("Finished execution of " + this.getMethodName(joinPoint) + ", IP address: " + remoteAddress);
    }
    @Around("controllerMethods()")
    public Object measureControllerMethodExecutionTime(ProceedingJoinPoint proceedingJoinPoint)
            throws Throwable {
        long start = System.nanoTime();
        //proceedingJoinPoint.getArgs()
        Object returnValue = proceedingJoinPoint.proceed();
        long end = System.nanoTime();
        String methodName = proceedingJoinPoint.getSignature().getName();

        logger.info("Execution of " + methodName + " took " +
                TimeUnit.NANOSECONDS.toMillis(end - start) + " ms");
        return returnValue;
    }
}
