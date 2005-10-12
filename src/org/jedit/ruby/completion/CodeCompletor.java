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

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Method;
import org.jedit.ruby.cache.RubyCache;
import org.jedit.ruby.RubyPlugin;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class CodeCompletor {

    private final CodeAnalyzer analyzer;
    private final List<Method> methods;

    public CodeCompletor(View view) {
        analyzer = new CodeAnalyzer(view.getTextArea(), view.getBuffer());
        methods = findMethods();
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

    public final boolean isInsertionPoint() {
        return analyzer.isInsertionPoint();
    }

    private List<Method> findMethods() {
        Set<Method> methods;

//        if (CodeAnalyzer.hasLastReturnTypes()) {
//            methods = getMethodsOfParents(CodeAnalyzer.getLastReturnTypes());
//
//            if (getPartialMethod() != null) {
//                filterMethods(methods, getPartialMethod());
//            }
//        } else if (analyzer.getMethodCalledOnThis() != null) {
        if (analyzer.getMethodCalledOnThis() != null) {
            String className = analyzer.getClassName();
            if (className != null) {
                methods = completeUsingClass(className);
            } else {
                methods = completeUsingMethods(analyzer.getMethodsCalledOnVariable());
            }

            if (getPartialMethod() != null) {
                filterMethods(methods, getPartialMethod());
            }

        } else {
            methods = new HashSet<Method>();
        }

        List<Method> methodList = new ArrayList<Method>(methods);
        int size = methods.size();
        if (size > 0) {
            CodeCompletionComparator.instance.setObjectMethodsLast(size > 8);
            Collections.sort(methodList, CodeCompletionComparator.instance);
        }
        return methodList;
    }

    private static void filterMethods(Set<Method> methods, String partialMethod) {
        for (Iterator<Method> iterator = methods.iterator(); iterator.hasNext();) {
            Method method = iterator.next();

            if (!method.getShortName().startsWith(partialMethod)) {
                iterator.remove();
            }
        }
    }

    private Set<Method> completeUsingClass(String className) {
        RubyPlugin.log("class: " + className, getClass());
        Set<Method> methods = RubyCache.instance().getMethodsOfMember(className);
        RubyPlugin.log("methods: " + methods.size(), getClass());

        if (analyzer.isClass()) {
            for (Iterator<Method> iterator = methods.iterator(); iterator.hasNext();) {
                Method method = iterator.next();
                if (!method.isClassMethod()) {
                    iterator.remove();
                }
            }
        }

        return methods;
    }

    private Set<Method> completeUsingMethods(List<String> methods) {
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

    private static final class CodeCompletionComparator implements Comparator<Method> {
        private static final CodeCompletionComparator instance = new CodeCompletionComparator();
        private boolean objectMethodsLast;

        public final int compare(Method method, Method otherMethod) {
            boolean onObjectClass = onObjectClass(method);
            boolean otherOnObjectClass = onObjectClass(otherMethod);

            if (objectMethodsLast && onObjectClass && !otherOnObjectClass) {
                return 1;
            } else if (objectMethodsLast && !onObjectClass && otherOnObjectClass) {
                return -1;
            } else {
                int compare = method.getName().compareTo(otherMethod.getName());

                if (compare == 0) {
                    return method.getFullName().compareTo(otherMethod.getFullName());
                } else {
                    return compare;
                }
            }
        }

        private boolean onObjectClass(Method method) {
            return method.getParentMember().getName().equals("Object");
        }

        public final void setObjectMethodsLast(boolean objectMethodsLast) {
            this.objectMethodsLast = objectMethodsLast;
        }
    }
}