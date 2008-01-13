/*
 * TestRubyParser.java -
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
import org.jedit.ruby.cache.RubyCache;
import org.jruby.ast.Node;
import org.jruby.parser.RubyParserConfiguration;
import org.jruby.parser.DefaultRubyParser;
import org.jruby.parser.RubyParserResult;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.common.NullWarnings;

import java.io.Reader;
import java.io.StringReader;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class TestJRubyParser extends TestCase {

    private static final String ERROR_CODE = "fmodule Red class Head\n" +
            "  end\n" +
            "end";

    
    public void setUp() {
        RubyCache.resetCache();
    }

    public final void testErrors() {
        Reader content = new StringReader(ERROR_CODE);
        try {
            parse("/home/rob.rb", content, new RubyParserConfiguration());
        } catch (SyntaxException e) {
            assertCorrect(e.getPosition(), e.getMessage(), 2, "syntax error");
        }
    }

    private static void assertCorrect(ISourcePosition position, String message, int expectedLine, String expectedMessage) {
        assertEquals("Error position correct.", expectedLine, position.getEndLine());
        assertEquals("Error message correct.", expectedMessage, message);
    }

    private Node parse(String name, Reader content, RubyParserConfiguration config) {
        DefaultRubyParser parser = new DefaultRubyParser() {
            public void yyerror(String message) {
                assertEquals("assert error message correct", message, message);
                super.yyerror(message);
            }

            public void yyerror(String message, String[] expected, String found) {
                assertEquals("assert error message correct", "syntax error", message);
                assertEquals("assert expected correct", 0, expected.length);
                assertEquals("assert found correct", "kEND", found);
                super.yyerror(message, expected, found);
            }
        };

        parser.setWarnings(new Warnings());
        LexerSource lexerSource = LexerSource.getSource(name, content, 0, true);
        RubyParserResult result = parser.parse(config, lexerSource);
        return result.getAST();
    }

    private static void failTest(String message) {
        fail("Unexpected callback: " + message);
    }

    private final class Warnings extends NullWarnings {
        public Warnings() {
        }
        public final void warn(String message) {
            failTest(message);
        }
        public final void warning(String message) {
            failTest(message);
        }
        public void warn(ISourcePosition position, String message) {
            System.out.println(message);
        }
        public void warning(ISourcePosition position, String message) {
            System.out.println(message);
        }
    }
}