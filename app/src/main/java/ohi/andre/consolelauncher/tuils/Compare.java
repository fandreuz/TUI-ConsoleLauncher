package ohi.andre.consolelauncher.tuils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by francescoandreuzzi on 27/07/2017.
 */

public class Compare {

    static final char[] allowed_separators = {' ', '-', '_'};

    private static final String ACCENTS_PATTERN = "\\p{InCombiningDiacriticalMarks}+";
    public static String removeAccents(String s) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
            Pattern pattern = Pattern.compile(ACCENTS_PATTERN);
            String decomposed = Normalizer.normalize(s, Normalizer.Form.NFD);
            return pattern.matcher(decomposed).replaceAll(Tuils.EMPTYSTRING);
        }

        return s;
    }

    public static int matches(String compared, String comparator, boolean allowSkip) {
        compared = removeAccents(compared).toLowerCase().trim();
        comparator = removeAccents(comparator).toLowerCase().trim();

        List<String> s = new ArrayList<>();
        if(allowSkip) {
            for(char sep : allowed_separators) {
                String[] split = compared.split(String.valueOf(sep));
                s.addAll(Arrays.asList(split));
            }
        }
        s.add(compared);

        float maxRate = -1;
        for(String st : s) {
            float rate = 0;
            for(int i = 0; i < st.length() && i < comparator.length(); i++) {
                char c1 = st.charAt(i);
                char c2 = comparator.charAt(i);

                if(c1 == c2) {
                    rate += (double) (st.length() - i) / (double) st.length();
                }
            }

            if(rate >= (double) comparator.length() / 2d) maxRate = Math.max(maxRate, rate);
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

//        Collections.sort(ms, new Comparator<SimpleMutableEntry<String, Integer>>() {
//            @Override
//            public int compare(SimpleMutableEntry<String, Integer> o1, SimpleMutableEntry<String, Integer> o2) {
//                return o1.getValue() - o2.getValue();
//            }
//        });

        return ms;
    }

    public static List<SimpleMutableEntry<String, Integer>> matchesWithRate(String[] compared, String comparator, boolean allowSkip) {
        return matchesWithRate(Arrays.asList(compared), comparator, allowSkip);
    }
}
