/*
 * RubyNodeVisitor.java - 
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
package org.jedit.ruby.parser;

import org.jruby.ast.visitor.AbstractVisitor;
import org.jruby.ast.*;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.evaluator.SingleNodeVisitor;
import org.jedit.ruby.ast.*;
import org.jedit.ruby.RubyPlugin;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author robmckinnon at users.sourceforge.net
 */
final class RubyNodeVisitor extends AbstractVisitor {

    private final List<String> namespaceNames;
    private final List<String> compositeNamespaceNames;
    private final LinkedList<Member> currentMember;
    private int methodIndex;

    private final List<Member> methods;
    private final List<RubyParser.WarningListener> problemListeners;
    private final LineCounter lineCounter;
    private final NameVisitor nameVisitor;
    private static final String CLASS = "class";
    private static final String MODULE = "module";

    public RubyNodeVisitor(LineCounter lineCounts, List<Member> methodMembers, List<RubyParser.WarningListener> listeners) {
        lineCounter = lineCounts;
        namespaceNames = new ArrayList<String>();
        compositeNamespaceNames = new ArrayList<String>();
        currentMember = new LinkedList<Member>();
        currentMember.add(new Root(RubyPlugin.getEndOfFileOffset()));
        nameVisitor = new NameVisitor();
        problemListeners = listeners;
        methods = methodMembers;
        methodIndex = 0;
    }

    public final List<Member> getMembers() {
        return currentMember.getFirst().getChildMembersAsList();
    }

    protected final SingleNodeVisitor visitNode(Node node) {
//        if (printNode(node)) {
//            String name = node.getClass().getName();
//            int index = name.lastIndexOf('.');
//            ISourcePosition position = node.getPosition();
//            if (position != null) {
//                System.out.print("Line " + position.getStartLine() +"-"+ position.getEndLine() +
//                        ": " + position.getStartOffset() + "-" + position.getEndOffset() +
//                        " " + name.substring(index + 1));
//            } else {
//                System.out.print("          Node: " + name.substring(index + 1));
//            }
//        }
        return null;
    }

    private static boolean printNode(Node node) {
        return !(node instanceof NewlineNode);
    }

    private void visitNodeIterator(Iterator iterator) {
        while (iterator.hasNext()) {
            Node node = (Node) iterator.next();
            visitNode(node);
            if (printNode(node)) {
                RubyPlugin.log("", getClass());
            }
            node.accept(this);
        }
    }

    public final SingleNodeVisitor visitBlockNode(BlockNode node) {
        visitNode(node);
        RubyPlugin.log("", getClass());
        visitNodeIterator(node.iterator());
        return null;
    }

    public final SingleNodeVisitor visitNewlineNode(NewlineNode node) {
        visitNode(node);
        node.getNextNode().accept(this);
        return null;
    }

    public final SingleNodeVisitor visitModuleNode(ModuleNode module) {
//        System.out.print("[");
        addParentNode(MODULE, module, module, module.getBodyNode());
//        System.out.print("]");
        return null;
    }

    public final SingleNodeVisitor visitClassNode(ClassNode classNode) {
        Member member = addParentNode(CLASS, classNode, classNode, classNode.getBodyNode());
        Node superNode = classNode.getSuperNode();

        if (superNode != null) {
            superNode.accept(nameVisitor);
            StringBuffer name = new StringBuffer();
            for (String namespace : nameVisitor.namespaces) {
                name.append(namespace).append("::");
            }
            nameVisitor.namespaces.clear();
            name.append(nameVisitor.name);
            ((ClassMember)member).setSuperClassName(name.toString());
        }

        return null;
    }

