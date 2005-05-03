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

    private NameToMethods nameToMethod = new NameToMethods();
    private MethodToParents methodToParent = new MethodToParents();
    private ParentToMethods parentToMethod = new ParentToMethods();
    private Map pathToMembers = new HashMap();

    public static RubyCache instance() {
        return instance;
    }

    public static void clear() {
        instance.pathToMembers.clear();
        instance.methodToParent.clear();
        instance.parentToMethod.clear();
        instance.nameToMethod.clear();
    }

    public static void add(String text, String path) {
        RubyMembers members = RubyParser.getMembers(text, path, null, true);

        if (!members.containsErrors()) {
            instance.add(path, members);
        }
    }

    public static void add(RubyMembers members, String path) {
        if (!members.containsErrors()) {
            instance.add(path, members);
        }
    }

    public static List<Method> getMethods(String method) {
        return instance.nameToMethod.getMethods(method);
    }

    public static List<Member> getMembersWithMethod(String method) {
        return instance.getAllMembersWithMethod(method);
    }

    public static List<Method> getMethodsOfMember(String memberName) {
        return instance.parentToMethod.getMethodList(memberName);
    }

    private List<Member> getAllMembersWithMethod(String method) {
        return methodToParent.getParentList(method);
    }

    private void add(String path, RubyMembers members) {
        parentToMethod.reset();
        pathToMembers.put(path, members);
        members.visitMembers(new MemberVisitorAdapter() {
            public void handleModule(Module module) {
                parentToMethod.add(module);
            }

            public void handleClass(ClassMember classMember) {
                parentToMethod.add(classMember);
            }

            public void handleMethod(Method method) {
                methodToParent.add(method);
                nameToMethod.add(method);
            }
        });
    }

    public static List<Method> getAllMethods() {
        return instance.parentToMethod.getAllMethods();
    }

    public static List<Member> getAllMembers() {
        return instance.parentToMethod.getAllMembers();
    }

    public void populateSuperclassMethods() {

    }

}
