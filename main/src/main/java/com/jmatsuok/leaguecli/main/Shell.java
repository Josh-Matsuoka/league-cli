package com.jmatsuok.leaguecli.main;

import com.jmatsuok.leaguecli.main.client.CommandServer;
import com.jmatsuok.leaguecli.main.command.Command;
import com.jmatsuok.leaguecli.main.command.CommandOptions;
import com.jmatsuok.leaguecli.main.command.MalformedOptionsException;
import com.jmatsuok.leaguecli.main.command.impl.*;
import jline.ArgumentCompletor;
import jline.ConsoleReader;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import jline.ArgumentCompletor.ArgumentDelimiter;
import jline.ArgumentCompletor.ArgumentList;

/**
 * Shell for executing commands
 */
public class Shell {

    private Scanner sc;
    private ShellContext ctx;
    private CommandServer server;
    private Map<String, Command> commandRegistry = new HashMap<String, Command>();
    private CLIConfig config;
    private final Logger logger = Logger.getLogger(Shell.class.getName());

    /**
     * Constructs a new instance
     *
     * @param ctx context of execution for this shell
     * @param config configuration options for the League CLI
     */
    public Shell(ShellContext ctx, CLIConfig config) {
        this.sc = new Scanner(ctx.getInput());
        this.ctx = ctx;
        this.server = new CommandServer();
        this.config = config;
        // TODO: Implement automatic command discovery and registration
        commandRegistry.put("current-match", new CurrentMatch(config, server, ctx));
        commandRegistry.put("help", new Help(ctx, commandRegistry));
        commandRegistry.put("summoner-info", new SummonerInfo(config, server, ctx));
        commandRegistry.put("summoner-stats", new SummonerStats(config, server, ctx));
        commandRegistry.put("ranked-info", new RankedInfo(config, server, ctx));
    }

    /**
     * Begins running the shell's main loop
     */
    public void run() {
        try {
            ConsoleReader reader = new ConsoleReader();
            reader.setDefaultPrompt("> ");
            String next = "";
            while(!next.equals("exit")) {
                next = reader.readLine("> ");
                parseAndRun(next);
            }
            server.tearDown();
        } catch (Exception e) {
            logger.warning("Unable to construct ConsoleReader");
            e.printStackTrace();
        }
    }

    private void parseAndRun(String input) {
        CommandOptions opts = parseArgs(input);
        if (opts == null) {
            logger.warning("Unable to parse, or no arguments to parse.");
            return;
        } else if (opts.getCommandName().equals("exit")) {
            return;
        }
        logger.info(opts.getCommandName());
        Command c = commandRegistry.get(opts.getCommandName());
        if (c == null) {
            logger.warning("Unrecognized command: " + opts.getCommandName());
        } else {
            try {
                c.run(opts);
                if (c.getExitCode() < 0) {
                    ctx.getOutput().println("Command Failed with exit code: " + c.getExitCode());
                }
            } catch (Exception e) {
                logger.warning("Command Failed with exit code: " + c.getExitCode());
            }
        }
    }

    //TODO: Handle multiple whitespaces
    private CommandOptions parseArgs(String input) {
        return new CommandOptions(input);
    }

    /**
     * Registers a command with the shell
     *
     * @param c Command to register
     */
    public void registerCommand(Command c) {
        this.commandRegistry.put(c.getName(), c);
    }
}
