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
package org.jedit.ruby.structure;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.search.RESearchMatcher;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.jedit.ruby.RubyPlugin;
import gnu.regexp.*;

/**
 * <p>This action is intended to be executed each time the enter key is pressed.
 * Add the 'ENTER' key shortcut via:<ul><li>
 * Utilities -> Global Options -> Shortcuts -> Edit Shortcuts: RubyPlugin
 * </li></ul>
 * </p>
 * <p/>
 * Currently auto inserts and indents <code>end</code> after the following patterns:<pre>
 *   if x
 *   for x
 *   while x
 *   until x
 *   unless x
 *   def x
 *   case x
 *   class x
 *   module x
 *   begin
 *   loop do
 *   y do |z|
 * </pre>
 * It also auto-aligns <code>else</code> and <code>end</code> keywords.
 * </p>
 *
 * @author robmckinnon at users.sourceforge.net
 */
public class AutoIndentAndInsertEnd {

    private static final AutoIndentAndInsertEnd instance = new AutoIndentAndInsertEnd();

    private final RE IGNORE_REG_EXP = new IgnoreRegularExpression();
    private final RE DO_REG_EXP = new DoRegularExpression();
    private final RE MATCH_REG_EXP = new MatchRegularExpression();
    private final RE END_REG_EXP = new EndRegularExpression();
    private final RE ENHANCED_END_REG_EXP = new EnhancedEndRegularExpression();
    private final RE COMMENT_REG_EXP = new CommentRegularExpression();
    private final RE TRAILING_CONDITION_REG_EXP = new TrailingConditionRegularExpression();
    private final RE IF_ELSIF_REG_EXP = new IfRegularExpression();
    private final RE WHEN_REG_EXP = new WhenRegularExpression();

    private JEditTextArea area;

    /**
     * Singleton private constructor
     */
    private AutoIndentAndInsertEnd() {
    }

    public static void performIndent(View view) {
        instance.innerPerformIndent(view);
    }

    private void innerPerformIndent(View view) {
        area = view.getTextArea();
        Buffer buffer = view.getBuffer();
        buffer.writeLock();
        buffer.beginCompoundEdit();
        try {
            doIndent();
        } finally {
            if (buffer.insideCompoundEdit()) {
                buffer.endCompoundEdit();
            }
            buffer.writeUnlock();
        }
    }

    private void doIndent() {
        area.removeTrailingWhiteSpace();
        int row = area.getCaretLine();
        String line = area.getLineText(row);
        String trimLine = line.trim();
        int caretPosition = area.getCaretPosition() - area.getLineStartOffset(row);
        boolean openingBrace = line.indexOf("{") != -1 && line.indexOf("}") == -1;

        if (caretPosition != line.length() || openingBrace) {
            if (COMMENT_REG_EXP.isMatch(line)) {
                handleComment(line, row);
            } else {
                area.insertEnterAndIndent();
            }
        } else if (trimLine.startsWith("else") || trimLine.startsWith("elsif")) {
            handleElse(trimLine, row);
        } else {
            handleInsertEnter(trimLine, row, line);
        }
    }

    private void handleInsertEnter(String trimLine, int row, String line) {
        boolean matchesConditionalAssignment = TRAILING_CONDITION_REG_EXP.isMatch(line);
        boolean matchesDo = DO_REG_EXP.isMatch(line) && !isDoInComment(line);
        boolean matchesSyntax = MATCH_REG_EXP.isMatch(line);

        boolean ignore = IGNORE_REG_EXP.isMatch(line);

        if (matchesConditionalAssignment || (!ignore && (matchesDo || matchesSyntax))) {
            handleInsertEnd(matchesDo, matchesConditionalAssignment, line);

        } else if (END_REG_EXP.isMatch(trimLine)) {
            handleEnd(trimLine, row);
        } else if (COMMENT_REG_EXP.isMatch(trimLine)) {
            handleComment(line, row);
        } else {
            area.insertEnterAndIndent();
            while(line.trim().length() == 0 && row > 0) {
                row--;
                line = area.getLineText(row);
            }
            line = line.trim();
            if(line.endsWith("; end") || line.endsWith(";end")) {
                area.shiftIndentLeft();
            }
        }
    }

    private void handleElse(String trimLine, int row) {
        area.insertEnterAndIndent();

        if (row > 0) {
            int index = row;

            while (index > 0) {
                index--;
                String line = area.getLineText(index);
                if (TRAILING_CONDITION_REG_EXP.isMatch(line) && line.indexOf("elsif") == -1) {
                    indentAfterTrailingConditionalAssignment(line, trimLine);
                    break;

                } else if(IF_ELSIF_REG_EXP.isMatch(line)) {
                    REMatch matches = IF_ELSIF_REG_EXP.getMatch(line);
                    String indent = matches.toString(1);
                    reIndent(trimLine, indent);
                    area.selectLine();
                    area.setSelectedText(indent + area.getSelectedText().trim());
                    area.shiftIndentRight();
                    break;
                }
            }
        }
    }

