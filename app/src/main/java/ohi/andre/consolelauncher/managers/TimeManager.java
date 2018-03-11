package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 26/07/2017.
 */

public class TimeManager {

    SimpleDateFormat[] dateFormatList;

    public static Pattern extractor = Pattern.compile("%t([0-9]+)", Pattern.CASE_INSENSITIVE);

    public static TimeManager instance;

    public TimeManager() {
        final Pattern NEWLINE_PATTERN = Pattern.compile("%n");

        String format = XMLPrefsManager.get(Behavior.time_format);
        String separator = XMLPrefsManager.get(Behavior.time_format_separator);

        String[] formats = format.split(separator);
        dateFormatList = new SimpleDateFormat[formats.length];

        for(int c = 0; c < dateFormatList.length; c++) {
            try {
                formats[c] = NEWLINE_PATTERN.matcher(formats[c]).replaceAll(Tuils.NEWLINE);
                dateFormatList[c] = new SimpleDateFormat(formats[c]);
            } catch (Exception e) {
                dateFormatList[c] = dateFormatList[0];
            }
        }

        instance = this;
    }

    private SimpleDateFormat get(int index) {
        if(dateFormatList == null) return null;
        if(index < 0 || index >= dateFormatList.length) index = 0;
        if(index == 0 && dateFormatList.length == 0) return null;

        return dateFormatList[index];
    }

    public CharSequence replace(CharSequence cs) {
        return replace(cs, -1, Integer.MAX_VALUE);
    }

    public CharSequence replace(CharSequence cs, int color) {
        return replace(cs, -1, color);
    }

    public CharSequence replace(CharSequence cs, long tm, int color) {
        return replace(null, Integer.MAX_VALUE, cs, tm, color);
    }

    public CharSequence replace(Context context, int size, CharSequence cs, int color) {
        return replace(context, size, cs, -1, color);
    }

    public CharSequence replace(Context context, int size, CharSequence cs, long tm, int color) {
        if(tm == -1) {
            tm = System.currentTimeMillis();
        }

        Date date = new Date(tm);

        Matcher matcher = extractor.matcher(cs.toString());
        if(matcher.find()) {
            for(int count = 1; count <= matcher.groupCount(); count++) {
                SimpleDateFormat formatter = get(Integer.parseInt(matcher.group(count)));
                if(formatter == null) return cs;

                String tf = formatter.format(date);

                SpannableString spannableString = new SpannableString(tf);
                if(color != Integer.MAX_VALUE) {
                    spannableString.setSpan(new ForegroundColorSpan(color), 0, tf.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                if(size != Integer.MAX_VALUE && context != null) {
                    spannableString.setSpan(new AbsoluteSizeSpan(Tuils.convertSpToPixels(size, context)), 0, tf.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                cs = TextUtils.replace(cs, new String[] {"%t" + matcher.group(count)}, new CharSequence[] {spannableString});
            }
        }

        SimpleDateFormat formatter = get(0);
        if(formatter == null) return cs;

        String tf = formatter.format(date);

        SpannableString spannableString = new SpannableString(tf);
        if(color != Integer.MAX_VALUE) {
            spannableString.setSpan(new ForegroundColorSpan(color), 0, tf.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if(size != Integer.MAX_VALUE && context != null) {
            spannableString.setSpan(new AbsoluteSizeSpan(Tuils.convertSpToPixels(size, context)), 0, tf.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        cs = TextUtils.replace(cs, new String[] {"%t"}, new CharSequence[] {spannableString});

        return cs;
    }

    public void dispose() {
        dateFormatList = null;

        instance = null;
    }
}
