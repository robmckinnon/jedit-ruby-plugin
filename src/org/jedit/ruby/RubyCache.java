/*
 * RubyCache.java - 
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
package org.jedit.ruby;

import org.jruby.lexer.yacc.SourcePosition;

import java.util.*;


/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RubyCache {

    private static final RubyCache instance = new RubyCache();
    private HashMap pathToMembers = new HashMap();
    private MethodToParent methodToParent = new MethodToParent();
    private ParentToMethod parentToMethod = new ParentToMethod();

    public static void clear() {
        instance.pathToMembers.clear();
        instance.methodToParent.clear();
        instance.parentToMethod.clear();
    }

    public static void add(String text, String path) {
        RubyMembers members = RubyParser.getMembers(text, path, null, true);
        instance.add(path, members);
    }

    public static List<Member> getMembersWithMethod(String method) {
        return instance.membersWithMethod(method);
    }

    public static List<Member.Method> getMethodsOfMember(String memberName) {
        return instance.methodsOfMember(memberName);
    }

    private void add(String path, RubyMembers members) {
        pathToMembers.put(path, members);
        members.visitMembers(new Member.VisitorAdapter() {
            public void handleModule(Member.Module module) {
                parentToMethod.add(module);
            }

            public void handleClass(Member.Class classMember) {
                parentToMethod.add(classMember);
            }

            public void handleMethod(Member.Method method) {
                methodToParent.add(method);
            }
        });
    }

    private List<Member> membersWithMethod(String method) {
        return methodToParent.getParentList(method);
    }

    private List<Member.Method> methodsOfMember(String memberName) {
        return parentToMethod.getMethodList(memberName);
    }

    private static class ParentToMethod {

        private Map<String, Set<Member.Method>> parentToMethodsMap = new HashMap<String, Set<Member.Method>>();

        public List<Member.Method> getMethodList(String memberName) {
            Set<Member.Method> methodSet = parentToMethodsMap.get(memberName);
            List<Member.Method> methods = new ArrayList<Member.Method>(methodSet);

            if (methods.size() > 0) {
                Collections.sort(methods);
            }

            return methods;
        }

        public void add(Member.ParentMember member) {
            Set<Member.Method> methods = member.getMethods();
            String name = member.getFullName();

            if (parentToMethodsMap.containsKey(name)) {
                parentToMethodsMap.get(name).addAll(methods);
            } else {
                parentToMethodsMap.put(name, methods);
            }
        }

        public void clear() {
            parentToMethodsMap.clear();
        }
    }

    private static class MethodToParent {
        private Map<String, Set<Member>> methodToParentMap = new HashMap<String, Set<Member>>();

        public void add(Member.Method method) {
            if (method.hasParentMember()) {
                String methodName = method.getName();
                Set<Member> parentList = getParentSet(methodName);
                parentList.add(method.getParentMember());
            }
        }

        private List<Member> getParentList(String methodName) {
            Set<Member> memberSet = getParentSet(methodName);
            List<Member> members = new ArrayList<Member>(memberSet);
            if (members.size() > 1) {
                Collections.sort(members);
            }
            return members;
        }

        private Set<Member> getParentSet(String methodName) {
            if (!methodToParentMap.containsKey(methodName)) {
                methodToParentMap.put(methodName, new HashSet<Member>());
            }
            Set<Member> memberSet = methodToParentMap.get(methodName);
            return memberSet;
        }

        public void clear() {
            methodToParentMap.clear();
        }
    }
}
