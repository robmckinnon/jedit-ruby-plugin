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
import gnu.regexp.REMatch;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class TestAutoIndent extends TestCase {

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
        REMatch match = AutoIndentAndInsertEnd.MatchRegExp.instance.getMatch("  if true");
        assertMatchCorrect(match, " true");
    }

    public final void testMatchIfWithBracket() {
        REMatch match = AutoIndentAndInsertEnd.MatchRegExp.instance.getMatch("  if(true)");
        assertMatchCorrect(match, "(true)");
    }

    private void assertMatchCorrect(REMatch match, String partMatch) {
        assertNotNull("Assert match not null.", match);
        assertEquals("Assert match correct.", "  ", match.toString(1));
        assertEquals("Assert match correct.", "", match.toString(2));
        assertEquals("Assert match correct.", "if"+partMatch, match.toString(3));
        assertEquals("Assert match correct.", "if"+partMatch, match.toString(4));
        assertEquals("Assert match correct.", "if", match.toString(5));
        assertEquals("Assert match correct.", partMatch, match.toString(6));
    }

    private boolean hasEnd(String line) {
        return AutoIndentAndInsertEnd.hasEndKeyword(line.trim());
    }
}
