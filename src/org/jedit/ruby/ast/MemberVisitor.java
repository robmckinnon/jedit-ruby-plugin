/*
 * MemberVisitor.java - 
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
package org.jedit.ruby.ast;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public interface MemberVisitor {

    void handleModule(Module module);

    void handleClass(ClassMember classMember);

    void handleMethod(Method method);

    void handleMethodCallWithSelfAsAnImplicitReceiver(MethodCallWithSelfAsAnImplicitReceiver methodCall);

    void handleWarning(Warning warning);

    void handleError(Error warning);

    void handleRoot(Root root);

    void handleKeyword(KeywordMember keywordMember);
}
