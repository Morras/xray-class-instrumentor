package dk.tildeslash.xray.instrumentor;

/**
 * Class used for testing to see if all methods within an annotated class is altered.
 */
@Instrument
public class AnnotatedClass {

    private String privateMethod() {
        return "return value from private method";
    }

    public String nestedMethod() {
        return privateMethod();
    }
}
