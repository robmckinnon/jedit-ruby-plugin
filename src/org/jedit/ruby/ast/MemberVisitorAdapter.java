/*
 * MemberVisitorAdapter.java - 
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
public class MemberVisitorAdapter implements MemberVisitor {
    public void handleModule(Module module) {
    }

    public void handleClass(ClassMember classMember) {
    }

    public void handleMethod(Method method) {
    }

    public void handleWarning(Warning warning) {
    }

    public void handleError(Error warning) {
    }
}
