/*
 * RubyCompletion.java -
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

import sidekick.SideKickCompletion;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.Buffer;
import org.jedit.ruby.ast.Method;

import java.util.List;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RubyCompletion extends SideKickCompletion {

    private CodeCompletor completor;
    private View view;
    private List<Method> methods;

    public RubyCompletion(View view, CodeCompletor completor) {
        this.view = view;
        this.completor = completor;
        methods = completor.getMethods();
    }

    public int getTokenLength() {
        String partialMethod = completor.getPartialMethod();
        if(partialMethod == null) {
            return 1;
        } else {
            return partialMethod.length() + 1;
        }
    }

    public boolean handleKeystroke(int selectedIndex, char keyChar) {
        switch(keyChar) {
            case ' ': case '\t': case '\n':
                // execute below code
                break;
            default:
                view.getTextArea().userInput(keyChar);
                return true;
        }

        RubyPlugin.log("selected: " + selectedIndex, getClass());
        RubyPlugin.log("key: " + keyChar, getClass());
        if(selectedIndex != -1)
            insert(methods.get(selectedIndex), keyChar);
        else
            view.getTextArea().userInput(keyChar);

        return false;
    }

    public void insert(int index) {
        insert(methods.get(index),'\n');
    }

    private void insert(Method method, char keyChar) {
        Buffer buffer = view.getBuffer();
        RubyPlugin.log("method: " + method.getName(), getClass());
        int caretPosition = view.getTextArea().getCaretPosition();
        int offset = caretPosition;
        String partialMethod = completor.getPartialMethod();

        if(partialMethod != null) {
            offset -= partialMethod.length();
            buffer.remove(offset, partialMethod.length());
        }

        buffer.insert(offset, method.getName());
    }

    public int size() {
        return methods.size();
    }

    public Object get(int index) {
        return methods.get(index);
    }

}
