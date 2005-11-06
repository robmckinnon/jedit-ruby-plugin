/*
 * RubyPlugin.java - Ruby editor plugin for jEdit
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

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.syntax.DefaultTokenHandler;
import org.gjt.sp.jedit.syntax.Token;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public final class RubyTokenHandler extends DefaultTokenHandler {

    public RubyToken getTokenAtCaret(Buffer buffer, int caret) {
        init(); // reset
        int line = buffer.getLineOfOffset(caret);
        buffer.markTokens(line, this);
        int offset = caret;
        offset -= buffer.getLineStartOffset(line);
        if (offset != 0) {
            offset--;
        }
        Token token = TextUtilities.getTokenAtOffset(firstToken, offset);
        return new RubyToken(token, firstToken);
    }
}
