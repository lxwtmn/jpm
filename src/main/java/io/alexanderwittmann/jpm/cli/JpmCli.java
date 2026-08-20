package io.alexanderwittmann.jpm.cli;

import java.io.PrintStream;
import java.io.PrintWriter;
import picocli.CommandLine;

/**
 * Die Nahtstelle, die das Launcher-Skript aufruft: Argumente und Ausgabeströme hinein,
 * Exit-Code heraus. Die Ströme werden hereingereicht statt {@code System.out} zu benutzen,
 * damit das Verhalten beobachtbar ist, ohne globalen Zustand anzufassen.
 */
public final class JpmCli {

  public int run(String[] args, PrintStream out, PrintStream err) {
    return new CommandLine(new JpmCommand())
        .setOut(new PrintWriter(out, true))
        .setErr(new PrintWriter(err, true))
        .setParameterExceptionHandler(new ParameterErrorHandler())
        .setExecutionExceptionHandler(new ExecutionErrorHandler())
        .execute(args);
  }
}
