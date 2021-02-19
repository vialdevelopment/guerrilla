package io.github.vialdevelopment.guerrilla;

import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * A logger to let me pretty print out instead of the System.out.println abomination
 */
public class Logger {

    private final java.util.logging.Logger logger;
    private final boolean verbose;

    public Logger(String name, boolean verbose) {
        this.verbose = verbose;
        this.logger = java.util.logging.Logger.getLogger(name);
        this.logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord logRecord) {
                return String.format("[%tT] [%s/%s] %s\n", logRecord.getMillis(), name, logRecord.getLevel().getName(), logRecord.getMessage());
            }
        });
        this.logger.addHandler(handler);
    }

    public void info(String s) {
        logger.info(s);
    }

    public void verbose(String s) {
        if (verbose) logger.info(s);
    }

}
