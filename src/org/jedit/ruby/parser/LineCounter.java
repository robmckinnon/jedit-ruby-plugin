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
     * @param index starting at 0
     */
    public final String getLine(int index) {
        return getLineUpTo(index, getEndOffset(index));
    }

    /**
     * Returns part of line at given index
     * up to the given upToOffset.
     */
    public final String getLineUpTo(int index, int upToOffset) {
        return getLine(getStartOffset(index), upToOffset);
    }

    /**
     * Returns start offset for line.
     * @param index starting at 0
     */
    public final int getStartOffset(int index) {
        return (index == 0) ? 0 : getEndOffset(index - 1) + 1;
    }

    /**
     * Returns end offset for index.
     * @param index starting at 0.
     */
    public final int getEndOffset(int index) {
        return endOffsets.get(index);
    }

    private String getLine(int beginIndex, int endIndex) {
        char endChar = charAt(endIndex);
        if (isNewLineCharacter(endChar)) {
            return text.substring(beginIndex, endIndex);
        } else {
            return text.substring(beginIndex, endIndex + 1);
        }
    }

    public char charAt(int index) {
      if (index < text.length()) {
          return text.charAt(index);
      } else {
          return (char)-1;
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

    public String getText() {
        return text;
    }
}