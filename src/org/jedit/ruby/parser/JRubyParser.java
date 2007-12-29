package org.jedit.ruby.parser;

import org.jruby.ast.*;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.RubyParserConfiguration;
import org.jruby.parser.DefaultRubyParser;
import org.jruby.parser.RubyParserResult;
import org.jruby.common.NullWarnings;
import org.jruby.common.IRubyWarnings;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.RubyPlugin;

import java.util.List;
import java.io.StringReader;
import java.io.Reader;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class JRubyParser {

    private static final JRubyParser instance = new JRubyParser();

    private static String found = "found";
    private static String expected = "expected";
    private static String nothing = "nothing";

    private IRubyWarnings warnings;

    /** singleton private constructor */
    private JRubyParser() {
    }

    public static void setFoundLabel(String found) {
        JRubyParser.found = found;
    }

    public static void setExpectedLabel(String expected) {
        JRubyParser.expected = expected;
    }

    public static void setNothingLabel(String nothing) {
        JRubyParser.nothing = nothing;
    }

    static List<Member> getMembers(String text, List<Member> methodMembers, List<RubyParser.WarningListener> listeners, String filePath, LineCounter lineCounter) {
        return instance.parse(text, listeners, methodMembers, filePath, lineCounter);
    }

    private List<Member> parse(String text, List<RubyParser.WarningListener> listeners, List<Member> methodMembers, String filePath, LineCounter lineCounter) {
        this.warnings = new Warnings(listeners);

        Reader content = new StringReader(text);
        RubyNodeVisitor visitor = new RubyNodeVisitor(lineCounter, methodMembers, listeners);
        List<Member> members;

        try {
            Node node = parse(filePath, content, new RubyParserConfiguration());
            if (node != null) {
                node.accept(visitor);
            }
            members = visitor.getMembers();
        } catch (SyntaxException e) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.error(e.getPosition(), e.getMessage());
            }
            String message = e.getPosition().getEndLine() + ": " + e.getMessage();
            RubyPlugin.log(message, getClass());
            members = null;
        }

        return members;
    }

    private Node parse(String name, Reader content, RubyParserConfiguration config) {
        DefaultRubyParser parser = new DefaultRubyParser() {
            /** Hack to ensure we get original error message */
            public void yyerror(String message, String[] expected, String found) {
                try {
                    super.yyerror(message, expected, found);
                } catch (SyntaxException e) {
                    String errorMessage = formatErrorMessage(message, expected, found);
                    throw new SyntaxException(e.getPosition(), errorMessage);
                }
            }
        };

        parser.setWarnings(warnings);
        LexerSource lexerSource = LexerSource.getSource(name, content, 0, true);
        RubyParserResult result = parser.parse(config, lexerSource);
        return result.getAST();
    }

    private static String formatErrorMessage(String message, String[] expectedValues, String found) {
        if (message.equals("syntax error")) {
            message = "";
        }

        StringBuffer buffer = new StringBuffer(message);
        if (found != null) {
            buffer.append(JRubyParser.found).append(" ").append(reformatValue(found)).append("; ");
            buffer.append(expected).append(" ");
            if (expectedValues == null || expectedValues.length == 0) {
                buffer.append(nothing);
            } else {
                for (String value : expectedValues) {
                    value = reformatValue(value);
                    buffer.append(value).append(", ");
                }
            }
        }

        return buffer.toString();
    }

    private static String reformatValue(String value) {
        if (value.startsWith("k")) {
            value = "'" + value.substring(1).toLowerCase() + "'";
        } else if (value.startsWith("t")) {
            value = value.substring(1).toLowerCase();
        }
        return value;
    }

    private static final class Warnings extends NullWarnings {
        private final List<RubyParser.WarningListener> listeners;

        public Warnings(List<RubyParser.WarningListener> listeners) {
            this.listeners = listeners;
        }

        public final void warn(ISourcePosition position, String message) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warn(position, message);
            }
        }

        public final void warn(String message) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warn(message);
            }
        }

        public final void warning(ISourcePosition position, String message) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warning(position, message);
            }
        }

        public final void warning(String message) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warning(message);
            }
        }
    }

}