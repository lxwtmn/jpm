package io.alexanderwittmann.jpm.metadata;

import io.alexanderwittmann.jpm.domain.Coordinate;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import java.util.stream.Collectors;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;

/**
 * The live version source: the embedded Maven Resolver, configured from the user's
 * {@code settings.xml} so that mirrors, proxies and authentication against a private artifact
 * repository behave exactly as they do for Maven itself (ADR-0004).
 *
 * <p>Its local repository deliberately points at jpm's own cache rather than {@code ~/.m2}: P5
 * says jpm reads Maven's state but never writes into it, and a resolver handed {@code ~/.m2} would
 * start populating it on the first lookup.
 *
 * <p>Caching is the resolver's own update-policy mechanism rather than a second layer on top —
 * {@code --refresh} maps to "always", the default to a one-hour interval, and {@code --offline} to
 * an offline session (DESIGN D-4).
 */
public final class ResolverVersionSource implements VersionSource {

  private static final String CENTRAL_URL = "https://repo.maven.apache.org/maven2/";

  private final RepositorySystem system;
  private final DefaultRepositorySystemSession session;
  private final List<RemoteRepository> repositories;

  private ResolverVersionSource(
      RepositorySystem system,
      DefaultRepositorySystemSession session,
      List<RemoteRepository> repositories) {
    this.system = system;
    this.session = session;
    this.repositories = repositories;
  }

  public static ResolverVersionSource create(Path cacheDirectory, boolean offline, boolean refresh) {
    var system = new RepositorySystemSupplier().get();
    var session = MavenRepositorySystemUtils.newSession();

    session.setLocalRepositoryManager(
        system.newLocalRepositoryManager(session, new LocalRepository(cacheDirectory.toFile())));
    session.setOffline(offline);
    session.setUpdatePolicy(
        refresh
            ? RepositoryPolicy.UPDATE_POLICY_ALWAYS
            : RepositoryPolicy.UPDATE_POLICY_INTERVAL + ":60");

    var settings = readSettings();
    applyMirrors(session, settings);
    applyProxies(session, settings);
    applyAuthentication(session, settings);

    var repositories =
        system.newResolutionRepositories(session, withCentral(repositoriesFrom(settings)));

    return new ResolverVersionSource(system, session, repositories);
  }

  /**
   * The artifact repositories a {@code settings.xml} contributes. A company's internal Nexus is
   * almost always declared as a {@code <repository>} inside a profile rather than as a mirror, so
   * reading only mirrors would leave jpm blind in exactly the setting DESIGN section 1 names as
   * driving (D-1).
   */
  static List<RemoteRepository> repositoriesFrom(Settings settings) {
    var active = new HashSet<>(settings.getActiveProfiles());
    var repositories = new ArrayList<RemoteRepository>();

    for (var profile : settings.getProfiles()) {
      boolean isActive =
          active.contains(profile.getId())
              || (profile.getActivation() != null && profile.getActivation().isActiveByDefault());
      if (!isActive) {
        continue;
      }
      for (var repository : profile.getRepositories()) {
        repositories.add(
            new RemoteRepository.Builder(repository.getId(), "default", repository.getUrl())
                .build());
      }
    }
    return repositories;
  }

  /** Appends Maven Central, kept last so a configured internal repository answers first. */
  static List<RemoteRepository> withCentral(List<RemoteRepository> repositories) {
    var all = new ArrayList<>(repositories);
    all.add(
        new RemoteRepository.Builder("central", "default", CENTRAL_URL)
            .setSnapshotPolicy(new RepositoryPolicy(false, null, null))
            .build());
    return all;
  }

  @Override
  public List<String> versionsOf(Coordinate coordinate) {
    // An unbounded range asks the repositories for everything they list for the coordinate.
    var artifact =
        new DefaultArtifact(coordinate.groupId(), coordinate.artifactId(), "pom", "[0,)");
    var request = new VersionRangeRequest(artifact, repositories, null);
    try {
      var result = system.resolveVersionRange(session, request);
      var versions =
          result.getVersions().stream().map(Object::toString).collect(Collectors.toList());

      // Aether reports an unreachable repository by returning an empty list with the causes
      // attached, not by throwing. Left unchecked, every network, proxy or authentication
      // failure would reach the user as "unknown coordinate" — a typo they did not make.
      if (versions.isEmpty() && !result.getExceptions().isEmpty()) {
        throw new MetadataUnavailableException(
            "could not read versions for " + coordinate + ": " + firstMessage(result.getExceptions()),
            result.getExceptions().get(0));
      }
      return versions;
    } catch (VersionRangeResolutionException e) {
      throw new MetadataUnavailableException(
          "could not read versions for " + coordinate + ": " + e.getMessage(), e);
    }
  }

