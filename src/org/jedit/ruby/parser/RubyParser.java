/*
 * RubyParser.java - Parses ruby file
 *
 * Copyright 2005 Robert McKinnon
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.jedit.ruby.parser;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.Buffer;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.ast.*;
import org.jedit.ruby.ast.Error;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.common.IRubyWarnings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

/**
 * <p>Parses ruby file.</p>
 * <p><i>
 * Not thread safe.</i>
 * Should only be accessed by one thread at a time.
 * </p>
 *
 * @author robmckinnon at users.sourceforge.net
 */
public final class RubyParser {

    private static final Member[] EMPTY_MEMBER_ARRAY = new Member[0];

    private static final RubyParser instance = new RubyParser();

    private final RubyParser.LogWarningListener logListener;
    private final MemberMatcher methodMatcher;

    private final Map<File, Long> fileToLastModified;
    private final Map<File, String> fileToOldText;
    private final Map<File, Member[]> fileToMembers;
    private final Map<File, RubyMembers> fileToLastGoodMembers;
    private final Map<File, List<Problem>> fileToProblems;

    private RubyParser() {
        logListener = new LogWarningListener();
        methodMatcher = new MemberMatcher.MethodMatcher();
        fileToLastModified = new HashMap<File, Long>();
        fileToOldText = new HashMap<File, String>();
        fileToMembers = new HashMap<File, Member[]>();
        fileToLastGoodMembers = new HashMap<File, RubyMembers>();
        fileToProblems = new HashMap<File, List<Problem>>();
    }

    public static RubyMembers getMembers(View view) {
        String text = view.getTextArea().getText();
        String filePath = view.getBuffer().getPath();
        return getMembers(text, filePath);
    }

    public static RubyMembers getMembers(String text, String filePath) {
        return getMembers(text, filePath, null, false);
    }

    public static RubyMembers getMembers(String text, String filePath, WarningListener listener, boolean forceReparse) {
        return instance.createMembers(text, filePath, listener, forceReparse);
    }

    public static List<Member> getMembersAsList(String text, String filePath, WarningListener listener) {
        return instance.createMembersAsList(text, filePath, listener);
    }

    public static boolean hasLastGoodMembers(Buffer buffer) {
        return instance.hasLastGoodMembers(buffer.getPath());
    }

    public static RubyMembers getLastGoodMembers(Buffer buffer) {
        return instance.getLastGoodMembers(buffer.getPath());
    }

    private synchronized boolean hasLastGoodMembers(String path) {
        return fileToLastGoodMembers.containsKey(new File(path));
    }

    private synchronized RubyMembers getLastGoodMembers(String path) {
        return fileToLastGoodMembers.get(new File(path));
    }

    private synchronized RubyMembers createMembers(String text, String path, WarningListener listener, boolean forceReparse) {
        Member[] members;
        List<Problem> problems;
        File file = new File(path);

        if(!forceReparse
                && fileToMembers.containsKey(file)
                && fileToLastModified.get(file) == file.lastModified()
                && fileToOldText.get(file).equals(text)) {
            members = fileToMembers.get(file);
            problems = fileToProblems.get(file);
        } else {
            List<Member> memberList = createMembersAsList(text, path, listener);
            problems = logListener.getProblems();
            members = memberList != null ? memberList.toArray(EMPTY_MEMBER_ARRAY) : null;

            fileToMembers.put(file, members);

            if (members != null) {
                fileToLastGoodMembers.put(file, new RubyMembers(members, null, text.length()));
            } else if (fileToLastGoodMembers.containsKey(file)) {
                RubyMembers lastGoodMembers = fileToLastGoodMembers.get(file);
                lastGoodMembers.setProblems(problems);
            }

            fileToLastModified.put(file, file.lastModified());
            fileToOldText.put(file, text);
            fileToProblems.put(file, problems);
        }

        return new RubyMembers(members, problems, text.length());
    }

    private synchronized List<Member> createMembersAsList(String text, String filePath, WarningListener listener) {
        LineCounter lineCounter = new LineCounter(text);

        List<Member> methods = createMembers(text, filePath, lineCounter, methodMatcher);
        List<WarningListener> listeners = getListeners(listener);

        return JRubyParser.getMembers(text, methods, listeners, filePath, lineCounter);
    }

    private List<WarningListener> getListeners(WarningListener listener) {
        logListener.clear();
        List<WarningListener> listeners = new ArrayList<WarningListener>();
        listeners.add(logListener);
        if(listener != null) {
            listeners.add(listener);
        }
        return listeners;
    }

    private static List<Member> createMembers(String text, String filePath, LineCounter lineCounter, MemberMatcher matcher) {
        List<MemberMatcher.Match> matches = matcher.getMatches(text, lineCounter);
        List<Member> members = new ArrayList<Member>();

        for(MemberMatcher.Match match : matches) {
            String name = match.value().trim();
            int startOffset = match.startOffset();
//            int startOuterOffset = match.startOuterOffset;
            String params = match.params();
            members.add(matcher.createMember(name, filePath, startOffset, params, text));
        }

        return members;
    }

    /**
     * Interface defining methods called back
     * with parsing warnings.
     */
    public static interface WarningListener extends IRubyWarnings {

        void error(ISourcePosition position, String message);

        void clear();
    }

    private static final class LogWarningListener implements WarningListener {

        private final List<Problem> problems = new ArrayList<Problem>();

        public final List<Problem> getProblems() {
            return problems;
        }

        public boolean isVerbose() {
            return false;
        }

        public final void warn(ISourcePosition position, String message) {
            problems.add(new Warning(message, getLine(position)));
            log(position, message);
        }

        public final void warn(String message) {
            problems.add(new Warning(message, 0));
            RubyPlugin.log("warn:  " + message, getClass());
        }

        public final void warning(ISourcePosition position, String message) {
            problems.add(new Warning(message, getLine(position)));
            log(position, message);
        }

        public final void warning(String message) {
            RubyPlugin.log("warn:  " + message, getClass());
            problems.add(new Warning(message, 0));
        }

        public final void error(ISourcePosition position, String message) {
            RubyPlugin.log("error: " + position.getFile() + " " + position.getEndLine() + " " + message, getClass());
            problems.add(new Error(message, getLine(position)));
        }

        public final void clear() {
            problems.clear();
        }

        private void log(ISourcePosition position, String message) {
            RubyPlugin.log("warn:  " + position.getFile() + " " + position.getEndLine() + " " + message, getClass());
        }

        private int getLine(ISourcePosition position) {
            return position == null ? 0 : position.getEndLine();
        }

    }

}
