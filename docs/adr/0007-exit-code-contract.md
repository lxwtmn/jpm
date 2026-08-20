# Exit codes: informational commands never fail over their own content

The contract is: `0` success, `1` failure, `2` aborted for want of input (no TTY, but a
question would have been necessary). In particular `jpm outdated` always returns **0** when it
worked technically — even when it found updates. Anyone who wants CI to fail on that uses
`--fail-on-outdated`.

`npm outdated` does the opposite, and the result is well known: script authors append
`|| true` and switch off genuine error detection along with it. "There are updates" is
information, not a failure.

## Consequences

The dedicated code `2` lets scripts distinguish "I would have had to ask" from "it broke" —
necessary because several commands deliberately abort on ambiguity when no TTY is available
rather than guessing.

The contract covers both failure paths. Unparseable input is handled by a parameter exception
handler, and errors raised while a command executes by an execution exception handler —
without the latter an exception would escape as a Java stack trace and the exit code would come
from the JVM default instead of from this contract.
