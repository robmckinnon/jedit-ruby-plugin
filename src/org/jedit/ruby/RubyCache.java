/*
 * RubyCache.java - Cache of Ruby methods, classes, modules
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

import java.util.*;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RubyCache {

    private static final RubyCache instance = new RubyCache();
    private HashMap pathToMembers = new HashMap();
    private NameToMethod nameToMethod = new NameToMethod();
    private MethodToParent methodToParent = new MethodToParent();
    private ParentToMethod parentToMethod = new ParentToMethod();

    public static void clear() {
        instance.pathToMembers.clear();
        instance.methodToParent.clear();
        instance.parentToMethod.clear();
        instance.nameToMethod.clear();
    }

    public static void add(String text, String path) {
        RubyMembers members = RubyParser.getMembers(text, path, null, true);

        if(!members.containsErrors()) {
            instance.add(path, members);
        }
    }

    public static List<Member.Method> getMethods(String method) {
        return instance.nameToMethod.getMethods(method);
    }

    public static List<Member> getMembersWithMethod(String method) {
        return instance.methodToParent.getParentList(method);
    }

    public static List<Member.Method> getMethodsOfMember(String memberName) {
        return instance.parentToMethod.getMethodList(memberName);
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
                nameToMethod.add(method);
            }
        });
    }

    private static class ParentToMethod {

        private Map<String, Set<Member.Method>> fullNameToMethodsMap = new HashMap<String, Set<Member.Method>>();
        private Map<String, Set<Member.Method>> nameToMethodsMap = new HashMap<String, Set<Member.Method>>();

        public List<Member.Method> getMethodList(String memberName) {
            Set<Member.Method> methodSet = fullNameToMethodsMap.get(memberName);
            if(methodSet == null) {
                methodSet = nameToMethodsMap.get(memberName);
            }

            List<Member.Method> methods;
            if(methodSet != null) {
                methods = new ArrayList<Member.Method>(methodSet);

                if (methods.size() > 0) {
                    Collections.sort(methods);
                }
            } else {
                methods = new ArrayList<Member.Method>();
            }

            return methods;
        }

        public void add(Member.ParentMember member) {
            Set<Member.Method> methods = member.getMethods();
            String fullName = member.getFullName();
            String name = member.getName();

            if (fullNameToMethodsMap.containsKey(fullName)) {
                fullNameToMethodsMap.get(fullName).addAll(methods);
                nameToMethodsMap.get(name).addAll(methods);
            } else {
                fullNameToMethodsMap.put(fullName, methods);
                nameToMethodsMap.put(name, methods);
            }
        }

        public void clear() {
            fullNameToMethodsMap.clear();
            nameToMethodsMap.clear();
        }
    }

    private static class NameToMethod {

        private Map<String, Set<Member.Method>> nameToMethodMap = new HashMap<String, Set<Member.Method>>();

        public void add(Member.Method method) {
            String methodName = method.getShortName();
            Set<Member.Method> methodSet = getMethodSet(methodName);

            if(methodName.equals("add_topic")) {
                System.out.println("adding: " + methodName);
            }

            if (!methodSet.contains(method)) {
                methodSet.add(method);
            }
        }

        public List<Member.Method> getMethods(String methodName) {
            Set<Member.Method> methodSet = getMethodSet(methodName);
            List<Member.Method> members = new ArrayList<Member.Method>(methodSet);
            if (members.size() > 1) {
                Collections.sort(members);
            }
            return members;
        }

        public Set<Member.Method> getMethodSet(String methodName) {
            if (!nameToMethodMap.containsKey(methodName)) {
                nameToMethodMap.put(methodName, new HashSet<Member.Method>());
            }
            return nameToMethodMap.get(methodName);
        }

        public void clear() {
            nameToMethodMap.clear();
        }
    }

    private static class MethodToParent {

        private Map<String, Set<Member>> methodToParentMap = new HashMap<String, Set<Member>>();

        public void add(Member.Method method) {
            if (method.hasParentMember()) {
                String methodName = method.getName();
                Set<Member> parentSet = getParentSet(methodName);
                parentSet.add(method.getParentMember());
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
            return methodToParentMap.get(methodName);
        }

        public void clear() {
            methodToParentMap.clear();
        }
    }
}
