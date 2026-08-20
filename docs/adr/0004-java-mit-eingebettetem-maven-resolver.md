# Java mit eingebettetem Maven Resolver statt Rust oder Go

jpm ist in Java geschrieben (Bytecode-Ziel 17) und bettet `org.apache.maven.resolver`
samt `maven-model-builder` ein, statt in einer schnell startenden Sprache mit
selbstgebautem Repository-Handling anzutreten. Damit sind `settings.xml`, Mirrors, Proxies,
Authentifizierung gegen private Nexus-/Artifactory-Instanzen und das effektive Modell aus
[ADR-0003](./0003-effektives-modell-statt-dateitext.md) korrekt statt nachempfunden. Das
ist der Unterschied zwischen „funktioniert in der Firma am ersten Tag" und drei Runden
Auth-Bugs.

## Considered Options

Rust oder Go hätten ~10 ms Startzeit und eine abhängigkeitsfreie Binary geliefert, aber
Metadaten-Handling, Auth, Mirror- und Proxy-Logik von Hand verlangt — Wochen Arbeit für
etwas, das in Java eine Abhängigkeit ist. TypeScript/Node schied aus, weil ein Werkzeug
namens *jpm* keine Node-Installation von Java-Entwicklern verlangen sollte.

## Consequences

Die JVM-Startzeit ist der Preis. Gegenmittel ist GraalVM native-image — aber bewusst erst
nach der Funktionalität: der Resolver lädt Komponenten dynamisch, und
Reflection-Konfiguration ist eine Aufgabe, die man einmal am Ende löst statt bei jedem
Feature neu.

Solange ein Fat-JAR ausgeliefert wird, brauchen Nutzer eine JRE 17. Diese JVM ist
unabhängig von der Java-Version des bearbeiteten Projekts — ein Team mit Java-8-Projekten
kann jpm auf 17 fahren.
