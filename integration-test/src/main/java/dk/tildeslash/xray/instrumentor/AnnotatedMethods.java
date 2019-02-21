package dk.tildeslash.xray.instrumentor;

public class AnnotatedMethods {

    @Instrument
    public void exceptionThrowingMethod() {
        throw new RuntimeException();
    }

    public void unannotatedMethod() {

    }
}
