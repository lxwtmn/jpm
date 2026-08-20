# Decisions are made against the effective model, not the file text

Before every write, jpm resolves the effective Maven model of the target Maven module: parent
POM, `dependencyManagement` and Maven BOMs pulled in via `scope=import`. If the coordinate is
already managed there, jpm inserts the dependency **without a `<version>`** and reports where
the version comes from. The naive path — always writing a version — silently overrides the
curated version set of a BOM-managed project, and BOM-managed is the norm in the modern Java
ecosystem, not the exception.

## Consequences

`maven-model-builder` therefore belongs to the core, not the periphery: jpm cannot treat a POM
as a mere text file, even though it writes one.

`jpm update` inherits the same rule. A managed dependency is never bumped individually —
instead jpm reports the managing Maven BOM as the thing that would have to move. Without this
shared foundation `add` and `update` would contradict each other: one respecting the BOM, the
other overwriting it.

This is also the point where jpm is demonstrably better than copy-pasting from
mvnrepository.com — that site always hands you a version, even when it is the wrong one.
