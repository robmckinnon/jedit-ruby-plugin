/*
 * CodeCompletor.java - 
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
package org.jedit.ruby.completion;

import java.util.*;

import org.jedit.ruby.ast.*;
import org.jedit.ruby.cache.RubyCache;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.utils.EditorView;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class CodeCompletor {

    private final CodeAnalyzer analyzer;
    private final List<Method> methods;
    private final List<ParentMember> classesAndModules;
    private final EditorView view;
    private static final ArrayList EMPTY_LIST = new ArrayList();

    public CodeCompletor(EditorView editorView) {
        RubyPlugin.log("completing", getClass());
        view = editorView;
        analyzer = new CodeAnalyzer(editorView);
        methods = findMethods();

        if (isClassComplete()) {
            classesAndModules = findClassesAndModules();
        } else {
            classesAndModules = null;
        }
    }

    private boolean isClassComplete() {
        return methods.size() == 0 && getPartialClass() != null;
    }

    public final List<Method> getMethods() {
        return methods;
    }

    public final String getRestOfLine() {
        return analyzer.getRestOfLine();
    }

    public final String getPartialMethod() {
        return analyzer.getPartialMethod();
    }

    private String getPartialClass() {
        return analyzer.getPartialClass();
    }

    public final boolean isDotInsertionPoint() {
        return analyzer.isDotInsertionPoint();
    }

    public boolean hasCompletion() {
        boolean haveMethods = methods.size() > 0 && !analyzer.isLastCompleted();
        return haveMethods || (classesAndModules != null && classesAndModules.size() > 0);
    }

    public RubyCompletion getDotCompletion() {
        List members = methods;
        if (classesAndModules != null && classesAndModules.size() > 0) {
            members = new ArrayList(members);
            members.addAll(classesAndModules);
        }
        return new RubyCompletion(view, getPartialClass(), getPartialMethod(), members);
    }

    public RubyCompletion getEmptyCompletion() {
        return new RubyCompletion(view, getPartialClass(), getPartialMethod(), EMPTY_LIST);
    }

    public RubyCompletion getCompletion() {
        if (isClassComplete()) {
            return new RubyCompletion(view, getPartialClass(), getPartialMethod(), classesAndModules);
        } else {
            return new RubyCompletion(view, getPartialClass(), getPartialMethod(), methods);
        }
    }

    private List<ParentMember> findClassesAndModules() {
        RubyCache cache = RubyCache.instance();
        List<ParentMember> members = cache.getParentsStartingWith(getPartialClass());

        if (members.size() > 0) {
            ParentCompletionComparator.instance.setPartialName(getPartialClass());
            Collections.sort(members, ParentCompletionComparator.instance);
        }

        return members;
    }

    private List<Method> findMethods() {
//        if (CodeAnalyzer.hasLastReturnTypes()) {
//            methods = getMethodsOfParents(CodeAnalyzer.getLastReturnTypes());
//
//            if (getPartialMethod() != null) {
//                filterMethods(methods, getPartialMethod());
//            }
//        } else if (analyzer.getMethodCalledOnThis() != null) {
        Set<Method> methods;
        if (analyzer.getMethodCalledOnThis() != null) {
            methods = findMethodsFromCallee();

        } else if (getPartialClass() == null) {
            methods = findMethodsFromPosition();

        } else {
            methods = new HashSet<Method>();
        }

        List<Method> methodList = new ArrayList<Method>(methods);

        if (methods.size() > 0) {
            MethodCompletionComparator.instance.setObjectMethodsLast(methods.size() > 8);
            Collections.sort(methodList, MethodCompletionComparator.instance);
        }
        return methodList;
    }

    private Set<Method> findMethodsFromPosition() {
        Member member = view.getMemberAtCaretPosition();
        Set<Method> methods;

        if (member == null) {
            methods = findMethods("Kernel", false, true);
        } else {
            MethodFinderVisitor visitor = new MethodFinderVisitor();
            member.accept(visitor);
            methods = visitor.methods;
        }

        if (methods == null) {
            methods = findMethods("Kernel", false, true);
        }
        return methods;
    }

    public static void setLastCompleted(String partialName, Member member) {
        MethodCompletionComparator.instance.addLastCompleted(member);
        ParentCompletionComparator.instance.addLastCompleted(partialName, member);
        CodeAnalyzer.setLastCompleted(member.getName());
    }

    private class MethodFinderVisitor extends MemberVisitorAdapter {
        Set<Method> methods;

        public void handleModule(Module module) {
            methods = findMethods(module.getFullName(), true);
        }

        public void handleClass(ClassMember classMember) {
            methods = findMethods(classMember.getFullName(), true);
            String superClass = classMember.getSuperClassName();

            if (superClass != null) {
                methods.addAll(findMethods(superClass, true));
            }
        }

        public void handleMethod(Method method) {
            methods = findMethods(method.getParentMemberName(), false);
        }

        public void handleRoot(Root root) {
            methods = findMethods("Kernel", false, true);
        }
    }

    private Set<Method> findMethods(String parentName, boolean removeInstanceMethods) {
        return findMethods(parentName, removeInstanceMethods, false);
    }

    private Set<Method> findMethods(String parentName, boolean removeInstanceMethods, boolean removeCommonClassMethods) {
        Set<Method> methods = getMethodsOfParentMember(parentName, removeInstanceMethods, removeCommonClassMethods);
        return filterMethods(methods, getPartialMethod());
    }

    private Set<Method> findMethodsFromCallee() {
        Set<Method> methods;
        String className = analyzer.getClassMethodCalledFrom();
        if (className != null) {
            methods = getMethodsOfParentMember(className, analyzer.isClass(), false);
        } else {
            methods = completeUsingMethods(analyzer.getMethodsCalledOnVariable());
        }

        if (getPartialMethod() != null) {
            filterMethods(methods, getPartialMethod());
        }
        return methods;
    }

    private Set<Method> filterMethods(Set<Method> methods, String partialMethod) {
        if (partialMethod != null) {
            for (Iterator<Method> iterator = methods.iterator(); iterator.hasNext();) {
                Method method = iterator.next();

                if (!method.getShortName().startsWith(partialMethod)) {
                    iterator.remove();
                }
            }
        }

        return methods;
    }

    private Set<Method> getMethodsOfParentMember(String parentMember, boolean removeInstanceMethods, boolean removeCommonClassMethods) {
        RubyPlugin.log("parent: " + parentMember, getClass());
        Set<Method> methods = RubyCache.instance().getMethodsOfMember(parentMember);
        RubyPlugin.log("methods: " + methods.size(), getClass());

        for (Iterator<Method> iterator = methods.iterator(); iterator.hasNext();) {
            Method method = iterator.next();

            if (method.isClassMethod()) {
                if (removeCommonClassMethods) {
                    String name = method.getName();
                    if (name.equals("new") || name.equals("[]")) {
                        iterator.remove();
                    }
                }
            } else if (removeInstanceMethods) {
                iterator.remove();
            }
        }

        return methods;
    }

    private static Set<Method> completeUsingMethods(List<String> methods) {
        Set<Member> members = null;

        for (String method : methods) {
            Set<Member> classes = RubyCache.instance().getMembersWithMethod(method);
            if (members != null) {
                members = intersection(members, classes);
            } else {
                members = classes;
            }
        }

        return getMethodsOfParents(members);
    }

    private static Set<Method> getMethodsOfParents(Set<Member> members) {
        Set<Method> results = new HashSet<Method>();

        if (members != null) {
            for (Member member : members) {
                results.addAll(RubyCache.instance().getMethodsOfMemberAsList(member.getFullName()));
            }
        } else {
            results.addAll(RubyCache.instance().getAllMethods());
        }

        return results;
    }

    private static Set<Member> intersection(Set<Member> list, Set<Member> otherList) {
        Set<Member> intersection = new HashSet<Member>();

        if (!list.isEmpty()) {
            intersection.addAll(list);
        }

        if (!intersection.isEmpty()) {
            intersection.retainAll(otherList);
        }

        return intersection;
    }

    private static final class ParentCompletionComparator implements Comparator<ParentMember> {
        private static final ParentCompletionComparator instance = new ParentCompletionComparator();
        private final Map<String, Member> partialNameToMember = new HashMap<String, Member>();
        private String partialName;

        public final int compare(ParentMember parent, ParentMember otherParent) {
            Member member = partialNameToMember.get(partialName);
            if (member != null) {
                boolean isLastCompleted = parent.equals(member);
                boolean isOtherLastCompleted = otherParent.equals(member);

                if (isLastCompleted && !isOtherLastCompleted) {
                    return -1;
                } else if (!isLastCompleted && isOtherLastCompleted) {
                    return 1;
                } else {
                    return nameCompare(parent, otherParent);
                }
            } else {
                return nameCompare(parent, otherParent);
            }
        }

        private int nameCompare(ParentMember parent, ParentMember otherParent) {
            return parent.getFullName().compareTo(otherParent.getFullName());
        }

        public void addLastCompleted(String partialName, Member member) {
            if (partialName != null) {
                while (partialName.length() > 0) {
                    partialNameToMember.put(partialName, member);
                    partialName = partialName.substring(0, partialName.length() - 1);
                }
            }
        }

        public void setPartialName(String partialName) {
            this.partialName = partialName;
        }
    }

    private static final class MethodCompletionComparator extends MemberVisitorAdapter implements Comparator<Method> {
        private static final MethodCompletionComparator instance = new MethodCompletionComparator();

        private final Map<String, Method> classToLastCompletedMethod = new HashMap<String, Method>();
        private boolean objectMethodsLast;

        public final int compare(Method method, Method otherMethod) {
            boolean onObjectClass = onObjectClass(method);
            boolean otherOnObjectClass = onObjectClass(otherMethod);

            if (objectMethodsLast && onObjectClass && !otherOnObjectClass) {
                return 1;
            } else if (objectMethodsLast && !onObjectClass && otherOnObjectClass) {
                return -1;
            } else {
                boolean isLastCompleted = isLastCompleted(method);
                boolean isOtherLastCompleted = isLastCompleted(otherMethod);

                if (isLastCompleted && !isOtherLastCompleted) {
                    return -1;
                } else if (!isLastCompleted && isOtherLastCompleted) {
                    return 1;
                } else {
                    int compare = method.getName().compareTo(otherMethod.getName());

                    if (compare == 0) {
                        return method.getFullName().compareTo(otherMethod.getFullName());
                    } else {
                        return compare;
                    }
                }
            }
        }

        private boolean isLastCompleted(Method method) {
            if (method.hasParentMemberName()) {
                Method last = classToLastCompletedMethod.get(method.getParentMemberName());
                return method.equals(last);
            } else {
                return false;
            }
        }

        private boolean onObjectClass(Method method) {
            return method.getParentMember().getName().equals("Object");
        }

        public final void setObjectMethodsLast(boolean objectMethodsLast) {
            this.objectMethodsLast = objectMethodsLast;
        }

        public void handleMethod(Method method) {
            RubyPlugin.log("add last completed: " + String.valueOf(method), getClass());
            if (method != null && method.hasParentMemberName()) {
                RubyPlugin.log("add last completed: " + method.getParentMemberName() + "." + method.getName(), getClass());
                classToLastCompletedMethod.put(method.getParentMemberName(), method);
            }
        }

        public void addLastCompleted(Member member) {
            member.accept(this);
        }
    }

}