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
package org.jedit.ruby.cache;

import org.jedit.ruby.ast.*;
import org.jedit.ruby.parser.RubyParser;

import java.util.*;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RubyCache {

    private static final RubyCache instance = new RubyCache();

    private NameToMethods nameToMethods;
    private NameToParents nameToParents;
    private MethodToParents methodToParents;
    private ParentToMethods parentToMethods;
    private ParentToImmediateMethods parentToImmediateMethods;

    private Map pathToMembers;

    private RubyCache() {
        nameToMethods = new NameToMethods();
        nameToParents = new NameToParents();
        methodToParents = new MethodToParents();
        parentToMethods = new ParentToMethods();
        parentToImmediateMethods = new ParentToImmediateMethods();
        pathToMembers = new HashMap();
        nameToParents.setParentToImmediateMethods(parentToImmediateMethods);
        parentToMethods.setNameToParents(nameToParents);
    }

    public static RubyCache instance() {
        return instance;
    }

    public synchronized void clear() {
        pathToMembers.clear();
        methodToParents.clear();
        parentToMethods.clear();
        parentToImmediateMethods.clear();
        nameToMethods.clear();
    }

    public synchronized void add(String text, String path) {
        RubyMembers members = RubyParser.getMembers(text, path, null, true);
        add(members, path);
    }

    public synchronized void add(RubyMembers members, String path) {
        if (!members.containsErrors()) {
            add(path, members);
        }
    }

    public synchronized List<Method> getMethods(String method) {
        return nameToMethods.getMethods(method);
    }

    public synchronized List<Member> getMembersWithMethodAsList(String method) {
        return methodToParents.getParentList(method);
    }

    public synchronized Set<Member> getMembersWithMethod(String method) {
        return methodToParents.getParentSet(method);
    }

    public synchronized List<Method> getMethodsOfMemberAsList(String memberName) {
        return parentToMethods.getMethodList(memberName);
    }

    public synchronized Set<Method> getMethodsOfMember(String memberName) {
        return parentToMethods.getMethodSet(memberName);
    }

    public synchronized List<Method> getAllMethods() {
        return parentToMethods.getAllMethods();
    }

    public synchronized List<Member> getAllImmediateMembers() {
        return nameToParents.getAllMembers();
    }

    public synchronized void populateSuperclassMethods() {
        Collection<ParentMember> allParents = nameToParents.getAllParents();

        for (ParentMember member : allParents) {
            populateSuperclassMethods(member, member, "null");
        }
    }

    private void add(String path, RubyMembers members) {
        parentToMethods.reset();
        pathToMembers.put(path, members);
        members.visitMembers(new MemberVisitorAdapter() {
            public void handleModule(Module module) {
                parentToImmediateMethods.add(module);
                parentToMethods.add(module);
                nameToParents.add(module);
            }

            public void handleClass(ClassMember classMember) {
                parentToImmediateMethods.add(classMember);
                parentToMethods.add(classMember);
                nameToParents.add(classMember);
            }

            public void handleMethod(Method method) {
                methodToParents.add(method);
                nameToMethods.add(method);
            }
        });
    }

    private void populateSuperclassMethods(ParentMember member, ParentMember memberOrSuperclass, String test) {
        if (memberOrSuperclass.hasParentMemberName()) {
            String parentName = memberOrSuperclass.getParentMemberName();
            ParentMember parent = nameToParents.getMember(parentName);
            if (parent != null) {
                Set<Method> methods = parent.getMethods();
                parentToMethods.add(member, methods);
                for (Method method : methods) {
                    methodToParents.add(method, member);
                }
                populateSuperclassMethods(member, parent, "null");
            }
        }
    }

}
