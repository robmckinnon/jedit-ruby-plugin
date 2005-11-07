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
    private String superClassName;

    public ClassMember(String name) {
        super(name);
    }

    public final void accept(MemberVisitor visitor) {
        visitor.handleClass(this);
    }

    public boolean hasSuperClassName() {
        return superClassName != null;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    public final String getDocumentation() {
        if (fullDocumentation == null) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("<p>Class: ").append(getFullName());

            String superClassName = getSuperClassName();
            appendSuperClassToDocumentation(superClassName, buffer);

            buffer.append("</p><br>");
            buffer.append(super.getDocumentation());
            fullDocumentation = buffer.toString();
        }

        return fullDocumentation;
    }

    private void appendSuperClassToDocumentation(String superClassName, StringBuffer buffer) {
        if (superClassName != null && superClassName.trim().length() > 0) {
            ClassMember superClass = RubyCache.instance().getClass(superClassName);
            if (superClass != null && !superClass.getFullName().equals("Object")) {
                buffer.append(" &lt; ").append(superClass.getFullName());
                appendSuperClassToDocumentation(superClass.getSuperClassName(), buffer);
            }
        }
    }

}
