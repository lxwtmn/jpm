package io.alexanderwittmann.jpm.cli;

import java.nio.file.Path;

/**
 * Entry point of the fat JAR. Deliberately thin: its only jobs are to read the process state the
 * CLI needs and to hand the exit code back to the shell. Everything testable sits one level
 * below, where it stays observable without {@code System.exit}.
 */
public final class Main {

  public static void main(String[] args) {
    var workingDirectory = Path.of(System.getProperty("user.dir"));
    System.exit(new JpmCli().run(workingDirectory, args, System.out, System.err));
  }

  private Main() {}
}
