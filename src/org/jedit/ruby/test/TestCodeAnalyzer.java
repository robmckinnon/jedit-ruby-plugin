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
import org.jedit.ruby.completion.CodeAnalyzer;

import java.util.List;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class TestCodeAnalyzer extends TestCase {

    private static final String TEXT = "a.respond_to?() \n" +
                    "a.== \n" +
                    "3 \n";


    public void testFindMethod() {
        List<String> methods = CodeAnalyzer.getMethods(TEXT, "a");
        assertEquals("assert found method", "respond_to?", methods.get(0));
        assertEquals("assert found method", "==", methods.get(1));
    }

}
