/*
 * ClassMember.java -
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

import org.jedit.ruby.cache.RubyCache;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class ClassMember extends ParentMember {

    private String fullDocumentation;

    public ClassMember(String name, int startOuterOffset, int startOffset) {
        super(name, startOuterOffset, startOffset);
    }

    public final void accept(MemberVisitor visitor) {
        visitor.handleClass(this);
    }

    public final String getDocumentation() {
        if (fullDocumentation == null) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("<p>Class: ").append(getFullName());

            String parentMemberName = getParentMemberName();
            appendParentToDocumentation(parentMemberName, buffer);

            buffer.append("</p><br>");
            buffer.append(super.getDocumentation());
            fullDocumentation = buffer.toString();
        }

        return fullDocumentation;
    }

    private void appendParentToDocumentation(String parentMemberName, StringBuffer buffer) {
        if (parentMemberName != null && parentMemberName.trim().length() > 0) {
            ParentMember parentMember = RubyCache.instance().getParentMember(parentMemberName);
            if (parentMember != null && !parentMember.getFullName().equals("Object")) {
                buffer.append(" &lt; ").append(parentMember.getFullName());
                appendParentToDocumentation(parentMember.getParentMemberName(), buffer);
            }
        }
    }
}
