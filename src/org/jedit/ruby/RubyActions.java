/*
 * RubyActions.java - Actions for Ruby plugin
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

import org.gjt.sp.jedit.View;
import gnu.regexp.REException;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public class RubyActions {

    public static void fileStructurePopup(View view) {
        FileStructurePopup fileStructurePopup = new FileStructurePopup(view);
        fileStructurePopup.show();
    }

    public static void autoIndentAndInsertEnd(View view) {
        try {
            AutoIndentAndInsertEnd indenter = new AutoIndentAndInsertEnd(view);
            indenter.performIndent();
        } catch (REException e) {
            e.printStackTrace();
        }
    }
}
