/*
 * MemberMatcher.java - 
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

import java.util.List;
import java.util.ArrayList;
import java.io.File;

/**
 * @author robmckinnon at users.sourceforge.net
 */
interface MemberMatcher {

    List<Match> getMatches(String text) throws REException;

    Member createMember(String name, String filePath, int index);

    static class Match {
        String value;
        int startOffset;

        public Match(String value, int startOffset) {
            this.value = value;
            this.startOffset = startOffset;
        }
    }

    static class ModuleMatcher extends AbstractMatcher {

        public List<Match> getMatches(String text) throws REException {
            return getMatchList("([ ]*)(module[ ]+)(\\w+[^;\\s]*)", text);
        }

        public Member createMember(String name, String filePath, int index) {
            return new Member.Module(name, index);
        }
    }

    static class ClassMatcher extends AbstractMatcher {

        public List<Match> getMatches(String text) throws REException {
            return getMatchList("([ ]*)(class[ ]+)(\\w+.*)", text);
        }

        public Member createMember(String name, String filePath, int index) {
            RubyPlugin.log("class: " + name);
            return new Member.Class(name, index);
        }
    }

    static class MethodMatcher extends AbstractMatcher {

        public List<Match> getMatches(String text) throws REException {
            return getMatchList("([ ]*)(def[ ]+)(.*)", text);
        }

        public Member createMember(String name, String filePath, int index) {
            String fileName = (new File(filePath)).getName();
            return new Member.Method(name, filePath, fileName, index);
        }
    }

    static abstract class AbstractMatcher implements MemberMatcher {

        private REMatch[] getMatches(String expression, String text) throws REException {
            RE re = new RE(expression, 0);
            return re.getAllMatches(text);
        }

        protected List<Match> getMatchList(String pattern, String text) throws REException {
            REMatch[] matches = getMatches(pattern, text);
            List<Match> matchList = new ArrayList<Match>();
            int start = 0;

            for(REMatch reMatch : matches) {
                if(onlySpacesBeforeMatch(reMatch, text, start)) {
                    String value = reMatch.toString(3).trim();
                    int index = reMatch.getStartIndex(3);
                    Match match = new Match(value, index);
                    matchList.add(match);
                    start = text.indexOf(reMatch.toString()) + reMatch.toString().length();
                }
            }

            return matchList;
        }

        private boolean onlySpacesBeforeMatch(REMatch match, String text, int stop) {
            int index = match.getStartIndex() - 1;
            boolean onlySpaces = true;

            if(index >= stop) {
                char nextCharacter = text.charAt(index);

                while(onlySpaces && index >= stop && nextCharacter != '\n' && nextCharacter != '\r') {
                    char character = text.charAt(index--);
                    onlySpaces = character == ' ' || character == '\t' || character == ';';
                    if(index >= stop) {
                        nextCharacter = text.charAt(index);
                    }
                }
            }
            return onlySpaces;
        }
    }

}
