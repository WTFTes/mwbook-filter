package net.dramacydal.omegat;

import org.omegat.core.Core;
import org.omegat.filters2.AbstractFilter;
import org.omegat.filters2.FilterContext;
import org.omegat.filters2.Instance;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MWBookFilter extends AbstractFilter {
    public static void loadPlugins() {
        Core.registerFilterClass(MWBookFilter.class);
        Core.registerFilterClass(MWScriptFilter.class);
    }

    public static void unloadPlugins() {
    }

    @Override
    public String getFileFormatName() {
        return "MWBook filter";
    }

    @Override
    public Instance[] getDefaultInstances() {
        return new Instance[] { new Instance("*.mwbook", "UTF-8", "UTF-8") };
    }

    @Override
    public boolean isSourceEncodingVariable() {
        return true;
    }

    @Override
    public boolean isTargetEncodingVariable() {
        return true;
    }

    @Override
    protected boolean requirePrevNextFields() {
        return true;
    }

    @Override
    public void processFile(BufferedReader in, BufferedWriter out, FilterContext fc) throws IOException {
        for (; ; ) {
            String line = in.readLine();
            if (line == null)
                break;

            if (line.trim().equals("")) {
                out.write(line);
                out.write("\r\n");
                continue;
            }

            Pattern p = Pattern.compile("(<[^>]+>)");
            Matcher matcher = p.matcher(line);

            ArrayList<MyPair<Integer, Integer>> matches = new ArrayList<>();

            while (matcher.find()) {
                matches.add(new MyPair<>(matcher.start(), matcher.end()));
            }

            if (matches.isEmpty()) {
                subParse(line, out);
                out.write("\r\n");
                continue;
            }

            int lastTokenStart = 0;
            for (int i = 0; i < matches.size(); ++i) {
                MyPair<Integer, Integer> match = matches.get(i);
                if (match.getKey() - lastTokenStart > 0) {
                    String tok = line.substring(lastTokenStart, match.getKey());
                    subParse(tok, out);
                }

                out.write(line.substring(match.getKey(), match.getValue()));
                lastTokenStart = match.getValue() + 1;
            }

            if (lastTokenStart < line.length()) {
                String tok = line.substring(lastTokenStart, line.length());
                subParse(tok, out);
            }

            out.write("\r\n");
        }
    }

    private void subParse(String match, Writer out) throws IOException {
        String trimmed = match.trim();
        if (trimmed.equals("")) {
            out.write(match);
            return;
        }

        int strPos = match.indexOf(trimmed);
        if (strPos > 0)
            out.write(match.substring(0, strPos));

        String trans = processEntry(trimmed);
        out.write(trans);

        out.write(match.substring(strPos + trimmed.length(), match.length()));
    }

    /**
     * Returns true to indicate that Text filter has options.
     *
     * @return True, because Text filter has options.
     */
    @Override
    public boolean hasOptions() {
        return false;
    }
}
