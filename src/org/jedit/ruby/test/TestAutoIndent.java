/*
 * TestAutoIndent.java - 
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
import org.jedit.ruby.structure.AutoIndentAndInsertEnd;

import java.util.regex.MatchResult;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class TestAutoIndent extends TestCase {

    public final void testTrailingIfCondition() {
        boolean match = AutoIndentAndInsertEnd.TrailingConditionRegExp.instance.isMatch("    red if true");
        assertFalse("Assert doesn't match trailing condition", match);
    }

    public final void testMatchIgnoreSyntax() {
        boolean match = AutoIndentAndInsertEnd.IgnoreRegExp.instance.isMatch("   red if true");
        assertTrue("Assert matchs ignore condition", match);
    }

    public final void testDoesntMatchIgnoreSyntax() {
        boolean match = AutoIndentAndInsertEnd.IgnoreRegExp.instance.isMatch("   if true");
        assertFalse("Assert doesn't match ignore condition", match);
    }

    public final void testEndCount() {
        int endCount = AutoIndentAndInsertEnd.getEndCount("end");
        assertEquals("Assert end count correct.", 1, endCount);
    }

    public final void testEndTrue() {
        assertTrue(hasEnd("end"));
    }

    public final void testEndTrue2() {
        assertTrue(hasEnd(" end "));
    }

    public final void testEndTrue3() {
        assertTrue(hasEnd(" end #red"));
    }

    public final void testEndTrue4() {
        assertTrue(hasEnd(" end if true"));
    }

    public final void testEndTrue5() {
        assertTrue(hasEnd("  def quack a, b, d;    c blue;  puts 'quack';  end"));
    }

    public final void testEndFalse() {
        assertFalse(hasEnd("#end"));
    }

    public final void testEndFalse2() {
        assertFalse(hasEnd(" #end"));
    }

    public final void testEndFalse3() {
        assertFalse(hasEnd("# end"));
    }

    public final void testEndFalse4() {
        assertFalse(hasEnd(" # end"));
    }

    public final void testEndFalse5() {
        assertFalse(hasEnd("  def tag_end name"));
    }

    public final void testMatchIf() {
//        REMatch match = AutoIndentAndInsertEnd.MatchRegExp.instance.getMatch("  if true");
        MatchResult match = AutoIndentAndInsertEnd.MatchRegExp.instance.firstMatch("  if true");
        assertMatchResultCorrect(match, " true");
    }

    public final void testMatchIfWithBracket() {
//        REMatch match = AutoIndentAndInsertEnd.MatchRegExp.instance.getMatch("  if(true)");
        MatchResult match = AutoIndentAndInsertEnd.MatchRegExp.instance.firstMatch("  if(true)");
        assertMatchResultCorrect(match, "(true)");
    }

    private static void assertMatchResultCorrect(MatchResult match, String partMatch) {
        assertNotNull("Assert match not null.", match);
        assertEquals("Assert match correct.", "  ", match.group(1));
        assertEquals("Assert match correct.", "", match.group(2));
        assertEquals("Assert match correct.", "if"+partMatch, match.group(3));
        assertEquals("Assert match correct.", "if"+partMatch, match.group(4));
        assertEquals("Assert match correct.", "if", match.group(5));
        assertEquals("Assert match correct.", partMatch, match.group(6));
    }

    private static boolean hasEnd(String line) {
        return AutoIndentAndInsertEnd.hasEndKeyword(line.trim());
    }

}
