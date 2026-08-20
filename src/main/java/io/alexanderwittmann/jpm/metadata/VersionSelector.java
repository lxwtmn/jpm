package io.alexanderwittmann.jpm.metadata;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Chooses one version out of what a repository offers, following DESIGN D-2.
 *
 * <p>The stability rule is a deny list of qualifier tokens rather than an allow list of shapes,
 * because most qualifiers are perfectly stable: {@code 33.2.1-jre}, {@code 9.4.53.v20231009} and
 * {@code 1.0.0.Final} are releases, while {@code 6.0.0-M1} looks like an ordinary release and is
 * not. Guessing from shape alone gets both halves wrong.
 */
public final class VersionSelector {

  /**
   * Qualifier tokens that mark a pre-release, anchored to a segment boundary so that {@code -jre}
   * and {@code -build5} are left alone.
   *
   * <p>{@code +} counts as a boundary because SemVer build metadata follows one: without it,
   * {@code 28-ea+4} — a real early-access build of {@code org.openjfx:javafx-base} — reads as a
   * stable release.
   *
   * <p>{@code pr}, {@code dev}, {@code eap} and {@code nightly} are listed because Maven's own
   * ordering does not know them, and an unknown qualifier sorts <em>above</em> the plain release.
   * Missing one therefore does not merely admit a bad version, it makes that version win:
   * {@code jackson-databind:2.10.0.pr3} would beat {@code 2.10.0}.
   *
   * <p>There is deliberately no {@code b\d+} rule. It would catch {@code 1.6.0-b28}-style betas,
   * but it also rejects the entire GlassFish and Metro family — {@code javax.el:3.0.1-b12} and
   * {@code jaxb-runtime:2.3.0-b170201.1204} are final releases. Silently installing an older
   * version of a widely used artifact is the greater harm.
   */
  private static final Pattern PRE_RELEASE =
      Pattern.compile(
          "(?i)(^|[-._+])"
              + "((alpha|beta|milestone|snapshot|preview|nightly|eap|rc|cr|ea|dev|pre|pr)\\d*"
              + "|m\\d+)"
              + "([-._+]|$)");

  private VersionSelector() {}

  public static Optional<String> select(
      List<String> available, Selector selector, boolean allowPreReleases) {
    return candidates(available, selector, allowPreReleases).stream().findFirst();
  }

  /**
   * Every version that satisfies the selector and the stability rule, newest first. A list rather
   * than a single answer because the newest entry may turn out not to be fetchable, and the caller
   * then has to walk down.
   */
  public static List<String> candidates(
      List<String> available, Selector selector, boolean allowPreReleases) {
    return available.stream()
        .filter(version -> matches(version, selector))
        .filter(version -> allowPreReleases || !isPreRelease(version) || namedExactly(version, selector))
        .sorted(Comparator.comparing(ComparableVersion::new).reversed())
        .collect(Collectors.toList());
  }

  /**
   * Whether the user typed this version out in full. Naming a version exactly is intent rather
   * than a search, so the stability rule does not second-guess it — {@code @6.0.0-M1} resolves
   * without {@code --pre}, while {@code @6.0} still skips the milestones in that line.
   *
   * <p>This is why the command line needs no mode switch between "exact" and "prefix": one rule
   * covers both, and {@code @2.15} means "the newest thing in the 2.15 line, which may be 2.15
   * itself".
   */
  private static boolean namedExactly(String version, Selector selector) {
    if (selector instanceof Selector.Exact exact) {
      return version.equals(exact.version());
    }
    return selector instanceof Selector.Prefix prefix && version.equals(prefix.prefix());
  }

  public static boolean isPreRelease(String version) {
    return PRE_RELEASE.matcher(version).find();
  }

  // Written with instanceof rather than a pattern switch: pattern matching in switch is Java 21,
  // and DESIGN E-1 pins the bytecode target at 17.
  private static boolean matches(String version, Selector selector) {
    if (selector instanceof Selector.Exact exact) {
      return version.equals(exact.version());
    }
    if (selector instanceof Selector.Prefix prefix) {
      // Segment-wise, so that 2.1 selects from the 2.1 line and does not reach 2.10.
      return version.equals(prefix.prefix())
          || version.startsWith(prefix.prefix() + ".")
          || version.startsWith(prefix.prefix() + "-");
    }
    return true;
  }
}
