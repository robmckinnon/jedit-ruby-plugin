/*
 * MemberIcon.java - 
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

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class MemberIcon extends Member.VisitorAdapter {

    private Icon icon;

    public MemberIcon(Member member) {
        member.accept(this);
    }

    public Icon getIcon() {
        return icon;
    }

    public void handleModule(Member.Module module) {
        icon = loadIcon("module");
    }

    public void handleClass(Member.Class classMember) {
        icon = loadIcon("class");
    }

    public void handleMethod(Member.Method method) {
        icon = loadIcon("method");
    }

    private Icon loadIcon(String name) {
       return new ImageIcon(MemberIcon.class.getResource("icons/" + name + ".png"));
    }
}
