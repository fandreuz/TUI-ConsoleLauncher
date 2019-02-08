package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.regex.Pattern;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.tuils.Tuils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by francescoandreuzzi on 26/03/2018.
 */

public class ChangelogManager {

    private static String PREFS_NAME = "changelogPrefs";

    public static void printLog(final Context context, final OkHttpClient client) {
        printLog(context, client, false);
    }

    public static void printLog(final Context context, final OkHttpClient client, final boolean force) {

        final String originalUrl = "https://pastebin.com/n5AYHd26";
        String url;
        if(!originalUrl.contains("raw")) {
            url = originalUrl.replace(".com/", ".com/raw/");
        } else url = originalUrl;

        final SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, 0);

//        if true, it's the first time the user opens the app. no need to show changelog
        if(!force && MessagesManager.isShowingFirstTimeTutorial(context)) {
            preferences.edit()
                .putBoolean(url, true)
                .apply();

            return;
        }

        final int MAX_LENGTH = 200;

        final String changelogUrl = url;

        if(force || !preferences.getBoolean(changelogUrl, false)) {
            new Thread() {
                @Override
                public void run() {
                    super.run();

                    if (!Tuils.hasInternetAccess()) {
                        if(force) Tuils.sendOutput(context, R.string.no_internet);
                        return;
                    }

                    final Pattern newlinePattern = Pattern.compile("^-", Pattern.MULTILINE);

                    try {
                        Request.Builder builder = new Request.Builder()
                                .url(changelogUrl)
                                .get();

                        Response response = client.newCall(builder.build()).execute();

                        if (!response.isSuccessful() || response.code() == 304) {
                            if(force) Tuils.sendOutput(context, R.string.internet_error + Tuils.SPACE + response.code());
                            return;
                        }

                        String header = "Changelog " + BuildConfig.VERSION_NAME;

                        String log = response.body().string();
                        log = newlinePattern.matcher(log).replaceAll(Tuils.DOUBLE_SPACE + "-");

                        boolean cut = !force && log.length() >= MAX_LENGTH;
                        if(cut) log = log.substring(0, MAX_LENGTH) + "...";

                        Tuils.sendOutput(context, header + Tuils.NEWLINE + log);

                        if(cut) {
                            SpannableString sp = new SpannableString("Click here to see the full changelog");
                            sp.setSpan(new ForegroundColorSpan(XMLPrefsManager.getColor(Theme.output_color)), 0, sp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            sp.setSpan(new ForegroundColorSpan(XMLPrefsManager.getColor(Theme.link_color)), 6, 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            Tuils.sendOutput(context, sp, TerminalManager.CATEGORY_NO_COLOR, Uri.parse(changelogUrl));
                        }

                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(changelogUrl, true);
                        editor.apply();
                    } catch (Exception e) {
                        Tuils.sendOutput(context, e.toString());
                        Tuils.log(e);
                    }
                }
            }.start();
        }
    }
}
