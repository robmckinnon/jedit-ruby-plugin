/*
 * AutoIndentAndInsertEnd.java - auto indents and inserts end keyword
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

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.search.RESearchMatcher;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import gnu.regexp.*;

/**
 * TO INSTALL:
 *
 * 1) First install Ruby edit mode 0.3.
 *    This macro is designed to work with the auto-indenting
 *    defined in Ruby edit mode v0.3.
 *
 * 2) Place this macro in directory: [user_home_dir]/.jedit/macros/Ruby
 *
 * 3) This macro is intended to be executed each time the enter key is pressed.
 *    Add the 'ENTER' key shortcut via:
 *       Utilities -> Global Options -> Shortcuts -> Edit Shortcuts: Macros
 *
 * ABOUT:
 *    Currently auto inserts and indents 'end' after the following patterns:
 *      if <x>, for <x>, while <x>, until <x>, unless <x>, def <x>,
 *      case <x>, class <x>, module <x>, begin, loop do, <y> do |<z>|
 *
 *    It also auto-aligns else and end keywords.
 * @author robmckinnon at users.sourceforge.net
 */
public class AutoIndentAndInsertEnd {
    private JEditTextArea area;
    private View view;

    public AutoIndentAndInsertEnd(View view) {
// start = System.currentTimeMillis();
        this.view = view;
        area = view.getTextArea();
// end = System.currentTimeMillis();
// Macros.message(view, "" + (end - start));
    }

    public void performIndent() throws REException {
        Buffer buffer = view.getBuffer();

        if(!buffer.getMode().getName().equals("ruby")) {
            area.insertEnterAndIndent();
        } else {
            buffer.writeLock();
            buffer.beginCompoundEdit();
            try {
                area.removeTrailingWhiteSpace();
                int row = area.getCaretLine();
                String line = area.getLineText(row);
                String trimLine = line.trim();
                int caretPosition = area.getCaretPosition() - area.getLineStartOffset(row);
                boolean openingBrace = line.indexOf("{") != -1 && line.indexOf("}") == -1;
                RE commentRegExp = new RE("(\\s*)(##?)(.*)", 0, RESearchMatcher.RE_SYNTAX_JEDIT);

                if(caretPosition != line.length() || openingBrace) {
                    if(commentRegExp.isMatch(line)) {
                        handleComment(line, commentRegExp, row);
                    } else {
                        area.insertEnterAndIndent();
                    }
                } else if(trimLine.startsWith("else") || trimLine.startsWith("elsif")) {
                    handleElse(trimLine, row);
                } else {
                    handleInsertEnter(trimLine, row, commentRegExp, line);
                }
            } finally {
                if(buffer.insideCompoundEdit()) {
                    buffer.endCompoundEdit();
                }
                buffer.writeUnlock();                
            }
        }
    }
    void handleElse(String trimLine, int row) throws REException {
        area.insertEnterAndIndent();

        if(row > 0) {
            RE re = new RE("(\\s*)([^#]*)(((if)|(unless)|(case)).*)", 0, RESearchMatcher.RE_SYNTAX_JEDIT);
            int index = row;

            while(index > 0) {
                index--;
                String line = area.getLineText(index);
                if(re.isMatch(line) && line.indexOf("elsif") == -1) {
                    REMatch matches = re.getMatch(line);
                    String indent = matches.toString(1);
                    for (int i = 0; i < matches.toString(2).length(); i++) {
                        indent += " ";
                    }
                    reIndent(trimLine, indent);
                    area.selectLine();
                    area.setSelectedText(indent + area.getSelectedText().trim());
                    area.shiftIndentRight();

                    if(matches.toString(3).startsWith("case")) {
                        area.goToPrevLine(false);
                        area.shiftIndentRight();
                        area.goToNextLine(false);
                        area.shiftIndentRight();
                    }
                    break;
                }
            }
        }
    }

    void reIndent(String trimLine, String indent) {
        area.goToPrevLine(false);
        area.selectLine();
        area.setSelectedText(indent + trimLine);
        area.goToNextLine(false);
    }

    void handleComment(String line, RE commentRegExp, int row) {
        area.insertEnterAndIndent();
        if(row > 0) {
            int index = row;
            while(index > 0) {
                line = area.getLineText(index);
                index--;

                if(commentRegExp.isMatch(line)) {
                    REMatch matches = commentRegExp.getMatch(line);
                    String hashes = matches.toString(2);
                    if(hashes.equals("##")) {
                        String indent = matches.toString(1);
                        area.selectLine();
                        String text = area.getSelectedText() == null ? "" : area.getSelectedText();
                        text = text.trim();
                        area.setSelectedText(indent + "# " + text);
                        area.goToEndOfLine(false);
                        break;
                    }
                } else {
                    break;
                }
            }
        }

    }

    void handleEnd(String trimLine, RE endRegExp, RE doRegExp, RE syntaxRegExp, RE ignoreRegExp, int row) {
        area.insertEnterAndIndent();
        if(row > 0) {
            int index = row;
            int endCount = 0;
            while(index > 0) {
                index--;
                String line = area.getLineText(index);

                if(endRegExp.isMatch(line)) {
                    endCount++;

                } else if(!ignoreRegExp.isMatch(line)) {
                    boolean isDoStatement = doRegExp.isMatch(line) && !isDoInComment(line);
                    boolean isSyntaxStatement = syntaxRegExp.isMatch(line) &&
                      line.indexOf("elsif") == -1;
                    //Macros.message(view, "here " + line + isDoStatement + isSyntaxStatement);

                    if(isDoStatement || isSyntaxStatement) {
                        if(endCount > 0) {
                            endCount--;
                        } else {
                            RE re = isDoStatement ? doRegExp : syntaxRegExp;
                            REMatch matches = re.getMatch(line);
                            String indent = matches.toString(1);
                            if(!isDoStatement) {
                                for (int i = 0; i < matches.toString(2).length(); i++) {
                                    indent += " ";
                                }
                            }
                            reIndent(trimLine, indent);
                            area.selectLine();
                            area.setSelectedText(matches.toString(1));
                            break;
                        }
                    }
                }
            }
        }
    }

