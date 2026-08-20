# Java with an embedded Maven Resolver rather than Rust or Go

jpm is written in Java (bytecode target 17) and embeds `org.apache.maven.resolver` together
with `maven-model-builder`, rather than starting in a fast-launching language with a
hand-built repository layer. That makes `settings.xml`, mirrors, proxies, authentication
against private Nexus/Artifactory instances and the effective model from
[ADR-0003](./0003-effective-model-over-file-text.md) correct rather than approximated. It is
the difference between "works inside the company on day one" and three rounds of auth bugs.

## Considered options

Rust or Go would have delivered ~10 ms startup and a dependency-free binary, but demanded
hand-written metadata handling, auth, mirror and proxy logic — weeks of work for something
that is one dependency in Java. TypeScript/Node was ruled out because a tool called *jpm*
should not require Java developers to install Node.

## Consequences

JVM startup time is the price. The remedy is GraalVM native-image — but deliberately after the
functionality, not before it: the resolver loads components dynamically, and reflection
configuration is a task you solve once at the end rather than anew with every feature.

As long as a fat JAR is shipped, users need a JRE 17. That JVM is independent of the Java
version of the project being edited — a team on Java 8 projects can run jpm on 17.
