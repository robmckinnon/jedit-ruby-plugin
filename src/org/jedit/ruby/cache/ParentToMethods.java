/*
 * ParentToMethods.java - 
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

import org.jedit.ruby.ast.Method;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.ParentMember;

import java.util.*;

/**
 * @author robmckinnon at users.sourceforge.net
 */
class ParentToMethods {

    private Map<String, Set<Method>> fullNameToMethods = new HashMap<String, Set<Method>>();
    private Map<String, Set<Method>> nameToMethods = new HashMap<String, Set<Method>>();
    private Map<String, Member> fullNameToMember = new HashMap<String, Member>();
    private Map<String, Member> nameToMember = new HashMap<String, Member>();
    private List<Member> allMembers;
    private List<Method> allMethods;

    List<Method> getMethodList(String memberName) {
        Set<Method> methodSet = fullNameToMethods.get(memberName);
        if (methodSet == null) {
            methodSet = nameToMethods.get(memberName);
        }

        List<Method> methods;
        if (methodSet != null) {
            methods = new ArrayList<Method>(methodSet);

            if (methods.size() > 0) {
                Collections.sort(methods);
            }
        } else {
            methods = new ArrayList<Method>();
        }

        return methods;
    }

    /**
     * Note: Have to add methods separately because there
     * may be some classes defined across more than one file.
     */
    void add(ParentMember member) {
        Set<Method> methods = member.getMethods();
        String fullName = member.getFullName();
        String name = member.getName();

        if (fullNameToMethods.containsKey(fullName)) {
            fullNameToMethods.get(fullName).addAll(methods);
            nameToMethods.get(name).addAll(methods);
        } else {
            fullNameToMethods.put(fullName, methods);
            nameToMethods.put(name, methods);
        }

        fullNameToMember.put(fullName, member);
        nameToMember.put(name, member);
    }

    void clear() {
        fullNameToMethods.clear();
        nameToMethods.clear();
        fullNameToMember.clear();
        nameToMember.clear();
    }

    List<Method> getAllMethods() {
        if(allMethods == null) {
            allMethods = new ArrayList<Method>();

            for (String parentName : getAllParentNames()) {
                allMethods.addAll(getMethodList(parentName));
            }
        }

        return allMethods;
    }

    List<Member> getAllMembers() {
        if (allMembers == null) {
            allMembers = new ArrayList<Member>();

            for (String parentName : getAllParentNames()) {
                allMembers.add(fullNameToMember.get(parentName));
                allMembers.addAll(getMethodList(parentName));
            }
        }

        return allMembers;
    }

    private List<String> getAllParentNames() {
        List<String> names = new ArrayList<String>(fullNameToMember.keySet());
        Collections.sort(names);
        return names;
    }

    void reset() {
        allMembers = null;
        allMethods = null;
    }
}
