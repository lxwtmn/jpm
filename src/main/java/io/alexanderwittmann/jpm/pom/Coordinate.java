package io.alexanderwittmann.jpm.pom;

/** The identity of an artifact without a version, written {@code groupId:artifactId}. */
public record Coordinate(String groupId, String artifactId) {

  @Override
  public String toString() {
    return groupId + ":" + artifactId;
  }
}
