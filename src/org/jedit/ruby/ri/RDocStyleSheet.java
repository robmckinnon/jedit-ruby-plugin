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
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * @author robmckinnon at users.sourceforge.net
 */
final class RDocStyleSheet extends StyleSheet {

    private final View view;

    public RDocStyleSheet(View view) {
        this.view = view;
        addRule(body());
        addRule(pre());
        addRule(preparam());
        addRule(hr());
        addRule(tt());
        addRule(em());
    }

    private Rule preparam() {
        Rule r = new Rule("pre.param");
        r.add("color", getColor("view.fgColor"));
        return r;
    }

    private void addRule(Rule rule) {
        super.addRule(rule.toString());
    }

    private Rule hr() {
        Rule r = new Rule("hr");
        r.add("border-width", "0px");
        r.add("height", "1px");
        r.add("background-color", getColor(Token.KEYWORD2));
        return r;
    }

    private Rule body() {
        Rule r = new Rule("body");
        r.add("font-size", getViewFontSize());
        r.add("background-color", getColor("view.bgColor"));
        r.add("color", getColor("view.fgColor"));
        return r;
    }

    private Rule pre() {
        Rule r = new Rule("pre");
        r.add("font-family", "monospace");
        r.add("font-size", getViewFontSize() - 1);
        r.add("color", getColor(Token.KEYWORD2));
        return r;
    }

    private Rule tt() {
        return new Rule("tt", pre().attributes);
    }

    private Rule em() {
        Rule r = new Rule("em");
//        r.addMembers("font-style", "italic");
        r.add("color", getColor(Token.KEYWORD1));
        return r;
    }

    private static int getViewFontSize() {
        return jEdit.getFontProperty("view.font").getSize();
    }

    private String getColor(byte styleId) {
        Color color = view.getTextArea().getPainter().getStyles()[styleId].getForegroundColor();
        return getHexColor(color);
    }

    private static String getColor(String property) {
        Color color = jEdit.getColorProperty(property);
        return getHexColor(color);
    }

    private static String getHexColor(Color color) {
        StringBuffer buffer = new StringBuffer("#");
        buffer.append(getHex(color.getRed()));
        buffer.append(getHex(color.getGreen()));
        buffer.append(getHex(color.getBlue()));
        return buffer.toString();
    }

    private static String getHex(int part) {
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

    private static final class Rule {

        private final String name;
        private final Map<String, String> attributes;

        public Rule(String name, Map<String, String> attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        public Rule(String name) {
            this(name, new HashMap<String, String>());
        }

        public final void add(String attribute, int value) {
            attributes.put(attribute, "" + value);
        }

        public final void add(String attribute, String value) {
            attributes.put(attribute, value);
        }

        public final String toString() {
            StringBuffer buffer = new StringBuffer(name + " {\n");
            Set<String> keys = attributes.keySet();
            for (String attribute : keys) {
                String value = attributes.get(attribute);
                buffer.append("    ").append(attribute).append(": ").append(value).append(";\n");
            }
            buffer.append("}");
            return buffer.toString();
        }
    }

}
