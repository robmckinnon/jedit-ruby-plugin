/*
 * RubyTestSuite.java - 
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

import junit.framework.TestSuite;
import junit.framework.Test;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class RubyTestSuite extends TestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite("ruby");
        suite.addTestSuite(TestRubyParser.class);
        suite.addTestSuite(TestRubyCache.class);
        suite.addTestSuite(TestRDocSeacher.class);
        suite.addTestSuite(TestYamlParser.class);
        suite.addTestSuite(TestLineCounter.class);
        suite.addTestSuite(TestCodeAnalyzer.class);
        suite.addTestSuite(TestAutoIndent.class);
        suite.addTestSuite(TestJRubyParser.class);
        return suite;
    }
}
