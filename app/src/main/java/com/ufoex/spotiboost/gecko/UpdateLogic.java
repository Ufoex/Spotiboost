package com.ufoex.spotiboost.gecko;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure logic of the update prompt (no Android classes, so it has a plain unit test). */
final class UpdateLogic {
    private static final Pattern LANG = Pattern.compile("<!--\\s*lang:(\\w+)\\s*-->");

    private UpdateLogic() {}

    /** Numeric, component by component: 0.10.0 > 0.9.0. Anything after "-" (like "-dev") is ignored. */
    static boolean isNewer(String remote, String local) {
        String[] r = remote.split("-")[0].split("\\."), l = local.split("-")[0].split("\\.");
        for (int i = 0; i < Math.max(r.length, l.length); i++) {
            int x = i < r.length ? num(r[i]) : 0, y = i < l.length ? num(l[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int num(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    /**
     * Release body (markdown) in the user's language. A body may hold one block per language, each starting with a
     * `<!-- lang:es -->` line; a language without a block gets `en`, a body without blocks is used whole. Whatever
     * follows a line that is just `---` (install instructions for the GitHub page) and GitHub's own
     * "Full Changelog" line are dropped.
     */
    static String notes(String body, String lang) {
        body = body.replace("\r", "");
        Matcher m = LANG.matcher(body);
        String en = null, wanted = null;
        int start = -1;
        String key = null;
        while (true) {
            boolean found = m.find();
            if (key != null) {
                String block = body.substring(start, found ? m.start() : body.length());
                if (key.equals(lang)) wanted = block;
                if (key.equals("en")) en = block;
            }
            if (!found) break;
            key = m.group(1);
            start = m.end();
        }
        String text = wanted != null ? wanted : en != null ? en : body;
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            if (line.trim().equals("---")) break;
            if (line.contains("Full Changelog")) continue;
            out.append(line.replaceAll("\\s+$", "")).append('\n');
        }
        return out.toString().replaceAll("\n{3,}", "\n\n").trim();
    }
}