    boolean isDoInComment(String line) {
        boolean inComment = false;
        int commentIndex = line.indexOf("#");
        if(commentIndex != -1) {
            int doIndex = line.indexOf(" do ");
            if(doIndex > commentIndex) {
                inComment = true;
            }
        }
        return inComment;
    }

    void handleInsertEnd(boolean matchesDo, RE doRegExp, RE syntaxRegExp, RE ignoreRegExp, String line) throws REException {
        area.insertEnterAndIndent();
        RE regExp = null;
        if(matchesDo) {
            regExp = doRegExp;
        } else {
            regExp = syntaxRegExp;
        }
        REMatch matches = regExp.getMatch(line);
        String indent = matches.toString(1);
        if(!matchesDo && line.indexOf("begin") == -1) {
            for(int i = 0; i < matches.toString(2).length(); i++) {
                indent += " ";
            }
        }

        area.selectLine();
        area.setSelectedText(indent + "end");

//        int row = area.getCaretLine();
        int count = area.getLineCount();
        int balanced = 0;
        RE endRegExp = new RE("[^#]*end(\\s*|(\\s+.*))");
        // buffer = new StringBuffer("");
        boolean isString = false;

        for(int i = 0; i < count; i++) {
            line = area.getLineText(i).trim();
            if(endRegExp.isMatch(line)) {
                // buffer.append(balanced + "");
                // for(int i=0; i < balanced; buffer.append(i++ > -1 ? "    " : ""));
                // buffer.append(line+"\n");
                balanced -= 1;
            }
            if(line.indexOf("<<-EOF") != -1) {
                isString = true;
            } else if(line.indexOf("EOF") != -1) {
                isString = false;
            }
            if(!isString) {
                boolean isDoStatement = doRegExp.isMatch(line) && !isDoInComment(line);
                boolean ignore = ignoreRegExp.isMatch(line);

                if(!ignore && (isDoStatement || syntaxRegExp.isMatch(line))) {
                    boolean openingBrace = line.indexOf("{") != -1 && line.indexOf("}") == -1;
                    boolean elsif = line.indexOf("elsif") != -1;
                    if(!openingBrace && !elsif) {
                        // buffer.append(balanced + "");
                        // for(int i=0; i < balanced; buffer.append(i++ > -1 ? "    " : ""));
                        // buffer.append(line+"\n");
                        balanced += 1;
                    }
                }
            }
        }

        // Macros.message(view, buffer.toString());
        if(balanced < 0) {
            area.deleteLine();
        }

        area.goToPrevLine(false);
        area.goToEndOfWhiteSpace(false);
        area.insertEnterAndIndent();
        area.selectLine();
        String text = area.getSelectedText() != null ? area.getSelectedText().trim() : "";
        area.setSelectedText(indent + text);
        area.shiftIndentRight();
    }

    void handleInsertEnter(String trimLine, int row, RE commentRegExp, String line) throws REException {
        // matches <x>.<y> do |<z>| expressions
        String doExp = "(\\s*)(\\S+\\s+)+do\\s+\\|+[^\\|]*\\|\\s*";

        // matches other syntax that requires end
        String syntaxExp = "(\\s*)([^#]*)(" +
            "((if|for|while|until|unless|def|case|class|module)(\\s+\\S+)+)|" +
            "(begin|loop[ ]do|do)" +
            ")\\s*";

        String ignoreExp = "((.*)(" +
            "([[:graph:]]\\s+(if|unless)(\\s+\\S+)+)" +
            ")\\s*)" +
            "|" +
            "([^\"]*(\"|')[^\"]*" +
            "(if|for|while|until|unless|def|case|class|module|do|begin|loop[ ]do)" +
            "[^\"]*(\"|')[^\"]*)";

        RE doRegExp = new RE(doExp, 0, RESearchMatcher.RE_SYNTAX_JEDIT);
        RE syntaxRegExp = new RE(syntaxExp, 0, RESearchMatcher.RE_SYNTAX_JEDIT);
        RE endRegExp = new RE("[^#]*end\\s*");
        RE ignoreRegExp = new RE(ignoreExp, 0, RESearchMatcher.RE_SYNTAX_JEDIT);

        boolean matchesDo = doRegExp.isMatch(line) && !isDoInComment(line);
        boolean matchesSyntax = syntaxRegExp.isMatch(line);

        boolean ignore = ignoreRegExp.isMatch(line);

        if(!ignore && (matchesDo || matchesSyntax)) {
            handleInsertEnd(matchesDo, doRegExp, syntaxRegExp, ignoreRegExp, line);

        } else if(endRegExp.isMatch(trimLine)) {
            handleEnd(trimLine, endRegExp, doRegExp, syntaxRegExp, ignoreRegExp, row);
        } else if(commentRegExp.isMatch(trimLine)) {
            handleComment(line, commentRegExp, row);
        } else {
            area.insertEnterAndIndent();
        }

    }

}
