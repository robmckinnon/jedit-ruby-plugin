/*
 * RDocViewerKeyHandler.java -
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
package org.jedit.ruby.ri;

import org.jedit.ruby.RubyPlugin;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @author robmckinnon at users.sourceforge.net
 */
final class RDocViewerKeyHandler extends KeyAdapter {

    private final RDocViewer viewer;
    private boolean lastWasBackspace;

    public RDocViewerKeyHandler(RDocViewer viewer) {
        this.viewer = viewer;
    }

    public final boolean lastWasBackspace() {
        return lastWasBackspace;
    }

    public final void keyPressed(KeyEvent event) {
        lastWasBackspace = false;

        switch (event.getKeyCode()) {
            case KeyEvent.VK_F4:
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
                pageUp(event);
                break;

            case KeyEvent.VK_PAGE_DOWN:
                pageDown(event);
                break;

            case KeyEvent.VK_HOME:
                home(event);
                break;

            case KeyEvent.VK_END:
                end(event);
                break;

            case KeyEvent.VK_BACK_SPACE:
                handleBackSpacePressed();
                break;

            default:
                handleOtherKeys(event);
                break;
        }
    }

    private void end(KeyEvent event) {
        viewer.end();
        event.consume();
    }

    private void home(KeyEvent event) {
        viewer.home();
        event.consume();
    }

    private void pageDown(KeyEvent event) {
        viewer.pageDown();
        event.consume();
    }

    private void pageUp(KeyEvent event) {
        viewer.pageUp();
        event.consume();
    }

    private static void handleOtherKeys(KeyEvent event) {
        if (event.isAltDown() || event.isMetaDown()) {
//            char keyChar = event.getKeyChar();
//            boolean handled = false;
//            if (handled) {
//                event.consume();
//            }
        }
    }

    public final void keyTyped(KeyEvent event) {
        char character = event.getKeyChar();
        int keyCode = event.getKeyCode();
        int keyChar = event.getKeyChar();
        boolean tabOrEnter = keyCode == KeyEvent.VK_TAB || keyCode == KeyEvent.VK_ENTER ||
                        keyChar == KeyEvent.VK_TAB || keyChar == KeyEvent.VK_ENTER;
        
        if (!tabOrEnter) {
            if (!RubyPlugin.ignoreKeyTyped(keyCode, character, event)) {
                handleCharacterTyped(character, event);
            }
        }
    }

    private void handleBackSpacePressed() {
        lastWasBackspace = true;
        viewer.handleBackSpacePressed();
    }

    private void incrementSelection(KeyEvent event, int increment) {
        viewer.incrementSelection(increment);
        event.consume();
    }

    private void handleEscapePressed(KeyEvent event) {
        viewer.handleEscapePressed();
        event.consume();
    }

    private void handleCharacterTyped(char character, KeyEvent event) {
        if (viewer.consumeKeyEvent(character)) {
            event.consume();
        }
    }
}
