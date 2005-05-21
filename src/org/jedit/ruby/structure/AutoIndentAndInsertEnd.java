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
            if (CommentRegExp.instance.isMatch(line)) {
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
        boolean matchesConditionalAssignment = TrailingConditionRegExp.instance.isMatch(line);
        boolean matchesDo = DoRegExp.instance.isMatch(line) && !isDoInComment(line);
        boolean matchesSyntax = MatchRegExp.instance.isMatch(line);

        boolean ignore = IgnoreRegExp.instance.isMatch(line);

        if (matchesConditionalAssignment) {
            handleInsertEnd(TrailingConditionRegExp.instance, line);

        } else if (!ignore && matchesDo) {
            handleInsertEnd(DoRegExp.instance, line);

        } else if (!ignore && matchesSyntax) {
            handleInsertEnd(MatchRegExp.instance, line);

        } else if (EndRegExp.instance.isMatch(trimLine)) {
            handleEnd(trimLine, row);
        } else if (CommentRegExp.instance.isMatch(trimLine)) {
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
                if (TrailingConditionRegExp.instance.isMatch(line) && line.indexOf("elsif") == -1) {
                    indentAfterTrailingConditionalAssignment(line, trimLine);
                    break;

                } else if(IfElsifRegExp.instance.isMatch(line)) {
                    REMatch matches = IfElsifRegExp.instance.getMatch(line);
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
        String indent = TrailingConditionRegExp.instance.indent(line);
        reIndent(trimLine, indent);
        area.selectLine();
        area.setSelectedText(indent + area.getSelectedText().trim());
        area.shiftIndentRight();

        REMatch matches = TrailingConditionRegExp.instance.getMatch(line);
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

                if (CommentRegExp.instance.isMatch(line)) {
                    REMatch matches = CommentRegExp.instance.getMatch(line);
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

                if (EndRegExp.instance.isMatch(line)) {
                    endCount++;

                } else if (!IgnoreRegExp.instance.isMatch(line) || TrailingConditionRegExp.instance.isMatch(line)) {
                    boolean notElse = line.indexOf("elsif") == -1 && line.indexOf("else") == -1;

                    boolean isDoStatement = DoRegExp.instance.isMatch(line) && !isDoInComment(line);
                    boolean isSyntaxStatement = MatchRegExp.instance.isMatch(line) && notElse;
                    boolean isTrailingCondition = TrailingConditionRegExp.instance.isMatch(line);

//                    Macros.message(area, line + " "
//                            + isDoStatement + " " + isSyntaxStatement + " " + isTrailingCondition);

                    if (isDoStatement || isSyntaxStatement || isTrailingCondition) {
                        if (endCount > 0) {
                            endCount--;
                        } else {
                            RegularExpression re = isDoStatement ? DoRegExp.instance : MatchRegExp.instance;
                            re = isTrailingCondition ? TrailingConditionRegExp.instance : re;
                            String indent = re.indent(line);
                            reIndent(trimLine, indent);

                            REMatch matches = re.getMatch(line);
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

    private void handleInsertEnd(RegularExpression re, String line) {
        String indent = re.indent(line);

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
            if (hasEndKeyword(line)) {
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
                boolean isDoMatch = DoRegExp.instance.isMatch(line);
                boolean doInComment = isDoInComment(line);
//                if(line.indexOf("File.open") != -1) {
//                    buffer.append("do: " + isDoMatch + ", in comment: " + doInComment + ',' + line + '\n');
//                }
                boolean isDoStatement = isDoMatch && !doInComment;
                boolean ignore = IgnoreRegExp.instance.isMatch(line);
                boolean conditionalAssignment = TrailingConditionRegExp.instance.isMatch(line);

                if (conditionalAssignment || (!ignore && (isDoStatement || MatchRegExp.instance.isMatch(line)))) {
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

    public static boolean hasEndKeyword(String line) {
        return EnhancedEndRegExp.instance.isMatch(line) ||
                EnhancedEndRegExp2.instance.isMatch(line);
    }

    public static abstract class RegularExpression extends RE {
        public RegularExpression() {
            try {
                initialize(getPattern(), 0, RESearchMatcher.RE_SYNTAX_JEDIT, 0, 0);
            } catch (REException e) {
                RubyPlugin.error(e, getClass());
            }
        }

        protected abstract String getPattern();

        String indent(String line) {
            return getMatch(line).toString(1);
        }
    }

    private static abstract class IndentRegularExpression extends RegularExpression {
        String indent(String line) {
            REMatch match = instance().getMatch(line);
            StringBuffer indent = new StringBuffer(match.toString(1));

            if(extraIndent(line)) {
                for (int i = 0; i < match.toString(2).length(); i++) {
                    indent.append(" ");
                }
            }

            return indent.toString();
        }

        protected abstract RE instance();

        boolean extraIndent(String line) {
            return true;
        }
    }

    /**
     * matches other syntax that requires end
     */
    private static class MatchRegExp extends IndentRegularExpression {
        private static final RegularExpression instance = new MatchRegExp();
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

        protected RE instance() {
            return instance;
        }

        boolean extraIndent(String line) {
            return line.indexOf("begin") == -1 && line.indexOf("do") == -1;
        }
    }

    /**
     * matches lines to ignore
     */
    private static class IgnoreRegExp extends RegularExpression {
        private static final RE instance = new IgnoreRegExp();
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
    private static class DoRegExp extends RegularExpression {
        private static final RegularExpression instance = new DoRegExp();
        protected String getPattern() {
            return "(\\s*)(\\S+\\s+)+do\\s+\\|+[^\\|]*\\|\\s*";
        }
    }

    private static class EndRegExp extends RegularExpression {
        private static final RE instance = new EndRegExp();
        protected String getPattern() {
            return "[^#]*end\\s*";
        }
    }

    private static class EnhancedEndRegExp extends RegularExpression {
        private static final RE instance = new EnhancedEndRegExp();
        protected String getPattern() {
            return "^end(\\s*|(\\s+.*))";
        }
    }

    private static class EnhancedEndRegExp2 extends RegularExpression {
        private static final RE instance = new EnhancedEndRegExp();
        protected String getPattern() {
            return "[^#]*\\s+end(\\s*|(\\s+.*))";
        }
    }

    private static class CommentRegExp extends RegularExpression {
        private static final RE instance = new CommentRegExp();
        protected String getPattern() {
            return "(\\s*)(##?)(.*)";
        }
    }

    private static class TrailingConditionRegExp extends IndentRegularExpression {
        private static final RegularExpression instance = new TrailingConditionRegExp();
        protected String getPattern() {
            return "(\\s*)([^#]*=\\s*)(((if)|(unless)|(case)).*)";
        }

        protected RE instance() {
            return instance;
        }
    }

    private static class IfElsifRegExp extends RegularExpression {
        private static final RE instance = new IfElsifRegExp();
        protected String getPattern() {
            return "(\\s*)((if)|(elsif))(.*)";
        }
    }

//    private static class WhenRegExp extends RegularExpression {
//        private static final RE instance = new WhenRegExp();
//        protected String getPattern() {
//            return "(\\s*)(when)(.*)";
//        }
//    }

}
