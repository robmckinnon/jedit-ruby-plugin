/*
 * Member.java - Ruby file structure member
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
package org.jedit.ruby;

/**
 * Ruby file structure member
 * @author robmckinnon at users.sourceforge.net
 */
public class Member {

    private String displayName;
    private String name;
    private int offset;

    public Member(String name, int offset, boolean indent) {
        this.offset = offset;
        if(indent) {
            displayName = "    " + name;
        } else {
            displayName = name;
        }
        this.name = name.toLowerCase();
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLowerCaseName() {
        return name;
    }

    public int getOffset() {
        return offset;
    }

    public String toString() {
        return getDisplayName();
    }
}