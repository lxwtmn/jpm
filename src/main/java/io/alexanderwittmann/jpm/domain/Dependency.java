package io.alexanderwittmann.jpm.domain;

/** An entry to be written into a POM: a coordinate together with the version it is pinned to. */
public record Dependency(Coordinate coordinate, String version) {}
