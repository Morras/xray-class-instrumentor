package dk.tildeslash.xray.instrumentor;

import com.amazonaws.services.xray.AWSXRayClientBuilder;
import com.amazonaws.services.xray.model.*;
import com.amazonaws.xray.AWSXRay;

import com.amazonaws.xray.entities.Segment;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Integration test to make sure aspect weaving is working as intended.
 *
 * To run the integration tests, you need to have a running X-Ray daemon, as the tests
 * are creating traces and sending them to X-Ray and then retrieves the traces to see
 * if the match the expected traces.
 *
 * Since the weaved method only adds sub segments, all the tests needs to start and end
 * a trace that the sub segments can be added to.
 */
public class AdviceIntegrationTest {

    private AnnotatedClass annotatedClass = new AnnotatedClass();
    private AnnotatedMethods annotatedMethods = new AnnotatedMethods();
    private static com.amazonaws.services.xray.AWSXRay xRayClient;

    @BeforeClass
    public static void setup() {
        xRayClient = AWSXRayClientBuilder.standard().withRegion("eu-west-1").build();
    }

    @Test
    public void subsegmentsShouldBeRecordedForNestedPrivateMethodsIfClassIsAnnotated() throws InterruptedException {
        String traceName = UUID.randomUUID().toString();
        Segment segment = AWSXRay.beginSegment(traceName);

        annotatedClass.nestedMethod();
        AWSXRay.endSegment();

        String traceDocument = getTraceDocument(segment);

        assertTrue("Raw trace should contains the right trace name",
                traceDocument.contains("\"name\":\"" + traceName + "\"") );

        assertTrue("Raw trace should contain subsegments",
                traceDocument.contains("\"subsegments\""));

        assertTrue("Raw trace should contain the name of the public method being instrumented",
                traceDocument.contains("\"name\":\"AnnotatedClass#nestedMethod\""));

        assertTrue("Raw trace should contain the name of the private method being instrumented",
                traceDocument.contains("\"name\":\"AnnotatedClass#privateMethod\""));
    }

    @Test(expected = RuntimeException.class)
    public void thrownExceptionShouldShowUpInTraceSubsegment() throws InterruptedException {
        String traceName = UUID.randomUUID().toString();
        Segment segment = AWSXRay.beginSegment(traceName);

        try {
            annotatedMethods.exceptionThrowingMethod();
        } finally {
            AWSXRay.endSegment();
            String traceDocument = getTraceDocument(segment);

            assertTrue("Raw trace should contains the right trace name",
                    traceDocument.contains("\"name\":\"" + traceName + "\"") );

            assertTrue("Raw trace should contain subsegments",
                    traceDocument.contains("\"subsegments\""));

            assertTrue("Raw trace should contain the name of the method being instrumented",
                    traceDocument.contains("\"name\":\"AnnotatedMethods#exceptionThrowingMethod\""));

            assertTrue("Raw trace should contain informatino about the exception thrown",
                    traceDocument.contains("\"type\":\"java.lang.RuntimeException\""));

            assertTrue("Raw trace should say that a fault occurred",
                    traceDocument.contains("\"fault\":true"));
        }
    }

    @Test
    public void unannotatedMethodShouldNotGiveSubsegmentTrace() throws InterruptedException {
        String traceName = UUID.randomUUID().toString();
        Segment segment = AWSXRay.beginSegment(traceName);

        annotatedMethods.unannotatedMethod();
        AWSXRay.endSegment();
        String traceDocument = getTraceDocument(segment);

        assertFalse("Raw trace should not contain any subsegments",
                traceDocument.contains("sub\"subsegments\""));
    }

    private String getTraceDocument(Segment segment) throws InterruptedException {
        Date startTime = new Date((long) (segment.getStartTime() * 1000));
        Date endTime = new Date((long) (segment.getEndTime() * 1000));

        GetTraceSummariesRequest traceRequest =
                new GetTraceSummariesRequest().withStartTime(startTime).withEndTime(endTime);

        GetTraceSummariesResult summariesResult = xRayClient.getTraceSummaries(traceRequest);

        while(summariesResult.getTraceSummaries().isEmpty() && new Date().getTime() - endTime.getTime() < 5 * 60 * 1000) {
            Thread.sleep(5000);
            summariesResult = xRayClient.getTraceSummaries(traceRequest);
        }

        if (summariesResult.getTraceSummaries().isEmpty()) {
            fail("Unable to get traces from XRay service, " +
                    "either they were not delivered or test timed out before they could be fetched");
        }

        BatchGetTracesRequest tracesRequest = new BatchGetTracesRequest().withTraceIds(summariesResult.getTraceSummaries().get(0).getId());
        BatchGetTracesResult batchGetTracesResult = xRayClient.batchGetTraces(tracesRequest);

        List<Trace> traces = batchGetTracesResult.getTraces();
        assertEquals("There should only be one trace", 1, traces.size());
        Trace trace = traces.get(0);

        List<com.amazonaws.services.xray.model.Segment> segments = trace.getSegments();
        assertEquals("There should only be one segment", 1, segments.size());

        return segments.get(0).getDocument();
    }
}
