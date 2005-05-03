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
package org.jedit.ruby.test;

import junit.framework.TestCase;
import org.jedit.ruby.parser.LineCounter;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class TestLineCounter extends TestCase {

    private static final String TEXT = "1 \n" +
                    "  2 \n" +
                    "3 \n";

    private static final String TEXT2 = "1 \n" +
                    "  2 \n" +
                    "3 ";

    public void testEnhancedForLoop() {
        StringBuffer buffer = new StringBuffer();
        for (String i : getJunk(buffer)) {
            i.toLowerCase();
        }
        assertEquals("Enhance loop values called once", "*", buffer.toString());
    }

    public String[] getJunk(StringBuffer buffer) {
        buffer.append("*");
        return new String[] {"red", "blue", "green"};
    }
    public void testOffsets() {
        LineCounter lineCounter = new LineCounter(TEXT);
        checkOffset(0, 2, '\n', lineCounter, TEXT);
        checkOffset(1, 7, '\n', lineCounter, TEXT);
        checkOffset(2, 10, '\n', lineCounter, TEXT);
    }

    public void testOffsets2() {
        LineCounter lineCounter = new LineCounter(TEXT2);
        checkOffset(0, 2, '\n', lineCounter, TEXT2);
        checkOffset(1, 7, '\n', lineCounter, TEXT2);
        checkOffset(2, 9, ' ', lineCounter, TEXT2);
    }

    public void testLines() {
        LineCounter lineCounter = new LineCounter(TEXT);
        checkLine(0, "1 ", lineCounter);
        checkLine(1, "  2 ", lineCounter);
        checkLine(2, "3 ", lineCounter);
    }

    public void testLines2() {
        LineCounter lineCounter = new LineCounter(TEXT2);
        checkLine(0, "1 ", lineCounter);
        checkLine(1, "  2 ", lineCounter);
        checkLine(2, "3 ", lineCounter);
    }

    private void checkLine(int line, String expected, LineCounter lineCounter) {
        assertEquals("Assert line " + line + " correct.", expected, lineCounter.getLine(line));
    }

    private void checkOffset(int line, int expected, char character, LineCounter lineCounter, String text) {
        int endOffset = lineCounter.getEndOffset(line);
        assertEquals("Assert end offset for " + line + " correct.", expected, endOffset);
        assertEquals("Assert end char for " + line + " correct.", character, text.charAt(endOffset));
    }

}
