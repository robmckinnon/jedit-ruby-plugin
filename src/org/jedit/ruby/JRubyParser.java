package org.jedit.ruby;

import org.jruby.ast.visitor.AbstractVisitor;
import org.jruby.ast.*;
import org.jruby.lexer.yacc.SourcePosition;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.parser.RubyParserPool;
import org.jruby.parser.RubyParserConfiguration;
import org.jruby.parser.DefaultRubyParser;
import org.jruby.parser.RubyParserResult;
import org.jruby.common.RubyWarnings;
import org.jruby.common.NullWarnings;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.StringReader;
import java.io.Reader;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class JRubyParser {

    public static Member[] getMembers(String ruby, Member[] members) {
        Parser parser = new Parser();
        Node node = parser.parse("", ruby);

        MockNodeVisitor visitor = new MockNodeVisitor(members);
        if (node != null) {
            node.accept(visitor);
        }
        return visitor.getMembers();
    }

    private static class MockNodeVisitor extends AbstractVisitor {

        private List<String> modules;
        private Member[] members;
        private int index;

        public MockNodeVisitor() {
        }

        public MockNodeVisitor(Member[] members) {
            modules = new ArrayList<String>();
            this.members = members;
            index = 0;
        }

        public Member[] getMembers() {
            return members;
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
            String module = node.getName();
            System.out.println(": " + module);
            modules.add(module);
            node.getBodyNode().accept(this);
            System.out.println("]");
            modules.remove(module);
        }

        public void visitClassNode(ClassNode node) {
            visitNode(node);
            String className = node.getClassName();
            System.out.println(": " + className);

            if(modules.size() > 0) {
                String namespace = "";
                for(String module : modules) {
                    namespace += module + "::";
                }
                members[index].setNamespace(namespace);
            }
            index++;
            node.getBodyNode().accept(this);
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

        public void visitDefnNode(DefnNode node) {
            visitNode(node);
            String method = node.getName();
            System.out.println(": " + method);
            index++;
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

        public void visitDefsNode(DefsNode node) {
            visitNode(node);
            System.out.println(": " + node.getName());
            node.getBodyNode().accept(this);
        }

    }

    private static class Parser {

        private final RubyParserPool pool;
        private RubyWarnings warnings;

        public Parser() {
            this(new NullWarnings());
        }

        public Parser(RubyWarnings warnings) {
            this.warnings = warnings;
            this.pool = RubyParserPool.getInstance();
        }

        public Node parse(String name, String content) {
            return parse(name, new StringReader(content));
        }

        public Node parse(String name, Reader content) {
            return parse(name, content, new RubyParserConfiguration());
        }

        private Node parse(String name, Reader content, RubyParserConfiguration config) {
            DefaultRubyParser parser = null;
            RubyParserResult result = null;
            try {
                parser = pool.borrowParser();
                parser.setWarnings(warnings);
                parser.init(config);
                LexerSource lexerSource = LexerSource.getSource(name, content);
                result = parser.parse(lexerSource);
            } catch (SyntaxException e) {
                throw e;
            } finally {
                pool.returnParser(parser);
            }
            return result.getAST();
        }

        /**
         * @param b
         */
        public static void setDebugging(boolean b) {
            // FIXME Enable/Disable debug logging!
        }

    }
}
