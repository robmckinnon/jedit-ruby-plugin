/*
 * CharCaretListener.java - 
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

package org.jedit.ruby.utils;

import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.jedit.ruby.structure.RubyTokenHandler;
import org.jedit.ruby.structure.RubyToken;
import org.jedit.ruby.structure.BufferChangeHandler;

import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class CharCaretListener implements CaretListener {

    private static final RubyTokenHandler tokenHandler = new RubyTokenHandler();
    private static final char NONE = (char)-1;

    private static RubyToken lastToken = null;
    private static RubyToken currentToken = null;
    private static char charLastBehind = NONE;
    private static char charBehind = NONE;
    private static char charAhead = NONE;

    public static boolean hasCharLastBehind() {
        return charLastBehind != NONE;
    }

    public static char getCharBehind() {
        return charBehind;
    }

    public static char getCharLastBehind() {
        return charLastBehind;
    }

    public static char getCharAhead() {
        return charAhead;
    }

    public static RubyToken getLastToken() {
        return lastToken;
    }

    public static RubyToken getCurrentToken() {
        return currentToken;
    }

    public final void caretUpdate(CaretEvent e) {
        if (!BufferChangeHandler.instance().isGotoPreviousEditAction()) {
            BufferChangeHandler.instance().resetEditLocationIndex();
        }
        int mark = e.getMark();
        int dot = e.getDot();
        charLastBehind = charBehind;
        lastToken = currentToken;

        if (dot == mark && dot > 0) {
            JEditTextArea textArea = (JEditTextArea)e.getSource();
            String text = textArea.getText();
            charBehind = text.charAt(dot - 1);
            currentToken = tokenHandler.getTokenAtCaret(CommandUtils.getBuffer(textArea), dot);

            if (text.length() > dot) {
                charAhead = text.charAt(dot);
            } else {
                charAhead = NONE;
            }
        } else {
            currentToken = null;
            lastToken = null;
            charBehind = NONE;
            charLastBehind = NONE;
            charAhead = NONE;
        }
    }

}