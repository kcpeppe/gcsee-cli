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

        // Force JVM exit. Some of the parsing dependencies (notably gcsee-vertx)
        // start non-daemon thread pools that outlive analyze() and would keep
        // the JVM alive indefinitely after the shell loop or a batch run ends.
        // If upstream ever exposes a proper close(), switch to that instead.
        System.exit(0);
    }
}
