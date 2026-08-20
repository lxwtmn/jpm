package io.alexanderwittmann.jpm.cli;

/**
 * Einstiegspunkt des Fat-JAR. Bewusst dünn: die einzige Aufgabe ist, den Exit-Code von
 * {@link JpmCli} an die Shell weiterzureichen. Alles Testbare liegt eine Ebene tiefer, damit
 * es ohne {@code System.exit} beobachtbar bleibt.
 */
public final class Main {

  public static void main(String[] args) {
    System.exit(new JpmCli().run(args, System.out, System.err));
  }

  private Main() {}
}
