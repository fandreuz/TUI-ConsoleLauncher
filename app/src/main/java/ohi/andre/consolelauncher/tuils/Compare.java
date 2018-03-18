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

    public static int MAX_RATE = 1000000, MIN_RATE = -1000000;

    static Pattern unconsideredSymbols = Pattern.compile("[\\s_-]");
    static Pattern accentPattern = Pattern.compile(ACCENTS_PATTERN);

    private static Comparator<SimpleMutableEntry<? extends Object, Integer>> entryComparator = new Comparator<SimpleMutableEntry<? extends Object, Integer>>() {
        @Override
        public int compare(SimpleMutableEntry<? extends Object, Integer> o1, SimpleMutableEntry<? extends Object, Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    };

    public static String removeAccents(String s) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
            String decomposed = Normalizer.normalize(s, Normalizer.Form.NFD);
            return accentPattern.matcher(decomposed).replaceAll(Tuils.EMPTYSTRING);
        }

        return s;
    }

//    private static String lastComparator = null;
//    private static double averageTime;
//    private static int n, averageLength;

    public static int compare(String compared, String comparator, boolean allowSkip, int maximum) {
//        Tuils.log("-----------------");

//        if(lastComparator != null && lastComparator.equals(comparator)) {}
//        else {
//            Tuils.log("#######################");
//            Tuils.log("average for cmp: " + lastComparator);
//            Tuils.log("average: " + averageTime + ", n: " + n);
//            Tuils.log("average compared length", averageLength);
//            Tuils.log("#######################");
//
//            averageTime = 0;
//            n = 0;
//            averageLength = 0;
//
//            lastComparator = comparator;
//        }

//        long time = System.currentTimeMillis();

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

        s.add(new ComparePack(compared, 0, 1));

        String unconsidered = unconsideredSymbols.matcher(compared).replaceAll(Tuils.EMPTYSTRING);
        if(unconsidered.length() != compared.length()) {
            s.add(new ComparePack(unconsidered, 0, 1));
        }

        float maxRate = -1;
        int maxRateIndex = -1;

        Main:
        for(ComparePack cmp : s) {
//            Tuils.log("s: " + cmp.s);

            int stop = Math.min(cmp.s.length(), comparator.length());
//            Tuils.log("stop", stop);
            float minus = (float) (0.5 * (comparator.length() / 5));

            float rate = 0;
            for(int i = 0; i < stop; i++) {
                char c1 = cmp.s.charAt(i);
                char c2 = comparator.charAt(i);

//                Tuils.log("index: " + i);

                if(c1 == c2) {
//                    Tuils.log("equals");
                    rate++;
                } else {
//                    Tuils.log(c1 + " is not " + c2);
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
                maxRateIndex = cmp.index;
//                Tuils.log("maxRate changed");
            }
        }

//        int delay = (int) (System.currentTimeMillis() - time);
//        Tuils.log("return", Math.round(maxRate));
//        Tuils.log("normal time: " + delay);
//        Tuils.log("started: " + time, "end: " + System.currentTimeMillis());
//        if(delay > 4) Tuils.log("!!!!!!!!!!!!!");
//
//        averageTime = (averageTime * n + delay) / (n + 1);
//        averageLength = (averageLength * n + compared.length()) / (n + 1);
//        n++;

        int r = Math.round(maxRate);
        if(r == comparator.length() && maxRateIndex == 0) {
            return maximum;
        }
        return r;
    }

    public static int compare(int minimum, String compared, String comparator, boolean allowSkip, int maximum) throws CompareStringLowerThanMinimumException {
//        Tuils.log("-----------------");

//        if(lastComparator != null && lastComparator.equals(comparator)) {}
//        else {
//            Tuils.log("#######################");
//            Tuils.log("average for cmp: " + lastComparator);
//            Tuils.log("average: " + averageTime + ", n: " + n);
//            Tuils.log("average compared length", averageLength);
//            Tuils.log("#######################");
//
//            averageTime = 0;
//            n = 0;
//            averageLength = 0;
//
//            lastComparator = comparator;
//        }

//        long time = System.currentTimeMillis();

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

        s.add(new ComparePack(compared, 0, 1));

        String unconsidered = unconsideredSymbols.matcher(compared).replaceAll(Tuils.EMPTYSTRING);
        if(unconsidered.length() != compared.length()) {
            s.add(new ComparePack(unconsidered, 0, 1));
        }

        float maxRate = -1;
        int maxRateIndex = -1;

        Main:
        for(ComparePack cmp : s) {
//            Tuils.log("s: " + cmp.s);

            int stop = Math.min(cmp.s.length(), comparator.length());
//            Tuils.log("stop", stop);
            float minus = (float) (0.5 * (comparator.length() / 5));

            float rate = 0;
            for(int i = 0; i < stop; i++) {
                char c1 = cmp.s.charAt(i);
                char c2 = comparator.charAt(i);

//                Tuils.log("index: " + i);

                if(c1 == c2) {
//                    Tuils.log("equals");
                    rate++;
                } else {
//                    Tuils.log(c1 + " is not " + c2);
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
                maxRateIndex = cmp.index;
//                Tuils.log("maxRate changed");
            }
        }

//        int delay = (int) (System.currentTimeMillis() - time);
//        Tuils.log("return", Math.round(maxRate));
//        Tuils.log("normal time: " + delay);
//        Tuils.log("started: " + time, "end: " + System.currentTimeMillis());
//        if(delay > 4) Tuils.log("!!!!!!!!!!!!!");
//
//        averageTime = (averageTime * n + delay) / (n + 1);
//        averageLength = (averageLength * n + compared.length()) / (n + 1);
//        n++;

        int r = Math.round(maxRate);
        if(r < minimum) throw new CompareStringLowerThanMinimumException();
        if(r == comparator.length() && maxRateIndex == 0) {
            return maximum;
        }
        return r;
    }

    public static int compare(String compared, String comparator, boolean allowSkip) {
        return compare(compared, comparator, allowSkip, MAX_RATE);
    }




    public static List<SimpleMutableEntry<String, Integer>> compareWithRates(int minimum, List<String> compared, String comparator, boolean allowSkip, int maximum, boolean sort) {
        List<SimpleMutableEntry<String, Integer>> ms = new ArrayList<>();

        for(String s : compared) {
            if(Thread.currentThread().isInterrupted()) return ms;

            try {
                ms.add(new SimpleMutableEntry<>(s, compare(minimum, s, comparator, allowSkip, maximum)));
            } catch (CompareStringLowerThanMinimumException e) {}
        }

        if(sort) {
            Collections.sort(ms, entryComparator);
        }

        return ms;
    }

    public static List<SimpleMutableEntry<String, Integer>> compareWithRates(List<String> compared, String comparator, boolean allowSkip, int maximum, boolean sort) {
        List<SimpleMutableEntry<String, Integer>> ms = new ArrayList<>();

        for(String s : compared) {
            if(Thread.currentThread().isInterrupted()) return ms;

            ms.add(new SimpleMutableEntry<>(s, compare(s, comparator, allowSkip, maximum)));
        }

        if(sort) {
            Collections.sort(ms, entryComparator);
        }

        return ms;
    }

    public static List<SimpleMutableEntry<String, Integer>> compareWithRates(int minimum, List<String> compared, String comparator, boolean allowSkip, boolean sort) {
        return compareWithRates(minimum, compared, comparator, allowSkip, MAX_RATE, sort);
    }

    public static List<SimpleMutableEntry<String, Integer>> compareWithRates(List<String> compared, String comparator, boolean allowSkip, boolean sort) {
        return compareWithRates(compared, comparator, allowSkip, MAX_RATE, sort);
    }

    public static List<SimpleMutableEntry<String, Integer>> compareWithRates(int minimum, String[] compared, String comparator, boolean allowSkip, int maximum, boolean sort) {
        return compareWithRates(minimum, Arrays.asList(compared), comparator, allowSkip, maximum, sort);
    }

    public static List<SimpleMutableEntry<String, Integer>> compareWithRates(String[] compared, String comparator, boolean allowSkip, int maximum, boolean sort) {
        return compareWithRates(Arrays.asList(compared), comparator, allowSkip, maximum, sort);
    }

    public static List<SimpleMutableEntry<String, Integer>> compareWithRates(String[] compared, String comparator, boolean allowSkip, boolean sort) {
        return compareWithRates(compared, comparator, allowSkip, MAX_RATE, sort);
    }

    public static List<SimpleMutableEntry<Stringable, Integer>> compareWithRates(int minimum, List<? extends Stringable> compared, boolean allowSkip, String comparator, boolean sort) {
        return compareWithRates(minimum, compared, allowSkip, comparator, MAX_RATE, sort);
    }

    public static List<SimpleMutableEntry<Stringable, Integer>> compareWithRates(int minimum, List<? extends Stringable> compared, boolean allowSkip, String comparator, int maximum, boolean sort) {
        List<SimpleMutableEntry<Stringable, Integer>> ms = new ArrayList<>();

        for(Stringable s : compared) {
            if(Thread.currentThread().isInterrupted()) return ms;

            try {
                ms.add(new SimpleMutableEntry<>(s, compare(minimum, s.getString(), comparator, allowSkip, maximum)));
            } catch (CompareStringLowerThanMinimumException e) {}
        }

        if(sort) {
            Collections.sort(ms, entryComparator);
        }

        return ms;
    }

    public static List<SimpleMutableEntry<Stringable, Integer>> compareWithRates(List<? extends Stringable> compared, boolean allowSkip, String comparator, int maximum, boolean sort) {
        List<SimpleMutableEntry<Stringable, Integer>> ms = new ArrayList<>();

        for(Stringable s : compared) {
            if(Thread.currentThread().isInterrupted()) return ms;

            ms.add(new SimpleMutableEntry<>(s, compare(s.getString(), comparator, allowSkip, maximum)));
        }

        if(sort) {
            Collections.sort(ms, entryComparator);
        }

        return ms;
    }

    public static List<SimpleMutableEntry<Stringable, Integer>> compareWithRates(List<? extends Stringable> compared, boolean allowSkip, String comparator, boolean sort) {
        return compareWithRates(compared, allowSkip, comparator, MAX_RATE, sort);
    }

    public static List<SimpleMutableEntry<Stringable, Integer>> compareWithRates(Stringable[] compared, boolean allowSkip, String comparator, int maximum, boolean sort) {
        return compareWithRates(Arrays.asList(compared), allowSkip, comparator, maximum, sort);
    }

    public static List<SimpleMutableEntry<Stringable, Integer>> compareWithRates(Stringable[] compared, boolean allowSkip, String comparator, boolean sort) {
        return compareWithRates(compared, allowSkip, comparator, MAX_RATE, sort);
    }




    public static List<String> compareList(int minimum, List<String> compared, String comparator, boolean allowSkip, int maximum, boolean sort) {
        List<String> ms = new ArrayList<>();
        if(!sort) {
            for (String s : compared) {
                if (Thread.currentThread().isInterrupted()) return ms;

                try {
                    compare(minimum, s, comparator, allowSkip, maximum);
                    ms.add(s);
                } catch (CompareStringLowerThanMinimumException e) {}
            }
        } else {
            List<SimpleMutableEntry<String, Integer>> list = compareWithRates(minimum, compared, comparator, allowSkip, maximum, sort);
            for(SimpleMutableEntry<String, Integer> s : list) ms.add(s.getKey());
        }

        return ms;
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
    }

    public interface Stringable {
        String getString();
    }

    public static class CompareStringLowerThanMinimumException extends Exception {}
}
