/*
 * FileStructurePopup.java - File structure popup
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
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

import java.awt.Point;

/**
 * Shows file structure popup to allow user to navigate
 * to member locations within a file.
 * @author robmckinnon at users,sourceforge,net
 */
public class FileStructurePopup {

    private View view;
    private long start;

    public FileStructurePopup(View view) {
        this.view = view;
    }

    public void show() {
        try {
            log("showing file structure popup");
            view.showWaitCursor();
            showPopup(view);
        } catch(Exception e) {
        } finally {
            view.hideWaitCursor();
        }
    }

    private void showPopup(View view) {
        String text = view.getTextArea().getText();
        start = now();
        Member[] members = RubyParser.getMembers(text);

        int count = members.length;
        if(count > 0) {
            JEditTextArea textArea = view.getTextArea();
            textArea.scrollToCaret(false);
            Point location = new Point(textArea.getSize().width / 3, textArea.getSize().height / 5);
            new TypeAheadPopup(view, members, location);
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private void log(String msg) {
        Log.log(Log.DEBUG, this, msg + (now() - start));
        start = now();
    }

}
