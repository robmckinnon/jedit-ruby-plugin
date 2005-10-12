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

import sidekick.SideKickParser;
import sidekick.SideKickParsedData;
import sidekick.SideKickCompletion;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.syntax.Token;
import org.jruby.lexer.yacc.SourcePosition;
import org.jedit.ruby.ast.Member;
import org.jedit.ruby.parser.RubyParser;
import org.jedit.ruby.ast.RubyMembers;
import org.jedit.ruby.RubyPlugin;
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

    private static DefaultErrorSource errorSource;
    private static final ErrorSource.Error[] EMPTY_ERROR_LIST = new ErrorSource.Error[0];

    public RubySideKickParser() {
        super("ruby");
    }

    public final boolean supportsCompletion() {
        return true;
    }

    public final boolean canHandleBackspace() {
        return true;
    }

    public static ErrorSource.Error[] getErrors() {
        ErrorSource.Error[] errors = errorSource.getAllErrors();
        if(errors != null) {
            return errors;
        } else {
            return EMPTY_ERROR_LIST;
        }
    }

    public final SideKickParsedData parse(final Buffer buffer, final DefaultErrorSource errorSource) {
        String text = buffer.getText(0, buffer.getLength());
        RubySideKickParser.errorSource = errorSource;

        RubyParser.WarningListener listener = new RubySideKickWarningListener(errorSource);
        SideKickParsedData data = new RubyParsedData(buffer.getName());
        DefaultMutableTreeNode parentNode = data.root;
        RubyMembers members = RubyParser.getMembers(text, buffer.getPath(), listener, false);

        if (!members.containsErrors()) {
            addNodes(parentNode, members.getMembers(), buffer);
        } else if(RubyParser.hasLastGoodMembers(buffer)) {
            members = RubyParser.getLastGoodMembers(buffer);
            addNodes(parentNode, members.combineMembersAndProblems(0), buffer);
        } else {
            addNodes(parentNode, members.getProblems(), buffer);
        }

        return data;
    }

    public static final class RubyParsedData extends SideKickParsedData {
        public RubyParsedData(String fileName) {
            super(fileName);
        }
    }

    public final SideKickCompletion complete(EditPane editPane, int caret) {
        RubyCompletion completion = null;
        Token syntaxType = RubyPlugin.getToken(editPane.getBuffer(), caret);

        if (!ignore(syntaxType)) {
            RubyPlugin.log("completing", getClass());
            CodeCompletor completor = new CodeCompletor(editPane.getView());

            if (completor.isInsertionPoint()) {
                completion = new RubyCompletion(editPane.getView(), completor.getPartialMethod(), completor.getMethods());
            }
        }

        if (completion == null) {
            CodeAnalyzer.setLastReturnTypes(null);
            CodeAnalyzer.setLastCompleted(null);
        }

        return completion;
    }

    private boolean ignore(Token token) {
        switch(token.id) {
            case Token.COMMENT1:
            case Token.COMMENT2:
            case Token.COMMENT3:
            case Token.COMMENT4:
            case Token.LITERAL1:
            case Token.LITERAL2:
            case Token.LITERAL3:
            case Token.LITERAL4:
                RubyPlugin.log("ignoring: " + Token.TOKEN_TYPES[token.id], getClass());
                return true;
            default:
                String tokenType = token.id < Token.TOKEN_TYPES.length ? Token.TOKEN_TYPES[token.id] : String.valueOf(token.id);
                RubyPlugin.log("not ignoring: " + tokenType, getClass());
                return false;
        }
    }

    private void addWarning(String message, SourcePosition position, DefaultErrorSource errorSource) {
        addToErrorList(ErrorSource.WARNING, position, errorSource, message);
    }

    private void addError(String message, SourcePosition position, DefaultErrorSource errorSource) {
        addToErrorList(ErrorSource.ERROR, position, errorSource, message);
    }

    private void addToErrorList(int type, SourcePosition position, DefaultErrorSource errorSource, String message) {
        int line = position == null ? 0 : position.getLine() - 1;
        String file = position == null ? null : position.getFile();

        int startOffset = RubyPlugin.getStartOffset(line);
        int nonSpaceStartOffset = RubyPlugin.getNonSpaceStartOffset(line);
        int endOffset = RubyPlugin.getEndOffset(line);

        int startOffsetInLine = nonSpaceStartOffset - startOffset;
        int endOffsetInLine = endOffset - startOffset;

        RubyPlugin.log("start " + startOffsetInLine + ", end " + endOffsetInLine, getClass());
        errorSource.addError(type, file, line, startOffsetInLine, endOffsetInLine, message);
    }

    private void addNodes(DefaultMutableTreeNode parentNode, Member[] members, Buffer buffer) {
        if(members != null) {
            for(Member member : members) {
                MemberNode node = new org.jedit.ruby.structure.MemberNode(member);
                node.start = buffer.createPosition(Math.min(buffer.getLength(), member.getStartOffset()));
                node.end = buffer.createPosition(Math.min(buffer.getLength(), member.getEndOffset()));
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

        public final void warn(SourcePosition position, String message) {
            addWarning(message, position, errorSource);
        }

        public final void warn(String message) {
            addWarning(message, null, errorSource);
        }

        public final void warning(SourcePosition position, String message) {
            addWarning(message, position, errorSource);
        }

        public final void warning(String message) {
            addWarning(message, null, errorSource);
        }

        public final void error(SourcePosition position, String message) {
            addError(message, position, errorSource);
        }

        public final void clear() {
        }
    }
}
