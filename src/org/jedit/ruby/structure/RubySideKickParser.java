/*
 * RubySideKickParser.java - Side Kick Parser for Ruby
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

import org.jrubyparser.SourcePosition;
import sidekick.SideKickParser;
import sidekick.SideKickParsedData;
import sidekick.SideKickCompletion;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.parser.RubyParser;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.RubyPlugin;
import org.jedit.ruby.utils.EditorView;
import org.jedit.ruby.completion.CodeCompletor;
import org.jedit.ruby.completion.RubyCompletion;
import org.jedit.ruby.completion.CodeAnalyzer;
import errorlist.DefaultErrorSource;
import errorlist.ErrorSource;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public final class RubySideKickParser extends SideKickParser {

    private static final ErrorSource.Error[] EMPTY_ERROR_LIST = new ErrorSource.Error[0];
    private static DefaultErrorSource errorSource;

    private final RubyTokenHandler tokenHandler;

    public RubySideKickParser() {
        super("ruby");
        tokenHandler = new RubyTokenHandler();
    }

    public final boolean supportsCompletion() {
        return true;
    }

    public final boolean canHandleBackspace() {
        return true;
    }

    public static ErrorSource.Error[] getErrors() {
        ErrorSource.Error[] errors = errorSource.getAllErrors();
        return errors != null ? errors : EMPTY_ERROR_LIST;
    }

    public final SideKickParsedData parse(final Buffer buffer, final DefaultErrorSource errorSource) {
        String text = buffer.getText(0, buffer.getLength());
        RubySideKickParser.errorSource = errorSource;

        RubyParser.WarningListener listener = new RubySideKickWarningListener(errorSource);
        SideKickParsedData data = new SideKickParsedData(buffer.getName());
        RubyMembers members = RubyParser.getMembers(text, buffer.getPath(), listener, false);

        if (!members.containsErrors()) {
            addNodes(data.root, members.getMembers(), buffer);

        } else if (RubyParser.hasLastGoodMembers(buffer)) {
            members = RubyParser.getLastGoodMembers(buffer);
            addNodes(data.root, members.combineMembersAndProblems(0), buffer);

        } else {
            addNodes(data.root, members.getProblems(), buffer);
        }

        return data;
    }

    /**
     * True if caret is at a method insertion
     * point, else false.
     * This means that if the user types a dot or
     * equivalent, this method will return true
     * permitting completion to automatically
     * occur after a delay. In other cases
     * there won't be automatic completion
     * after a delay, the user will have to
     * manually hit the completion shortcut.
     */
    public boolean canCompleteAnywhere() {
        EditorView view = RubyPlugin.getActiveView();
        return CodeAnalyzer.isDotInsertionPoint(view) || RubyCompletion.continueCompleting();
    }

    public final SideKickCompletion complete(EditPane editPane, int caret) {
        Buffer buffer = editPane.getBuffer();
        RubyToken syntaxType = tokenHandler.getTokenAtCaret(buffer, caret);
        RubyCompletion completion = null;

        if (!ignore(syntaxType)) {
            CodeCompletor completor = new CodeCompletor(RubyPlugin.getActiveView());

            if (completor.isDotInsertionPoint()) {
                completion = completor.getDotCompletion();

            } else if (completor.hasCompletion()) {
                completion = completor.getCompletion();

            } else {
                completion = completor.getEmptyCompletion();
                clearLastCompletion();
            }
        } else {
            clearLastCompletion();
        }

        return completion;
    }

    private static void clearLastCompletion() {
        CodeAnalyzer.setLastReturnTypes(null);
        CodeAnalyzer.setLastCompleted(null);
    }

    private boolean ignore(RubyToken token) {
        if (token.isComment() || token.isLiteral()) {
            RubyPlugin.log("ignoring: " + token, getClass());
            return true;
        } else {
            RubyPlugin.log("not ignoring: " + token, getClass());
            return false;
        }
    }

    private void addWarning(String message, DefaultErrorSource errorSource, SourcePosition position) {
        addToErrorList(ErrorSource.WARNING, errorSource, message, position);
    }

    private void addWarning(String message, DefaultErrorSource errorSource, int lineNumber, String fileName) {
        addToErrorList(ErrorSource.WARNING, errorSource, message, lineNumber, fileName);
    }

    private void addError(String message, DefaultErrorSource errorSource, SourcePosition position) {
        addToErrorList(ErrorSource.ERROR, errorSource, message, position);
    }

    private void addToErrorList(int type, DefaultErrorSource errorSource, String message, SourcePosition position) {
        int line = position == null ? 0 : position.getEndLine();
        String file = position == null ? null : position.getFile();

        addToErrorList(type, errorSource, message, line, file);
    }

    private void addToErrorList(int type, DefaultErrorSource errorSource, String message, int line, String file) {
        int startOffset = RubyPlugin.getStartOffset(line);
        int nonSpaceStartOffset = RubyPlugin.getNonSpaceStartOffset(line);
        int endOffset = RubyPlugin.getEndOffset(line);

        int startOffsetInLine = nonSpaceStartOffset - startOffset;
        int endOffsetInLine = endOffset - startOffset;

        RubyPlugin.log("start " + startOffsetInLine + ", end " + endOffsetInLine, getClass());
        errorSource.addError(type, file, line, startOffsetInLine, endOffsetInLine, message);
    }

    private void addNodes(DefaultMutableTreeNode parentNode, Member[] members, Buffer buffer) {
        if (members != null) {
            for (Member member : members) {
                MemberNode node = new MemberNode(member);
                node.setStart(buffer.createPosition(Math.min(buffer.getLength(), member.getStartOffset())));
                node.setEnd(buffer.createPosition(Math.min(buffer.getLength(), member.getEndOffset())));
                DefaultMutableTreeNode treeNode = node.createTreeNode();
                if (member.hasChildMembers()) {
                    Member[] childMembers = member.getChildMembers();
                    addNodes(treeNode, childMembers, buffer);
                }
                parentNode.add(treeNode);
            }
        }
    }

    private final class RubySideKickWarningListener implements RubyParser.WarningListener {

        private final DefaultErrorSource errorSource;

        public RubySideKickWarningListener(DefaultErrorSource errorSource) {
            this.errorSource = errorSource;
        }

        public boolean isVerbose() {
            return false;
        }

        @Override
        public void warn(ID id, SourcePosition position, String message, Object... data) {
            addWarning(message, errorSource, position);
        }

        @Override
        public void warn(ID id, String fileName, int lineNumber, String message, Object... data) {
            addWarning(message, errorSource, lineNumber, fileName);
        }

        @Override
        public void warn(ID id, String message, Object... data) {
            addWarning(message, errorSource, null);
        }

        @Override
        public void warning(ID id, String message, Object... data) {
            addWarning(message, errorSource, null);
        }

        @Override
        public void warning(ID id, SourcePosition position, String message, Object... data) {
            addWarning(message, errorSource, position);
        }

        @Override
        public void warning(ID id, String fileName, int lineNumber, String message, Object... data) {
            addWarning(message, errorSource, lineNumber, fileName);
        }

        public final void error(SourcePosition position, String message) {
            addError(message, errorSource, position);
        }

        public final void clear() {
        }
    }

}
