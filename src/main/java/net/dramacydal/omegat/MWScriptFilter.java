package net.dramacydal.omegat;

import org.omegat.core.Core;
import org.omegat.filters2.AbstractFilter;
import org.omegat.filters2.FilterContext;
import org.omegat.filters2.Instance;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MWScriptFilter extends AbstractFilter {
    public static void loadPlugins() {
        Core.registerFilterClass(MWScriptFilter.class);
    }

    public static void unloadPlugins() {
    }

    @Override
    public String getFileFormatName() {
        return "MWScript filter";
    }

    @Override
    public Instance[] getDefaultInstances() {
        return new Instance[] { new Instance("*.mwscript", "UTF-8", "UTF-8") };
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

    private static final Pattern choicePattern = Pattern.compile("^\\s*choice", Pattern.CASE_INSENSITIVE);
    private static final Pattern sayPattern = Pattern.compile("^\\s*say", Pattern.CASE_INSENSITIVE);
    private static final Pattern messageboxPattern = Pattern.compile("^\\s*messagebox", Pattern.CASE_INSENSITIVE);

    private static final Pattern messageboxTextPattern = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern sayTextPattern = Pattern.compile("\"([^\"]*)\"");

    private boolean isTransitionChar(char c)
    {
        switch (c)
        {
            case '.':
            case ',':
            case ':':
            case ' ':
            case '\t':
                return true;
            default:
                break;
        }

        if (c >= '0' && c <= '9')
            return true;

        return false;
    }

    @Override
    public void processFile(BufferedReader in, BufferedWriter out, FilterContext fc) throws IOException {
        for (; ; ) {
            String line = in.readLine();
            if (line == null)
                break;

            parseLine(line, out);
        }
    }

    private void parseLine(String line, Writer out) throws IOException {
        if (line.trim().equals(""))
        {
            out.write(line);
            out.write("\r\n");
            return;
        }

        Matcher choiceMatcher = choicePattern.matcher(line);
        if (choiceMatcher.find())
        {
            parseChoice(choiceMatcher, line, out);
            return;
        }

        Matcher messageboxMatcher = messageboxPattern.matcher(line);
        if (messageboxMatcher.find())
        {
            parseMessagebox(line, out);
            return;
        }

        Matcher sayMatcher = sayPattern.matcher(line);
        if (sayMatcher.find())
        {
            parseSay(line, out);
            return;
        }

        out.write(line);
        out.write("\r\n");
    }

    private void processSections(String line, ArrayList<MyPair<Integer,Integer>> sections, Writer out) throws IOException {
        if (sections.isEmpty()) {
            out.write(line);
            out.write("\r\n");
            return;
        }

        for (int i = 0; i < sections.size(); ++i) {
            MyPair<Integer, Integer> match = sections.get(i);

            if (i == 0)
                out.write(line.substring(0, match.getKey()));
            else {
                MyPair<Integer, Integer> prevMatch = sections.get(i - 1);
                out.write(line.substring(prevMatch.getValue(), match.getKey()));
            }

            String trans = processEntry(line.substring(match.getKey(), match.getValue()));
            out.write(trans);

            if (i == sections.size() - 1)
                out.write(line.substring(match.getValue()));
        }
        out.write("\r\n");
    }

    private void parseChoice(Matcher choiceMatcher, String line, Writer out) throws IOException {
        int startIndex = choiceMatcher.end();

        ArrayList<MyPair<Integer, Integer>> matches = new ArrayList<>();
        boolean inBraces = false;
        int foundIndex = -1;
        for (int i = startIndex; i < line.length(); ++i) {
            char c = line.charAt(i);

            if (c == '"') {
                inBraces = !inBraces;

                if (!inBraces) {
                    if (foundIndex == -1) {
                        // something went wrong
                        out.write(line);
                        out.write("\r\n");
                        return;
                    } else {
                        matches.add(new MyPair<>(foundIndex, i));
                        foundIndex = -1;
                    }
                }
                continue;
            }

            if (inBraces) {
                if (foundIndex == -1) {
                    foundIndex = i;
                }
                continue;
            }

            boolean isTransition = isTransitionChar(c);
            if (isTransition) {
                if (foundIndex != -1) {
                    matches.add(new MyPair<>(foundIndex, i));
                    foundIndex = -1;
                }
            } else {
                if (foundIndex == -1) {
                    foundIndex = i;
                }
            }
        }

        if (foundIndex != -1) {
            // something went wrong
            out.write(line);
            out.write("\r\n");
            return;
        }

        processSections(line, matches, out);
    }

    private void parseMessagebox(String line, Writer out) throws IOException {
        Matcher matcher = messageboxTextPattern.matcher(line);
        ArrayList<MyPair<Integer, Integer>> matches = new ArrayList<>();

        while (matcher.find()) {
            matches.add(new MyPair<>(matcher.start(1), matcher.end(1)));
        }

        processSections(line, matches, out);
    }

    private void parseSay(String line, Writer out) throws IOException {
        Matcher matcher = sayTextPattern.matcher(line);
        ArrayList<MyPair<Integer, Integer>> matches = new ArrayList<>();

        while (matcher.find()) {
            matches.add(new MyPair<>(matcher.start(1), matcher.end(1)));
        }

        if (matches.size() != 2)
        {
            // always two matches, 0 - sound, 1 - text
            out.write(line);
            out.write("\r\n");
            return;
        }

        matches.remove(0);

        processSections(line, matches, out);
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
