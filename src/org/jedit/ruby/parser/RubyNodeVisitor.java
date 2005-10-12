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
import org.jruby.lexer.yacc.SourcePosition;
import org.jruby.lexer.yacc.SyntaxException;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Root;
import org.jedit.ruby.ast.Method;
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
    private final LinkedList<Member> currentMember;
    private int moduleIndex;
    private int classIndex;
    private int methodIndex;

    private final List<Member> modules;
    private final List<Member> classes;
    private final List<Member> methods;
    private final List<RubyParser.WarningListener> problemListeners;
    private final LineCounter lineCounter;
    private final NameVisitor nameVisitor;

    public RubyNodeVisitor(LineCounter lineCounts, List<Member> moduleMembers, List<Member> classMembers, List<Member> methodMembers, List<RubyParser.WarningListener> listeners) {
        lineCounter = lineCounts;
        namespaceNames = new ArrayList<String>();
        currentMember = new LinkedList<Member>();
        currentMember.add(new Root(RubyPlugin.getEndOfFileOffset()));
        nameVisitor = new NameVisitor();
        problemListeners = listeners;
        modules = moduleMembers;
        classes = classMembers;
        methods = methodMembers;
        moduleIndex = 0;
        classIndex = 0;
        methodIndex = 0;
    }

    public final List<Member> getMembers() {
        return currentMember.getFirst().getChildMembersAsList();
    }

    protected final void visitNode(Node node) {
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

    public final void visitBlockNode(BlockNode node) {
        visitNode(node);
        RubyPlugin.log("", getClass());
        visitNodeIterator(node.iterator());
    }

    public final void visitNewlineNode(NewlineNode node) {
        visitNode(node);
        node.getNextNode().accept(this);
    }

    public final void visitModuleNode(ModuleNode node) {
        System.out.print("[");
        visitNode(node);
        node.getCPath().accept(nameVisitor);
        String moduleName = nameVisitor.name;
        System.out.print(": " + moduleName);

        Member module;
        try {
            module = getMember("module", moduleIndex, modules, moduleName, node.getPosition());
        } catch (IndexAdjustmentException e) {
            moduleIndex = e.getIndex();
            module = getMemberNoCheckedException("module", moduleIndex, modules, moduleName, node.getPosition());
        }
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
        RubyPlugin.log("]", getClass());
    }

    public final void visitClassNode(ClassNode node) {
        visitNode(node);
        node.getCPath().accept(nameVisitor);
        String className = nameVisitor.name;
        System.out.print(": " + className);

        Member clas;
        try {
            clas = getMember("class", classIndex, classes, className, node.getPosition());
        } catch (IndexAdjustmentException e) {
            classIndex = e.getIndex();
            clas = getMemberNoCheckedException("class", classIndex, classes, className, node.getPosition());
        }
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

    public final void visitDefnNode(DefnNode node) {
        visitNode(node);
        String methodName = node.getName();
        System.out.print(": " + methodName);

        Member method;
        try {
            method = getMember("method", methodIndex, methods, node.getName(), node.getPosition());
        } catch (IndexAdjustmentException e) {
            methodIndex = e.getIndex();
            method = getMemberNoCheckedException("method", methodIndex, methods, node.getName(), node.getPosition());
        }
        methodIndex++;
        method.setEndOffset(getEndOffset(node));
        Member parent = currentMember.getLast();
        parent.addChildMember(method);
    }

    public final void visitDefsNode(DefsNode node) {
        visitNode(node);

        Method method = (Method)methods.get(methodIndex++);
        method.setEndOffset(getEndOffset(node));

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

    private Member getMemberNoCheckedException(String memberType, int index, List<Member> members, String memberName, SourcePosition position) {
        try {
            return getMember(memberType, index, members, memberName, position);
        } catch (IndexAdjustmentException e) {
            return throwCantFindException(memberType, memberName, position);
        }
    }

    private Member getMember(String memberType, int index, List<Member> members, String memberName, SourcePosition position) throws IndexAdjustmentException {
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
        return member;
    }

    private Member throwCantFindException(String memberType, String memberName, SourcePosition position) {
        String message = "parser can't find " + memberType + " " + memberName;
        for (RubyParser.WarningListener listener : problemListeners) {
            listener.error(position, message);
        }
        throw new SyntaxException(position, message);
    }

    public final void visitScopeNode(ScopeNode node) {
        visitNode(node);
        RubyPlugin.log("", getClass());
        if (node.getBodyNode() != null) {
            node.getBodyNode().accept(this);
        }
    }

    public final void visitIfNode(IfNode node) {
        visitNode(node);
        if (node.getThenBody() != null) {
            node.getThenBody().accept(this);
        }
    }

    public final void visitIterNode(IterNode node) {
        visitNode(node);
        RubyPlugin.log("", getClass());
        if (node.getBodyNode() != null) {
            node.getBodyNode().accept(this);
        }
    }

    public final void visitFCallNode(FCallNode node) {
        visitNode(node);
        RubyPlugin.log(": " + node.getName(), getClass());
    }

    public final void visitClassVarDeclNode(ClassVarDeclNode node) {
        visitNode(node);
        RubyPlugin.log(": " + node.getName(), getClass());
    }

    public final void visitClassVarAsgnNode(ClassVarAsgnNode node) {
        visitNode(node);
        RubyPlugin.log(": " + node.getName(), getClass());
    }

    private int getEndOffset(Node node) {
        int endLine = getEndLine(node);
        return lineCounter.getEndOffset(endLine);
    }

    private int getEndLine(Node node) {
        int line = node.getPosition().getLine() - 1;
        String text = lineCounter.getLine(line);
        if (text.indexOf("end") == -1 && text.indexOf("}") == -1) {
            line = node.getPosition().getLine();
        }
        return line;
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
        private String name;

        protected final void visitNode(Node node) {
        }

        public final void visitColon2Node(Colon2Node node) {
            this.name = node.getName();
        }
    }
}
