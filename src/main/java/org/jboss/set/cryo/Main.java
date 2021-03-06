/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.set.cryo;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.google.common.collect.ImmutableSet;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
    private static final String[] ARG_REPOSITORY = new String[] {"-r", "--repository"};
    private static final String[] ARG_DRYRUN = new String[] {"-d", "--dry-run"};
    private static final String[] ARG_INVERT = new String[] {"-i", "--invert"};
    private static final String[] ARG_EXCLUDE = new String[] {"-e","--exclude"};
    private static final String[] ARG_SUFIX = new String[] {"-s","--sufix"};
    private static final String[] ARG_OPS = new String[] {"-o","--ops"};
    private static final String[] ARG_FAST = new String[] {"-f","--fast"};
    private static String CONVERT_TO_ARG_ID(final String id) {
        return id.replaceFirst("--", "").replaceAll("-", "_");
    }
    private static final Logger LOGGER = Logger.getLogger(Main.class.getPackage().getName());
    private static final TimeTracker TIME_TRACKER = new TimeTracker();
    private static boolean fastLogging = true;
//    static {
//        try {
//            //TODO fix this or run with: -Djava.util.logging.SimpleFormatter.format='%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n'
//            //LogManager.getLogManager().readConfiguration(Main.class.getClassLoader().getResourceAsStream("logging.properties"));
//            LOGGER = Logger.getLogger(Main.class.getPackage().getName());
//        } catch (SecurityException | IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) throws Exception {
        TIME_TRACKER.start();
          // NOTE: does not seem we need args? branchName, URL etc can be retrieved from repo.
        ArgumentParser parser = ArgumentParsers.newArgumentParser("cryo");
        parser.description("Hadny dandy contraption to create some branch. Clone clean repo, check PRs you want. Do a dry run for fun and safety(push might be manual step after verification).");
        parser.addArgument(ARG_REPOSITORY).nargs(1).required(true)
                .help("Full patht o local clone of remote repository");
        parser.addArgument(ARG_DRYRUN).action(Arguments.storeTrue()).required(false)
        .help("If present no changes will be pushed to remote repo, only local one will contain. Good for validation.");
        parser.addArgument(ARG_INVERT).action(Arguments.storeTrue()).required(false)
        .help("Invert order of PRs. By default aphrodite/github return new PRs first.");
        parser.addArgument(ARG_EXCLUDE).nargs(1).required(false).help("Comma separated list of PR IDs(integer) that will be excluded from reactor.");
        parser.addArgument(ARG_SUFIX).nargs(1).required(false).setDefault(".future").help("Sufix to use, default is '.future'.");
        parser.addArgument(ARG_OPS).nargs(1).required(false).setDefault("DirectoryOrientedOperationCenter").help("Class of ops source. If not present first service available will be used");
        parser.addArgument(ARG_FAST).action(Arguments.storeTrue()).required(false).setDefault(false).help("Disable time stamp overlay and buffering for build output.");

        try {
            Namespace ns = parser.parseArgs(args);
            final File directory = new File(ns.getString(CONVERT_TO_ARG_ID(ARG_REPOSITORY[1])).replace("[", "").replace("]", ""));
            Set<String> excludeSet = null;
            if(ns.get(CONVERT_TO_ARG_ID(ARG_EXCLUDE[1])) != null) {
                String interimExclude = ns.getString(CONVERT_TO_ARG_ID(ARG_EXCLUDE[1])).replace("[", "").replace("]", "");
                excludeSet = ImmutableSet.copyOf(Arrays.asList(interimExclude.split(",")));
            } else {
                excludeSet = ImmutableSet.copyOf(new ArrayList<String>());
            }
            final String suffix = ns.getString(CONVERT_TO_ARG_ID(ARG_SUFIX[1])).replace("[", "").replace("]", "");
            final String opsCore = ns.getString(CONVERT_TO_ARG_ID(ARG_OPS[1])).replace("[", "").replace("]", "");
            final boolean dryRun = ns.getBoolean(CONVERT_TO_ARG_ID(ARG_DRYRUN[1]));
            final boolean invertPullRequests = ns.getBoolean(CONVERT_TO_ARG_ID(ARG_INVERT[1]));
            final boolean fast = ns.getBoolean(CONVERT_TO_ARG_ID(ARG_FAST[1]));
            //BAD...
            Main.fastLogging = fast;
            final Cryo freezerProgram = new Cryo(directory, dryRun, invertPullRequests, excludeSet, suffix, opsCore);
            freezerProgram.createStorage();
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        } finally {
            TIME_TRACKER.stop();
        }
    }

    public static void log(final Level level, final String msg) {
        LOGGER.log(level, "[CRYO]["+TIME_TRACKER.interim()+"]: " + msg);
    }

    public static void log(final Level level, final String msg, final Object param) {
        LOGGER.log(level, "[CRYO]["+TIME_TRACKER.interim()+"]: " + msg, param);
    }

    public static void log(final Level level, final String msg, final Object[] params) {
        LOGGER.log(level, "[CRYO]["+TIME_TRACKER.interim()+"]: " + msg, params);
    }

    public static void log(final String msg, final Throwable t) {
        LOGGER.log(Level.SEVERE, "[CRYO]["+TIME_TRACKER.interim()+"]: " + msg, t);
    }

    /**
     * Determine if fast logging is enabled.(lack of overlay and buffering)
     * @return
     */
    public static boolean isFast() {
        return Main.fastLogging;
    }

    private static class TimeTracker {
        private LocalDateTime start;
        private LocalDateTime end;

        public Temporal getStart() {
            return start;
        }

        public Temporal getEnd() {
            return end;
        }

        public void start() {
            this.start = LocalDateTime.now();
        }

        public void stop() {
            this.end = LocalDateTime.now();
        }

        public String interim() {
            return DurationFormatUtils.formatDuration(Duration.between(this.start, LocalDateTime.now()).toMillis(), "**dd:HH:mm:ss:SSS**", true);
        }

        public String total() {
            if (this.end == null) {
                this.stop();
            }
            return DurationFormatUtils.formatDuration(Duration.between(this.start, this.end).toMillis(), "**dd:HH:mm:ss:SSS**", true) ;
        }
    }
}
