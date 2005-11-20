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
package org.jedit.ruby.parser;

import gnu.regexp.REException;
import gnu.regexp.REMatch;
import gnu.regexp.RE;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

import org.jedit.ruby.ast.*;
import org.jedit.ruby.RubyPlugin;

/**
 * @author robmckinnon at users.sourceforge.net
 */
interface MemberMatcher {

    List<Match> getMatches(String text, LineCounter lineCounter) throws REException;

    Member createMember(String name, String filePath, int startOffset, String params, String text);

    static final class Match {
        final String value;
        final int startOuterOffset;
        final int startOffset;
        final int endOffset;
        final String params;

        public Match(String value, int startOuterOffset, int startOffset, int endOffset, String params) {
            this.params = params;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.startOuterOffset = startOuterOffset;
            this.value = value;
        }
    }

    static final class ModuleMatcher extends AbstractMatcher {

        public final List<Match> getMatches(String text, LineCounter lineCounter) throws REException {
            return getMatchList("([ ]*)(module[ ]+)(\\w+[^;\\s]*)", text);
        }

        public final Member createMember(String name, String filePath, int startOffset, String params, String text) {
            return new Module(name);
        }
    }

    static final class ClassMatcher extends AbstractMatcher {

        public final List<Match> getMatches(String text, LineCounter lineCounter) throws REException {
            text = adjustForSingleLine(text, lineCounter, "module");
            return getMatchList("([ ]*)(class[ ]+)(\\w+[^;\\s]*)", text);
        }

        public final Member createMember(String name, String filePath, int startOffset, String params, String text) {
            RubyPlugin.log("class: " + name, getClass());
            return new ClassMember(name);
        }
    }

    static final class MethodMatcher extends AbstractMatcher {

        public final List<Match> getMatches(String text, LineCounter lineCounter) throws REException {
            text = adjustForSingleLine(text, lineCounter, "module");
            text = adjustForSingleLine(text, lineCounter, "class");
            String paramPattern =
                    "(\\(.*\\))"+
                    "|"+
                    "(.*)";
            return getMatchList("([ ]*)(def[ ]+)([^;\\(\\s]*)(" + paramPattern + ")?", text);
        }

        public final Member createMember(String name, String filePath, int startOffset, String params, String text) {
            String fileName = (new File(filePath)).getName();
            boolean continueLine = params.indexOf('\\') != -1;
            if (continueLine) {
                params = concatLines(text, startOffset, continueLine, params);
            }
            params = formatParameters(params);

            return new Method(name, params, filePath, fileName, false);
        }

        private String concatLines(String text, int startOffset, boolean continueLine, String params) {
            LineCounter lineCounter = new LineCounter(text);
            int line = 0;
            while (lineCounter.getEndOffset(line) < startOffset) {
                line++;
            }

            while (continueLine) {
                line++;
                params = params.substring(0, params.length() - 1).trim(); // remove \
                if (line < lineCounter.getLineCount()) {
                    params += lineCounter.getLine(line).trim();
                    continueLine = params.indexOf('\\') != -1;
                } else {
                    break;
                }
            }
            return params;
        }

        private String formatParameters(String params) {
            params = upUntil("#", params);
            params = upUntil(";", params);

            int endParenthesis = params.indexOf(")");
            if (endParenthesis != -1 && endParenthesis < params.length() - 1) {
                params = params.substring(0, endParenthesis + 1);
            }

            params = params.trim();
            if (params.length() > 0 && !params.startsWith("(") && !params.endsWith(")")) {
                params = '('+params+')';
            }

            if (params.length() == 2) {
                params = "";
            } else if (params.length() > 2) {
                String vars = params.substring(1, params.length()-1).trim();
                if (vars.length() == 0) {
                    params = "";
                } else {
                    params = '(' + vars + ')';
                }
            }
            return params;
        }

        private String upUntil(String character, String params) {
            int commentIndex = params.indexOf(character);
            if (commentIndex != -1) {
                params = params.substring(0, commentIndex);
            }
            return params;
        }
    }

    static abstract class AbstractMatcher implements MemberMatcher {

        private REMatch[] getMatches(String expression, String text) throws REException {
            RE re = new RE(expression, 0);
            return re.getAllMatches(text);
        }

        final List<Match> getMatchList(String pattern, String text) throws REException {
            REMatch[] matches = getMatches(pattern, text);
            List<Match> matchList = new ArrayList<Match>();
            int start = 0;

            for (REMatch reMatch : matches) {
                if (onlySpacesBeforeMatch(reMatch, text, start)) {
                    String value = reMatch.toString(3).trim();
                    int delimiter = value.lastIndexOf("::");
                    if (delimiter != -1) {
                        value = value.substring(delimiter+2);
                    }
                    int startOuterOffset = reMatch.getStartIndex(1);
                    int startIndex = reMatch.getStartIndex(3);
                    int endIndex = reMatch.getEndIndex(3);
                    String params = reMatch.toString(4);
                    if (params != null && params.indexOf(';') != -1) {
                        params = params.substring(0, params.indexOf(';'));
                    }

                    Match match = new Match(value, startOuterOffset, startIndex, endIndex, params);
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

        static final String SPACES = "                                                     ";

        final String adjustForSingleLine(String text, LineCounter lineCounter, String keyword) throws REException {
            List<Match> oneLineMatches = getMatchList("([ ]*)("+keyword +"[ ]+)(\\w+[^;\\s]*;[ ]*)", text);

            if (!oneLineMatches.isEmpty() && keyword.equals("class")) {
                lineCounter = new LineCounter(text);
            }
            for (Match match : oneLineMatches) {
                int lineIndex = lineCounter.getLineAtOffset(match.startOffset);
                String line = lineCounter.getLine(lineIndex);
                int endIndex = line.lastIndexOf("end");
                if (endIndex != -1) {
                    int classIndex = line.indexOf(keyword);
                    String prefix = line.substring(0, classIndex);
                    String classBlanks = MethodMatcher.SPACES.substring(0, match.endOffset - (match.startOuterOffset+classIndex));
                    String contents = line.substring((match.endOffset - match.startOuterOffset), endIndex);
                    String endBlanks = "   ";
                    String suffix = line.substring(endIndex + 3, line.length());

                    StringBuffer newLine = new StringBuffer();
                    newLine.append(prefix).append(classBlanks).append(contents).append(endBlanks).append(suffix);
                    int insertPoint = text.indexOf(line);
                    String substring = text.substring(lineCounter.getEndOffset(lineIndex), text.length());

                    text = text.substring(0, insertPoint) + newLine + substring;
                }
            }
            return text;
        }
    }

}
