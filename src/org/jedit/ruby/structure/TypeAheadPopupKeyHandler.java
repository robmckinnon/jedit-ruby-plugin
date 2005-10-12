/*
 * TypeAheadPopupKeyHandler.java -
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
package org.jedit.ruby.structure;

import org.jedit.ruby.structure.*;
import org.jedit.ruby.structure.TypeAheadPopup;
import org.jedit.ruby.RubyPlugin;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @author robmckinnon at users.sourceforge.net
 */
final class TypeAheadPopupKeyHandler extends KeyAdapter {

    private final TypeAheadPopup typeAheadPopup;
    private final String validCharacters;

    public TypeAheadPopupKeyHandler(TypeAheadPopup typeAheadPopup) {
        this.typeAheadPopup = typeAheadPopup;
        validCharacters = "_(),.[]";
    }

    public final void keyPressed(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                handleEscapePressed(event);
                break;

            case KeyEvent.VK_UP:
                incrementSelection(event, -1);
                break;

            case KeyEvent.VK_DOWN:
                incrementSelection(event, 1);
                break;

            case KeyEvent.VK_PAGE_UP:
            case KeyEvent.VK_LEFT:
                incrementSelection(event, -1 * (TypeAheadPopup.VISIBLE_LIST_SIZE - 1));
                break;

            case KeyEvent.VK_PAGE_DOWN:
            case KeyEvent.VK_RIGHT:
                incrementSelection(event, (org.jedit.ruby.structure.TypeAheadPopup.VISIBLE_LIST_SIZE - 1));
                break;

            case KeyEvent.VK_HOME:
                incrementSelection(event, -1 * getListSize());
                break;

            case KeyEvent.VK_END:
                incrementSelection(event, getListSize());
                break;

            case KeyEvent.VK_BACK_SPACE:
                handleBackSpacePressed(event);
                break;

            case KeyEvent.VK_F4:
                handleSelection(event, false);

            default:
                handleOtherKeys(event);
                break;
        }
    }

    private void handleOtherKeys(KeyEvent event) {
        if (event.isAltDown() || event.isMetaDown()) {
            char keyChar = event.getKeyChar();
            boolean handled = typeAheadPopup.handleAltPressedWith(keyChar);
            if (handled) {
                event.consume();
            }
        }
    }

    public final void keyTyped(KeyEvent event) {
        char character = event.getKeyChar();
        int keyCode = event.getKeyCode();
        int keyChar = event.getKeyChar();
        if (keyCode == KeyEvent.VK_TAB || keyCode == KeyEvent.VK_ENTER ||
                keyChar == KeyEvent.VK_TAB || keyChar == KeyEvent.VK_ENTER) {
            handleSelection(event, true);

        } else {
            if (!RubyPlugin.ignoreKeyTyped(keyCode, character, event)) {
                handleCharacterTyped(character, event);
            }
        }
    }

    private void handleBackSpacePressed(KeyEvent event) {
        typeAheadPopup.handleBackSpacePressed();
        event.consume();
    }

    private int getListSize() {
        return typeAheadPopup.getListSize();
    }

    private void incrementSelection(KeyEvent event, int increment) {
        typeAheadPopup.incrementSelection(increment);
        event.consume();
    }

    private void handleEscapePressed(KeyEvent event) {
        typeAheadPopup.handleEscapePressed();
        event.consume();
    }

    private void handleSelection(KeyEvent event, boolean showMenu) {
        event.consume();
        typeAheadPopup.handleSelection(showMenu);
    }

    private void handleCharacterTyped(char character, KeyEvent event) {
        boolean valid = Character.isLetterOrDigit(character)
                || validCharacters.indexOf(character) != -1;

        if (valid) {
            typeAheadPopup.updateMatchedMembers(character);
            event.consume();
        }
    }
}
