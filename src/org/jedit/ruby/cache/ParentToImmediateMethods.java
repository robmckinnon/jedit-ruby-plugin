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
import org.jedit.ruby.ast.ParentMember;

import java.util.*;

/**
 * @author robmckinnon at users.sourceforge.net
 */
final class ParentToImmediateMethods {

    private final Map<String, Set<Method>> fullNameToImmediateMethods = new HashMap<String, Set<Method>>();
    private final Map<String, Set<Method>> nameToImmediateMethods = new HashMap<String, Set<Method>>();

    final List<Method> getImmediateMethodList(String memberName) {
        Set<Method> methodSet = getImmediateMethodSet(memberName);
        List<Method> methods = new ArrayList<Method>(methodSet);

        if (methods.size() > 0) {
            Collections.sort(methods);
        }

        return methods;
    }

    private Set<Method> getImmediateMethodSet(String memberName) {
        Set<Method> methodSet = fullNameToImmediateMethods.get(memberName);
        if (methodSet == null) {
            methodSet = nameToImmediateMethods.get(memberName);
        }
        if (methodSet == null) {
            methodSet = new HashSet<Method>();
        }
        return methodSet;
    }

    /**
     * Note: Have to addMembers methods separately because there
     * may be some classes defined across more than one file.
     */
    final void add(ParentMember member) {
        Set<Method> methods = member.getMethods();
        String fullName = member.getFullName();
        String name = member.getName();
        load(fullName, methods, name, fullNameToImmediateMethods, nameToImmediateMethods);
    }

    private static void load(String fullName, Set<Method> methods, String name, Map<String, Set<Method>> fullNameToMethods, Map<String, Set<Method>> nameToMethods) {
        if (fullNameToMethods.containsKey(fullName)) {
            fullNameToMethods.get(fullName).addAll(methods);
            nameToMethods.get(name).addAll(methods);
        } else {
            fullNameToMethods.put(fullName, methods);
            nameToMethods.put(name, methods);
        }
    }

    final void clear() {
        fullNameToImmediateMethods.clear();
        nameToImmediateMethods.clear();
    }

}