  @Override
  public boolean exists(Coordinate coordinate, String version) {
    var artifact =
        new DefaultArtifact(coordinate.groupId(), coordinate.artifactId(), "pom", version);
    try {
      system.resolveArtifact(session, new ArtifactRequest(artifact, repositories, null));
      return true;
    } catch (ArtifactResolutionException e) {
      // DESIGN D-2 suggests an HTTP HEAD. Resolving the .pom instead costs a few kilobytes but
      // reuses the mirror, proxy and authentication setup, which a raw HEAD would have to redo.
      //
      // "Not there" and "could not look" are different answers. Treating the second as the first
      // would make jpm walk quietly down to an older version whenever the network hiccups.
      boolean genuinelyAbsent =
          e.getResult().getExceptions().stream()
              .allMatch(cause -> cause instanceof ArtifactNotFoundException);
      if (genuinelyAbsent) {
        return false;
      }
      throw new MetadataUnavailableException(
          "could not check whether "
              + coordinate
              + ":"
              + version
              + " exists: "
              + firstMessage(e.getResult().getExceptions()),
          e);
    }
  }

  private static String firstMessage(List<? extends Exception> causes) {
    return causes.isEmpty() ? "no detail reported" : String.valueOf(causes.get(0).getMessage());
  }

  /** The Maven installation's own {@code conf/settings.xml}, when a Maven home is discoverable. */
  private static Optional<File> globalSettingsFile() {
    for (var variable : List.of("MAVEN_HOME", "M2_HOME")) {
      var home = System.getenv(variable);
      if (home != null && !home.isBlank()) {
        var file = new File(new File(home, "conf"), "settings.xml");
        if (file.isFile()) {
          return Optional.of(file);
        }
      }
    }
    return Optional.empty();
  }

  // ------------------------------------------------------------ settings.xml

  private static Settings readSettings() {
    var request = new DefaultSettingsBuildingRequest();
    request.setUserSettingsFile(
        new File(new File(System.getProperty("user.home"), ".m2"), "settings.xml"));
    // Corporate installations put mirrors and repositories in the global file, so reading only
    // the user's own would miss them on exactly the machines that need them most.
    globalSettingsFile().ifPresent(request::setGlobalSettingsFile);
    request.setSystemProperties(System.getProperties());
    try {
      return new DefaultSettingsBuilderFactory().newInstance().build(request).getEffectiveSettings();
    } catch (SettingsBuildingException e) {
      throw new MetadataUnavailableException("could not read settings.xml: " + e.getMessage(), e);
    }
  }

  private static void applyMirrors(DefaultRepositorySystemSession session, Settings settings) {
    var selector = new DefaultMirrorSelector();
    for (var mirror : settings.getMirrors()) {
      selector.add(
          mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirror.isBlocked(),
          mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
    }
    session.setMirrorSelector(selector);
  }

  private static void applyProxies(DefaultRepositorySystemSession session, Settings settings) {
    var selector = new DefaultProxySelector();
    for (var proxy : settings.getProxies()) {
      if (!proxy.isActive()) {
        continue;
      }
      Authentication authentication =
          proxy.getUsername() == null
              ? null
              : new AuthenticationBuilder()
                  .addUsername(proxy.getUsername())
                  .addPassword(proxy.getPassword())
                  .build();
      selector.add(
          new Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authentication),
          proxy.getNonProxyHosts());
    }
    session.setProxySelector(selector);
  }

  private static void applyAuthentication(DefaultRepositorySystemSession session, Settings settings) {
    var selector = new org.eclipse.aether.util.repository.DefaultAuthenticationSelector();
    for (var server : settings.getServers()) {
      var builder = new AuthenticationBuilder();
      if (server.getUsername() != null) {
        builder.addUsername(server.getUsername()).addPassword(server.getPassword());
      }
      if (server.getPrivateKey() != null) {
        builder.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
      }
      selector.add(server.getId(), builder.build());
    }
    session.setAuthenticationSelector(selector);
  }

}
