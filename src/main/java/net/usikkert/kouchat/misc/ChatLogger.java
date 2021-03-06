
/***************************************************************************
 *   Copyright 2006-2013 by Christian Ihle                                 *
 *   kontakt@usikkert.net                                                  *
 *                                                                         *
 *   This file is part of KouChat.                                         *
 *                                                                         *
 *   KouChat is free software; you can redistribute it and/or modify       *
 *   it under the terms of the GNU Lesser General Public License as        *
 *   published by the Free Software Foundation, either version 3 of        *
 *   the License, or (at your option) any later version.                   *
 *                                                                         *
 *   KouChat is distributed in the hope that it will be useful,            *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU      *
 *   Lesser General Public License for more details.                       *
 *                                                                         *
 *   You should have received a copy of the GNU Lesser General Public      *
 *   License along with KouChat.                                           *
 *   If not, see <http://www.gnu.org/licenses/>.                           *
 ***************************************************************************/

package net.usikkert.kouchat.misc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.usikkert.kouchat.event.SettingsListener;
import net.usikkert.kouchat.util.Tools;
import net.usikkert.kouchat.util.Validate;

/**
 * This is a simple logger. Creates a new unique log file for each time
 * KouChat is started.
 *
 * @author Christian Ihle
 */
public class ChatLogger implements SettingsListener {

    /**
     * The name of the log file. Uses date, time, and milliseconds to make sure
     * it is unique.
     */
    private static final String LOG_FILE_POSTFIX = "-" + Tools.dateToString(null, "yyyy.MM.dd-HH.mm.ss-SSS") + ".log";

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(ChatLogger.class.getName());

    private final Settings settings;
    private final ErrorHandler errorHandler;
    private final String logFilePrefix;
    private BufferedWriter writer;
    private boolean open;

    /**
     * Default constructor. Sets the log file prefix to <code>kouchat</code>.
     *
     * @param settings The settings to use.
     */
    public ChatLogger(final Settings settings) {
        this("kouchat", settings);
    }

    /**
     * Constructor for setting a custom log file prefix.
     *
     * Adds a shutdown hook to make sure the log file is closed on shutdown.
     *
     * @param logFilePrefix The prefix for the log file name.
     * @param settings The settings to use.
     */
    public ChatLogger(final String logFilePrefix, final Settings settings) {
        Validate.notEmpty(logFilePrefix, "Log file prefix can not be empty");
        Validate.notNull(settings, "Settings can not be null");

        this.logFilePrefix = logFilePrefix;
        this.settings = settings;

        settings.addSettingsListener(this);

        errorHandler = ErrorHandler.getErrorHandler();

        if (settings.isLogging()) {
            open();
        }

        Runtime.getRuntime().addShutdownHook(new Thread("ChatLoggerShutdownHook") {
            @Override
            public void run() {
                close();
            }
        });
    }

    /**
     * Opens the log file for writing.
     * Will append if the log file already exists.
     */
    public void open() {
        close();

        try {
            final String logLocation = settings.getLogLocation();
            final File logdir = new File(logLocation);

            if (!logdir.exists()) {
                if (!logdir.mkdirs()) {
                    throw new IOException("Unable to create path for logging: " + logdir);
                }
            }

            final String fileName = logLocation + logFilePrefix + LOG_FILE_POSTFIX;
            writer = new BufferedWriter(new FileWriter(fileName, true));
            open = true;
        }

        catch (final IOException e) {
            LOG.log(Level.SEVERE, e.toString(), e);
            settings.setLogging(false);
            errorHandler.showError("Could not initialize the logging:\n" + e);
        }
    }

    /**
     * Flushed and closes the current open log file.
     */
    public void close() {
        if (open) {
            try {
                writer.flush();
                writer.close();
            }

            catch (final IOException e) {
                LOG.log(Level.SEVERE, e.toString(), e);
            }

            finally {
                open = false;
            }
        }
    }

    /**
     * Adds a new line of text to the current open log file, if any.
     *
     * @param line The line of text to add to the log.
     */
    public void append(final String line) {
        if (open) {
            try {
                writer.append(line);
                writer.newLine();
                writer.flush();
            }

            catch (final IOException e) {
                LOG.log(Level.SEVERE, e.toString(), e);
                close();
            }
        }
    }

    /**
     * Returns if a log file is opened for writing or not.
     *
     * @return True if a log file is open.
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Opens or closes the log file when the logging setting is changed.
     *
     * @param setting The setting that was changed.
     */
    @Override
    public void settingChanged(final String setting) {
        if (setting.equals("logging")) {
            if (settings.isLogging()) {
                if (!isOpen()) {
                    open();
                }
            }

            else {
                close();
            }
        }
    }
}
