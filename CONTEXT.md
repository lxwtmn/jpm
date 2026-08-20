# jpm

jpm manages Maven dependencies from the command line by editing existing `pom.xml` files.
This glossary fixes the words we use for the domain — especially where Maven's vocabulary,
npm's vocabulary and jpm's own terms collide.

## Language

### Maven's world

**Coordinate**:
The identity of an artifact without a version, written `groupId:artifactId`.
_Avoid_: GAV (that includes the version), package name, artifact name

**Artifact**:
A published, versioned package living in an artifact repository.
_Avoid_: library, package, dependency

**Dependency**:
An entry in a POM that makes an artifact available to a Maven module. The dependency is the
entry, not the artifact itself.
_Avoid_: requirement, package

**Maven module**:
A subproject with its own POM inside a reactor.
_Avoid_: module (unqualified), subproject, project

**Reactor**:
The set of Maven modules built together under an aggregator POM.
_Avoid_: workspace, monorepo, project tree

**Aggregator POM**:
The POM that lists other Maven modules under `<modules>`. Not necessarily the same file as
the parent POM.
_Avoid_: root POM, master POM, parent

**Parent POM**:
The POM another one inherits from via `<parent>`.
_Avoid_: base POM, super POM

**Maven BOM**:
An artifact of type `pom` that dictates versions for other artifacts via `scope=import`.
_Avoid_: BOM (unqualified — see Colliding terms), bill of materials, version set

**Managed dependency**:
A dependency whose version comes from `<dependencyManagement>` and which therefore appears
in the POM without a `<version>`.
_Avoid_: pinned dependency, BOM dependency

**Scope**:
The classpath reach of a dependency: `compile`, `test`, `provided`, `runtime`.
_Avoid_: visibility, configuration (Gradle's word for the same idea)

**Artifact repository**:
A server that serves artifacts — Maven Central, Nexus, Artifactory.
_Avoid_: repository (unqualified — see Colliding terms), registry (npm's word)

### jpm's world

**Effective model**:
The resolved Maven model of a Maven module including its parent POM, imported Maven BOMs and
properties. The truth jpm decides against.
_Avoid_: effective POM (Maven's name for the output of `help:effective-pom`), resolved POM

**Selector**:
The optional part after `@` in a command, such as `@2.15`. It decides which version is chosen
at the moment the command runs, and never appears in the POM.
_Avoid_: range, constraint, version spec

**Latest stable version**:
The highest version of a coordinate whose qualifier does not denote a pre-release.
_Avoid_: latest, release (both are fields in `maven-metadata.xml` with a different meaning)

**Version placement**:
Whether jpm writes a version inline in `<version>` or as an entry under `<properties>`.
_Avoid_: version style, strategy

**jpm module**:
A code module of jpm itself — `cli`, `pom`, `model`, `metadata`, `search`, `commands`.
_Avoid_: module (unqualified), component, package

### Colliding terms

Three words carry two meanings in this domain. They are never used unqualified — neither in
prose nor in identifiers.

**BOM**: either **Maven BOM** or **byte order mark** (the UTF-8 signature at the start of a
file, which the POM editor must preserve).

**Module**: either **Maven module** (in the user's reactor) or **jpm module** (in jpm's own
code).

**Repository**: either **artifact repository** or **Git repository**.
