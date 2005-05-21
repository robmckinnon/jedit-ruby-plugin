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

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class TestAutoIndent extends TestCase {

    public void testEndTrue() {
        assertTrue(hasEnd("end"));
    }

    public void testEndTrue2() {
        assertTrue(hasEnd(" end "));
    }

    public void testEndTrue3() {
        assertTrue(hasEnd(" end #red"));
    }

    public void testEndTrue4() {
        assertTrue(hasEnd(" end if true"));
    }

    public void testEndFalse() {
        assertFalse(hasEnd("#end"));
    }

    public void testEndFalse2() {
        assertFalse(hasEnd(" #end"));
    }

    public void testEndFalse3() {
        assertFalse(hasEnd("# end"));
    }

    public void testEndFalse4() {
        assertFalse(hasEnd(" # end"));
    }

    public void testEndFalse5() {
        assertFalse(hasEnd("  def tag_end name"));
    }

    private boolean hasEnd(String line) {
        return AutoIndentAndInsertEnd.hasEndKeyword(line.trim());
    }
}
