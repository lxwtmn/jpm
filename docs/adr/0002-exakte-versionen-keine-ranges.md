# Exakte Versionen in der pom — keine Ranges, kein `^` und `~`

jpm schreibt ausschließlich exakte Versionen in die `pom.xml`. Maven-Ranges wie
`[1.0,2.0)` werden nie erzeugt: sie machen Builds nicht reproduzierbar und brechen offline.
Folglich gibt es auch die npm-Selektoren `^` und `~` nicht — sie versprechen eine
persistente Range, die hier nicht existiert, denn nach `jpm add g:a` steht eine feste
Version in der Datei. Ausdrücke wie `@2.15` sind reine Auswahlanweisungen zum
Befehlszeitpunkt und hinterlassen keine Spur.

## Consequences

Die Frage „bis wohin darf die Version springen" gehört damit an `jpm update`
(`--patch` / `--minor` / `--major`), nicht an den Eintrag in der pom.

Die Einordnung als Patch, Minor oder Major ist im Java-Ökosystem eine Heuristik über den
Versions-String, weil sich viele Artefakte nicht an SemVer halten. Die Ausgabe muss das
ehrlich benennen statt Präzision vorzutäuschen.
