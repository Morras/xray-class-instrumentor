package dk.tildeslash.xray.instrumentor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target( {ElementType.METHOD, ElementType.TYPE} )
public @interface Instrument {
}