    private void indentAfterTrailingConditionalAssignment(String line, String trimLine) {
        REMatch matches = TRAILING_CONDITION_REG_EXP.getMatch(line);
        String indent = matches.toString(1);
        for (int i = 0; i < matches.toString(2).length(); i++) {
            indent += " ";
        }
        reIndent(trimLine, indent);
        area.selectLine();
        area.setSelectedText(indent + area.getSelectedText().trim());
        area.shiftIndentRight();

        if (matches.toString(3).startsWith("case")) {
            area.goToPrevLine(false);
            area.shiftIndentRight();
            area.goToNextLine(false);
            area.shiftIndentRight();
        }
    }

    private void reIndent(String trimLine, String indent) {
        area.goToPrevLine(false);
        area.selectLine();
        area.setSelectedText(indent + trimLine);
        area.goToNextLine(false);
    }

    private void handleComment(String line, int row) {
        area.insertEnterAndIndent();
        if (row > 0) {
            int index = row;
            while (index > 0) {
                line = area.getLineText(index);
                index--;

                if (COMMENT_REG_EXP.isMatch(line)) {
                    REMatch matches = COMMENT_REG_EXP.getMatch(line);
                    String hashes = matches.toString(2);
                    if (hashes.equals("##")) {
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

    private void handleEnd(String trimLine, int row) {
        area.insertEnterAndIndent();
        if (row > 0) {
            int index = row;
            int endCount = 0;
            while (index > 0) {
                index--;
                String line = area.getLineText(index);

                if (END_REG_EXP.isMatch(line)) {
                    endCount++;

                } else if (!IGNORE_REG_EXP.isMatch(line)) {
                    boolean isDoStatement = DO_REG_EXP.isMatch(line) && !isDoInComment(line);
                    boolean isSyntaxStatement = MATCH_REG_EXP.isMatch(line) &&
                            line.indexOf("elsif") == -1;
                    //Macros.message(view, "here " + line + isDoStatement + isSyntaxStatement);

                    if (isDoStatement || isSyntaxStatement) {
                        if (endCount > 0) {
                            endCount--;
                        } else {
                            RE re = isDoStatement ? DO_REG_EXP : MATCH_REG_EXP;
                            REMatch matches = re.getMatch(line);
                            String indent = matches.toString(1);
                            if (!isDoStatement) {
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

    private boolean isDoInComment(String line) {
        boolean inComment = false;
        int commentIndex = line.indexOf("#");
        int paramIndex = line.indexOf("#{");
        boolean hasHash = commentIndex != -1;
        boolean hashNotParamStart = commentIndex != paramIndex;

        if (hasHash && hashNotParamStart) {
            int doIndex = line.indexOf(" do ");
            if (doIndex > commentIndex) {
                inComment = true;
            }
        }
        return inComment;
    }

    private void handleInsertEnd(boolean matchesDo, boolean matchesConditionalAssignment, String line) {
        String indent;
        if(matchesConditionalAssignment) {
            REMatch matches = TRAILING_CONDITION_REG_EXP.getMatch(line);
            indent = matches.toString(1);
            for (int i = 0; i < matches.toString(2).length(); i++) {
                indent += " ";
            }
        } else {
            indent = getIndent(matchesDo, line);
        }

        area.insertEnterAndIndent();
        area.selectLine();
        area.setSelectedText(indent + "end");
        if (endsNotBalanced()) {
            area.deleteLine();
        }

        area.goToPrevLine(false);
        area.goToEndOfWhiteSpace(false);
        area.insertEnterAndIndent();
        area.selectLine();
        String text = area.getSelectedText() != null ? area.getSelectedText().trim() : "";
        area.setSelectedText(indent + text);

        if(!line.endsWith("end")) {
            area.shiftIndentRight();
        }
    }

    private boolean endsNotBalanced() {
        String line;
//        int row = area.getCaretLine();
        int count = area.getLineCount();
        int balancedCount = 0;
//        StringBuffer buffer = new StringBuffer("");
        boolean isString = false;

        for (int i = 0; i < count; i++) {
            line = area.getLineText(i).trim();
            if (ENHANCED_END_REG_EXP.isMatch(line)) {
//                buffer.append(balancedCount + "");
//                for (int j = 0; j < balancedCount; buffer.append(j++ > -1 ? "    " : "")) ;
//                buffer.append(line + "\n");
                balancedCount--;
            }
            if (line.indexOf("<<-EOF") != -1) {
                isString = true;
            } else if (line.indexOf("EOF") != -1) {
                isString = false;
            }
            if (!isString) {
                boolean isDoMatch = DO_REG_EXP.isMatch(line);
                boolean doInComment = isDoInComment(line);
//                if(line.indexOf("File.open") != -1) {
//                    buffer.append("do: " + isDoMatch + ", in comment: " + doInComment + ',' + line + '\n');
//                }
                boolean isDoStatement = isDoMatch && !doInComment;
                boolean ignore = IGNORE_REG_EXP.isMatch(line);
                boolean conditionalAssignment = TRAILING_CONDITION_REG_EXP.isMatch(line);

                if (conditionalAssignment || (!ignore && (isDoStatement || MATCH_REG_EXP.isMatch(line)))) {
                    boolean openingBrace = line.indexOf("{") != -1 && line.indexOf("}") == -1;
                    boolean elsif = line.indexOf("elsif") != -1;
                    if (!openingBrace && !elsif) {
//                        buffer.append(balancedCount + "");
//                        for (int j = 0; j < balancedCount; buffer.append(j++ > -1 ? "    " : "")) ;
//                        buffer.append(line + "\n");
                        balancedCount++;

                        int moduleIndex = line.indexOf("module");
                        while(moduleIndex != -1) {
                            moduleIndex = line.indexOf("module", moduleIndex+5);
                            if(moduleIndex != -1) {
                                balancedCount++;
                            }
                        }
                    }
                }
            }
        }

//        RubyPlugin.log(buffer.toString(), AutoIndentAndInsertEnd.class);
        boolean endsNotBalanced = balancedCount < 0;
        RubyPlugin.log("Ends " + (endsNotBalanced ? "not " : "") + "balanced", AutoIndentAndInsertEnd.class);
        return endsNotBalanced;
    }

    private String getIndent(boolean matchesDo, String line) {
        RE regExp = null;
        if (matchesDo) {
            regExp = DO_REG_EXP;
        } else {
            regExp = MATCH_REG_EXP;
        }
        REMatch matches = regExp.getMatch(line);
        String indent = matches.toString(1);

        if (!matchesDo && line.indexOf("begin") == -1 && line.indexOf("do") == -1) {
            String leadingText = matches.toString(2);

            for (int i = 0; i < leadingText.length(); i++) {
                indent += " ";
            }
        }
        return indent;
    }

    private static abstract class RegularExpression extends RE {
        public RegularExpression() {
            try {
                initialize(getPattern(), 0, RESearchMatcher.RE_SYNTAX_JEDIT, 0, 0);
            } catch (REException e) {
                RubyPlugin.error(e, getClass());
            }
        }

        protected abstract String getPattern();
    }

    /**
     * matches lines to ignore
     */
    private static class IgnoreRegularExpression extends RegularExpression {
        protected String getPattern() {
            return "((.*)(" +
                    "([[:graph:]]\\s+(if|unless)(\\s+\\S+)+)" +
                    ")\\s*)" +
                    "|" +
                    "([^\"]*(\"|')[^\"]*" +
                    "(if|for|while|until|unless|def|case|class|module|do|begin|loop[ ]do)" +
                    "[^\"]*(\"|')[^\"]*)";
        }
    }

    /**
     * matches x.y do |z| expressions
     */
    private static class DoRegularExpression extends RegularExpression {
        protected String getPattern() {
            return "(\\s*)(\\S+\\s+)+do\\s+\\|+[^\\|]*\\|\\s*";
        }
    }

    /**
     * matches other syntax that requires end
     */
    private static class MatchRegularExpression extends RegularExpression {
        protected String getPattern() {
            String indent = "(\\s*)";
            String leadingText = "([^#]*)";
            String trailingSpace = "\\s*";

            return indent + leadingText
                    + "("
                    + "((if|for|while|until|unless|def|case|class|module)(\\s+\\S+)+)"
                    + "|"
                    + "(begin|loop[ ]do|do)"
                    + ")" + trailingSpace;
        }
    }

    private static class EndRegularExpression extends RegularExpression {
        protected String getPattern() {
            return "[^#]*end\\s*";
        }
    }

    private static class EnhancedEndRegularExpression extends RegularExpression {
        protected String getPattern() {
            return "[^#]*end(\\s*|(\\s+.*))";
        }
    }

    private static class CommentRegularExpression extends RegularExpression {
        protected String getPattern() {
            return "(\\s*)(##?)(.*)";
        }
    }

    private static class TrailingConditionRegularExpression extends RegularExpression {
        protected String getPattern() {
            return "(\\s*)([^#]*=\\s*)(((if)|(unless)|(case)).*)";
        }
    }

    private static class IfRegularExpression extends RegularExpression {
        protected String getPattern() {
            return "(\\s*)((if)|(elsif))(.*)";
        }
    }

    private static class WhenRegularExpression extends RegularExpression {
        protected String getPattern() {
            return "(\\s*)(when)(.*)";
        }
    }

}
