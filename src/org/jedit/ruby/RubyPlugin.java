/*
 * RubyPlugin.java - Ruby plugin for jEdit
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

import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.Buffer;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public class RubyPlugin extends EditPlugin {

    public void start() {
        super.start();
        JRubyParser.setExpectedLabel(jEdit.getProperty("ruby.syntax-error.expected.label"));
        JRubyParser.setFoundLabel(jEdit.getProperty("ruby.syntax-error.found.label"));
        JRubyParser.setNothingLabel(jEdit.getProperty("ruby.syntax-error.nothing.label"));
    }

    public void stop() {
        super.stop();
    }

    public static int getNonSpaceStartOffset(int line) {
        int offset = 0;
        View view = jEdit.getActiveView();
        if (view != null) {
            Buffer buffer = view.getBuffer();
            if (buffer != null) {
                offset = buffer.getLineStartOffset(line);
                int end = buffer.getLineEndOffset(line);
                String text = buffer.getLineText(line);

                if(text.length() > 0) {
                    int index = 0;
                    while (text.charAt(index) == ' ' && (offset - index) < end) {
                        index++;
                    }
                    offset += index;
                }
            }
        }

        return offset;
    }

    public static int getEndOffset(int line) {
        int offset = 0;
        View view = jEdit.getActiveView();
        if (view != null) {
            Buffer buffer = view.getBuffer();
            if (buffer != null) {
                offset = buffer.getLineEndOffset(line) - 1;
            }
        }

        return offset;
    }

    public static int getStartOffset(int line) {
        int startOffset = 0;
        View view = jEdit.getActiveView();
        if (view != null) {
            Buffer buffer = view.getBuffer();
            if (buffer != null) {
                startOffset = buffer.getLineStartOffset(line);
            }
        }
        return startOffset;
    }
}
