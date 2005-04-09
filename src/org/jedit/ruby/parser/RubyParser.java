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

import gnu.regexp.REException;

import java.util.*;

import org.jruby.lexer.yacc.SourcePosition;
import org.gjt.sp.jedit.View;
import org.jedit.ruby.ast.*;
import org.jedit.ruby.parser.JRubyParser;
import org.jedit.ruby.parser.MemberMatcher;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.RubyPlugin;

/**
 * <p>Parses ruby file.</p>
 * <p><i>
 * Not thread safe.</i>
 * Should only be accessed by one thread at a time.
 * </p>
 *
 * @author robmckinnon at users.sourceforge.net
 */
public class RubyParser {

    private static final Member[] EMPTY_MEMBER_ARRAY = new Member[0];

    private static final RubyParser instance = new RubyParser();

    private final RubyParser.LogWarningListener logListener;
    private final MemberMatcher moduleMatcher;
    private final MemberMatcher classMatcher;
    private final MemberMatcher methodMatcher;

    private Map<String, Integer> fileToLengthMap;
    private Map<String, Member[]> fileToMembersMap;
//    private String lastFilePath;
//    private Member[] lastMembers;
//    private int lastTextLength;

    private RubyParser() {
        logListener = new LogWarningListener();
        moduleMatcher = new MemberMatcher.ModuleMatcher();
        classMatcher = new MemberMatcher.ClassMatcher();
        methodMatcher = new MemberMatcher.MethodMatcher();
        fileToLengthMap = new HashMap<String, Integer>();
        fileToMembersMap = new HashMap<String, Member[]>();
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

    private RubyMembers createMembers(String text, String filePath, WarningListener listener, boolean forceReparse) {
        Member[] members;

        if(!forceReparse
                && fileToMembersMap.containsKey(filePath)
                && fileToLengthMap.get(filePath) == text.length()) {
            members = fileToMembersMap.get(filePath);
        } else {
            List<Member> memberList = createMembersAsList(text, filePath, listener);

            if (memberList != null) {
                members = memberList.toArray(EMPTY_MEMBER_ARRAY);
                fileToMembersMap.put(filePath, members);
                fileToLengthMap.put(filePath, text.length());
            } else {
                members = null;
            }
        }

        return new RubyMembers(members, logListener.getProblems());
    }

    private List<Member> createMembersAsList(String text, String filePath, WarningListener listener) {
        List<Member> members = null;

        try {
            List<Member> modules = createMembers(text, filePath, moduleMatcher);
            List<Member> classes = createMembers(text, filePath, classMatcher);
            List<Member> methods = createMembers(text, filePath, methodMatcher);

            members = JRubyParser.getMembers(text, modules, classes, methods, getListeners(listener), filePath);
        } catch (REException e) {
            e.printStackTrace();
        }

        return members;
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

    private List<Member> createMembers(String text, String filePath, MemberMatcher matcher) throws REException {
        List<MemberMatcher.Match> matches = matcher.getMatches(text);
        List<Member> members = new ArrayList<Member>();

        for(MemberMatcher.Match match : matches) {
            String name = match.value.trim();
            int index = match.startOffset;
            members.add(matcher.createMember(name, filePath, index));
        }

        return members;
    }

    /**
     * Interface defining methods called back
     * with parsing warnings.
     */
    public static interface WarningListener {

        void warn(SourcePosition position, String message);

        void warn(String message);

        void warning(SourcePosition position, String message);

        void warning(String message);

        void error(SourcePosition position, String message);

        void clear();
    }

    private static class LogWarningListener implements WarningListener {

        private List<Problem> problems = new ArrayList<Problem>();

        public List<Problem> getProblems() {
            return problems;
        }

        public void warn(SourcePosition position, String message) {
            problems.add(new Warning(message, getLine(position)));
            log(position, message);
        }

        public void warn(String message) {
            problems.add(new Warning(message, 0));
            RubyPlugin.log("warn:  " + message);
        }

        public void warning(SourcePosition position, String message) {
            problems.add(new Warning(message, getLine(position)));
            log(position, message);
        }

        public void warning(String message) {
            problems.add(new Warning(message, 0));
            RubyPlugin.log("warn:  " + message);
        }

        public void error(SourcePosition position, String message) {
            problems.add(new org.jedit.ruby.ast.Error(message, getLine(position)));
            RubyPlugin.log("error: " + position.getFile() + " " + position.getLine() + " " + message);
        }

        public void clear() {
            problems.clear();
        }

        private void log(SourcePosition position, String message) {
            RubyPlugin.log("warn:  " + position.getFile() + " " + position.getLine() + " " + message);
        }

        private int getLine(SourcePosition position) {
            return position == null ? 0 : position.getLine() - 1;
        }

    }

}
