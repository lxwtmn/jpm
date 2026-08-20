package io.alexanderwittmann.jpm.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.maven.settings.Activation;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Which artifact repositories a {@code settings.xml} contributes.
 *
 * <p>This is the half of DESIGN D-1 that decides whether jpm can see a company's internal Nexus at
 * all: such a repository is almost always declared as a {@code <repository>} inside a profile, not
 * as a mirror. Wiring only Maven Central would make jpm useless in exactly the setting it was
 * built for.
 */
class SettingsRepositoriesTest {

  private static Profile profile(String id, boolean activeByDefault, String repositoryUrl) {
    var repository = new Repository();
    repository.setId(id + "-repo");
    repository.setUrl(repositoryUrl);

    var profile = new Profile();
    profile.setId(id);
    profile.addRepository(repository);
    if (activeByDefault) {
      var activation = new Activation();
      activation.setActiveByDefault(true);
      profile.setActivation(activation);
    }
    return profile;
  }

  @Test
  @DisplayName("Takes repositories from profiles that are active by default")
  void takesRepositoriesFromDefaultActiveProfiles() {
    var settings = new Settings();
    settings.addProfile(profile("company", true, "https://nexus.example.com/repository/maven/"));

    var repositories = ResolverVersionSource.repositoriesFrom(settings);

    assertThat(repositories).extracting("id").contains("company-repo");
    assertThat(repositories)
        .extracting("url")
        .contains("https://nexus.example.com/repository/maven/");
  }

  @Test
  @DisplayName("Takes repositories from profiles listed under activeProfiles")
  void takesRepositoriesFromExplicitlyActivatedProfiles() {
    var settings = new Settings();
    settings.addProfile(profile("company", false, "https://nexus.example.com/repository/maven/"));
    settings.setActiveProfiles(List.of("company"));

    assertThat(ResolverVersionSource.repositoriesFrom(settings))
        .extracting("id")
        .contains("company-repo");
  }

  @Test
  @DisplayName("Ignores repositories from profiles that are not active")
  void ignoresInactiveProfiles() {
    var settings = new Settings();
    settings.addProfile(profile("unused", false, "https://nowhere.example.com/"));

    assertThat(ResolverVersionSource.repositoriesFrom(settings)).isEmpty();
  }

  @Test
  @DisplayName("Central is tried last, so an internal repository answers first")
  void centralComesLast() {
    var settings = new Settings();
    settings.addProfile(profile("company", true, "https://nexus.example.com/repository/maven/"));

    var all = ResolverVersionSource.withCentral(ResolverVersionSource.repositoriesFrom(settings));

    assertThat(all).extracting("id").containsExactly("company-repo", "central");
  }
}
