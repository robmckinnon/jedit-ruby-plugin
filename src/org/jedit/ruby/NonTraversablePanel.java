/*
 * NonTraversablePanel.java - 
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

import javax.swing.JPanel;
import java.awt.LayoutManager;

/**
 * @author robmckinnon at users.sourceforge.net
 */
class NonTraversablePanel extends JPanel {
    public NonTraversablePanel(LayoutManager layout) {
        super(layout);
    }

    /**
     * Returns false to indicate this component can't
     * be traversed by pressing the Tab key.
     */
    public boolean isManagingFocus() {
        return false;
    }

    /**
     * Makes the tab key work in Java 1.4.
     */
    public boolean getFocusTraversalKeysEnabled() {
        return false;
    }
}