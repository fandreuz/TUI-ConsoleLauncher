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

    public static boolean matches(String compared, String comparator, boolean allowSkip, int minRate) {
        compared = removeAccents(compared);
        comparator = removeAccents(comparator);

        List<String> s = new ArrayList<>();
        if(allowSkip) {
            for(char sep : allowed_separators) {
                String[] split = compared.split(String.valueOf(sep));
                s.addAll(Arrays.asList(split));
            }
        }
        s.add(compared);

        for(String st : s) {
            int rate = 0;
            for(int i = 0; i < st.length() && i < comparator.length(); i++) {
                char c1 = st.charAt(i);
                char c2 = comparator.charAt(i);

                if(c1 == c2) rate++;
            }

            if(rate >= minRate) return true;
        }

        return false;
    }

    public static List<String> matches(List<String> compared, String comparator, boolean allowSkip, int minRate) {
        List<String> ms = new ArrayList<>();

        for(String s : compared) {
            if(matches(s, comparator, allowSkip, minRate)) {
                ms.add(s);
            }
        }

        return ms;
    }
}
