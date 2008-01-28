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
import org.jruby.evaluator.Instruction;
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

    private static final String CLASS = "class";
    private static final String MODULE = "module";

    private final List<String> namespaceNames;
    private final List<String> compositeNamespaceNames;
    private final LinkedList<Member> currentMember;
    private int methodIndex;

    private final List<Member> methods;
    private final List<RubyParser.WarningListener> problemListeners;
    private final LineCounter lineCounter;
    private final NameVisitor nameVisitor;
    private MethodCallWithSelfAsAnImplicitReceiver methodCall;
    private boolean inIfNode;
    private boolean underModuleNode;
    private Root root;

    public RubyNodeVisitor(LineCounter lineCounts, List<Member> methodMembers, List<RubyParser.WarningListener> listeners) {
        inIfNode = false;
        underModuleNode = false;
        lineCounter = lineCounts;
        namespaceNames = new ArrayList<String>();
        compositeNamespaceNames = new ArrayList<String>();
        currentMember = new LinkedList<Member>();
        root = new Root(RubyPlugin.getEndOfFileOffset());
        currentMember.add(root);
        nameVisitor = new NameVisitor();
        problemListeners = listeners;
        methods = methodMembers;
        methodCall = null;
        methodIndex = 0;
    }

    public final List<Member> getMembers() {
        return currentMember.getFirst().getChildMembersAsList();
    }

    protected final Instruction visitNode(Node node) {
        if (printNode()) {
            String name = node.getClass().getName();
            int index = name.lastIndexOf('.');
            ISourcePosition position = node.getPosition();
            if (position != null) {
                System.out.println("Line " + position.getStartLine() +"-"+ position.getEndLine() +
                        ": " + position.getStartOffset() + "-" + position.getEndOffset() +
                        " " + name.substring(index + 1));
            } else {
                System.out.print("          Node: " + name.substring(index + 1));
            }
        }
        return null;
    }

    private static boolean printNode() {
//        return !(node instanceof NewlineNode);
        return false;
    }

    private void visitNodeIterator(Iterator iterator) {
        while (iterator.hasNext()) {
            Node node = (Node) iterator.next();
            visitNode(node);
            if (printNode()) {
                RubyPlugin.log("", getClass());
            }
            node.accept(this);
        }
    }

    public final Instruction visitBlockNode(BlockNode node) {
        visitNode(node);
        RubyPlugin.log("", getClass());
        visitNodeIterator(node.childNodes().iterator());
        return null;
    }

    public final Instruction visitNewlineNode(NewlineNode node) {
        visitNode(node);
        node.getNextNode().accept(this);
        return null;
    }

    public final Instruction visitModuleNode(ModuleNode module) {
//        System.out.print("[");
        addParentNode(MODULE, module, module, module.getBodyNode());
//        System.out.print("]");
        return null;
    }

    public Instruction visitSClassNode(SClassNode selfClassNode) {
        selfClassNode.getBodyNode().accept(this);
        return null;
    }

    public final Instruction visitClassNode(ClassNode classNode) {
        boolean tempUnderModuleNode = underModuleNode;
        underModuleNode = false;
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

        underModuleNode = tempUnderModuleNode;
        return null;
    }

    private Member addParentNode(String memberType, Node node, IScopingNode scopeNode, Node bodyNode) {
        visitNode(node);
        scopeNode.getCPath().accept(nameVisitor);
        String name = nameVisitor.name;

        Member member;
        if (memberType == MODULE) {
            member = new Module(name);
        } else {
            member = new ClassMember(name);
        }

        member = populateOffsets(member, scopeNode.getCPath().getPosition(), node.getPosition(), memberType);

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

        if (memberType == MODULE) {
            underModuleNode = true;
        }
        tranverseChildren(bodyNode, node);
        if (memberType == MODULE) {
            underModuleNode = false;
        }

        namespaceNames.remove(name);
        while (colonNameCount > 0) {
            namespaceNames.remove(namespaceNames.size() - 1);
            colonNameCount--;
        }
        currentMember.removeLast();
        return member;
    }

    private void tranverseChildren(Node bodyNode, Node node) {
        if (bodyNode == null) {
            if (node.childNodes() != null) {
                for (Object child : node.childNodes()) {
                    if (child instanceof ArgumentNode) {
                        // do nothing
                    } else if (child instanceof Node) {
                        Node childNode = (Node) (child);
                        childNode.accept(this);
                    }
                }
            }
        } else {
            bodyNode.accept(this);
        }
    }

    public Instruction visitArgsCatNode(ArgsCatNode node) {
        visitNode(node);
        return null;
    }

    public Instruction visitArgsNode(ArgsNode node) {
        visitNode(node);
//        node.accept(this);
        return null;
    }

    public final Instruction visitDefnNode(DefnNode node) {
        visitNode(node);
        Member method;
        try {
            method = getMember("def", methodIndex, methods, node.getName(), node.getNameNode().getPosition(), node.getPosition());
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
            return getMember("def", index, members, memberName, position, position);
        } catch (IndexAdjustmentException e) {
            return throwCantFindException("def", memberName, position);
        }
    }

    public final Instruction visitDefsNode(DefsNode node) {
        visitNode(node);

        Method method = (Method)methods.get(methodIndex++);
        populateReceiverName(method, node);
        populateOffsets(method, node.getPosition(), node.getPosition(), "def");

        currentMember.getLast().addChildMember(method);
        currentMember.add(method);

        tranverseChildren(node.getBodyNode(), node);

        currentMember.removeLast();
        return null;
    }

    private void populateReceiverName(Method method, DefsNode node) {
        String methodName = node.getName();
        Node receiverNode = node.getReceiverNode();
        
        if (receiverNode instanceof ConstNode) {
            ConstNode constNode = (ConstNode)receiverNode;
            method.setReceiver(constNode.getName(), methodName);
        } else if (receiverNode instanceof SelfNode) {
            method.setReceiverToSelf(methodName);
        }
        RubyPlugin.log(": " + method.getFullName(), getClass());
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

    private Member getMember(String memberType, int index, List<Member> members, String memberName, ISourcePosition startPosition, ISourcePosition endPosition) throws IndexAdjustmentException {
        Member member;
        try {
            member = members.get(index);
            String shortName = member.getShortName();
            if (!memberName.equals(shortName)) {
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
                return throwCantFindException(memberType, memberName, endPosition);
            }
        }
        return populateOffsets(member, startPosition, endPosition, memberType);
    }

    private Member populateOffsets(Member member, ISourcePosition position, ISourcePosition endPosition, String memberType) {
        member.setStartOffset(getStartOffset(position, member));
        member.setEndOffset(getEndOffset(endPosition));
        member.setStartOuterOffset(getOuterOffset(member, memberType+" "));
        return member;
    }

    private Member throwCantFindException(String memberType, String memberName, ISourcePosition position) {
        String message = "parser can't find " + memberType + " " + memberName;
        for (RubyParser.WarningListener listener : problemListeners) {
            listener.error(position, message);
        }
        throw new SyntaxException(position, message);
    }

    public Instruction visitRootNode(RootNode node) {
        visitNode(node);
        RubyPlugin.log("",getClass());
        if (node.getBodyNode() != null) {
            node.getBodyNode().accept(this);
        }
        return null;
    }

    public final Instruction visitIfNode(IfNode node) {
        visitNode(node);
        if (node.getThenBody() != null) {
            inIfNode = true;
            node.getThenBody().accept(this);
            inIfNode = false;
        }
        return null;
    }

    public final Instruction visitIterNode(IterNode node) {
        visitNode(node);
        RubyPlugin.log("", getClass());
        if (node.getBodyNode() != null) {
            node.getBodyNode().accept(this);
        }
        return null;
    }

    public Instruction visitArrayNode(ArrayNode node) {
        if (isMethodCall()) {
            for (int i = 0; i < node.size(); i++) {
                node.get(i).accept(this);
            }   
        }
        return null;
    }

    private boolean isMethodCall() {
        return methodCall != null;
    }

    public Instruction visitConstNode(ConstNode node) {
        if (isMethodCall()) {
            methodCall.addArgument(node.getName());
        }
        return null;
    }

    public Instruction visitSymbolNode(SymbolNode node) {
        if (isMethodCall()) {
            methodCall.addArgument(":" + node.getName());
        }
        return null;
    }

    public Instruction visitStrNode(StrNode node) {
        if (isMethodCall()) {
            methodCall.addArgument("'" + node.getValue().toString() + "'");
        }
        return null;
    }

    public Instruction visitHashNode(HashNode node) {
        if (isMethodCall()) {
            node.getListNode().accept(this);
        }
        return null;
    }

    public final Instruction visitFCallNode(FCallNode node) {
        visitNode(node);
        String name = node.getName();
        RubyPlugin.log(": " + name, getClass());
        Member parent = currentMember.getLast();

        if (parent instanceof Root ||
                parent instanceof ClassMember ||
                parent instanceof Module ||
                (isRspecMethodName(parent.getName()) && isRspecMethodName(name)) ||
                (isRakeMethodName(parent.getName()) && isRakeMethodName(name)) ) {
            MethodCallWithSelfAsAnImplicitReceiver call = new MethodCallWithSelfAsAnImplicitReceiver(name);
            call.setStartOuterOffset(getStartOffset(node.getPosition(), call));
            call.setStartOffset(call.getStartOuterOffset() + name.length() + 1);
            call.setEndOffset(getEndOffset(node.getPosition()));

            parent.addChildMember(call);
            currentMember.add(call);
            methodCall = call;
            if (node.getArgsNode() != null) {
                node.getArgsNode().accept(this);
            }
            if (node.getIterNode() != null) {
                node.getIterNode().accept(this);
            }
            methodCall = null;
            currentMember.removeLast();
        }
        return null;
    }

    private boolean isRakeMethodName(String name) {
        // ignore 'desc' as always next to something else
        return name.equals("directory") ||
                name.equals("file") ||
                name.equals("file_create") ||
                name.equals("import") ||
                name.equals("multitask") ||
                name.equals("namespace") ||
                name.equals("rule") ||
                name.equals("task");
    }

    private boolean isRspecMethodName(String name) {
        return name.equals("describe") ||
                name.equals("it") ||
                name.equals("before") ||
                name.equals("after") ||
                name.equals("shared_examples_for") ||
                name.equals("it_should_behave_like") ||
                name.equals("fixtures") ||
                name.equals("context") ||
                name.equals("controller_name") ||
                name.equals("integrate_views");
    }

    public final Instruction visitClassVarDeclNode(ClassVarDeclNode node) {
        visitNode(node);
        RubyPlugin.log(": " + node.getName(), getClass());
        return null;
    }

    public final Instruction visitClassVarAsgnNode(ClassVarAsgnNode node) {
        visitNode(node);
        RubyPlugin.log(": " + node.getName(), getClass());
        return null;
    }

    private int getStartOffset(ISourcePosition position, Member member) {
        if (inIfNode ||
                underModuleNode ||
                (member instanceof Method && currentMember.getLast() == root) ||
                (member instanceof Method && !member.getName().equals(member.getFullName())) ) {
            int startOffset = position.getStartOffset();
            int index = lineCounter.getLineAtOffset(startOffset);
            String line = lineCounter.getLine(index);
            int offset = line.indexOf(member.getShortName());
            if (offset != -1) {
                return offset + lineCounter.getStartOffset(index);
            } else {
                return position.getStartOffset();
            }
        } else {
            return position.getStartOffset();
        }
    }

    private int getOuterOffset(Member member, String keyword) {
        int startOffset = member.getStartOffset();
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
        if (lineCounter.charAt(end - 4) == 'e'
                && lineCounter.charAt(end - 3) == 'n'
                && lineCounter.charAt(end - 2) == 'd') {
            return end - 1;
        }

        char endChar = lineCounter.charAt(end);
        if (((int)endChar) != 65535) {
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
            if (text.indexOf("end") != -1) {
                return text.indexOf("end") + 3 + beginIndex;
            } else {
                return position.getEndOffset();
            }
        } else {
            return position.getEndOffset();
        }
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

        protected final Instruction visitNode(Node node) {
            return null;
        }

        public final Instruction visitColon2Node(Colon2Node node) {
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

        public Instruction visitConstNode(ConstNode node) {
            if (visits == 0) {
                name = node.getName();
            } else {
                namespaces.add(node.getName());
            }
            return null;
        }
    }
}