    private Member addParentNode(String memberType, Node node, IScopingNode scopeNode, ScopeNode bodyNode) {
        visitNode(node);
        scopeNode.getCPath().accept(nameVisitor);
        String name = nameVisitor.name;
//        System.out.print(": " + name);

        Member member;
        if (memberType == MODULE) {
            member = new Module(name);
        } else {
            member = new ClassMember(name);
        }

        member = populateOffsets(member, node.getPosition(), memberType);

        int colonNameCount = nameVisitor.namespaces.size();
        for (String namespace : nameVisitor.namespaces) {
            compositeNamespaceNames.add(namespace);
            namespaceNames.add(namespace);
        }
        nameVisitor.namespaces.clear();
        populateNamespace(member);
        compositeNamespaceNames.clear();

        namespaceNames.add(name);
        Member parent = currentMember.getLast();
        parent.addChildMember(member);
        currentMember.add(member);

        bodyNode.accept(this);

        namespaceNames.remove(name);
        while (colonNameCount > 0) {
            namespaceNames.remove(namespaceNames.size() - 1);
            colonNameCount--;
        }
        currentMember.removeLast();
        return member;
    }

    public SingleNodeVisitor visitArgsCatNode(ArgsCatNode node) {
        visitNode(node);
        return null;
    }

    public SingleNodeVisitor visitArgsNode(ArgsNode node) {
        visitNode(node);
        node.accept(this);
        return null;
    }

    public final SingleNodeVisitor visitDefnNode(DefnNode node) {
        visitNode(node);
//        System.out.print(": " + node.getName());

        Member method;
        try {
            method = getMember("def", methodIndex, methods, node.getName(), node.getPosition());
        } catch (IndexAdjustmentException e) {
            methodIndex = e.getIndex();
            method = getMethodNoCheckedException(methodIndex, methods, node.getName(), node.getPosition());
        }
        methodIndex++;
        Member parent = currentMember.getLast();
        parent.addChildMember(method);
        return null;
    }

    private Member getMethodNoCheckedException(int index, List<Member> members, String memberName, ISourcePosition position) {
        try {
            return getMember("def", index, members, memberName, position);
        } catch (IndexAdjustmentException e) {
            return throwCantFindException("def", memberName, position);
        }
    }

    public final SingleNodeVisitor visitDefsNode(DefsNode node) {
        visitNode(node);

        Method method = (Method)methods.get(methodIndex++);
        method.setStartOffset(getStartOffset(node.getPosition()));
        method.setEndOffset(getEndOffset(node.getPosition()));
        method.setStartOuterOffset(getOuterOffset(method, "def "));
        String methodName = node.getName();
        String receiverName;
        if (node.getReceiverNode() instanceof ConstNode) {
            ConstNode constNode = (ConstNode) node.getReceiverNode();
            receiverName = constNode.getName();
            method.setReceiver(receiverName);
            method.setName(methodName);
            method.setClassMethod(true);
        } else {
            receiverName = "";
        }
        RubyPlugin.log(": " + receiverName + methodName, getClass());

        currentMember.getLast().addChildMember(method);
        currentMember.add(method);

        node.getBodyNode().accept(this);

        currentMember.removeLast();
        return null;
    }

    private void populateNamespace(Member member) {
        StringBuffer namespace = new StringBuffer();
        if (namespaceNames.size() > 0) {
            for (String name : namespaceNames) {
                namespace.append(name).append("::");
            }
            member.setNamespace(namespace.toString());
        }
        if (compositeNamespaceNames.size() > 0) {
            namespace = new StringBuffer();
            for (String name : compositeNamespaceNames) {
                namespace.append(name).append("::");
            }
            member.setCompositeNamespace(namespace.toString());
        }
    }

    private Member getMember(String memberType, int index, List<Member> members, String memberName, ISourcePosition position) throws IndexAdjustmentException {
        Member member;
        try {
            member = members.get(index);
            if (!memberName.equals(member.getShortName())) {
                index++;
                while(index < members.size()) {
                    member = members.get(index);
                    if (memberName.equals(member.getShortName())) {
                        throw new IndexAdjustmentException(index);
                    } else {
                        index++;
                    }
                }
                throw new Exception();
            }
        } catch (Exception e) {
            if (e instanceof IndexAdjustmentException) {
                throw (IndexAdjustmentException)e;
            } else {
                return throwCantFindException(memberType, memberName, position);
            }
        }
        return populateOffsets(member, position, memberType);
    }

