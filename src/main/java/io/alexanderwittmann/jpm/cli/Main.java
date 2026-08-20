package io.alexanderwittmann.jpm.cli;

/**
 * Entry point of the fat JAR. Deliberately thin: its only job is to hand the exit code from
 * {@link JpmCli} back to the shell. Everything testable sits one level below, where it stays
 * observable without {@code System.exit}.
 */
public final class Main {

  public static void main(String[] args) {
    System.exit(new JpmCli().run(args, System.out, System.err));
  }

  private Main() {}
}
