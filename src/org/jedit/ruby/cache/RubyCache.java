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

    private Map pathToMembers;

    private RubyCache() {
        nameToMethods = new NameToMethods();
        nameToParents = new NameToParents();
        methodToParents = new MethodToParents();
        parentToMethods = new ParentToMethods();
        pathToMembers = new HashMap();
        nameToParents.setParentToMethods(parentToMethods);
        parentToMethods.setNameToParents(nameToParents);
    }

    public static RubyCache instance() {
        return instance;
    }

    public void clear() {
        pathToMembers.clear();
        methodToParents.clear();
        parentToMethods.clear();
        nameToMethods.clear();
    }

    public void add(String text, String path) {
        RubyMembers members = RubyParser.getMembers(text, path, null, true);
        add(members, path);
    }

    public void add(RubyMembers members, String path) {
        if (!members.containsErrors()) {
            add(path, members);
        }
    }

    public List<Method> getMethods(String method) {
        return nameToMethods.getMethods(method);
    }

    public List<Member> getMembersWithMethod(String method) {
        return methodToParents.getParentList(method);
    }

    public List<Method> getMethodsOfMember(String memberName) {
        return parentToMethods.getMethodList(memberName);
    }

    private void add(String path, RubyMembers members) {
        parentToMethods.reset();
        pathToMembers.put(path, members);
        members.visitMembers(new MemberVisitorAdapter() {
            public void handleModule(Module module) {
                parentToMethods.add(module);
                nameToParents.add(module);
            }

            public void handleClass(ClassMember classMember) {
                parentToMethods.add(classMember);
                nameToParents.add(classMember);
            }

            public void handleMethod(Method method) {
                methodToParents.add(method);
                nameToMethods.add(method);
            }
        });
    }

    public List<Method> getAllMethods() {
        return parentToMethods.getAllMethods();
    }

    public List<Member> getAllMembers() {
        return nameToParents.getAllMembers();
    }

    public void populateSuperclassMethods() {

    }

}
