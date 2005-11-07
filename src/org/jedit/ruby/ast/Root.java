/*
 * Root.java - 
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
 * Represents root of the file, can be
 * used to represent a position that is
 * outside of any other {@link Member}.
 *
 * @author robmckinnon at users.sourceforge.net
 */
public final class Root extends Member {

    public Root(int endOffset) {
        super("root");
        setStartOuterOffset(0);
        setStartOffset(0);
        setEndOffset(endOffset);
    }

    public final void accept(MemberVisitor visitor) {
        visitor.handleRoot(this);
    }
}
