# pom.xml stays canonical — jpm is an editor, not an owner

jpm could own a manifest of its own (`jpm.json`) and generate `pom.xml` from it; that would be
the direct translation of npm's model. We decided against it: `pom.xml` remains the single
source of truth, and jpm edits it in place while preserving its formatting. A generator would
have to reimplement Maven's entire feature surface — profiles, `dependencyManagement`, BOM
imports, parent POMs, plugin configuration — and every feature it failed to cover would turn
jpm into a blocker for an existing project. An editor, by contrast, can at worst decline to
make an edit, leaving the project intact.

## Consequences

jpm needs a format-preserving XML editor that survives comments, indentation, CRLF line
endings and a UTF-8 byte order mark. That is the riskiest part of the system and the reason
for the golden-file testing strategy.

In exchange, jpm works on any existing project from day one — no migration, and neither the
IDE nor CI nor Maven itself notices anything.
