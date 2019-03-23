# xray-class-instrumentor

This package will add subsegments of AWS X-Ray traces around method bodies.

It is similar to the AWS provided package for adding subsegments via Spring aspect oriented programming (https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java-aop-spring.html), however the weaving is done at compile time without using Spring, allowing instrumentation of code with subsegments for a smaller runtime and upstart overhead. 

You can add the `@Instrument` annotation to either methods, or classes. If added to a class all methods within that class with be instrumented.

The generated code is equivalent to 
```
Subsegment subsegment = AWSXRay.beginSubsegment(subsegmentName);
try {
    // Method body
} catch (Throwable t) {
    // We only want to log unchecked exceptions or errors as they are not expected
    if (!(t instanceof Exception) || (t instanceof RuntimeException)) {
        subsegment.addException(t);
    }
    throw t;
} finally {
    AWSXRay.endSubsegment();
}
```
The `subsegmentName` is `classSimpleName#methodName`.

The package only adds subsegments to existing traces, so you must make sure that existing traces have already been created by the code paths calling the instrumented code.

To use the package with maven, add the follwing to your pom file.

```
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.0</version>
            <configuration>
                <source>11</source>
                <target>11</target>
            </configuration>
        </plugin>
        <plugin>
            <groupId>com.github.m50d</groupId>
            <artifactId>aspectj-maven-plugin</artifactId>
            <version>1.11.1</version>
            <configuration>
                <complianceLevel>1.9</complianceLevel>
                <source>1.9</source>
                <target>1.9</target>
                <aspectLibraries>
                    <aspectLibrary>
                        <groupId>dk.tildeslash.xray</groupId>
                        <artifactId>class-instrumentor</artifactId>
                    </aspectLibrary>
                </aspectLibraries>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>

<dependencies>
    <dependency>
        <groupId>dk.tildeslash.xray</groupId>
        <artifactId>class-instrumentor</artifactId>
        <version>1.0</version>
    </dependency>
</dependencies>
```

Notice that this uses a forked AspectJ plugin (https://github.com/m50d/aspectj-maven-plugin) as of the time of writing the official AspectJ plugin from `org.codehaus.mojo` had problems with the Java modules introduced in Java 9, see https://github.com/mojohaus/aspectj-maven-plugin/issues/24.  

This package is not affiliated or supported by AWS.
