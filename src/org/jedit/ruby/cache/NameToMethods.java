/*
 * NameToMethods.java - 
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
import org.jedit.ruby.RubyPlugin;

import java.util.*;

/**
 * Manages mapping of method name
 * to cached {@link Method} objects
 * that have that method name.
 *
 * @author robmckinnon at users.sourceforge.net
 */
final class NameToMethods {

    private final Map<String, Set<Method>> nameToMethodMap = new HashMap<String, Set<Method>>();

    final void add(Method method) {
        String methodName = method.getShortName();
        Set<Method> methodSet = getMethodSet(methodName);

        if (methodName.equals("add_topic")) {
            RubyPlugin.log("adding: " + methodName, getClass());
        }

        if (!methodSet.contains(method)) {
            methodSet.add(method);
        }
    }

    final List<Method> getMethods(String methodName) {
        Set<Method> methodSet = getMethodSet(methodName);
        List<Method> members = new ArrayList<Method>(methodSet);
        if (members.size() > 1) {
            Collections.sort(members);
        }
        return members;
    }

    private Set<Method> getMethodSet(String methodName) {
        if (!nameToMethodMap.containsKey(methodName)) {
            nameToMethodMap.put(methodName, new HashSet<Method>());
        }
        return nameToMethodMap.get(methodName);
    }

    final void clear() {
        nameToMethodMap.clear();
    }
}
