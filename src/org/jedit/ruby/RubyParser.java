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
package org.jedit.ruby;

import gnu.regexp.REException;
import gnu.regexp.REMatch;
import gnu.regexp.RE;

import java.util.*;

import org.gjt.sp.jedit.search.RESearchMatcher;

/**
 * Parses ruby file
 * @author robmckinnon at users.sourceforge.net
 */
public class RubyParser {

    private static final Member[] EMPTY_MEMBER_ARRAY = new Member[0];

    public static interface Matcher {
        List<REMatch> getMatches(String text) throws REException;
        Member createMember(String name, int index);
    }

    private static final Matcher moduleMatcher = new Matcher() {
        public List<REMatch> getMatches(String text) throws REException {
            return getMatchList("([ ]*)(module[ ]+)(\\w+.*)", text);
        }

        public Member createMember(String name, int index) {
            return new Member.Module(name, index);
        }
    };

    private static final Matcher classMatcher = new Matcher() {
        public List<REMatch> getMatches(String text) throws REException {
            return getMatchList("([ ]*)(class[ ]+)(\\w+.*)", text);
        }

        public Member createMember(String name, int index) {
            return new Member.Class(name, index);
        }
    };

    private static final Matcher methodMatcher = new Matcher() {
        public List<REMatch> getMatches(String text) throws REException {
            return getMatchList("([ ]*)(def[ ]+)(.*)", text);
        }

        public Member createMember(String name, int index) {
            return new Member.Method(name, index);
        }
    };

    public static Member[] getMembers(String text) {
        return getMembersAsList(text).toArray(EMPTY_MEMBER_ARRAY);
    }

    public static List<Member> getMembersAsList(String text) {
        List<Member> members = null;

        try {
            List<Member> modules = createMembers(text, moduleMatcher);
            List<Member> classes = createMembers(text, classMatcher);
            List<Member> methods = createMembers(text, methodMatcher);
            members = JRubyParser.getMembers(text, modules, classes, methods);
        } catch (REException e) {
            e.printStackTrace();
        }

        return members;
    }

    private static List<Member> createMembers(String text, Matcher matcher) throws REException {
        List<REMatch> matches = matcher.getMatches(text);
        List<Member> members = new ArrayList<Member>();

        for(REMatch match : matches) {
            String name = match.toString(3);
            int index = match.getStartIndex(3);
            members.add(matcher.createMember(name, index));
        }

        return members;
    }

    private static REMatch[] getMatches(String expression, String text) throws REException {
        RE re = new RE(expression, 0, RESearchMatcher.RE_SYNTAX_JEDIT);
        return re.getAllMatches(text);
    }

    private static List<REMatch> getMatchList(String pattern, String text) throws REException {
        REMatch[] matches = getMatches(pattern, text);
        List<REMatch> matchList = new ArrayList<REMatch>();

        for(REMatch match : matches) {
            String openingText = match.toString(1).trim();
            boolean isClass = openingText.length() == 0;

            if(isClass) {
                matchList.add(match);
            }
        }

        return matchList;
    }

}
