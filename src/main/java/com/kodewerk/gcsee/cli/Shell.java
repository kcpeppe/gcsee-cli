package com.kodewerk.gcsee.cli;

import com.kodewerk.gcsee.cli.command.Command;
import com.kodewerk.gcsee.cli.command.HelpCommand;
import com.kodewerk.gcsee.cli.command.ProcessCommand;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive REPL for the GCSee CLI.
 * Maintains a registry of commands and dispatches input to them.
 */
public class Shell {

    private static final String PROMPT = "gcsee> ";

    private final Map<String, Command> registry;

    public Shell(Analysis analysis) {
        registry = new LinkedHashMap<>();
        registry.put("process", new ProcessCommand(analysis));
        registry.put("h", new HelpCommand(registry));
    }

    public void run() {
        System.out.println("GCSee CLI — type h for help, quit or q to exit.");
        Scanner input = new Scanner(System.in);
        while (true) {
            System.out.print(PROMPT);
            if (!input.hasNextLine()) break;         // EOF (Ctrl-D)

            String line = input.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] tokens = line.split("\\s+");
            String name = tokens[0].toLowerCase();

            if (name.equals("quit") || name.equals("q")) break;

            Command command = registry.get(name);
            if (command == null) {
                System.out.printf("Unknown command '%s'. Type h for help.%n", name);
            } else {
                command.execute(Arrays.copyOfRange(tokens, 1, tokens.length));
            }
        }
        System.out.println("Goodbye.");
    }
}