    private Member populateOffsets(Member member, ISourcePosition position, String memberType) {
        member.setStartOffset(getStartOffset(position));
        member.setEndOffset(getEndOffset(position));
        member.setStartOuterOffset(getOuterOffset(member, memberType+" "));
        return member;
    }

    private static int getStartOffset(ISourcePosition position) {
        return position.getStartOffset() + 1;
    }

    private Member throwCantFindException(String memberType, String memberName, ISourcePosition position) {
        String message = "parser can't find " + memberType + " " + memberName;
        for (RubyParser.WarningListener listener : problemListeners) {
            listener.error(position, message);
        }
        throw new SyntaxException(position, message);
    }

    public final SingleNodeVisitor visitScopeNode(ScopeNode node) {
        visitNode(node);
        RubyPlugin.log("", getClass());
        if (node.getBodyNode() != null) {
            node.getBodyNode().accept(this);
        }
        return null;
    }

    public final SingleNodeVisitor visitIfNode(IfNode node) {
        visitNode(node);
        if (node.getThenBody() != null) {
            node.getThenBody().accept(this);
        }
        return null;
    }

    public final SingleNodeVisitor visitIterNode(IterNode node) {
        visitNode(node);
        RubyPlugin.log("", getClass());
        if (node.getBodyNode() != null) {
            node.getBodyNode().accept(this);
        }
        return null;
    }

    public final SingleNodeVisitor visitFCallNode(FCallNode node) {
        visitNode(node);
        RubyPlugin.log(": " + node.getName(), getClass());
        return null;
    }

    public final SingleNodeVisitor visitClassVarDeclNode(ClassVarDeclNode node) {
        visitNode(node);
        RubyPlugin.log(": " + node.getName(), getClass());
        return null;
    }

    public final SingleNodeVisitor visitClassVarAsgnNode(ClassVarAsgnNode node) {
        visitNode(node);
        RubyPlugin.log(": " + node.getName(), getClass());
        return null;
    }

    private int getOuterOffset(Member clas, String keyword) {
        int startOffset = clas.getStartOffset();
        int index = lineCounter.getLineAtOffset(startOffset);
        String line = lineCounter.getLineUpTo(index, startOffset);
        return line.lastIndexOf(keyword) + lineCounter.getStartOffset(index);
    }

    private int getEndOffset(ISourcePosition position) {
        int end = position.getEndOffset();
        if (lineCounter.charAt(end - 3) == 'e'
                && lineCounter.charAt(end - 2) == 'n'
                && lineCounter.charAt(end - 1) == 'd') {
            return end;
        }

        char endChar = lineCounter.charAt(end);
        switch (endChar) {
            case 'd': return end + 1;
            case 'n': return end + 2;
            case 'e': return end + 3;
        }
        int endLine = position.getEndLine();
        String line = lineCounter.getLine(endLine);
        int start = lineCounter.getStartOffset(endLine);
        int beginIndex = end - start;
        String text = line.substring(beginIndex);
        return text.indexOf("end") + 3 + beginIndex;
    }

    private static final class IndexAdjustmentException extends Exception {
        private final int index;

        public IndexAdjustmentException(int index) {
            this.index = index;
        }

        public final int getIndex() {
            return index;
        }
    }

    private static final class NameVisitor extends AbstractVisitor {
        private final List<String> namespaces;
        private String name;
        private int visits;

        public NameVisitor() {
            namespaces = new ArrayList<String>();
            visits = 0;
        }

        protected final SingleNodeVisitor visitNode(Node node) {
            return null;
        }

        public final SingleNodeVisitor visitColon2Node(Colon2Node node) {
            visits++;
            if (node.getLeftNode() != null) {
                node.getLeftNode().accept(this);
            }
            visits--;

            if (visits == 0) {
                name = node.getName();
            } else {
                namespaces.add(node.getName());
            }
            return null;
        }

        public SingleNodeVisitor visitConstNode(ConstNode node) {
            if (visits == 0) {
                name = node.getName();
            } else {
                namespaces.add(node.getName());
            }
            return null;
        }
    }
}
