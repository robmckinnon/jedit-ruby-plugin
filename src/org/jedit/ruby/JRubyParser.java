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
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;

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

    public static List<Member> getMembers(String text, List<Member> moduleMembers, List<Member> classMembers, List<Member> methodMembers, List<RubyParser.WarningListener> listeners, String filePath) {
        Reader content = new StringReader(text);
        List<Member> members;
        Parser parser = new Parser(listeners);
        RubyNodeVisitor visitor = new RubyNodeVisitor(moduleMembers, classMembers, methodMembers, listeners);

        try {
            Node node = parser.parse(filePath, content);
            if (node != null) {
                node.accept(visitor);
            }
            members = visitor.getMembers();
        } catch (SyntaxException e) {
            System.out.println(e.getPosition().getLine() + ": " + e.getMessage());
            members = null; // listeners already informed of syntax error
        }

        return members;
    }

    private static class RubyNodeVisitor extends AbstractVisitor {

        private List<String> namespaceNames;
        private LinkedList<Member> currentMember;
        private int moduleIndex;
        private int classIndex;
        private int methodIndex;

        private List<Member> modules;
        private List<Member> classes;
        private List<Member> methods;
        private List<RubyParser.WarningListener> problemListeners;

        public RubyNodeVisitor(List<Member> moduleMembers, List<Member> classMembers, List<Member> methodMembers, List<RubyParser.WarningListener> listeners) {
            namespaceNames = new ArrayList<String>();
            currentMember = new LinkedList<Member>();
            currentMember.add(new Member.Root(getEndOfFileOffset()));
            problemListeners = listeners;
            modules = moduleMembers;
            classes = classMembers;
            methods = methodMembers;
            moduleIndex = 0;
            classIndex = 0;
            methodIndex = 0;
        }

        private int getEndOfFileOffset() {
            View view = jEdit.getActiveView();
            int offset = 0;
            if (view != null) {
                Buffer buffer = view.getBuffer();
                offset = buffer.getLineEndOffset(buffer.getLineCount() - 1);
            }
            return offset;
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

            Member module = getMember("module", moduleIndex, modules, node.getName(), node.getPosition());
            moduleIndex++;
            module.setEndOffset(getEndOffset(node));
            populateNamespace(module);

            namespaceNames.add(moduleName);
            Member parent = currentMember.getLast();
            parent.addChildMember(module);
            currentMember.add(module);

            node.getBodyNode().accept(this);

            namespaceNames.remove(moduleName);
            currentMember.removeLast();
            System.out.println("]");
        }

        public void visitClassNode(ClassNode node) {
            visitNode(node);
            String className = node.getClassName();
            System.out.print(": " + className);

            Member clas = getMember("class", classIndex, classes, node.getClassName(), node.getPosition());
            classIndex++;
            clas.setEndOffset(getEndOffset(node));
            populateNamespace(clas);

            namespaceNames.add(className);
            Member parent = currentMember.getLast();
            parent.addChildMember(clas);
            currentMember.add(clas);

            node.getBodyNode().accept(this);

            namespaceNames.remove(className);
            currentMember.removeLast();
        }

        public void visitDefnNode(DefnNode node) {
            visitNode(node);
            String methodName = node.getName();
            System.out.print(": " + methodName);

            Member method = getMember("method", methodIndex, methods, node.getName(), node.getPosition());
            methodIndex++;
            method.setEndOffset(getEndOffset(node));
            Member parent = currentMember.getLast();
            parent.addChildMember(method);

        }

        public void visitDefsNode(DefsNode node) {
            visitNode(node);

            Member method = methods.get(methodIndex++);
            method.setEndOffset(getEndOffset(node));

            String methodName = node.getName();
            String receiverName;
            if (node.getReceiverNode() instanceof ConstNode) {
                ConstNode constNode = (ConstNode) node.getReceiverNode();
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

        private void populateNamespace(Member member) {
            if (namespaceNames.size() > 0) {
                String namespace = "";
                for (String module : namespaceNames) {
                    namespace += module + "::";
                }
                member.setNamespace(namespace);
            }
        }

        private Member getMember(String memberType, int index, List<Member> members, String memberName, SourcePosition position) {
            Member member = null;
            try {
                member = members.get(index);
                if(!memberName.equals(member.getShortName())) {
                    throw new Exception();
                }
            } catch (Exception e) {
                String message = "parser can't find " + memberType + " " + memberName;
                for (RubyParser.WarningListener listener : problemListeners) {
                    listener.error(position, message);
                }
                throw new SyntaxException(position, message);
            }
            return member;
        }

        public void visitScopeNode(ScopeNode node) {
            visitNode(node);
            System.out.println("");
            if (node.getBodyNode() != null) {
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

        private int getEndOffset(Node node) {
            View view = jEdit.getActiveView();
            int offset = 0;
            if (view != null) {
                int line = node.getPosition().getLine() - 1;
                try {
                    offset = view.getBuffer().getLineEndOffset(line) - 1;
                } catch (ArrayIndexOutOfBoundsException e) {
                    // todo get offset some other way
                }
            }
            return offset;
        }

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

    private static class Parser {

        private RubyWarnings warnings;
        private List<RubyParser.WarningListener> listeners;

        public Parser(List<RubyParser.WarningListener> listeners) {
            this(new Warnings(listeners));
            this.listeners = listeners;
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

    }
}
