# Entscheidungen fallen gegen das effektive Modell, nicht gegen den Dateitext

Vor jedem Schreibvorgang löst jpm das effektive Maven-Modell des Ziel-Maven-Moduls auf:
Parent, `dependencyManagement` und per `scope=import` eingebundene Maven-BOMs. Ist die
Koordinate dort bereits verwaltet, fügt jpm die Dependency **ohne `<version>`** ein und
meldet, woher die Version stammt. Der naive Weg — immer eine Version schreiben —
übersteuert in einem BOM-verwalteten Projekt still die kuratierte Versionsmenge, und
BOM-verwaltet ist im modernen Java-Ökosystem der Normalfall, nicht die Ausnahme.

## Consequences

`maven-model-builder` gehört damit zum Kern, nicht zur Peripherie: jpm kann eine pom nicht
als bloße Textdatei behandeln, auch wenn es sie als solche schreibt.

`jpm update` erbt dieselbe Regel. Eine verwaltete Dependency wird nie einzeln angehoben —
stattdessen meldet jpm das verwaltende Maven-BOM als das, was anzuheben wäre. Ohne diese
gemeinsame Grundlage würden sich `add` und `update` widersprechen: eines respektierte das
BOM, das andere überschriebe es.

Das ist außerdem der Punkt, an dem jpm nachweisbar besser ist als Copy-Paste von
mvnrepository.com — diese Seite liefert immer eine Version, auch wenn sie falsch ist.
