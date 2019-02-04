package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.tuils.SimpleMutableEntry;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 26/07/2017.
 */

public class TimeManager {

    Map.Entry<Integer, SimpleDateFormat>[] dateFormatList;

    public static Pattern extractor = Pattern.compile("%t([0-9]*)", Pattern.CASE_INSENSITIVE);

    public static TimeManager instance;

    public TimeManager(Context context) {
        String format = XMLPrefsManager.get(Behavior.time_format);
        String separator = XMLPrefsManager.get(Behavior.time_format_separator);

        String[] formats = format.split(separator);
        dateFormatList = new Map.Entry[formats.length];

        Pattern colorPattern = Pattern.compile("#(?:\\d|[a-fA-F]){6}");

        for(int c = 0; c < dateFormatList.length; c++) {
            try {
                formats[c] = Tuils.patternNewline.matcher(formats[c]).replaceAll(Tuils.NEWLINE);

                int color = XMLPrefsManager.getColor(Theme.time_color);
                Matcher m = colorPattern.matcher(formats[c]);
                if(m.find()) {
                    color = Color.parseColor(m.group());
                    formats[c] = m.replaceAll(Tuils.EMPTYSTRING);
                }

                dateFormatList[c] = new SimpleMutableEntry<>(color, new SimpleDateFormat(formats[c]));
            } catch (Exception e) {
                Tuils.sendOutput(Color.RED, context,"Invalid time format: " + formats[c]);
                dateFormatList[c] = dateFormatList[0];
            }
        }

        instance = this;
    }

    private Map.Entry<Integer, SimpleDateFormat> get(int index) {
        if(dateFormatList == null) return null;
        if(index < 0 || index >= dateFormatList.length) index = 0;
        if(index == 0 && dateFormatList.length == 0) return null;

        return dateFormatList[index];
    }

    public CharSequence replace(CharSequence cs) {
        return replace(cs, -1, TerminalManager.NO_COLOR);
    }

    public CharSequence replace(CharSequence cs, int color) {
        return replace(cs, -1, color);
    }

    public CharSequence replace(CharSequence cs, long tm, int color) {
        return replace(null, TerminalManager.NO_COLOR, cs, tm, color);
    }

    public CharSequence replace(CharSequence cs, long tm) {
        return replace(null, TerminalManager.NO_COLOR, cs, tm, TerminalManager.NO_COLOR);
    }

    public CharSequence replace(Context context, int size, CharSequence cs) {
        return replace(context, size, cs, -1, TerminalManager.NO_COLOR);
    }

    public CharSequence replace(Context context, int size, CharSequence cs, int color) {
        return replace(context, size, cs, -1, color);
    }

    public CharSequence replace(Context context, int size, CharSequence cs, long tm, int color) {
        if(tm == -1) {
            tm = System.currentTimeMillis();
        }

        if(cs instanceof String) {
            Tuils.log(Thread.currentThread().getStackTrace());
            Tuils.log("cant span a string!", cs.toString());
        }

        Date date = new Date(tm);

        Matcher matcher = extractor.matcher(cs);
        while(matcher.find()) {
            String number = matcher.group(1);
            if(number == null || number.length() == 0) number = "0";

            Map.Entry<Integer, SimpleDateFormat> entry = get(Integer.parseInt(number));
            if(entry == null) continue;

            CharSequence s = span(context, entry, color, date, size);
            cs = TextUtils.replace(cs, new String[] {matcher.group(0)}, new CharSequence[] {s});
        }

        Map.Entry<Integer, SimpleDateFormat> entry = get(0);
        cs = TextUtils.replace(cs, new String[] {"%t"}, new CharSequence[] {span(context, entry, color, date, size)});

        return cs;
    }

    public CharSequence getCharSequence(String s) {
        return getCharSequence(s, -1, TerminalManager.NO_COLOR);
    }

    public CharSequence getCharSequence(String s, int color) {
        return getCharSequence(s, -1, color);
    }

    public CharSequence getCharSequence(String s, long tm, int color) {
        return getCharSequence(null, TerminalManager.NO_COLOR, s, tm, color);
    }

    public CharSequence getCharSequence(String s, long tm) {
        return getCharSequence(null, TerminalManager.NO_COLOR, s, tm, TerminalManager.NO_COLOR);
    }

    public CharSequence getCharSequence(Context context, int size, String s) {
        return getCharSequence(context, size, s, -1, TerminalManager.NO_COLOR);
    }

    public CharSequence getCharSequence(Context context, int size, String s, int color) {
        return getCharSequence(context, size, s, -1, color);
    }

//    this can be "%t[\d]
    public CharSequence getCharSequence(Context context, int size, String s, long tm, int color) {
        if(tm == -1) {
            tm = System.currentTimeMillis();
        }

        Date date = new Date(tm);

        Matcher matcher = extractor.matcher(s);
        if(matcher.find()) {
            String number = matcher.group(1);
            if(number == null || number.length() == 0) number = "0";

            Map.Entry<Integer, SimpleDateFormat> entry = get(Integer.parseInt(number));
            if(entry == null) {
                return null;
            }

            return span(context, entry, color, date, size);
        } else return null;
    }

    private CharSequence span(Context context, Map.Entry<Integer, SimpleDateFormat> entry, int color, Date date, int size) {
        if(entry == null) return Tuils.EMPTYSTRING;

        String tf = entry.getValue().format(date);
        int clr = color != TerminalManager.NO_COLOR ? color : entry.getKey();

        SpannableString spannableString = new SpannableString(tf);
        spannableString.setSpan(new ForegroundColorSpan(clr), 0, tf.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if(size != Integer.MAX_VALUE && context != null) {
            spannableString.setSpan(new AbsoluteSizeSpan(Tuils.convertSpToPixels(size, context)), 0, tf.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString;
    }

    public void dispose() {
        dateFormatList = null;

        instance = null;
    }
}
