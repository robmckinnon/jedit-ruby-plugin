package org.jedit.ruby.parser;

import org.jruby.ast.*;
import org.jruby.lexer.yacc.SourcePosition;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.parser.RubyParserConfiguration;
import org.jruby.parser.DefaultRubyParser;
import org.jruby.parser.RubyParserResult;
import org.jruby.parser.SyntaxErrorState;
import org.jruby.common.RubyWarnings;
import org.jruby.common.NullWarnings;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.RubyPlugin;

import java.util.List;
import java.io.StringReader;
import java.io.Reader;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class JRubyParser {

    private static final JRubyParser instance = new JRubyParser();

    private static String found;
    private static String expected;
    private static String nothing;

    private RubyWarnings warnings;
    private List<RubyParser.WarningListener> listeners;

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

    static List<Member> getMembers(String text, List<Member> moduleMembers, List<Member> classMembers, List<Member> methodMembers, List<RubyParser.WarningListener> listeners, String filePath) {
        return instance.parse(text, listeners, moduleMembers, classMembers, methodMembers, filePath);
    }

    private List<Member> parse(String text, List<RubyParser.WarningListener> listeners, List<Member> moduleMembers, List<Member> classMembers, List<Member> methodMembers, String filePath) {
        this.listeners = listeners;
        this.warnings = new Warnings(listeners);

        Reader content = new StringReader(text);
        List<Member> members;
        RubyNodeVisitor visitor = new RubyNodeVisitor(moduleMembers, classMembers, methodMembers, listeners);

        try {
            Node node = parse(filePath, content);
            if (node != null) {
                node.accept(visitor);
            }
            members = visitor.getMembers();
        } catch (SyntaxException e) {
            RubyPlugin.log(e.getPosition().getLine() + ": " + e.getMessage());
            members = null; // listeners already informed of syntax error
        }

        return members;
    }

    private Node parse(String name, Reader content) {
        return parse(name, content, new RubyParserConfiguration());
    }

    private Node parse(String name, Reader content, RubyParserConfiguration config) {
        DefaultRubyParser parser = new DefaultRubyParser() {
            public void yyerror(String message, Object syntaxErrorState) {
                try {
                    super.yyerror(message, syntaxErrorState);
                } catch (SyntaxException e) {
                    String errorMessage = formatErrorMessage(message, syntaxErrorState);
                    for (RubyParser.WarningListener listener : listeners) {
                        listener.error(e.getPosition(), errorMessage);
                    }
                    throw e;
                }
            }
        };

        parser.setWarnings(warnings);
        parser.init(config);
        LexerSource lexerSource = LexerSource.getSource(name, content);
        RubyParserResult result = parser.parse(lexerSource);
        return result.getAST();
    }

    private String formatErrorMessage(String message, Object syntaxErrorState) {
        if (message.equals("syntax error")) {
            message = "";
        } else {
            message += ": ";
        }

        StringBuffer buffer = new StringBuffer(message);
        if (syntaxErrorState instanceof SyntaxErrorState) {
            SyntaxErrorState errorState = (SyntaxErrorState) syntaxErrorState;
            String[] expectedValues = errorState.expected();
            String found = errorState.found();
            if (found != null) {
                buffer.append(JRubyParser.found + " " + reformatValue(found) + "; ");
            }
            buffer.append(expected + " ");
            if (expectedValues.length == 0) {
                buffer.append(nothing);
            } else {
                for (String value : expectedValues) {
                    value = reformatValue(value);
                    buffer.append(value + ", ");
                }
            }
        }
        return buffer.toString();
    }

    private String reformatValue(String value) {
        if (value.startsWith("k")) {
            value = "'" + value.substring(1).toLowerCase() + "'";
        } else if (value.startsWith("t")) {
            value = value.substring(1).toLowerCase();
        }
        return value;
    }

    private static class Warnings extends NullWarnings {
        private List<RubyParser.WarningListener> listeners;

        public Warnings(List<RubyParser.WarningListener> listeners) {
            this.listeners = listeners;
        }

        public void warn(SourcePosition position, String message) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warn(position, message);
            }
        }

        public void warn(String message) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warn(message);
            }
        }

        public void warning(SourcePosition position, String message) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warning(position, message);
            }
        }

        public void warning(String message) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warning(message);
            }
        }
    }

}
