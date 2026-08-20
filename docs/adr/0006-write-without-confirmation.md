# Writing commands do not ask, and do not leave a backup

`jpm add` and `jpm remove` write immediately and report afterwards; there is no confirmation
prompt and no `.bak` file. `--dry-run` provides the preview for anyone who wants one. A prompt
on every invocation would make jpm slower than the copy-paste it is meant to replace, and it
buys nothing: the write is atomic and re-parsed before the final move, and every Java project
lives in Git, where `git diff` is a better backup than a file somebody later commits by
accident. Confirmation prompts are only worth their cost where there is no undo.

## Consequences

The message *after* the write carries the whole burden and must be correspondingly good: what
was written, into which file, at which scope, at which version — and where that version came
from.

Strictly distinct from this are questions asked on genuine **ambiguity** (which Maven module in
a reactor, which scope, a requested version that disagrees with a Maven BOM). Those resolve an
open question; they do not confirm a decision already made. Those prompts stay.

For the same reason jpm does not check whether the working tree is dirty in Git — that would
be paternalistic and would interfere with legitimate use in scripts.
