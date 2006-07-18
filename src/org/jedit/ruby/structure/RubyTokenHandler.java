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

import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.syntax.DefaultTokenHandler;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.syntax.TokenHandler;
import org.jedit.ruby.utils.CommandUtils;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public final class RubyTokenHandler extends DefaultTokenHandler {

    /**
     * In order to work with both jEdit 4.2 and 4.3,
     * this method takes a buffer as an Object parameter.
     *
     * In 4.2 buffer will be of type org.gjt.sp.jedit.Buffer,
     * in 4.3 buffer will be of type org.gjt.sp.jedit.buffer.JEditBuffer.
     * @param buffer in 4.2 Buffer in 4.3 JEditBuffer
     * @param caret
     * @return token at caret
     */
    public RubyToken getTokenAtCaret(Object buffer, int caret) {
        init(); // reset
        try {
            Buffer aBuffer = ((Buffer)buffer);
            int line = aBuffer.getLineOfOffset(caret);
            aBuffer.markTokens(line, this);
//            int line = (Integer)CommandUtils.invoke(buffer, "getLineOfOffset", new Class[]{Integer.class}, caret);
//            CommandUtils.invoke(buffer, "markTokens", new Class[]{Integer.class, TokenHandler.class}, line, this);
            int offset = caret;
            offset -= (Integer)CommandUtils.invoke(buffer, "getLineStartOffset", new Class[]{Integer.class}, line);
            if (offset != 0) {
                offset--;
            }
            Token token = TextUtilities.getTokenAtOffset(firstToken, offset);
            return new RubyToken(token, firstToken);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
