/*
 * KeywordMember.java - Ruby file structure member
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
 * Not really a member, used to represent
 * keywords for code completion popup.
 *
 * @author robmckinnon at users.sourceforge.net
 */
public class KeywordMember extends Member {

    public KeywordMember(String name) {
        super(name);
    }

    public void accept(MemberVisitor visitor) {
        visitor.handleKeyword(this);
    }

    public boolean startsWith(String text) {
        return getName().startsWith(text);
    }
}