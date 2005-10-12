/*
 * TestRDocSeacher.java - 
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

import org.jedit.ruby.ri.RDocSeacher;
import org.jedit.ruby.ast.Member;

import java.util.List;

import junit.framework.TestCase;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class TestRDocSeacher extends TestCase {

    private static final String RESULT = "More than one method matched your request. You can refine\n" +
            "your search by asking for information on one of:\n" +
            "\n" +
            "     Method#to_s, Vector#to_s, Struct#to_s, Time#to_s, CGI::Cookie#to_s,\n" +
            "     Array#to_s, Matrix#to_s, MatchData#to_s, FalseClass#to_s,\n" +
            "     Pathname#to_s, Hash#to_s, UnboundMethod#to_s, TrueClass#to_s,\n" +
            "     Module#to_s, Complex#to_s, Proc#to_s, Symbol#to_s, Symbol#to_sym,\n" +
            "     Exception#to_s, Bignum#to_s, Object#to_s, NilClass#to_s,\n" +
            "     Range#to_s, Date#to_s, NameError#to_s, NameError::message#to_str,\n" +
            "     Fixnum#to_s, Fixnum#to_sym, Float#to_s, String#to_s, String#to_str,\n" +
            "     String#to_sym, Regexp#to_s, Benchmark::Tms#to_s,\n" +
            "     Process::Status#to_s, Enumerable#to_set";

    public final void testParseMultipleMatches() {
        List<Member> methods = org.jedit.ruby.ri.RDocSeacher.parseMultipleResults(RESULT);
        assertEquals("Assert first method correct: ", "Array#to_s", methods.get(0).getFullName());
        assertEquals("Assert first method correct: ", "Vector#to_s", methods.get(35).getFullName());
    }
}
