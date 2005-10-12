/*
 * LineCounter.java - 
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

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class LineCounter {

    private final List<Integer> endOffsets;
    private final String text;

    public LineCounter(String text) {
        this.text = text;
        endOffsets = new ArrayList<Integer>();

        char[] chars = text.toCharArray();
        int line = 0;
        int index = 0;
        boolean lastWasNewLine = false;

        while (index < chars.length) {
            char character = chars[index];

            if (isNewLineCharacter(character)) {
                index = handleNewLine(line, index, chars, character);
                line++;
                lastWasNewLine = true;
            } else {
                index++;
                lastWasNewLine = false;
            }
        }

        if (!lastWasNewLine) {
            endOffsets.add(line, index - 1);
        }
    }

    public final int getLineCount() {
        return endOffsets.size();
    }

    /**
     * @return line starting at 0
     */
    public int getLineAtOffset(int startOffset) {
        int line = 0;

        for (int offset : endOffsets) {
            if (startOffset > offset) {
                line++;
            } else {
                return line;
            }
        }

        return line;
    }

    /**
     * @param line starting at 0
     */
    public final String getLine(int line) {
        if (line == 0) {
            return getLine(0, getEndOffset(0));
        } else {
            int beginIndex = getEndOffset(line - 1) + 1;
            int endOffset = getEndOffset(line);
            return getLine(beginIndex, endOffset);
        }
    }

    /**
     * Returns end offset for line.
     *
     * @param line starting at 0.
     */
    public final int getEndOffset(int line) {
        return endOffsets.get(line);
    }

    private String getLine(int beginIndex, int endIndex) {
        char endChar = text.charAt(endIndex);
        if (isNewLineCharacter(endChar)) {
            return text.substring(beginIndex, endIndex);
        } else {
            return text.substring(beginIndex, endIndex + 1);
        }
    }

    private int handleNewLine(int line, int index, char[] chars, char character) {
        endOffsets.add(line, index);
        index++;

        if (character == '\r' && index < chars.length) {
            character = chars[index];
            if (character == '\n') {
                index++;
            }
        }

        return index;
    }

    private static boolean isNewLineCharacter(char character) {
        return character == '\n' || character == '\r';
    }

}