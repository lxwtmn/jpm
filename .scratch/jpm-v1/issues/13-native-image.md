# 13 — native-image statt Fat-JAR

**What to build:** jpm startet ohne spürbare Verzögerung und ohne installierte JVM. Nutzer
installieren eine Binary statt JAR plus Launcher. Bewusst am Ende: der Maven Resolver lädt
Komponenten dynamisch, und Reflection-Konfiguration ist eine Aufgabe, die man einmal gegen
den fertigen Funktionsumfang löst statt bei jedem Feature neu.

**Blocked by:** 12

**Status:** ready-for-agent

- [ ] GraalVM-Konfiguration deckt Resolver und picocli ab; alle Befehle laufen nativ
- [ ] Der vollständige Testlauf läuft auch gegen die native Binary
- [ ] CI baut Binaries für Windows und Linux
- [ ] Die Startzeit von `jpm --version` liegt deutlich unter der JVM-Variante
- [ ] Scoop-Manifest für die Installation unter Windows
- [ ] Das Fat-JAR bleibt als Rückfallweg im Build erhalten
