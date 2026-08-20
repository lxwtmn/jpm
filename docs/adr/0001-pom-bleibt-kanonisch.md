# pom.xml bleibt kanonisch — jpm ist Editor, nicht Besitzer

jpm könnte ein eigenes Manifest (`jpm.json`) besitzen und `pom.xml` daraus generieren; das
wäre die direkte Übertragung des npm-Modells. Wir haben uns dagegen entschieden: die
`pom.xml` bleibt die einzige Wahrheit, jpm editiert sie formaterhaltend in-place. Ein
Generator müsste die komplette Maven-Feature-Fläche nachbauen — Profiles,
`dependencyManagement`, BOM-Import, Parent-Poms, Plugin-Konfiguration — und jedes nicht
abgebildete Feature machte jpm zum Blocker für ein bestehendes Projekt. Ein Editor kann im
schlimmsten Fall einen Edit nicht ausführen; das Projekt bleibt dabei heil.

## Consequences

jpm braucht einen formaterhaltenden XML-Editor, der Kommentare, Einrückung,
CRLF-Zeilenenden und eine UTF-8-Signatur überlebt. Das ist der riskanteste Teil des
Systems und der Grund für die Golden-File-Teststrategie.

Im Gegenzug funktioniert jpm am ersten Tag auf jedem bestehenden Projekt — ohne Migration,
und ohne dass IDE, CI oder Maven selbst etwas davon bemerken.
