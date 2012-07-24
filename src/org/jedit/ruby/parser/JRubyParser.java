package org.jedit.ruby.parser;

import org.jrubyparser.SourcePosition;
import org.jrubyparser.ast.*;
//import org.jruby.lexer.yacc.LexerSource;
import org.jrubyparser.lexer.LexerSource;
import org.jrubyparser.lexer.SyntaxException;
//import org.jruby.parser.DefaultRubyParser;
//import org.jruby.parser.RubyParserResult;
import org.jrubyparser.parser.ParserConfiguration;
import org.jrubyparser.Parser.NullWarnings;
import org.jrubyparser.IRubyWarnings;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.RubyPlugin;
import org.jrubyparser.parser.ParserResult;
import org.jrubyparser.parser.Ruby19Parser;

import java.io.IOException;
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
        RubyNodeRubyVisitor visitor = new RubyNodeRubyVisitor(lineCounter, methodMembers, listeners);
        List<Member> members;

        try {
            Node node = parse(filePath, content, new ParserConfiguration());
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
       } catch (IOException e) {
            RubyPlugin.log(e.getMessage(), getClass());
            members = null;
        }

        return members;
    }

    private Node parse(String name, Reader content, ParserConfiguration config) throws IOException {
//        DefaultRubyParser parser = new DefaultRubyParser() {
//            /** Hack to ensure we get original error message */
//            public void yyerror(String message, String[] expected, String found) {
//                try {
//                    super.yyerror(message, expected, found);
//                } catch (SyntaxException e) {
//                    String errorMessage = formatErrorMessage(message, expected, found);
//                    throw new SyntaxException(e.getPosition(), errorMessage);
//                }
//            }
//        };

        Ruby19Parser parser = new Ruby19Parser();
        parser.setWarnings(warnings);
        LexerSource lexerSource = LexerSource.getSource(name, content, config);
        ParserResult result = parser.parse(config, lexerSource);
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

    private static final class Warnings implements IRubyWarnings {
        private final List<RubyParser.WarningListener> listeners;

        public Warnings(List<RubyParser.WarningListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void warn(ID id, SourcePosition position, String message, Object... data) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warn(id, position, message);
            }
        }

        @Override
        public void warn(ID id, String fileName, int lineNumber, String message, Object... data) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warn(id, fileName, lineNumber, message, data);
            }
        }

        @Override
        public boolean isVerbose() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void warn(ID id, String message, Object... data) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warn(id, message, data);
            }
        }

        @Override
        public void warning(ID id, String message, Object... data) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warning(id, message, data);
            }
        }

        @Override
        public void warning(ID id, SourcePosition position, String message, Object... data) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warning(id, position, message, data);
            }
        }

        @Override
        public void warning(ID id, String fileName, int lineNumber, String message, Object... data) {
            for (RubyParser.WarningListener listener : listeners) {
                listener.warning(id, fileName, lineNumber, message, data);
            }
        }

    }

}