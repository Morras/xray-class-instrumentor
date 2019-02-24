package dk.tildeslash.xray.instrumentor;

/**
 * Class used for testing to see that only methods with annotations gets altered.
 */
public class AnnotatedMethods {

    @Instrument
    public void exceptionThrowingMethod() {
        throw new RuntimeException();
    }

    public void unannotatedMethod() {

    }
}
