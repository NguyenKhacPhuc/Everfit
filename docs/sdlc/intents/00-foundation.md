# Intent 00: Foundation

**Outcome:** a runnable app with a green, single-command feedback loop.

The repository arrived as an empty scaffold — no Activity, no source files, and
a build that did not compile. Nothing can be verified until this is true.

| Rung | Outcome | Verified by |
|---|---|---|
| 0.1 | `./gradlew build` exits 0 on a clean checkout | ✅ Done — PR #1 |
| 0.2 | App launches on a device and shows a placeholder screen | Launch on device; screenshot |
| 0.3 | Test harness runs: JVM test deps wired, `MainDispatcherRule` present, fixture on the test classpath | A real (non-example) JVM test executes |
| 0.4 | A single command runs build + unit tests + lint and exits non-zero on any failure | Deliberately break a test; confirm non-zero exit |
| 0.5 | `CLAUDE.md` records build/test/lint commands, conventions and healthy output | ✅ Done |

## Notes

**Rung 0.3 is not ceremony.** The first build of this project *appeared* to
pass because its output was piped to `tail`, so the shell returned tail's exit
status and masked a real failure. Any check written that way reports green over
a broken build indefinitely. The command must be verified to actually fail.

**Rung 0.2 gates every visual rung.** Without a launchable app there is no
screenshot loop, and "pixel-perfect" cannot be checked.

**Rung 0.3 is not implied by 0.4.** A gate command can exit zero while running
no tests at all. Standing the harness up — test dependencies, a
`MainDispatcherRule` so `Dispatchers.Main` resolves on the JVM, and the fixture
on the test classpath — is its own rung, and rung 0.4 then proves it works.
