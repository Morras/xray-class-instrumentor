package dk.tildeslash.xray.instrumentor;

@Instrument
public class AnnotatedClass {

    private String privateMethod() {
        return "return value from private method";
    }

    public String nestedMethod() {
        return privateMethod();
    }
}
