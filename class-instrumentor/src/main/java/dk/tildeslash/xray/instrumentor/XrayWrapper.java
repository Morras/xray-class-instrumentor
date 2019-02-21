package dk.tildeslash.xray.instrumentor;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;

/**
 * Simple wrapper around XRay, mainly to make it easy to write unit tests
 */
public class XrayWrapper {

    public Subsegment beginSubsegment(String subsegmentName) {
        return AWSXRay.beginSubsegment(subsegmentName);
    }

    public void endSubsegment() {
        AWSXRay.endSubsegment();
    }
}
