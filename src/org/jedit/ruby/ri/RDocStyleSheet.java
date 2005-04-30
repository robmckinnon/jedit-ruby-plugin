/*
 * RDocStyleSheet.java - 
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

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.syntax.Token;

import javax.swing.text.html.StyleSheet;
import java.awt.Color;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class RDocStyleSheet extends StyleSheet {
    private View view;

    public RDocStyleSheet(View view) {
        this.view = view;
        int size = jEdit.getFontProperty("view.font").getSize();

        addRule("body {" +
                        "font-size: " + size + ";\n" +
                        "color: " + getHexColor("view.fgColor") + ";\n" +
                        "background-color: " + getHexColor("view.bgColor") + ";\n" +
                   "}");
        addRule("tt {" +
                        "font-family: monospace;\n" +
                        "font-size: " + (size - 1) + ";\n" +
                        "color: " + getColor(Token.KEYWORD2) + ";\n" +
                   "}");
        addRule("pre {" +
                        "font-family: monospace;\n" +
                        "font-size: " + (size - 1) + ";\n" +
                        "color: " + getColor(Token.KEYWORD2) + ";\n" +
                   "}");
        addRule("em {" +
//                        "font-style: italic;\n" +
                        "color: " + getColor(Token.KEYWORD1) + ";\n" +
                   "}");
    }

    private String getColor(byte styleId) {
        Color color = view.getTextArea().getPainter().getStyles()[styleId].getForegroundColor();
        return getHexColor(color);
    }

    private String getHex(int part) {
        String hex = Integer.toHexString(part);
        int length = hex.length();
        if (length == 0) {
            return "00";
        } else if(length == 1) {
            return "0" + hex;
        } else {
            return hex;
        }
    }

    private String getHexColor(String property) {
        Color color = jEdit.getColorProperty(property);
        return getHexColor(color);
    }

    private String getHexColor(Color color) {
        StringBuffer buffer = new StringBuffer("#");
        buffer.append(getHex(color.getRed()));
        buffer.append(getHex(color.getGreen()));
        buffer.append(getHex(color.getBlue()));
        return buffer.toString();
    }

}
