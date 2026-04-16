package com.kodewerk.gcsee.cli.command;

/**
 * A command that the CLI knows how to execute.
 * Each implementation is a first-class object that owns its execution
 * logic and its contribution to the help output.
 */
public interface Command {

    /**
     * Execute the command.
     *
     * @param args the tokens following the command name, may be empty
     */
    void execute(String[] args);

    /**
     * A single line describing this command, printed by {@code h}.
     */
    String helpLine();
}
