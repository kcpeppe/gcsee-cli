package com.kodewerk.gcsee.cli.command;

import com.kodewerk.gcsee.cli.Analysis;
import com.kodewerk.gcsee.io.RotatingGCLogFile;
import com.kodewerk.gcsee.io.SingleGCLogFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProcessCommand implements Command {

    private static final String USAGE = "Usage: process <path> [single|rolling]";

    private final Analysis analysis;

    public ProcessCommand(Analysis analysis) {
        this.analysis = analysis;
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            System.out.println(USAGE);
            return;
        }

        Path path = Path.of(args[0]);
        if (Files.notExists(path)) {
            System.out.printf("File not found: %s%n", path);
            return;
        }

        String mode = args.length > 1 ? args[1].toLowerCase() : "single";

        try {
            switch (mode) {
                case "single" -> analysis.run(new SingleGCLogFile(path));
                case "rolling" -> analysis.run(new RotatingGCLogFile(path));
                default -> System.out.printf("Unknown mode '%s'. %s%n", mode, USAGE);
            }
        } catch (IOException e) {
            System.out.printf("Error processing %s: %s%n", path, e.getMessage());
        }
    }

    @Override
    public String helpLine() {
        return "  process <path> [single|rolling]   Analyse a GC log (default: single)";
    }
}
