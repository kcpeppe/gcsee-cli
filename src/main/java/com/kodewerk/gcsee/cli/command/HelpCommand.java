package com.kodewerk.gcsee.cli.command;

import java.util.Map;

public class HelpCommand implements Command {

    private final Map<String, Command> registry;

    public HelpCommand(Map<String, Command> registry) {
        this.registry = registry;
    }

    @Override
    public void execute(String[] args) {
        System.out.println("Commands:");
        registry.values().stream()
                .distinct()                     // quit/q share an instance — print once
                .forEach(cmd -> System.out.println(cmd.helpLine()));
        System.out.println("  quit, q                            Exit");
        System.out.println("  h                                  Print this help");
    }

    @Override
    public String helpLine() {
        return "  h                                  Print this help";
    }
}
