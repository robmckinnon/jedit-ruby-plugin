/*
 * MethodToParents.java - 
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

import org.jedit.ruby.ast.Member;
import org.jedit.ruby.ast.Method;

import java.util.*;

/**
 * @author robmckinnon at users.sourceforge.net
 */
final class MethodToParents {

    private final Map<String, Set<Member>> methodToParentMap = new HashMap<String, Set<Member>>();

    final void add(Method method) {
        if (method.hasParentMember()) {
            Member parentMember = method.getParentMember();
            add(method, parentMember);
        }
    }

    final void add(Method method, Member parentMember) {
        String methodName = method.getName();
        Set<Member> parentSet = getParentSet(methodName);
        parentSet.add(parentMember);
    }

    final List<Member> getParentList(String methodName) {
        Set<Member> memberSet = getParentSet(methodName);
        List<Member> members = new ArrayList<Member>(memberSet);
        if (members.size() > 1) {
            Collections.sort(members);
        }
        return members;
    }

    final Set<Member> getParentSet(String methodName) {
        if (!methodToParentMap.containsKey(methodName)) {
            methodToParentMap.put(methodName, new HashSet<Member>());
        }
        return methodToParentMap.get(methodName);
    }

    final void clear() {
        methodToParentMap.clear();
    }
}
