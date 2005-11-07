package org.jedit.ruby.utils;

import gnu.regexp.RE;
import gnu.regexp.REException;
import org.gjt.sp.jedit.search.RESearchMatcher;
import org.jedit.ruby.RubyPlugin;

/**
 * @author robmckinnon at users,sourceforge,net
 */
public abstract class RegularExpression extends RE {

    public RegularExpression() {
        try {
            initialize(getPattern(), 0, RESearchMatcher.RE_SYNTAX_JEDIT, 0, 0);
        } catch (REException e) {
            RubyPlugin.error(e, getClass());
        }
    }

    protected abstract String getPattern();

}
