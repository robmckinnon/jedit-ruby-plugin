/*
 * TopLineBorder.java - 
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

import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

/**
 * @author robmckinnon at users.sourceforge.net
 */
class TopLineBorder extends LineBorder {

    public TopLineBorder(Color color) {
        super(color);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();
        int i;
        g.setColor(lineColor);
        for (i = 0; i < thickness; i++) {
            int newWidth = width - i - i - 1;
            int x1 = x + i;
            int y1 = y + i;
            int x2 = x1 + newWidth;
            g.drawLine(x1, y1, x2, y1);
        }
        g.setColor(oldColor);
    }
}
