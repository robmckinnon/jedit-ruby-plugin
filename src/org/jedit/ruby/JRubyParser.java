package org.jedit.ruby;

import org.jruby.ast.visitor.AbstractVisitor;
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

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.io.StringReader;
import java.io.Reader;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class JRubyParser {

    private static String found;
    private static String expected;
    private static String nothing;

    public static void setFoundLabel(String found) {
        JRubyParser.found = found;
    }

    public static void setExpectedLabel(String expected) {
        JRubyParser.expected = expected;
    }

    public static void setNothingLabel(String nothing) {
        JRubyParser.nothing = nothing;
    }

    public static List<Member> getMembers(String ruby, List<Member> moduleMembers, List<Member> classMembers, List<Member> methodMembers, RubyParser.WarningListener listener) {
        List<Member> members;
        Parser parser = new Parser(listener);
        MockNodeVisitor visitor = new MockNodeVisitor(moduleMembers, classMembers, methodMembers);

        try {
            Node node = parser.parse("", ruby);
            if (node != null) {
                node.accept(visitor);
            }
            members = visitor.getMembers();
        } catch (SyntaxException e) {
            members = null;
        }

        return members;
    }

    private static class MockNodeVisitor extends AbstractVisitor {

        private List<String> moduleNames;
        private LinkedList<Member> currentMember;
        private int moduleIndex;
        private int classIndex;
        private int methodIndex;

        private List<Member> modules;
        private List<Member> classes;
        private List<Member> methods;

        public MockNodeVisitor() {
        }

        public MockNodeVisitor(List<Member> moduleMembers, List<Member> classMembers, List<Member> methodMembers) {
            moduleNames = new ArrayList<String>();
            currentMember = new LinkedList<Member>();
            currentMember.add(new Member.Root());
            modules = moduleMembers;
            classes = classMembers;
            methods = methodMembers;
            moduleIndex = 0;
            classIndex = 0;
            methodIndex = 0;
        }

        public List<Member> getMembers() {
            return currentMember.getFirst().getChildMembersAsList();
        }

        protected void visitNode(Node node) {
            if (printNode(node)) {
                String name = node.getClass().getName();
                int index = name.lastIndexOf('.');
                SourcePosition position = node.getPosition();
                if (position != null) {
                    System.out.print("Line " + position.getLine() + ": " + name.substring(index + 1));
                } else {
                    System.out.print("          Node: " + name.substring(index + 1));
                }
            }
        }

        private boolean printNode(Node node) {
            return !(node instanceof NewlineNode);
        }

        private void visitNodeIterator(Iterator iterator) {
            while (iterator.hasNext()) {
                Node node = (Node) iterator.next();
                visitNode(node);
                if (printNode(node)) {
                    System.out.println("");
                }
                node.accept(this);
            }
        }

        public void visitBlockNode(BlockNode node) {
            visitNode(node);
            System.out.println("");
            visitNodeIterator(node.iterator());
        }

        public void visitNewlineNode(NewlineNode node) {
            visitNode(node);
            node.getNextNode().accept(this);
        }

        public void visitModuleNode(ModuleNode node) {
            System.out.print("[");
            visitNode(node);
            String moduleName = node.getName();
            System.out.print(": " + moduleName);
            moduleNames.add(moduleName);

            Member module = modules.get(moduleIndex++);
            currentMember.getLast().addChildMember(module);
            currentMember.add(module);

            node.getBodyNode().accept(this);

            moduleNames.remove(moduleName);
            currentMember.removeLast();
            System.out.println("]");
        }

        public void visitClassNode(ClassNode node) {
            visitNode(node);
            String className = node.getClassName();
            System.out.print(": " + className);

            Member clas = classes.get(classIndex++);
            populateNamespace(clas);
            currentMember.getLast().addChildMember(clas);
            currentMember.add(clas);

            node.getBodyNode().accept(this);

            currentMember.removeLast();
        }

        public void visitDefnNode(DefnNode node) {
            visitNode(node);
            String methodName = node.getName();
            System.out.print(": " + methodName);

            Member method = methods.get(methodIndex++);
            currentMember.getLast().addChildMember(method);

        }

        public void visitDefsNode(DefsNode node) {
            visitNode(node);

            Member method = methods.get(methodIndex++);

            String methodName = node.getName();
            String receiverName;
            if(node.getReceiverNode() instanceof ConstNode) {
                ConstNode constNode = (ConstNode)node.getReceiverNode();
                receiverName = constNode.getName();
                method.setReceiver(receiverName);
            } else {
                receiverName = "";
            }
            System.out.println(": " + receiverName + methodName);

            currentMember.getLast().addChildMember(method);
            currentMember.add(method);

            node.getBodyNode().accept(this);

            currentMember.removeLast();
        }

        private void populateNamespace(Member clas) {
            if(moduleNames.size() > 0) {
                String namespace = "";
                for(String module : moduleNames) {
                    namespace += module + "::";
                }
                clas.setNamespace(namespace);
            }
        }

        public void visitScopeNode(ScopeNode node) {
            visitNode(node);
            System.out.println("");
            if(node.getBodyNode() != null) {
                node.getBodyNode().accept(this);
            }
        }

        public void visitIterNode(IterNode node) {
            visitNode(node);
            System.out.println("");
            node.getBodyNode().accept(this);
        }

        public void visitFCallNode(FCallNode node) {
            visitNode(node);
            System.out.println(": " + node.getName());
        }

        public void visitClassVarDeclNode(ClassVarDeclNode node) {
            visitNode(node);
            System.out.println(": " + node.getName());
        }

        public void visitClassVarAsgnNode(ClassVarAsgnNode node) {
            visitNode(node);
            System.out.println(": " + node.getName());
        }

    }

    private static class Warnings extends NullWarnings {
        private RubyParser.WarningListener listener;

        public Warnings(RubyParser.WarningListener listener) {
            this.listener = listener;
        }

        public void warn(SourcePosition position, String message) {
            listener.warn(position, message);
        }

        public void warn(String message) {
            listener.warn(message);
        }

        public void warning(SourcePosition position, String message) {
            listener.warning(position, message);
        }

        public void warning(String message) {
            listener.warning(message);
        }
    }

    private static class Parser {

        private RubyWarnings warnings;
        private RubyParser.WarningListener listener;

        public Parser(RubyParser.WarningListener listener) {
            this(new Warnings(listener));
            this.listener = listener;
        }

        public Parser(RubyWarnings warnings) {
            this.warnings = warnings;
        }

        public Node parse(String name, String content) {
            return parse(name, new StringReader(content));
        }

        public Node parse(String name, Reader content) {
            return parse(name, content, new RubyParserConfiguration());
        }

        private Node parse(String name, Reader content, RubyParserConfiguration config) {
            DefaultRubyParser parser = new DefaultRubyParser() {
                public void yyerror(String message, Object syntaxErrorState) {
                    try {
                        super.yyerror(message, syntaxErrorState);
                    } catch (SyntaxException e) {
                        String errorMessage = formatErrorMessage(message, syntaxErrorState);
                        listener.error(e.getPosition(), errorMessage);
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
            if(syntaxErrorState instanceof SyntaxErrorState) {
                SyntaxErrorState errorState = (SyntaxErrorState)syntaxErrorState;
                String[] expectedValues = errorState.expected();
                buffer.append(found + " " + reformatValue(errorState.found()));
                buffer.append("; " + expected + " ");
                if(expectedValues.length == 0) {
                    buffer.append(nothing);
                } else {
                    for(String value : expectedValues) {
                        value = reformatValue(value);
                        buffer.append(value + ", ");
                    }
                }
            }
            return buffer.toString();
        }

        private String reformatValue(String value) {
            if(value.startsWith("k")) {
                value = "'" + value.substring(1).toLowerCase() + "'";
            } else if(value.startsWith("t")) {
                value = value.substring(1).toLowerCase();
            }
            return value;
        }

    }
}
