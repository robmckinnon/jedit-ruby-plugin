package org.jedit.ruby.structure;

import org.gjt.sp.jedit.syntax.Token;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public final class RubyToken extends Token {
    private final Token firstToken;
    private RubyToken previousToken;
    private RubyToken nextToken;

    public RubyToken(Token token, Token firstToken) {
        super(token.id, token.offset, token.length, token.rules);
        this.next = token.next;
        this.firstToken = firstToken;
    }

    public final RubyToken getPreviousToken() {
        if (previousToken == null) {
            Token token = firstToken;
            Token previous = null;

            while (token != null && token.next != null) {
                if (isEqual(token.next)) {
                    previous = token;
                    break;
                }
                token = token.next;
            }

            previousToken = previous == null ? null : new RubyToken(previous, firstToken);
        }
        return previousToken;
    }

    public final RubyToken getNextToken() {
        if (nextToken == null) {
            nextToken = next == null ? null : new RubyToken(next, firstToken);
        }
        return nextToken;
    }

    public final boolean isNextLiteral() {
        return getNextToken() != null && getNextToken().isLiteral();
    }

    public final boolean isPreviousLiteral() {
        return getPreviousToken() != null && getPreviousToken().isLiteral();
    }

    public final boolean isOperator() {
        return id == Token.OPERATOR;
    }

    public final boolean isLiteral() {
        switch (id) {
            case Token.LITERAL1:
            case Token.LITERAL2:
            case Token.LITERAL3:
            case Token.LITERAL4:
                return true;
            default:
                return false;
        }
    }

    public final boolean isComment() {
        switch(id) {
            case Token.COMMENT1:
            case Token.COMMENT2:
            case Token.COMMENT3:
            case Token.COMMENT4:
                return true;
            default:
                return false;
        }
    }

    public final String toString() {
        String id = this.id < Token.TOKEN_TYPES.length ? Token.TOKEN_TYPES[this.id] : String.valueOf(this.id);
        return id + '['+offset+','+(offset+length)+']';
    }

    private boolean isEqual(Token token) {
        return token != null && token.id == id && token.offset == offset && token.length == length;
    }

}
