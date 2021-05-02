package instrumentor;

import com.amazonaws.xray.entities.Subsegment;
import dk.tildeslash.xray.instrumentor.Instrument;
import dk.tildeslash.xray.instrumentor.InstrumentationAdvice;
import dk.tildeslash.xray.instrumentor.XrayWrapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class InstrumentationAdviceTest {

    private Subsegment subsegment;
    private ProceedingJoinPoint joinPoint;

    private XrayWrapper xrayWrapper = mock(XrayWrapper.class);
    private InstrumentationAdvice advice = new InstrumentationAdvice(xrayWrapper);

    @Before
    public void setup() {
        subsegment = mock(Subsegment.class);
        when(xrayWrapper.beginSubsegment(anyString())).thenReturn(subsegment);

        joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethodName");
        // Choosing array list for no other reason than it does not show up in the source code,
        // so the name should be unique in the code
        when(joinPoint.getTarget()).thenReturn(new ArrayList());
    }

    @Test
    public void adviceShouldBeginAndEndSubsegmentBeforeMethodBody() throws Throwable {
        advice.instrumentMethod(joinPoint);
        verify(xrayWrapper).beginSubsegment("ArrayList#testMethodName");
        verify(xrayWrapper).endSubsegment();
    }

    @Test
    public void adviceShouldReturnValueFromMethodBody() throws Throwable {
        String methodReturn = "test value";
        when(joinPoint.proceed()).thenReturn(methodReturn);
        Object returnObject = advice.instrumentMethod(joinPoint);
        assertEquals(methodReturn, returnObject);
    }

    @Test(expected = RuntimeException.class)
    public void adviceShouldLogRuntimeExceptionsToXRayAndRethrow() throws Throwable {
        Exception thrownException = new RuntimeException();
        when(joinPoint.proceed()).thenThrow(thrownException);

        try {
            advice.instrumentMethod(joinPoint);
        } finally {
            verify(subsegment).addException(thrownException);
            verify(xrayWrapper).endSubsegment();
        }
    }

    @Test(expected = IOException.class)
    public void AdviceShouldNotLogCheckedExceptionsToXRayButRethrow() throws Throwable {
        Exception thrownException = new IOException();
        when(joinPoint.proceed()).thenThrow(thrownException);

        try {
            advice.instrumentMethod(joinPoint);
        } finally {
            verify(subsegment, never()).addException(thrownException);
            verify(xrayWrapper).endSubsegment();
        }
    }

    @Test(expected = Throwable.class)
    public void adviceShouldLogOtherThrowablesToXRayAndRethrow() throws Throwable {
        Throwable thrownException = new Throwable();
        when(joinPoint.proceed()).thenThrow(thrownException);

        try {
            advice.instrumentMethod(joinPoint);
        } finally {
            verify(subsegment).addException(thrownException);
            verify(xrayWrapper).endSubsegment();
        }
    }
}
