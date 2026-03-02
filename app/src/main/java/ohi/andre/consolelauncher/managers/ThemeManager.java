package ohi.andre.consolelauncher.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by francescoandreuzzi on 17/02/2018.
 */

public class ThemeManager {

    public static String ACTION_APPLY = BuildConfig.APPLICATION_ID + ".theme_apply";
    public static String ACTION_REVERT = BuildConfig.APPLICATION_ID + ".theme_revert";
    public static String ACTION_STANDARD = BuildConfig.APPLICATION_ID + ".theme_standard";

    public static String NAME = "name";

    OkHttpClient client;
    Context context;
    Reloadable reloadable;

    Pattern parser = Pattern.compile("(<SUGGESTIONS>.+<\\/SUGGESTIONS>).*(<THEME>.+<\\/THEME>)", Pattern.DOTALL);

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ACTION_APPLY)) {
                String name = intent.getStringExtra(NAME);
                if(name == null) return;

//                name needs to be the absolute path
                if(name.endsWith(".zip")) apply(new File(name));
                else apply(name);
            } else if(intent.getAction().equals(ACTION_REVERT)) {
                revert();
            } else if(intent.getAction().equals(ACTION_STANDARD)) {
                standard();
            }
        }
    };

    public ThemeManager(OkHttpClient client, Context context, Reloadable reloadable) {
        this.client = client;
        this.context = context;
        this.reloadable = reloadable;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_APPLY);
        filter.addAction(ACTION_REVERT);
        filter.addAction(ACTION_STANDARD);

        LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(receiver, filter);
    }

    public void dispose() {
        LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(receiver);
    }

    public void apply(final String themeName) {
        new Thread() {
            @Override
            public void run() {
                super.run();

                if(!Tuils.hasInternetAccess()) {
                    Tuils.sendOutput(Color.RED, context, R.string.no_internet);
                    return;
                }

                String url = "https://tui.tarunshankerpandey.com/show_data.php?data_type=xml&theme_id=" + themeName;

                Request.Builder builder = new Request.Builder()
                        .url(url)
                        .get();

                Response response;
                try {
                    response = client.newCall(builder.build()).execute();
                } catch (IOException e) {
                    Tuils.sendOutput(context, e.toString());
                    return;
                }

                if(response.isSuccessful()) {
                    String string;
                    try {
                        string = response.body().string();
                    } catch (IOException e) {
                        string = Tuils.EMPTYSTRING;
                    }

                    if(string.length() == 0) {
                        Tuils.sendOutput(context, R.string.theme_not_found);
                        return;
                    }

                    Matcher m = parser.matcher(string);
                    if(m.find()) {
                        String suggestions = m.group(1);
                        String theme = m.group(2);

                        applyTheme(theme, suggestions, true, themeName);
                    } else {
                        Tuils.sendOutput(context, R.string.theme_not_found);
                        return;
                    }
                }
            }
        }.start();
    }

    public void apply(File zip) {
        
    }

    private void applyTheme(File theme, File suggestions, boolean keepOld) {
        if(theme == null || suggestions == null) {
            Tuils.sendOutput(context, R.string.theme_unable);
            return;
        }

        File oldTheme = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.THEME.path);
        File oldSuggestions = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.path);
        if(keepOld) {
            Tuils.insertOld(oldTheme);
            Tuils.insertOld(oldSuggestions);
        }

        theme.renameTo(oldTheme);
        suggestions.renameTo(oldSuggestions);

        reloadable.reload();
    }

    private void applyTheme(String theme, String suggestions, boolean keepOld, String themeName) {
        if(theme == null || suggestions == null) {
            Tuils.sendOutput(context, R.string.theme_unable);
            return;
        }

        Matcher colorMatcher = colorParser.matcher(theme);
        while(colorMatcher.find()) {
            theme = Pattern.compile(Pattern.quote(colorMatcher.group())).matcher(theme).replaceAll(toHexColor(colorMatcher.group()));
        }

        colorMatcher = colorParser.matcher(suggestions);
        while(colorMatcher.find()) {
            suggestions = Pattern.compile(Pattern.quote(colorMatcher.group())).matcher(suggestions).replaceAll(toHexColor(colorMatcher.group()));
        }

        File oldTheme = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.THEME.path);
        File oldSuggestions = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.path);
        if(keepOld) {
            Tuils.insertOld(oldTheme);
            Tuils.insertOld(oldSuggestions);
        }
        oldTheme.delete();
        oldSuggestions.delete();

        try {
            FileOutputStream themeStream = new FileOutputStream(oldTheme);
            themeStream.write(theme.getBytes());
            themeStream.flush();
            themeStream.close();

            FileOutputStream suggestionsStream = new FileOutputStream(oldSuggestions);
            suggestionsStream.write(suggestions.getBytes());
            suggestionsStream.flush();
            suggestionsStream.close();

            reloadable.addMessage(context.getString(R.string.theme_applied) + Tuils.SPACE + themeName, null);
            reloadable.reload();
        } catch (IOException e) {
            Tuils.sendOutput(context, R.string.output_error);
        }
    }

    private void revert() {
        applyTheme(Tuils.getOld(XMLPrefsManager.XMLPrefsRoot.THEME.path), Tuils.getOld(XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.path), false);
    }

    private void standard() {
        File oldTheme = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.THEME.path);
        File oldSuggestions = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.path);
        Tuils.insertOld(oldTheme);
        Tuils.insertOld(oldSuggestions);

        oldTheme.delete();
        oldSuggestions.delete();

        reloadable.addMessage(context.getString(R.string.theme_applied) + Tuils.SPACE + "standard", null);
    }

//    rgba(255,87,34,1)
    Pattern colorParser = Pattern.compile("rgba\\([\\s]*(\\d+),[\\s]*(\\d+),[\\s]*(\\d+),[\\s]*(\\d.*\\d*)[\\s]*\\)");
    private String toHexColor(String color) {
        Matcher m = colorParser.matcher(color);
        if(m.find()) {
            int red = Integer.parseInt(m.group(1));
            int green = Integer.parseInt(m.group(2));
            int blue = Integer.parseInt(m.group(3));
            float alpha = Float.parseFloat(m.group(4));

            String redHex = Integer.toHexString(red);
            if(redHex.length() == 1) redHex = "0" + redHex;

            String greenHex = Integer.toHexString(green);
            if(greenHex.length() == 1) greenHex = "0" + greenHex;

            String blueHex = Integer.toHexString(blue);
            if(blueHex.length() == 1) blueHex = "0" + blueHex;

            String alphaHex = Integer.toHexString((int) alpha);
            if(alphaHex.length() == 1) alphaHex = "0" + alphaHex;

            return "#" + (alpha == 1 ? Tuils.EMPTYSTRING : alphaHex) + redHex + greenHex + blueHex;
        } else return Tuils.EMPTYSTRING;
    }
}
