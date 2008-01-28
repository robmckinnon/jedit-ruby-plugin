/*
 * Member.java - Ruby file structure member
 *
 * Copyright 2008 Robert McKinnon
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

import java.util.List;
import java.util.ArrayList;

/**
 * Ruby file structure member
 *
 * @author robmckinnon at users.sourceforge.net
 */
public class MethodCallWithSelfAsAnImplicitReceiver extends Member  {

    private List<String> arguments;

    public MethodCallWithSelfAsAnImplicitReceiver(String name) {
        super(name);
        arguments = new ArrayList<String>();
    }

    public void accept(MemberVisitor visitor) {
        visitor.handleMethodCallWithSelfAsAnImplicitReceiver(this);
    }

    public String getArgument(int index) {
        return arguments.get(index);
    }

    public String getFirstArgument() {
        if (arguments.size() > 0) {
            return arguments.get(0);
        } else {
            return null;
        }
    }

    public void addArgument(String argument) {
        arguments.add(argument);
    }

    public String getDisplayName() {
        if (getFirstArgument() != null) {
            if (getFirstArgument().length() > 73) {
                return super.getDisplayName() + " " + getFirstArgument().substring(0,70) + "...";
            } else if (arguments.size() > 1) {
                if (getFirstArgument().length() + getArgument(1).length() > 73) {            
                    return super.getDisplayName() + " " + getFirstArgument() + ", " + getArgument(1).substring(0, 69 - getFirstArgument().length());
                } else {
                    return super.getDisplayName() + " " + getFirstArgument() + ", " + getArgument(1);                    
                }
            } else {
                return super.getDisplayName() + " " + getFirstArgument();
            }
        } else {
            return super.getDisplayName();
        }
    }

}