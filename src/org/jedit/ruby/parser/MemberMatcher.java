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

import java.util.List;
import java.util.ArrayList;
import java.util.regex.MatchResult;
import java.io.File;

import org.jedit.ruby.ast.*;
import org.jedit.ruby.utils.RegularExpression;

/**
 * @author robmckinnon at users.sourceforge.net
 */
interface MemberMatcher {

    List<Match> getMatches(String text, LineCounter lineCounter);

    Member createMember(String name, String filePath, int startOffset, String params, String text);

    static final class Match {
        private final String value;
        private final int startOuterOffset;
        private final int startOffset;
        private final int endOffset;
        private final String params;

        public Match(String value, int startOuterOffset, int startOffset, int endOffset, String params) {
            this.params = params;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.startOuterOffset = startOuterOffset;
            this.value = value;
        }

        public String value() {
            return value;
        }

        public int startOuterOffset() {
            return startOuterOffset;
        }

        public int startOffset() {
            return startOffset;
        }

        public int endOffset() {
            return endOffset;
        }

        public String params() {
            return params;
        }
    }

    static final class MethodMatcher extends AbstractMatcher {

        private static final RegularExpression moduleRegExp = new RegularExpression("([ ]*)(module[ ]+)(\\w+[^;\\s]*;[ ]*)");
        private static final RegularExpression classRegExp = new RegularExpression("([ ]*)(class[ ]+)(\\w+[^;\\s]*;[ ]*)");
        private static final String PARAM_PATTERN = "(\\(.*\\))"+
                    "|"+
                    "(.*)";
        private static final RegularExpression defRegExp = new RegularExpression("([ ]*)(def[ ]+)([^;\\(\\s]*)(" + PARAM_PATTERN + ")?");

        public final List<Match> getMatches(String text, LineCounter lineCounter) {
            text = adjustForSingleLine(text, lineCounter, "module", moduleRegExp);
            text = adjustForSingleLine(text, lineCounter, "class", classRegExp);
            return getMatchList(defRegExp, text);
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

        final List<Match> getMatchList(RegularExpression expression, String text) {
            MatchResult[] matches = expression.getAllMatchResults(text);
            List<Match> matchList = new ArrayList<Match>();
            int start = 0;

            for (MatchResult matchResult : matches) {
                if (onlySpacesBeforeMatch(matchResult, text, start)) {
                    String value = matchResult.group(3).trim();
                    int delimiter = value.lastIndexOf("::");
                    if (delimiter != -1) {
                        value = value.substring(delimiter+2);
                    }
                    int startOuterOffset = matchResult.start(1);
                    int startIndex = matchResult.start(3);
                    int endIndex = matchResult.end(3);

                    String params;
                    if (matchResult.groupCount() > 3) {
                        params = matchResult.group(4);
                        if (params != null && params.indexOf(';') != -1) {
                            params = params.substring(0, params.indexOf(';'));
                        }
                    } else {
                        params = null;
                    }

                    Match match = new Match(value, startOuterOffset, startIndex, endIndex, params);
                    matchList.add(match);
                    start = text.indexOf(matchResult.toString()) + matchResult.toString().length();
                }
            }

            return matchList;
        }

        private boolean onlySpacesBeforeMatch(MatchResult match, String text, int stop) {
            int index = match.start() - 1;
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

        final String adjustForSingleLine(String text, LineCounter lineCounter, String keyword, RegularExpression expression) {
            List<Match> oneLineMatches = getMatchList(expression, text);

            if (!oneLineMatches.isEmpty() && keyword.equals("class")) {
                lineCounter = new LineCounter(text);
            }
            for (Match match : oneLineMatches) {
                int lineIndex = lineCounter.getLineAtOffset(match.startOffset());
                String line = lineCounter.getLine(lineIndex);
                int endIndex = line.lastIndexOf("end");
                if (endIndex != -1) {
                    int classIndex = line.indexOf(keyword);
                    String prefix = line.substring(0, classIndex);
                    String classBlanks = MethodMatcher.SPACES.substring(0, match.endOffset() - (match.startOuterOffset()+classIndex));
                    String contents = line.substring((match.endOffset() - match.startOuterOffset()), endIndex);
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