package jp.vmi.selenium.selenese.utils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * Record log message.
 */
public class LogRecorder {

    /**
     * Log level.
     */
    @SuppressWarnings("javadoc")
    public static enum Level {
        INFO, ERROR
    }

    /**
     * Log message.
     */
    public static class LogMessage {

        /** Recorded date */
        public final long date;

        /** Level */
        public final Level level;

        /** Message */
        public final String message;

        private LogMessage(long date, Level level, String message) {
            this.date = System.currentTimeMillis();
            this.level = level;
            this.message = message;
        }

        @Override
        public String toString() {
            return DATE_TIME_FORMAT.format(date) + "[" + level + "] " + message;
        }
    }

    private static final FastDateFormat DATE_TIME_FORMAT = FastDateFormat.getInstance("[yyyy-MM-dd HH:mm:ss.SSS] ");

    private PrintStream ps = new PrintStream(new NullOutputStream());

    private final List<LogMessage> messages = new ArrayList<LogMessage>();

    private final List<LogMessage> errorMessages = new ArrayList<LogMessage>();

    /**
     * Set PrintStream object.
     *
     * @param ps PrintStream object.
     */
    public void setPrintStream(PrintStream ps) {
        this.ps = ps;
    }

    /**
     * Get current time millis. (override for test)
     *
     * @return System.currentTimeMillis()
     */
    public long now() {
        return System.currentTimeMillis();
    }

    /**
     * Log info message.
     *
     * @param message log message.
     */
    public void info(String message) {
        LogMessage lmsg = new LogMessage(now(), Level.INFO, message);
        messages.add(lmsg);
        ps.println(lmsg);
    }

    /**
     * Log error message.
     * 
     * @param message error message.
     */
    public void error(String message) {
        LogMessage lmsg = new LogMessage(now(), Level.ERROR, message);
        messages.add(lmsg);
        errorMessages.add(lmsg);
        ps.println(lmsg);
    }

    /**
     * Get logged messages.
     *
     * @return messages.
     */
    public List<LogMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Get logged error messages.
     *
     * @return error messages.
     */
    public List<LogMessage> getErrorMessages() {
        return Collections.unmodifiableList(errorMessages);
    }
}
