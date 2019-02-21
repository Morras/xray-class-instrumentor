package dk.tildeslash.xray.instrumentor;

import com.amazonaws.xray.entities.Subsegment;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class InstrumentationAdvice {

    private XrayWrapper xrayWrapper;

    public InstrumentationAdvice() {
        xrayWrapper = new XrayWrapper();
    }

    public InstrumentationAdvice(XrayWrapper xrayWrapper){
        this.xrayWrapper = xrayWrapper;
    }

    @Around("(@annotation(Instrument) && execution(* *(..))) || " +
            "execution(* (@Instrument *).*(..))")
    public Object instrumentMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String joinPintClass = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String subsegmentName = joinPintClass + "#" + methodName;

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
