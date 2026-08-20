# Exact versions in the POM — no ranges, no `^` or `~`

jpm writes exact versions into `pom.xml` and nothing else. Maven ranges such as `[1.0,2.0)`
are never produced: they make builds irreproducible and break offline. Consequently the npm
selectors `^` and `~` do not exist either — they promise a persistent range that does not
exist here, because after `jpm add g:a` a fixed version sits in the file. Expressions like
`@2.15` are selection instructions for the moment the command runs and leave no trace.

## Consequences

The question "how far may the version jump" therefore belongs to `jpm update`
(`--patch` / `--minor` / `--major`), not to the entry in the POM.

Classifying a jump as patch, minor or major is a heuristic over the version string in the Java
ecosystem, because many artifacts do not follow SemVer. The output must say so plainly rather
than feign precision.
