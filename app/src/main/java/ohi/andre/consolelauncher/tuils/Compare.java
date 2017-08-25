package ohi.andre.consolelauncher.tuils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by francescoandreuzzi on 27/07/2017.
 */

public class Compare {

    static final char[] allowed_separators = {' ', '-', '_'};
    private static final String ACCENTS_PATTERN = "\\p{InCombiningDiacriticalMarks}+";

    static Pattern unconsideredSymbols = Pattern.compile("[\\s_-]");
    static Pattern accentPattern = Pattern.compile(ACCENTS_PATTERN);

    public static String removeAccents(String s) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
            String decomposed = Normalizer.normalize(s, Normalizer.Form.NFD);
            return accentPattern.matcher(decomposed).replaceAll(Tuils.EMPTYSTRING);
        }

        return s;
    }

    public static int matches(String compared, String comparator, boolean allowSkip) {
//        Tuils.log("compared: " + compared + ", comparator: " + comparator);

        compared = removeAccents(compared).toLowerCase().trim();
        comparator = removeAccents(comparator).toLowerCase().trim();

        double minRate = (double) comparator.length() / 2d;
//        Tuils.log("min: " + minRate);

        List<ComparePack> s = new ArrayList<>();
        if(allowSkip) {
            for(char sep : allowed_separators) {
                String[] split = compared.split(String.valueOf(sep));
                if(split.length > 1) {
                    for(int count = 1; count < split.length; count++) {
                        s.add(new ComparePack(split[count], count, split.length));
                    }
                }
            }
        }

        String unconsidered = unconsideredSymbols.matcher(compared).replaceAll(Tuils.EMPTYSTRING);
        if(unconsidered.length() != compared.length()) {
            s.add(new ComparePack(unconsidered, 0, 1));
        }

        s.add(new ComparePack(compared, 0, 1));

        float maxRate = -1;
        Main:
        for(ComparePack cmp : s) {
//            Tuils.log("s: " + cmp.s);

            int stop = Math.min(cmp.s.length(), comparator.length());
            float minus = (float) (0.5 * (comparator.length() / 5));

            float rate = cmp.coefficient() * -1;
//            Tuils.log("initialRate: " + rate);
            for(int i = 0; i < stop; i++) {
                char c1 = cmp.s.charAt(i);
                char c2 = comparator.charAt(i);

                if(c1 == c2) {
                    rate++;
                } else {
                    rate -= minus;

                    if(rate + (stop - 1 - i) < minRate) {
//                        Tuils.log("continue");
                        continue Main;
                    }
                }
//                Tuils.log("rate: " + rate);
            }

            if(rate >= minRate) {
                maxRate = Math.max(maxRate, rate);
//                Tuils.log("maxRate changed");
            }
        }

        return Math.round(maxRate);
    }

    public static List<String> matches(List<String> compared, String comparator, boolean allowSkip) {
        List<SimpleMutableEntry<String, Integer>> ms = matchesWithRate(compared, comparator, allowSkip);

        List<String> result = new ArrayList<>(ms.size());
        for(SimpleMutableEntry<String, Integer> e : ms) {
            result.add(e.getKey());
        }

        return result;
    }

    public static List<String> matches(String[] compared, String comparator, boolean allowSkip) {
        return matches(Arrays.asList(compared), comparator, allowSkip);
    }

    public static List<SimpleMutableEntry<String, Integer>> matchesWithRate(List<String> compared, String comparator, boolean allowSkip) {
        List<SimpleMutableEntry<String, Integer>> ms = new ArrayList<>();

        for(String s : compared) {
            if(Thread.currentThread().isInterrupted()) return ms;

            int rate = matches(s, comparator, allowSkip);
            if(rate != -1) ms.add(new SimpleMutableEntry<>(s, rate));
        }

        Collections.sort(ms, new Comparator<SimpleMutableEntry<String, Integer>>() {
            @Override
            public int compare(SimpleMutableEntry<String, Integer> o1, SimpleMutableEntry<String, Integer> o2) {
                return o1.getValue() - o2.getValue();
            }
        });

        return ms;
    }

    public static List<SimpleMutableEntry<String, Integer>> matchesWithRate(String[] compared, String comparator, boolean allowSkip) {
        return matchesWithRate(Arrays.asList(compared), comparator, allowSkip);
    }

    public static List<SimpleMutableEntry<Stringable, Integer>> matchesWithRate(List<? extends Stringable> compared, boolean allowSkip, String comparator) {
        List<SimpleMutableEntry<Stringable, Integer>> ms = new ArrayList<>();

        for(Stringable s : compared) {
            if(Thread.currentThread().isInterrupted()) return ms;

            int rate = matches(s.getString(), comparator, allowSkip);
            if(rate != -1) ms.add(new SimpleMutableEntry<>(s, rate));
        }

        Collections.sort(ms, new Comparator<SimpleMutableEntry<Stringable, Integer>>() {
            @Override
            public int compare(SimpleMutableEntry<Stringable, Integer> o1, SimpleMutableEntry<Stringable, Integer> o2) {
                return o1.getValue() - o2.getValue();
            }
        });

        return ms;
    }

    public static List<SimpleMutableEntry<Stringable, Integer>> matchesWithRate(Stringable[] compared, boolean allowSkip, String comparator) {
        return matchesWithRate(Arrays.asList(compared), allowSkip, comparator);
    }

    private static class ComparePack {
        String s;
        int index;
        int total;

        public ComparePack(String s, int index, int total) {
            this.s = s;
            this.index = index;
            this.total = total;
        }

        public int coefficient() {
            return index;
        }
    }

    public interface Stringable {
        String getString();
    }
}
