package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 31/08/2017.
 */

public class MessagesManager {

    private static final String PREFS_NAME = "tutorial", NEED_TUTORIAL_KEY = "needTutorial", LAST_TUTORIAL_COUNT = "lastTutorialCount";

    final String MARKER = "---------------";

    boolean donate = false;
    final int REACH_THIS = 20;

    List<String> original;
    List<String> copy;

    int count;
    Random random;

    Context context;
    int color;

    boolean tutorialMode;

    final int delay = 100;
    Handler handler = new Handler();
    Runnable post = this::tryPrint;

    public MessagesManager(Context context) {
        this.context = context;

        color = XMLPrefsManager.getColor(Theme.hint_color);

        tutorialMode = isShowingFirstTimeTutorial(context);
        if(tutorialMode) {
            SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, 0);
            count = preferences.getInt(LAST_TUTORIAL_COUNT, 0);

            String[] hints = context.getResources().getStringArray(R.array.tutorial);
            original = Arrays.asList(hints);

            SharedPreferences.Editor editor = preferences.edit();
            if(count < hints.length) {
                Tuils.sendOutput(color, context, original.get(count));
                editor.putInt(LAST_TUTORIAL_COUNT, ++count).apply();
            }
            else editor.putBoolean(NEED_TUTORIAL_KEY, false).apply();
        } else {
            String[] hints = context.getResources().getStringArray(R.array.hints);

            original = Arrays.asList(hints);
            copy = new ArrayList<>(original);

            count = 0;
            random = new Random();
        }
    }

    public void afterCmd() {
//        because otherwise we would risk to print this before a command. we want after
        handler.postDelayed(post, delay);
    }

    private void tryPrint() {
        count++;

        if(tutorialMode) {
            SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, 0).edit();
            if(count < original.size()) {
                Tuils.sendOutput(color, context, original.get(count));
                editor.putInt(LAST_TUTORIAL_COUNT, count).apply();
            } else {
                editor.putBoolean(NEED_TUTORIAL_KEY, false).apply();
            }
        } else if(count == REACH_THIS) {
            count = 0;

            if(donate) {
                Tuils.sendOutput(color, context, R.string.donate);
            } else {
                if(copy.size() == 0) {
                    copy = new ArrayList<>(original);
                    random = new Random();
                }

                int index = random.nextInt(copy.size());
                if(copy.size() <= index) {
                    return;
                }

                Tuils.sendOutput(color, context, MARKER + Tuils.NEWLINE + copy.remove(index) + Tuils.NEWLINE + MARKER);
            }

            donate = !donate;
        }
    }

    public void onDestroy() {
        if(tutorialMode) {
            SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, 0).edit();

            if(count + 1 < original.size()) {
                editor.putInt(LAST_TUTORIAL_COUNT, count + 1).apply();
            } else {
                editor.putBoolean(NEED_TUTORIAL_KEY, false).apply();
            }
        }
    }

    public static boolean isShowingFirstTimeTutorial(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, 0);

        if(preferences.getBoolean(NEED_TUTORIAL_KEY, true)) {
            return true;
        }

        if(preferences.getInt(LAST_TUTORIAL_COUNT, 0) >= context.getResources().getStringArray(R.array.tutorial).length) {
            preferences.edit().putBoolean(NEED_TUTORIAL_KEY, false).apply();
            return false;
        } else {
            return true;
        }
    }
}
