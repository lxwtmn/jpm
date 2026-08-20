# jpm

jpm verwaltet Maven-Dependencies über die Kommandozeile, indem es bestehende
`pom.xml`-Dateien editiert. Dieses Glossar hält die Begriffe fest, mit denen wir über die
Domäne sprechen — besonders dort, wo Mavens Vokabular, npms Vokabular und jpms eigene
Begriffe aufeinandertreffen.

## Language

### Mavens Welt

**Koordinate**:
Die Identität eines Artefakts ohne Version, geschrieben `groupId:artifactId`.
_Avoid_: GAV (schließt die Version mit ein), Paketname, Artefaktname

**Artefakt**:
Ein veröffentlichtes, versioniertes Paket in einem Artefakt-Repository.
_Avoid_: Library, Paket, Abhängigkeit

**Dependency**:
Ein Eintrag in einer pom, der ein Artefakt für ein Maven-Modul verfügbar macht. Die
Dependency ist der Eintrag, nicht das Artefakt selbst.
_Avoid_: Abhängigkeit, Package

**Maven-Modul**:
Ein Unterprojekt mit eigener pom innerhalb eines Reaktors.
_Avoid_: Modul (unqualifiziert), Subprojekt, Projekt

**Reaktor**:
Der Verbund aller Maven-Module, die unter einem Aggregator-Pom gemeinsam gebaut werden.
_Avoid_: Workspace, Monorepo, Projektbaum

**Aggregator-Pom**:
Die pom, die andere Maven-Module per `<modules>` auflistet. Nicht zwangsläufig dieselbe
Datei wie der Parent-Pom.
_Avoid_: Root-Pom, Master-Pom, Parent

**Parent-Pom**:
Die pom, von der eine andere per `<parent>` erbt.
_Avoid_: Elternprojekt, Basis-Pom

**Maven-BOM**:
Ein Artefakt vom Typ `pom`, das per `scope=import` Versionen für andere Artefakte vorgibt.
_Avoid_: BOM (unqualifiziert — siehe Kollidierende Begriffe), Stückliste, Versionssatz

**Verwaltete Dependency**:
Eine Dependency, deren Version aus `<dependencyManagement>` stammt und die deshalb ohne
`<version>` in der pom steht.
_Avoid_: gepinnte Dependency, BOM-Dependency, managed dependency

**Scope**:
Der Klassenpfad-Geltungsbereich einer Dependency: `compile`, `test`, `provided`, `runtime`.
_Avoid_: Sichtbarkeit, Konfiguration (Gradles Wort für dasselbe Konzept)

**Artefakt-Repository**:
Ein Server, der Artefakte ausliefert — Maven Central, Nexus, Artifactory.
_Avoid_: Repository (unqualifiziert — siehe Kollidierende Begriffe), Registry (npms Wort)

### jpms Welt

**Effektives Modell**:
Das aufgelöste Maven-Modell eines Maven-Moduls inklusive Parent, importierter Maven-BOMs
und Properties. Die Wahrheit, gegen die jpm seine Entscheidungen trifft.
_Avoid_: Effective POM (Mavens Name für die Ausgabe von `help:effective-pom`), aufgelöste pom

**Selektor**:
Der optionale Teil hinter `@` in einem Befehl, etwa `@2.15`. Bestimmt, welche Version zum
Befehlszeitpunkt gewählt wird, und steht nie in der pom.
_Avoid_: Range, Constraint, Versionsangabe

**Neueste stabile Version**:
Die höchste Version einer Koordinate, deren Qualifier keine Vorabversion bezeichnet.
_Avoid_: latest, release (beides Felder in `maven-metadata.xml` mit abweichender Bedeutung)

**Ablageform**:
Ob jpm eine Version inline in `<version>` schreibt oder als Eintrag in `<properties>`.
_Avoid_: Versionsstil, Strategie

**jpm-Modul**:
Ein Codemodul von jpm selbst — `cli`, `pom`, `model`, `metadata`, `search`, `commands`.
_Avoid_: Modul (unqualifiziert), Komponente, Package

### Kollidierende Begriffe

Drei Wörter haben in dieser Domäne zwei Bedeutungen. Sie werden nie unqualifiziert
verwendet — weder in Prosa noch in Bezeichnern.

**BOM**: entweder **Maven-BOM** oder **Byte Order Mark** (die UTF-8-Signatur am
Dateianfang, die der pom-Editor erhalten muss).

**Modul**: entweder **Maven-Modul** (im Reaktor des Nutzers) oder **jpm-Modul** (in jpms
eigenem Code).

**Repository**: entweder **Artefakt-Repository** (Central, Nexus, Artifactory) oder
**Git-Repository**.
