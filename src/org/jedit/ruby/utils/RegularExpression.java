package org.jedit.ruby.utils;

import java.util.regex.Pattern;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;


/**
 * @author robmckinnon at users,sourceforge,net
 */
public class RegularExpression {

    private Pattern pattern;

    public RegularExpression() {
        initPattern(getPattern());
    }

    public RegularExpression(String pattern) {
        initPattern(pattern);
    }

    private void initPattern(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public boolean hasMatch(String text) {
        return pattern.matcher(text).find();
    }

    public boolean isMatch(String text) {
        return pattern.matcher(text).matches();
    }

    protected String getPattern() {
        throw new IllegalStateException("override to use");
    }

    public MatchResult firstMatch(String line) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return new MatchResultWithoutNullGroups(matcher.toMatchResult());
        } else {
            return null;
        }
    }

    public MatchResult lastMatch(String line, int startGroupIndex) {
        Matcher matcher = pattern.matcher(line);
        MatchResult match = null;
        int start = 0;

        while (matcher.find(start)) {
            match = matcher.toMatchResult();
            start = match.start(startGroupIndex);
        }

        return match == null ? null : new MatchResultWithoutNullGroups(match);
    }

    public MatchResult[] getAllMatchResults(String line) {
        Matcher matcher = pattern.matcher(line);
        List<MatchResult> matches = new ArrayList<MatchResult>();
        while (matcher.find()) {
            matches.add(matcher.toMatchResult());
        }
        return matches.toArray(new MatchResult[matches.size()]);
    }

    public int allMatchResults(String line) {
        int count = 0;
        Matcher matcher = pattern.matcher(line);

        if (matcher.matches()) {
            boolean nextMatch = matcher.find(0);

            while (nextMatch) {
                count++;
                nextMatch = matcher.find(matcher.end());
            }
        }

        return count;
    }

    private static class MatchResultWithoutNullGroups implements MatchResult {
        private final MatchResult result;

        public MatchResultWithoutNullGroups(MatchResult result) {
            this.result = result;
        }

        public int start() {
            return result.start();
        }

        public int start(int group) {
            return result.start(group);
        }

        public int end() {
            return result.end();
        }

        public int end(int group) {
            return result.end(group);
        }

        public String group() {
            return result.group();
        }

        public String group(int group) {
            String match = result.group(group);
            return match == null ? "" : match;
        }

        public int groupCount() {
            return result.groupCount();
        }
    }
}
