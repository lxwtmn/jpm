package io.alexanderwittmann.jpm.metadata;

/**
 * Builds a version source for one command run. It exists so the command line can be exercised
 * without a network: the live factory drives the Maven Resolver, a test hands in a stand-in.
 */
@FunctionalInterface
public interface VersionSourceFactory {

  VersionSource create(boolean offline, boolean refresh);

  /** The live factory: the embedded resolver, caching into jpm's own directory. */
  static VersionSourceFactory live() {
    return (offline, refresh) ->
        ResolverVersionSource.create(CacheDirectory.resolve(), offline, refresh);
  }
}
