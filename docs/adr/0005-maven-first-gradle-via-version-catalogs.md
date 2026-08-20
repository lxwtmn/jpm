# Maven first; Gradle support starts at version catalogs

v1 supports Maven only. Gradle follows in phase 2 and starts at `gradle/libs.versions.toml`,
**not** at the build script. The reason: `pom.xml` is a data format and can be edited
deterministically; `build.gradle(.kts)` is a program, and "add a dependency" would mean
patching somebody else's source code. Version catalogs bring Gradle back into the data-format
world, where a machine edit can be correct at all.

## Consequences

For Gradle projects without a version catalog, DSL patching remains best-effort for the simple
case (`dependencies { implementation("g:a:v") }`). Loops, variables, `subprojects {}` and
convention plugins are explicitly out of scope — there jpm must decline cleanly rather than
guess.
