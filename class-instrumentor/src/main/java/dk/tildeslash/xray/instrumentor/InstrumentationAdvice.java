package dk.tildeslash.xray.instrumentor;

import com.amazonaws.xray.entities.Subsegment;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Aspect
public class InstrumentationAdvice {

    private XrayWrapper xrayWrapper;

    public InstrumentationAdvice() {
        xrayWrapper = new XrayWrapper();
    }

    public InstrumentationAdvice(XrayWrapper xrayWrapper){
        this.xrayWrapper = xrayWrapper;
    }

    // Execute on either all methods in a class annotated with Instrument,
    // or methods annotated directly with the annotation
    @Around("(@annotation(Instrument) && execution(* *(..))) || " +
            "execution(* (@Instrument *).*(..))")
    public Object instrumentMethod(ProceedingJoinPoint joinPoint) throws Throwable {

        Signature signature = joinPoint.getSignature();

        String subsegmentNameOverride = "";
        if (signature instanceof MethodSignature) {
            MethodSignature methodSignature = (MethodSignature) signature;
            Method method = methodSignature.getMethod();
            if (method.isAnnotationPresent(Instrument.class)) {
                Instrument annotation = method.getAnnotation(Instrument.class);
                subsegmentNameOverride = annotation.subsegment();
            }
        }

        String joinPintClass = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        String subsegmentName;
        if (subsegmentNameOverride.isBlank()) {
            subsegmentName = joinPintClass + "#" + methodName;
        }  else {
            subsegmentName = subsegmentNameOverride;
        }

        Subsegment subsegment = xrayWrapper.beginSubsegment(subsegmentName);

        Object returnObject;
        try {
            returnObject = joinPoint.proceed();
        } catch (Throwable t) {
            // We only want to log unchecked exceptions or errors as they are not expected
            if (!(t instanceof Exception) || (t instanceof RuntimeException)) {
                subsegment.addException(t);
            }
            throw t;
        } finally {
            xrayWrapper.endSubsegment();
        }

        return returnObject;
    }
}
