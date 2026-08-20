# Maven zuerst; Gradle-Unterstützung beginnt bei Version Catalogs

v1 unterstützt ausschließlich Maven. Gradle folgt in Phase 2 und beginnt bei
`gradle/libs.versions.toml`, **nicht** beim Build-Skript. Grund: `pom.xml` ist ein
Datenformat und deterministisch editierbar; `build.gradle(.kts)` ist ein Programm, und
„Dependency hinzufügen" hieße dort, fremden Quellcode zu patchen. Version Catalogs holen
Gradle zurück in die Datenformat-Welt, in der ein maschineller Edit überhaupt korrekt sein
*kann*.

## Consequences

Für Gradle-Projekte ohne Version Catalog bleibt DSL-Patching danach Best-Effort für den
einfachen Fall (`dependencies { implementation("g:a:v") }`). Schleifen, Variablen,
`subprojects {}` und Konventions-Plugins sind ausdrücklich nicht abgedeckt — dort muss jpm
sauber ablehnen statt zu raten.
