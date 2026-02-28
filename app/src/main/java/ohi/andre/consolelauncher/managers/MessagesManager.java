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
        // Disabled all hints and tutorials
    }

    public void afterCmd() {
        // Disabled
    }

    private void tryPrint() {
        // Disabled
    }

    public void onDestroy() {
        // Disabled
    }

    public static boolean isShowingFirstTimeTutorial(Context context) {
        return false;
    }
}
