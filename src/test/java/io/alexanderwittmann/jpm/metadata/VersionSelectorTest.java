package io.alexanderwittmann.jpm.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The stability rule from DESIGN D-2, exercised without touching the network.
 *
 * <p>The version lists are real ones taken from Maven Central, because the traps here are real:
 * {@code 33.2.1-jre} and {@code 9.4.53.v20231009} carry qualifiers and are perfectly stable,
 * while {@code 6.0.0-M1} looks like an ordinary release and is not.
 */
class VersionSelectorTest {

  @Test
  @DisplayName("Picks the highest stable version and skips pre-releases")
  void picksHighestStableVersion() {
    var available =
        List.of("2.15.0", "2.16.0", "2.17.1", "2.18.0-SNAPSHOT", "3.0.0-RC1", "6.0.0-M1");

    var chosen = VersionSelector.select(available, new Selector.Latest(), false);

    assertThat(chosen).contains("2.17.1");
  }

  @Test
  @DisplayName("Treats real release qualifiers as stable, not as pre-releases")
  void releaseQualifiersAreStable() {
    // Every one of these is a genuine release on Central. A shape-based guess would reject them.
    assertThat(VersionSelector.isPreRelease("33.2.1-jre")).isFalse();
    assertThat(VersionSelector.isPreRelease("9.4.53.v20231009")).isFalse();
    assertThat(VersionSelector.isPreRelease("1.0.0.Final")).isFalse();
    assertThat(VersionSelector.isPreRelease("2.2-groovy-4.0")).isFalse();
    assertThat(VersionSelector.isPreRelease("1.2.3-build5")).isFalse();
    // org.glassfish:javax.el:3.0.1-b12 and jaxb-runtime:2.3.0-b170201.1204 are final releases.
    // A -bNN rule rejects the whole GlassFish and Metro family, which is the larger harm.
    assertThat(VersionSelector.isPreRelease("3.0.1-b12")).isFalse();
    assertThat(VersionSelector.isPreRelease("2.3.0-b170201.1204")).isFalse();
  }

  @Test
  @DisplayName("Recognises pre-releases that carry SemVer build metadata")
  void buildMetadataDoesNotHidePreReleases() {
    // org.openjfx:javafx-base publishes 28-ea+4. The '+' is a segment boundary too; without that
    // an early-access build is written into a production POM as "the latest stable version".
    assertThat(VersionSelector.isPreRelease("28-ea+4")).isTrue();
    assertThat(VersionSelector.isPreRelease("21.0.12")).isFalse();
  }

  @Test
  @DisplayName("Recognises the qualifiers Maven itself does not know")
  void unknownToMavenButStillPreRelease() {
    // com.fasterxml.jackson.core:jackson-databind:2.10.0.pr3 is a pre-release, and Maven's own
    // ordering places unknown qualifiers *above* the plain release — so missing one here does not
    // merely include a bad version, it makes that version win.
    assertThat(VersionSelector.isPreRelease("2.10.0.pr3")).isTrue();
    assertThat(VersionSelector.isPreRelease("1.0.0-dev")).isTrue();
    assertThat(VersionSelector.isPreRelease("2.0-eap7")).isTrue();
  }

  @Test
  @DisplayName("A pre-release never outranks the release it precedes")
  void preReleaseNeverOutranksItsRelease() {
    var available = List.of("2.9.10", "2.10.0", "2.10.0.pr1", "2.10.0.pr3");

    assertThat(VersionSelector.select(available, new Selector.Prefix("2.10.0"), false))
        .contains("2.10.0");
  }

  @Test
  @DisplayName("Treats the usual pre-release qualifiers as unstable")
  void preReleaseQualifiersAreUnstable() {
    assertThat(VersionSelector.isPreRelease("6.0.0-M1")).isTrue();
    assertThat(VersionSelector.isPreRelease("3.0.0-RC1")).isTrue();
    assertThat(VersionSelector.isPreRelease("1.0-alpha-3")).isTrue();
    assertThat(VersionSelector.isPreRelease("5.0.0-beta2")).isTrue();
    assertThat(VersionSelector.isPreRelease("2.18.0-SNAPSHOT")).isTrue();
    // Not asserted: 1.6.0-b28. A -bNN rule would catch that beta but reject the GlassFish and
    // Metro releases above, and installing an older version of those is the greater harm.
  }

  @Test
  @DisplayName("--pre lets the newest pre-release win")
  void preReleasesAllowedOnRequest() {
    var available = List.of("2.17.1", "6.0.0-M1");

    assertThat(VersionSelector.select(available, new Selector.Latest(), true)).contains("6.0.0-M1");
  }

  @Test
  @DisplayName("A prefix selector matches by segment, so 2.1 does not reach 2.10")
  void prefixMatchesBySegment() {
    var available = List.of("2.1", "2.1.9", "2.10.0", "2.15.3");

    assertThat(VersionSelector.select(available, new Selector.Prefix("2.1"), false))
        .as("2.10.0 is a different line and must not be selected by the 2.1 prefix")
        .contains("2.1.9");
  }

  @Test
  @DisplayName("An exact selector is honoured even when it names a pre-release")
  void exactSelectorHonoursIntent() {
    var available = List.of("2.17.1", "6.0.0-M1");

    // Naming a version in full is intent, not a search — the stability rule does not override it.
    assertThat(VersionSelector.select(available, new Selector.Exact("6.0.0-M1"), false))
        .contains("6.0.0-M1");
  }

  @Test
  @DisplayName("A prefix that names a pre-release in full resolves to it")
  void prefixNamingAPreReleaseExactlyIsHonoured() {
    var available = List.of("5.9.3", "6.0.0-M1", "6.0.0-M2");

    // Typing the version out is intent: @6.0.0-M1 works without --pre ...
    assertThat(VersionSelector.select(available, new Selector.Prefix("6.0.0-M1"), false))
        .contains("6.0.0-M1");
    // ... while a genuine prefix still skips the milestones in that line.
    assertThat(VersionSelector.select(available, new Selector.Prefix("6.0"), false)).isEmpty();
  }

  @Test
  @DisplayName("Returns nothing when only pre-releases exist and they were not asked for")
  void nothingWhenOnlyPreReleasesExist() {
    var available = List.of("1.0.0-M1", "1.0.0-RC1");

    assertThat(VersionSelector.select(available, new Selector.Latest(), false)).isEmpty();
  }
}
