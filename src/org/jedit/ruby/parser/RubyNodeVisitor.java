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
class RubyNodeVisitor extends AbstractVisitor {

    private List<String> namespaceNames;
    private LinkedList<Member> currentMember;
    private int moduleIndex;
    private int classIndex;
    private int methodIndex;

    private List<Member> modules;
    private List<Member> classes;
    private List<Member> methods;
    private List<RubyParser.WarningListener> problemListeners;
    private LineCounter lineCounter;

    public RubyNodeVisitor(String text, List<Member> moduleMembers, List<Member> classMembers, List<Member> methodMembers, List<RubyParser.WarningListener> listeners) {
        lineCounter = new LineCounter(text);
        namespaceNames = new ArrayList<String>();
        currentMember = new LinkedList<Member>();
        currentMember.add(new Root(RubyPlugin.getEndOfFileOffset()));
        problemListeners = listeners;
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
                RubyPlugin.log("", getClass());
            }
            node.accept(this);
        }
    }

    public void visitBlockNode(BlockNode node) {
        visitNode(node);
        RubyPlugin.log("", getClass());
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
        RubyPlugin.log("]", getClass());
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
        RubyPlugin.log("", getClass());
        if (node.getBodyNode() != null) {
            node.getBodyNode().accept(this);
        }
    }

    public void visitIfNode(IfNode node) {
        visitNode(node);
        if (node.getThenBody() != null) {
            node.getThenBody().accept(this);
        }
    }

    public void visitIterNode(IterNode node) {
        visitNode(node);
        RubyPlugin.log("", getClass());
        if (node.getBodyNode() != null) {
            node.getBodyNode().accept(this);
        }
    }

    public void visitFCallNode(FCallNode node) {
        visitNode(node);
        RubyPlugin.log(": " + node.getName(), getClass());
    }

    public void visitClassVarDeclNode(ClassVarDeclNode node) {
        visitNode(node);
        RubyPlugin.log(": " + node.getName(), getClass());
    }

    public void visitClassVarAsgnNode(ClassVarAsgnNode node) {
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
    
}
