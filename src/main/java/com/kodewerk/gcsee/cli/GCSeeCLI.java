package com.kodewerk.gcsee.cli;

import com.kodewerk.gcsee.cli.command.ProcessCommand;

public class GCSeeCLI {

    public static void main(String[] args) {
        Analysis analysis = new Analysis();

        if (args.length > 0) {
            // Batch mode: treat args as "process <path> [single|rolling]" and exit
            new ProcessCommand(analysis).execute(args);
        } else {
            // Interactive mode
            new Shell(analysis).run();
        }
    }
}
