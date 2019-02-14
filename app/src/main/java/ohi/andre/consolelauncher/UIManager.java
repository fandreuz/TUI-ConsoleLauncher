package ohi.andre.consolelauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GestureDetectorCompat;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.RedirectCommand;
import ohi.andre.consolelauncher.managers.HTMLExtractManager;
import ohi.andre.consolelauncher.managers.NotesManager;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.TimeManager;
import ohi.andre.consolelauncher.managers.TuiLocationManager;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionTextWatcher;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionsManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Suggestions;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.managers.xml.options.Toolbar;
import ohi.andre.consolelauncher.managers.xml.options.Ui;
import ohi.andre.consolelauncher.tuils.AllowEqualsSequence;
import ohi.andre.consolelauncher.tuils.NetworkUtils;
import ohi.andre.consolelauncher.tuils.OutlineEditText;
import ohi.andre.consolelauncher.tuils.OutlineTextView;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.OnBatteryUpdate;
import ohi.andre.consolelauncher.tuils.interfaces.OnRedirectionListener;
import ohi.andre.consolelauncher.tuils.interfaces.OnTextChanged;
import ohi.andre.consolelauncher.tuils.stuff.PolicyReceiver;

public class UIManager implements OnTouchListener {

    public static String ACTION_UPDATE_SUGGESTIONS = BuildConfig.APPLICATION_ID + ".ui_update_suggestions";
    public static String ACTION_UPDATE_HINT = BuildConfig.APPLICATION_ID + ".ui_update_hint";
    public static String ACTION_ROOT = BuildConfig.APPLICATION_ID + ".ui_root";
    public static String ACTION_NOROOT = BuildConfig.APPLICATION_ID + ".ui_noroot";
    public static String ACTION_LOGTOFILE = BuildConfig.APPLICATION_ID + ".ui_log";
    public static String ACTION_CLEAR = BuildConfig.APPLICATION_ID + "ui_clear";
    public static String ACTION_WEATHER = BuildConfig.APPLICATION_ID + "ui_weather";
    public static String ACTION_WEATHER_GOT_LOCATION = BuildConfig.APPLICATION_ID + "ui_weather_location";
    public static String ACTION_WEATHER_DELAY = BuildConfig.APPLICATION_ID + "ui_weather_delay";
    public static String ACTION_WEATHER_MANUAL_UPDATE = BuildConfig.APPLICATION_ID + "ui_weather_update";

    public static String FILE_NAME = "fileName";
    public static String PREFS_NAME = "ui";

    private enum Label {
        ram,
        device,
        time,
        battery,
        storage,
        network,
        notes,
        weather,
        unlock
    }

    private final int RAM_DELAY = 3000;
    private final int TIME_DELAY = 1000;
    private final int STORAGE_DELAY = 60 * 1000;

    protected Context mContext;

    private Handler handler;

    private DevicePolicyManager policy;
    private ComponentName component;
    private GestureDetectorCompat gestureDetector;

    SharedPreferences preferences;

    private InputMethodManager imm;
    private TerminalManager mTerminalAdapter;

    int mediumPercentage, lowPercentage;
    String batteryFormat;

    boolean hideToolbarNoInput;
    View toolbarView;

    //    never access this directly, use getLabelView
    private TextView[] labelViews = new TextView[Label.values().length];

    private float[] labelIndexes = new float[labelViews.length];
    private int[] labelSizes = new int[labelViews.length];
    private CharSequence[] labelTexts = new CharSequence[labelViews.length];

    private TextView getLabelView(Label l) {
        return labelViews[(int) labelIndexes[l.ordinal()]];
    }

    private int notesMaxLines;
    private NotesManager notesManager;
    private NotesRunnable notesRunnable;
    private class NotesRunnable implements Runnable {

        int updateTime = 2000;

        @Override
        public void run() {
            if(notesManager != null) {
                if(notesManager.hasChanged) {
                    UIManager.this.updateText(Label.notes, Tuils.span(mContext, labelSizes[Label.notes.ordinal()], notesManager.getNotes()));
                }

                handler.postDelayed(this, updateTime);
            }
        }
    };

    private BatteryUpdate batteryUpdate;
    private class BatteryUpdate implements OnBatteryUpdate {

//        %(charging:not charging)

        //        final Pattern optionalCharging = Pattern.compile("%\\(([^\\/]*)\\/([^)]*)\\)", Pattern.CASE_INSENSITIVE);
        Pattern optionalCharging;
        final Pattern value = Pattern.compile("%v", Pattern.LITERAL | Pattern.CASE_INSENSITIVE);

        boolean manyStatus, loaded;
        int colorHigh, colorMedium, colorLow;

        boolean charging;
        float last = -1;

        @Override
        public void update(float p) {
            if(batteryFormat == null) {
                batteryFormat = XMLPrefsManager.get(Behavior.battery_format);

                Intent intent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if(intent == null) charging = false;
                else {
                    int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    charging = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
                }

                String optionalSeparator = "\\" + XMLPrefsManager.get(Behavior.optional_values_separator);
                String optional = "%\\(([^" + optionalSeparator + "]*)" + optionalSeparator + "([^)]*)\\)";
                optionalCharging = Pattern.compile(optional, Pattern.CASE_INSENSITIVE);
            }

            if(p == -1) p = last;
            last = p;

            if(!loaded) {
                loaded = true;

                manyStatus = XMLPrefsManager.getBoolean(Ui.enable_battery_status);
                colorHigh = XMLPrefsManager.getColor(Theme.battery_color_high);
                colorMedium = XMLPrefsManager.getColor(Theme.battery_color_medium);
                colorLow = XMLPrefsManager.getColor(Theme.battery_color_low);
            }

            int percentage = (int) p;

            int color;

            if(manyStatus) {
                if(percentage > mediumPercentage) color = colorHigh;
                else if(percentage > lowPercentage) color = colorMedium;
                else color = colorLow;
            } else {
                color = colorHigh;
            }

            String cp = batteryFormat;

            Matcher m = optionalCharging.matcher(cp);
            while (m.find()) {
                cp = cp.replace(m.group(0), m.groupCount() == 2 ? m.group(charging ? 1 : 2) : Tuils.EMPTYSTRING);
            }

            cp = value.matcher(cp).replaceAll(String.valueOf(percentage));
            cp = Tuils.patternNewline.matcher(cp).replaceAll(Tuils.NEWLINE);

            UIManager.this.updateText(Label.battery, Tuils.span(mContext, cp, color, labelSizes[Label.battery.ordinal()]));
        }

        @Override
        public void onCharging() {
            charging = true;
            update(-1);
        }

        @Override
        public void onNotCharging() {
            charging = false;
            update(-1);
        }
    };

    private StorageRunnable storageRunnable;
    private class StorageRunnable implements Runnable {

        private final String INT_AV = "%iav";
        private final String INT_TOT = "%itot";
        private final String EXT_AV = "%eav";
        private final String EXT_TOT = "%etot";

        private List<Pattern> storagePatterns;
        private String storageFormat;

        int color;

        @Override
        public void run() {
            if(storageFormat == null) {
                storageFormat = XMLPrefsManager.get(Behavior.storage_format);
                color = XMLPrefsManager.getColor(Theme.storage_color);
            }

            if(storagePatterns == null) {
                storagePatterns = new ArrayList<>();

                storagePatterns.add(Pattern.compile(INT_AV + "tb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_AV + "gb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_AV + "mb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_AV + "kb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_AV + "b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_AV + "%", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

                storagePatterns.add(Pattern.compile(INT_TOT + "tb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_TOT + "gb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_TOT + "mb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_TOT + "kb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_TOT + "b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

                storagePatterns.add(Pattern.compile(EXT_AV + "tb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_AV + "gb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_AV + "mb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_AV + "kb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_AV + "b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_AV + "%", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

                storagePatterns.add(Pattern.compile(EXT_TOT + "tb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_TOT + "gb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_TOT + "mb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_TOT + "kb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_TOT + "b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

                storagePatterns.add(Tuils.patternNewline);

                storagePatterns.add(Pattern.compile(INT_AV, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_TOT, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_AV, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_TOT, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
            }

            double iav = Tuils.getAvailableInternalMemorySize(Tuils.BYTE);
            double itot = Tuils.getTotalInternalMemorySize(Tuils.BYTE);
            double eav = Tuils.getAvailableExternalMemorySize(Tuils.BYTE);
            double etot = Tuils.getTotalExternalMemorySize(Tuils.BYTE);

            String copy = storageFormat;

            copy = storagePatterns.get(0).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) iav, Tuils.TERA))));
            copy = storagePatterns.get(1).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) iav, Tuils.GIGA))));
            copy = storagePatterns.get(2).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) iav, Tuils.MEGA))));
            copy = storagePatterns.get(3).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) iav, Tuils.KILO))));
            copy = storagePatterns.get(4).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) iav, Tuils.BYTE))));
            copy = storagePatterns.get(5).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.percentage(iav, itot))));

            copy = storagePatterns.get(6).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) itot, Tuils.TERA))));
            copy = storagePatterns.get(7).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) itot, Tuils.GIGA))));
            copy = storagePatterns.get(8).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) itot, Tuils.MEGA))));
            copy = storagePatterns.get(9).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) itot, Tuils.KILO))));
            copy = storagePatterns.get(10).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) itot, Tuils.BYTE))));

            copy = storagePatterns.get(11).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) eav, Tuils.TERA))));
            copy = storagePatterns.get(12).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) eav, Tuils.GIGA))));
            copy = storagePatterns.get(13).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) eav, Tuils.MEGA))));
            copy = storagePatterns.get(14).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) eav, Tuils.KILO))));
            copy = storagePatterns.get(15).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) eav, Tuils.BYTE))));
            copy = storagePatterns.get(16).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.percentage(eav, etot))));

            copy = storagePatterns.get(17).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) etot, Tuils.TERA))));
            copy = storagePatterns.get(18).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) etot, Tuils.GIGA))));
            copy = storagePatterns.get(19).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) etot, Tuils.MEGA))));
            copy = storagePatterns.get(20).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) etot, Tuils.KILO))));
            copy = storagePatterns.get(21).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) etot, Tuils.BYTE))));

            copy = storagePatterns.get(22).matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));

            copy = storagePatterns.get(23).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) iav, Tuils.GIGA))));
            copy = storagePatterns.get(24).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) itot, Tuils.GIGA))));
            copy = storagePatterns.get(25).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) eav, Tuils.GIGA))));
            copy = storagePatterns.get(26).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) etot, Tuils.GIGA))));

            updateText(Label.storage, Tuils.span(mContext, copy, color, labelSizes[Label.storage.ordinal()]));

            handler.postDelayed(this, STORAGE_DELAY);
        }
    };

    private TimeRunnable timeRunnable;
    private class TimeRunnable implements Runnable {

        boolean active;

        @Override
        public void run() {
            if(!active) {
                active = true;
            }

            updateText(Label.time, TimeManager.instance.getCharSequence(mContext, labelSizes[Label.time.ordinal()], "%t0"));
            handler.postDelayed(this, TIME_DELAY);
        }
    };

    private ActivityManager.MemoryInfo memory;
    private ActivityManager activityManager;

    private RamRunnable ramRunnable;
    private class RamRunnable implements Runnable {
        private final String AV = "%av";
        private final String TOT = "%tot";

        List<Pattern> ramPatterns;
        String ramFormat;

        int color;

        @Override
        public void run() {
            if(ramFormat == null) {
                ramFormat = XMLPrefsManager.get(Behavior.ram_format);

                color = XMLPrefsManager.getColor(Theme.ram_color);
            }

            if(ramPatterns == null) {
                ramPatterns = new ArrayList<>();

                ramPatterns.add(Pattern.compile(AV + "tb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(AV + "gb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(AV + "mb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(AV + "kb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(AV + "b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(AV + "%", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

                ramPatterns.add(Pattern.compile(TOT + "tb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(TOT + "gb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(TOT + "mb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(TOT + "kb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(TOT + "b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

                ramPatterns.add(Tuils.patternNewline);
            }

            String copy = ramFormat;

            double av = Tuils.freeRam(activityManager, memory);
            double tot = Tuils.totalRam() * 1024L;

            copy = ramPatterns.get(0).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) av, Tuils.TERA))));
            copy = ramPatterns.get(1).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) av, Tuils.GIGA))));
            copy = ramPatterns.get(2).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) av, Tuils.MEGA))));
            copy = ramPatterns.get(3).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) av, Tuils.KILO))));
            copy = ramPatterns.get(4).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) av, Tuils.BYTE))));
            copy = ramPatterns.get(5).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.percentage(av, tot))));

            copy = ramPatterns.get(6).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) tot, Tuils.TERA))));
            copy = ramPatterns.get(7).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) tot, Tuils.GIGA))));
            copy = ramPatterns.get(8).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) tot, Tuils.MEGA))));
            copy = ramPatterns.get(9).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) tot, Tuils.KILO))));
            copy = ramPatterns.get(10).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) tot, Tuils.BYTE))));

            copy = ramPatterns.get(11).matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));

            updateText(Label.ram, Tuils.span(mContext, copy, color, labelSizes[Label.ram.ordinal()]));

            handler.postDelayed(this, RAM_DELAY);
        }
    };

    private NetworkRunnable networkRunnable;
    private class NetworkRunnable implements Runnable {
//        %() -> wifi
//        %[] -> data
//        %{} -> bluetooth

        final String zero = "0";
        final String one = "1";
        final String on = "on";
        final String off = "off";
        final String ON = on.toUpperCase();
        final String OFF = off.toUpperCase();
        final String _true = "true";
        final String _false = "false";
        final String TRUE = _true.toUpperCase();
        final String FALSE = _false.toUpperCase();

        final Pattern w0 = Pattern.compile("%w0", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern w1 = Pattern.compile("%w1", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern w2 = Pattern.compile("%w2", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern w3 = Pattern.compile("%w3", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern w4 = Pattern.compile("%w4", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern wn = Pattern.compile("%wn", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern d0 = Pattern.compile("%d0", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern d1 = Pattern.compile("%d1", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern d2 = Pattern.compile("%d2", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern d3 = Pattern.compile("%d3", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern d4 = Pattern.compile("%d4", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern b0 = Pattern.compile("%b0", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern b1 = Pattern.compile("%b1", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern b2 = Pattern.compile("%b2", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern b3 = Pattern.compile("%b3", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern b4 = Pattern.compile("%b4", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern ip4 = Pattern.compile("%ip4", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern ip6 = Pattern.compile("%ip6", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern dt = Pattern.compile("%dt", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

//        final Pattern optionalWifi = Pattern.compile("%\\(([^/]*)/([^)]*)\\)", Pattern.CASE_INSENSITIVE);
//        final Pattern optionalData = Pattern.compile("%\\[([^/]*)/([^\\]]*)\\]", Pattern.CASE_INSENSITIVE);
//        final Pattern optionalBluetooth = Pattern.compile("%\\{([^/]*)/([^}]*)\\}", Pattern.CASE_INSENSITIVE);

        Pattern optionalWifi, optionalData, optionalBluetooth;

        String format, optionalValueSeparator;
        int color;

        WifiManager wifiManager;
        BluetoothAdapter mBluetoothAdapter;

        ConnectivityManager connectivityManager;

        Class cmClass;
        Method method;

        int maxDepth;
        int updateTime;

        @Override
        public void run() {
            if (format == null) {
                format = XMLPrefsManager.get(Behavior.network_info_format);
                color = XMLPrefsManager.getColor(Theme.network_info_color);
                maxDepth = XMLPrefsManager.getInt(Behavior.max_optional_depth);

                updateTime = XMLPrefsManager.getInt(Behavior.network_info_update_ms);
                if (updateTime < 1000)
                    updateTime = Integer.parseInt(Behavior.network_info_update_ms.defaultValue());

                connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                optionalValueSeparator = "\\" + XMLPrefsManager.get(Behavior.optional_values_separator);

                String wifiRegex = "%\\(([^" + optionalValueSeparator + "]*)" + optionalValueSeparator + "([^)]*)\\)";
                String dataRegex = "%\\[([^" + optionalValueSeparator + "]*)" + optionalValueSeparator + "([^\\]]*)\\]";
                String bluetoothRegex = "%\\{([^" + optionalValueSeparator + "]*)" + optionalValueSeparator + "([^}]*)\\}";

                optionalWifi = Pattern.compile(wifiRegex, Pattern.CASE_INSENSITIVE);
                optionalBluetooth = Pattern.compile(bluetoothRegex, Pattern.CASE_INSENSITIVE);
                optionalData = Pattern.compile(dataRegex, Pattern.CASE_INSENSITIVE);

                try {
                    cmClass = Class.forName(connectivityManager.getClass().getName());
                    method = cmClass.getDeclaredMethod("getMobileDataEnabled");
                    method.setAccessible(true);
                } catch (Exception e) {
                    cmClass = null;
                    method = null;
                }
            }

//            wifi
            boolean wifiOn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
            String wifiName = null;
            if (wifiOn) {
                WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null) {
                    wifiName = connectionInfo.getSSID();
                }
            }

//            mobile data
            boolean mobileOn = false;
            try {
                mobileOn = method != null && connectivityManager != null && (Boolean) method.invoke(connectivityManager);
            } catch (Exception e) {
            }

            String mobileType = null;
            if (mobileOn) {
                mobileType = Tuils.getNetworkType(mContext);
            } else {
                mobileType = "unknown";
            }

//            bluetooth
            boolean bluetoothOn = mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();

            String copy = format;

            if (maxDepth > 0) {
                copy = apply(1, copy, new boolean[]{wifiOn, mobileOn, bluetoothOn}, optionalWifi, optionalData, optionalBluetooth);
                copy = apply(1, copy, new boolean[]{mobileOn, wifiOn, bluetoothOn}, optionalData, optionalWifi, optionalBluetooth);
                copy = apply(1, copy, new boolean[]{bluetoothOn, wifiOn, mobileOn}, optionalBluetooth, optionalWifi, optionalData);
            }

            copy = w0.matcher(copy).replaceAll(wifiOn ? one : zero);
            copy = w1.matcher(copy).replaceAll(wifiOn ? on : off);
            copy = w2.matcher(copy).replaceAll(wifiOn ? ON : OFF);
            copy = w3.matcher(copy).replaceAll(wifiOn ? _true : _false);
            copy = w4.matcher(copy).replaceAll(wifiOn ? TRUE : FALSE);
            copy = wn.matcher(copy).replaceAll(wifiName != null ? wifiName.replaceAll("\"", Tuils.EMPTYSTRING) : "null");
            copy = d0.matcher(copy).replaceAll(mobileOn ? one : zero);
            copy = d1.matcher(copy).replaceAll(mobileOn ? on : off);
            copy = d2.matcher(copy).replaceAll(mobileOn ? ON : OFF);
            copy = d3.matcher(copy).replaceAll(mobileOn ? _true : _false);
            copy = d4.matcher(copy).replaceAll(mobileOn ? TRUE : FALSE);
            copy = b0.matcher(copy).replaceAll(bluetoothOn ? one : zero);
            copy = b1.matcher(copy).replaceAll(bluetoothOn ? on : off);
            copy = b2.matcher(copy).replaceAll(bluetoothOn ? ON : OFF);
            copy = b3.matcher(copy).replaceAll(bluetoothOn ? _true : _false);
            copy = b4.matcher(copy).replaceAll(bluetoothOn ? TRUE : FALSE);
            copy = ip4.matcher(copy).replaceAll(NetworkUtils.getIPAddress(true));
            copy = ip6.matcher(copy).replaceAll(NetworkUtils.getIPAddress(false));
            copy = dt.matcher(copy).replaceAll(mobileType);
            copy = Tuils.patternNewline.matcher(copy).replaceAll(Tuils.NEWLINE);

            updateText(Label.network, Tuils.span(mContext, copy, color, labelSizes[Label.network.ordinal()]));
            handler.postDelayed(this, updateTime);
        }

        private String apply(int depth, String s, boolean[] on, Pattern... ps) {

            if(ps.length == 0) return s;

            Matcher m = ps[0].matcher(s);
            while (m.find()) {
                if(m.groupCount() < 2) {
                    s = s.replace(m.group(0), Tuils.EMPTYSTRING);
                    continue;
                }

                String g1 = m.group(1);
                String g2 = m.group(2);

                if(depth < maxDepth) {
                    for(int c = 0; c < ps.length - 1; c++) {

                        boolean[] subOn = new boolean[on.length - 1];
                        subOn[0] = on[c+1];

                        Pattern[] subPs = new Pattern[ps.length - 1];
                        subPs[0] = ps[c+1];

                        for(int j = 1, k = 1; j < subOn.length; j++, k++) {
                            if(k == c+1) {
                                j--;
                                continue;
                            }

                            subOn[j] = on[k];
                            subPs[j] = ps[k];
                        }

                        g1 = apply(depth + 1, g1, subOn, subPs);
                        g2 = apply(depth + 1, g2, subOn, subPs);
                    }
                }

                s = s.replace(m.group(0), on[0] ? g1 : g2);
            }

            return s;
        }
    }

    private int weatherDelay;

    private double lastLatitude, lastLongitude;
    private String location;
    private boolean fixedLocation = false;

    private boolean weatherPerformedStartupRun = false;
    private WeatherRunnable weatherRunnable;
    private int weatherColor;
    boolean showWeatherUpdate;

    private class WeatherRunnable implements Runnable {

        String key;
        String url;

        public WeatherRunnable() {

            if(XMLPrefsManager.wasChanged(Behavior.weather_key, false)) {
                weatherDelay = XMLPrefsManager.getInt(Behavior.weather_update_time);
                key = XMLPrefsManager.get(Behavior.weather_key);
            } else {
                key = Behavior.weather_key.defaultValue();
                weatherDelay = 60 * 60;
            }
            weatherDelay *= 1000;

            String where = XMLPrefsManager.get(Behavior.weather_location);
            if(where == null || where.length() == 0 || (!Tuils.isNumber(where) && !where.contains(","))) {
//                Tuils.location(mContext, new Tuils.ArgsRunnable() {
//                    @Override
//                    public void run() {
//                        setUrl(
//                                "lat=" + get(int.class, 0) + "&lon=" + get(int.class, 1),
//                                finalKey,
//                                XMLPrefsManager.get(Behavior.weather_temperature_measure));
//                        WeatherRunnable.this.run();
//                    }
//                }, new Runnable() {
//                    @Override
//                    public void run() {
//                        updateText(Label.weather, Tuils.span(mContext, mContext.getString(R.string.location_error), XMLPrefsManager.getColor(Theme.weather_color), labelSizes[Label.weather.ordinal()]));
//                    }
//                }, handler);

//                Location l = Tuils.getLocation(mContext);
//                if(l != null) {
//                    setUrl(
//                            "lat=" + l.getLatitude() + "&lon=" + l.getLongitude(),
//                            finalKey,
//                            XMLPrefsManager.get(Behavior.weather_temperature_measure));
//                    WeatherRunnable.this.run();
//                } else {
//                    updateText(Label.weather, Tuils.span(mContext, mContext.getString(R.string.location_error), XMLPrefsManager.getColor(Theme.weather_color), labelSizes[Label.weather.ordinal()]));
//                }

                TuiLocationManager l = TuiLocationManager.instance(mContext);
                l.add(ACTION_WEATHER_GOT_LOCATION);

            } else {
                fixedLocation = true;

                if(where.contains(",")) {
                    String[] split = where.split(",");
                    where = "lat=" + split[0] + "&lon=" + split[1];
                } else {
                    where = "id=" + where;
                }

                setUrl(where);
            }
        }

        @Override
        public void run() {
            weatherPerformedStartupRun = true;
            if(!fixedLocation) setUrl(lastLatitude, lastLongitude);

            send();

            if(handler != null) handler.postDelayed(this, weatherDelay);
        }

        private void send() {
            if(url == null) return;

            Intent i = new Intent(HTMLExtractManager.ACTION_WEATHER);
            i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, url);
            i.putExtra(HTMLExtractManager.BROADCAST_COUNT, HTMLExtractManager.broadcastCount);
            LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(i);
        }

        private void setUrl(String where) {
            url = "http://api.openweathermap.org/data/2.5/weather?" + where + "&appid=" + key + "&units=" + XMLPrefsManager.get(Behavior.weather_temperature_measure);
        }

        private void setUrl(double latitude, double longitude) {
            url = "http://api.openweathermap.org/data/2.5/weather?" + "lat=" + latitude + "&lon=" + longitude + "&appid=" + key + "&units=" + XMLPrefsManager.get(Behavior.weather_temperature_measure);
        }
    }

//    you need to use labelIndexes[i]
    private void updateText(Label l, CharSequence s) {
        labelTexts[l.ordinal()] = s;

        int base = (int) labelIndexes[l.ordinal()];

        List<Float> indexs = new ArrayList<>();
        for(int count = 0; count < Label.values().length; count++) {
            if((int) labelIndexes[count] == base && labelTexts[count] != null) indexs.add(labelIndexes[count]);
        }
//        now I'm sorting the labels on the same line for decimals (2.1, 2.0, ...)
        Collections.sort(indexs);

        CharSequence sequence = Tuils.EMPTYSTRING;

        for(int c = 0; c < indexs.size(); c++) {
            float i = indexs.get(c);

            for(int a = 0; a < Label.values().length; a++) {
                if(i == labelIndexes[a] && labelTexts[a] != null) sequence = TextUtils.concat(sequence, labelTexts[a]);
            }
        }

        if(sequence.length() == 0) labelViews[base].setVisibility(View.GONE);
        else {
            labelViews[base].setVisibility(View.VISIBLE);
            labelViews[base].setText(sequence);
        }
    }

    private SuggestionsManager suggestionsManager;

    private TextView terminalView;

    private String doubleTapCmd;
    private boolean lockOnDbTap;

    private BroadcastReceiver receiver;

    public MainPack pack;

    private boolean clearOnLock;

    protected UIManager(final Context context, final ViewGroup rootView, MainPack mainPack, boolean canApplyTheme, CommandExecuter executer) {

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_SUGGESTIONS);
        filter.addAction(ACTION_UPDATE_HINT);
        filter.addAction(ACTION_ROOT);
        filter.addAction(ACTION_NOROOT);
//        filter.addAction(ACTION_CLEAR_SUGGESTIONS);
        filter.addAction(ACTION_LOGTOFILE);
        filter.addAction(ACTION_CLEAR);
        filter.addAction(ACTION_WEATHER);
        filter.addAction(ACTION_WEATHER_GOT_LOCATION);
        filter.addAction(ACTION_WEATHER_DELAY);
        filter.addAction(ACTION_WEATHER_MANUAL_UPDATE);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(action.equals(ACTION_UPDATE_SUGGESTIONS)) {
                    if(suggestionsManager != null) suggestionsManager.requestSuggestion(Tuils.EMPTYSTRING);
                } else if(action.equals(ACTION_UPDATE_HINT)) {
                    mTerminalAdapter.setDefaultHint();
                } else if(action.equals(ACTION_ROOT)) {
                    mTerminalAdapter.onRoot();
                } else if(action.equals(ACTION_NOROOT)) {
                    mTerminalAdapter.onStandard();
//                } else if(action.equals(ACTION_CLEAR_SUGGESTIONS)) {
//                    if(suggestionsManager != null) suggestionsManager.clear();
                } else if(action.equals(ACTION_LOGTOFILE)) {
                    String fileName = intent.getStringExtra(FILE_NAME);
                    if(fileName == null || fileName.contains(File.separator)) return;

                    File file = new File(Tuils.getFolder(), fileName);
                    if(file.exists()) file.delete();

                    try {
                        file.createNewFile();

                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(mTerminalAdapter.getTerminalText().getBytes());

                        Tuils.sendOutput(context, "Logged to " + file.getAbsolutePath());
                    } catch (Exception e) {
                        Tuils.sendOutput(Color.RED, context, e.toString());
                    }
                } else if(action.equals(ACTION_CLEAR)) {
                    mTerminalAdapter.clear();
                    if (suggestionsManager != null)
                        suggestionsManager.requestSuggestion(Tuils.EMPTYSTRING);
                } else if(action.equals(ACTION_WEATHER)) {
                    Calendar c = Calendar.getInstance();

                    CharSequence s = intent.getCharSequenceExtra(XMLPrefsManager.VALUE_ATTRIBUTE);
                    if(s == null) s = intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE);
                    if(s == null) return;

                    s = Tuils.span(context, s, weatherColor, labelSizes[Label.weather.ordinal()]);

                    updateText(Label.weather, s);

                    if(showWeatherUpdate) {
                        String message = context.getString(R.string.weather_updated) + Tuils.SPACE + c.get(Calendar.HOUR_OF_DAY) + "." + c.get(Calendar.MINUTE) + Tuils.SPACE + "(" + lastLatitude + ", " + lastLongitude + ")";
                        Tuils.sendOutput(context, message, TerminalManager.CATEGORY_OUTPUT);
                    }
                } else if(action.equals(ACTION_WEATHER_GOT_LOCATION)) {
//                    int result = intent.getIntExtra(XMLPrefsManager.VALUE_ATTRIBUTE, 0);
//                    if(result == PackageManager.PERMISSION_DENIED) {
//                        updateText(Label.weather, Tuils.span(context, context.getString(R.string.location_error), weatherColor, labelSizes[Label.weather.ordinal()]));
//                    } else handler.post(weatherRunnable);

                    if(intent.getBooleanExtra(TuiLocationManager.FAIL, false)) {
                        handler.removeCallbacks(weatherRunnable);
                        weatherRunnable = null;

                        CharSequence s = Tuils.span(context, context.getString(R.string.location_error), weatherColor, labelSizes[Label.weather.ordinal()]);

                        updateText(Label.weather, s);
                    } else {
                        lastLatitude = intent.getDoubleExtra(TuiLocationManager.LATITUDE, 0);
                        lastLongitude = intent.getDoubleExtra(TuiLocationManager.LONGITUDE, 0);

                        location = Tuils.locationName(context, lastLatitude, lastLongitude);

                        if(!weatherPerformedStartupRun || XMLPrefsManager.wasChanged(Behavior.weather_key, false)) {
                            handler.removeCallbacks(weatherRunnable);
                            handler.post(weatherRunnable);
                        }
                    }
                } else if(action.equals(ACTION_WEATHER_DELAY)) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(System.currentTimeMillis() + 1000 * 10);

                    if(showWeatherUpdate) {
                        String message = context.getString(R.string.weather_error) + Tuils.SPACE + c.get(Calendar.HOUR_OF_DAY) + "." + c.get(Calendar.MINUTE);
                        Tuils.sendOutput(context, message, TerminalManager.CATEGORY_OUTPUT);
                    }

                    handler.removeCallbacks(weatherRunnable);
                    handler.postDelayed(weatherRunnable, 1000 * 60);
                } else if(action.equals(ACTION_WEATHER_MANUAL_UPDATE)) {
                    handler.removeCallbacks(weatherRunnable);
                    handler.post(weatherRunnable);
                }
            }
        };

        LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(receiver, filter);

        policy = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        component = new ComponentName(context, PolicyReceiver.class);

        mContext = context;

        preferences = mContext.getSharedPreferences(PREFS_NAME, 0);

        handler = new Handler();

        imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (!XMLPrefsManager.getBoolean(Ui.system_wallpaper) || !canApplyTheme) {
            rootView.setBackgroundColor(XMLPrefsManager.getColor(Theme.bg_color));
        } else {
            rootView.setBackgroundColor(XMLPrefsManager.getColor(Theme.overlay_color));
        }

//        scrolllllll
        if(XMLPrefsManager.getBoolean(Behavior.auto_scroll)) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
                if (heightDiff > Tuils.dpToPx(context, 200)) { // if more than 200 dp, it's probably a keyboard...
                    if(mTerminalAdapter != null) mTerminalAdapter.scrollToEnd();
                }
            });
        }

        clearOnLock = XMLPrefsManager.getBoolean(Behavior.clear_on_lock);

        lockOnDbTap = XMLPrefsManager.getBoolean(Behavior.double_tap_lock);
        doubleTapCmd = XMLPrefsManager.get(Behavior.double_tap_cmd);
        if(!lockOnDbTap && doubleTapCmd == null) {
            policy = null;
            component = null;
            gestureDetector = null;
        } else {
            gestureDetector = new GestureDetectorCompat(mContext, new GestureDetector.OnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return false;
                }

                @Override
                public void onShowPress(MotionEvent e) {}

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return false;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    return false;
                }

                @Override
                public void onLongPress(MotionEvent e) {}

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    return false;
                }
            });

            gestureDetector.setOnDoubleTapListener(new OnDoubleTapListener() {

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    return false;
                }

                @Override
                public boolean onDoubleTapEvent(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {

                    if(doubleTapCmd != null && doubleTapCmd.length() > 0) {
                        String input = mTerminalAdapter.getInput();
                        mTerminalAdapter.setInput(doubleTapCmd);
                        mTerminalAdapter.simulateEnter();
                        mTerminalAdapter.setInput(input);
                    }

                    if(lockOnDbTap) {
                        boolean admin = policy.isAdminActive(component);

                        if (!admin) {
                            Intent i = Tuils.requestAdmin(component, mContext.getString(R.string.admin_permission));
                            mContext.startActivity(i);
                        } else {
                            policy.lockNow();
                        }
                    }

                    return true;
                }
            });
        }

        int[] displayMargins = getListOfIntValues(XMLPrefsManager.get(Ui.display_margin_mm), 4, 0);
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        rootView.setPadding(Tuils.mmToPx(metrics, displayMargins[0]), Tuils.mmToPx(metrics, displayMargins[1]), Tuils.mmToPx(metrics, displayMargins[2]), Tuils.mmToPx(metrics, displayMargins[3]));

        labelSizes[Label.time.ordinal()] = XMLPrefsManager.getInt(Ui.time_size);
        labelSizes[Label.ram.ordinal()] = XMLPrefsManager.getInt(Ui.ram_size);
        labelSizes[Label.battery.ordinal()] = XMLPrefsManager.getInt(Ui.battery_size);
        labelSizes[Label.storage.ordinal()] = XMLPrefsManager.getInt(Ui.storage_size);
        labelSizes[Label.network.ordinal()] = XMLPrefsManager.getInt(Ui.network_size);
        labelSizes[Label.notes.ordinal()] = XMLPrefsManager.getInt(Ui.notes_size);
        labelSizes[Label.device.ordinal()] = XMLPrefsManager.getInt(Ui.device_size);
        labelSizes[Label.weather.ordinal()] = XMLPrefsManager.getInt(Ui.weather_size);
        labelSizes[Label.unlock.ordinal()] = XMLPrefsManager.getInt(Ui.unlock_size);

        labelViews = new TextView[] {
                (TextView) rootView.findViewById(R.id.tv0),
                (TextView) rootView.findViewById(R.id.tv1),
                (TextView) rootView.findViewById(R.id.tv2),
                (TextView) rootView.findViewById(R.id.tv3),
                (TextView) rootView.findViewById(R.id.tv4),
                (TextView) rootView.findViewById(R.id.tv5),
                (TextView) rootView.findViewById(R.id.tv6),
                (TextView) rootView.findViewById(R.id.tv7),
                (TextView) rootView.findViewById(R.id.tv8),
        };

        boolean[] show = new boolean[Label.values().length];
        show[Label.notes.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_notes);
        show[Label.ram.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_ram);
        show[Label.device.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_device_name);
        show[Label.time.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_time);
        show[Label.battery.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_battery);
        show[Label.network.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_network_info);
        show[Label.storage.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_storage_info);
        show[Label.weather.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_weather);
        show[Label.unlock.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_unlock_counter);

        float[] indexes = new float[Label.values().length];
        indexes[Label.notes.ordinal()] = show[Label.notes.ordinal()] ? XMLPrefsManager.getFloat(Ui.notes_index) : Integer.MAX_VALUE;
        indexes[Label.ram.ordinal()] = show[Label.ram.ordinal()] ? XMLPrefsManager.getFloat(Ui.ram_index) : Integer.MAX_VALUE;
        indexes[Label.device.ordinal()] = show[Label.device.ordinal()] ? XMLPrefsManager.getFloat(Ui.device_index) : Integer.MAX_VALUE;
        indexes[Label.time.ordinal()] = show[Label.time.ordinal()] ? XMLPrefsManager.getFloat(Ui.time_index) : Integer.MAX_VALUE;
        indexes[Label.battery.ordinal()] = show[Label.battery.ordinal()] ? XMLPrefsManager.getFloat(Ui.battery_index) : Integer.MAX_VALUE;
        indexes[Label.network.ordinal()] = show[Label.network.ordinal()] ? XMLPrefsManager.getFloat(Ui.network_index) : Integer.MAX_VALUE;
        indexes[Label.storage.ordinal()] = show[Label.storage.ordinal()] ? XMLPrefsManager.getFloat(Ui.storage_index) : Integer.MAX_VALUE;
        indexes[Label.weather.ordinal()] = show[Label.weather.ordinal()] ? XMLPrefsManager.getFloat(Ui.weather_index) : Integer.MAX_VALUE;
        indexes[Label.unlock.ordinal()] = show[Label.unlock.ordinal()] ? XMLPrefsManager.getFloat(Ui.unlock_index) : Integer.MAX_VALUE;

        int[] statusLineAlignments = getListOfIntValues(XMLPrefsManager.get(Ui.status_lines_alignment), 9, -1);

        String[] statusLinesBgRectColors = getListOfStringValues(XMLPrefsManager.get(Theme.status_lines_bgrectcolor), 9, "#ff000000");
        String[] otherBgRectColors = {
                XMLPrefsManager.get(Theme.input_bgrectcolor),
                XMLPrefsManager.get(Theme.output_bgrectcolor),
                XMLPrefsManager.get(Theme.suggestions_bgrectcolor),
                XMLPrefsManager.get(Theme.toolbar_bgrectcolor)
        };
        String[] bgRectColors = new String[statusLinesBgRectColors.length + otherBgRectColors.length];
        System.arraycopy(statusLinesBgRectColors, 0, bgRectColors, 0, statusLinesBgRectColors.length);
        System.arraycopy(otherBgRectColors, 0, bgRectColors, statusLinesBgRectColors.length, otherBgRectColors.length);

        String[] statusLineBgColors = getListOfStringValues(XMLPrefsManager.get(Theme.status_lines_bg), 9, "#00000000");
        String[] otherBgColors = {
                XMLPrefsManager.get(Theme.input_bg),
                XMLPrefsManager.get(Theme.output_bg),
                XMLPrefsManager.get(Theme.suggestions_bg),
                XMLPrefsManager.get(Theme.toolbar_bg)
        };
        String[] bgColors = new String[statusLineBgColors.length + otherBgColors.length];
        System.arraycopy(statusLineBgColors, 0, bgColors, 0, statusLineBgColors.length);
        System.arraycopy(otherBgColors, 0, bgColors, statusLineBgColors.length, otherBgColors.length);

        String[] statusLineOutlineColors = getListOfStringValues(XMLPrefsManager.get(Theme.status_lines_shadow_color), 9, "#00000000");
        String[] otherOutlineColors = {
                XMLPrefsManager.get(Theme.input_shadow_color),
                XMLPrefsManager.get(Theme.output_shadow_color),
        };
        String[] outlineColors = new String[statusLineOutlineColors.length + otherOutlineColors.length];
        System.arraycopy(statusLineOutlineColors, 0, outlineColors, 0, statusLineOutlineColors.length);
        System.arraycopy(otherOutlineColors, 0, outlineColors, 9, otherOutlineColors.length);

        int shadowXOffset, shadowYOffset;
        float shadowRadius;
        String[] shadowParams = getListOfStringValues(XMLPrefsManager.get(Ui.shadow_params), 3, "0");
        shadowXOffset = Integer.parseInt(shadowParams[0]);
        shadowYOffset = Integer.parseInt(shadowParams[1]);
        shadowRadius = Float.parseFloat(shadowParams[2]);

        final int INPUT_BGCOLOR_INDEX = 9;
        final int OUTPUT_BGCOLOR_INDEX = 10;
        final int SUGGESTIONS_BGCOLOR_INDEX = 11;
        final int TOOLBAR_BGCOLOR_INDEX = 12;

        int strokeWidth, cornerRadius;
        String[] rectParams = getListOfStringValues(XMLPrefsManager.get(Ui.bgrect_params), 2, "0");
        strokeWidth = Integer.parseInt(rectParams[0]);
        cornerRadius = Integer.parseInt(rectParams[1]);

        final int OUTPUT_MARGINS_INDEX = 1;
        final int INPUTAREA_MARGINS_INDEX = 2;
        final int INPUTFIELD_MARGINS_INDEX = 3;
        final int TOOLBAR_MARGINS_INDEX = 4;
        final int SUGGESTIONS_MARGINS_INDEX = 5;

        final int[][] margins = new int[6][4];
        margins[0] = getListOfIntValues(XMLPrefsManager.get(Ui.status_lines_margins), 4, 0);
        margins[1] = getListOfIntValues(XMLPrefsManager.get(Ui.output_field_margins), 4, 0);
        margins[2] = getListOfIntValues(XMLPrefsManager.get(Ui.input_area_margins), 4, 0);
        margins[3] = getListOfIntValues(XMLPrefsManager.get(Ui.input_field_margins), 4, 0);
        margins[4] = getListOfIntValues(XMLPrefsManager.get(Ui.toolbar_margins), 4, 0);
        margins[5] = getListOfIntValues(XMLPrefsManager.get(Ui.suggestions_area_margin), 4, 0);

        AllowEqualsSequence sequence = new AllowEqualsSequence(indexes, Label.values());

        LinearLayout lViewsParent = (LinearLayout) labelViews[0].getParent();

        int effectiveCount = 0;
        for(int count = 0; count < labelViews.length; count++) {
            labelViews[count].setOnTouchListener(this);

            Object[] os = sequence.get(count);

//            views on the same line
            for(int j = 0; j < os.length; j++) {
//                i is the object gave to the constructor
                int i = ((Label) os[j]).ordinal();
//                v is the adjusted index (2.0, 2.1, 2.2, ...)
                float v = (float) count + ((float) j * 0.1f);

                labelIndexes[i] = v;
            }

            if(count >= sequence.getMinKey() && count <= sequence.getMaxKey() && os.length > 0) {
                labelViews[count].setTypeface(Tuils.getTypeface(context));

                int ec = effectiveCount++;

//                -1 = left     0 = center     1 = right
                int p = statusLineAlignments[ec];
                if(p >= 0) labelViews[count].setGravity(p == 0 ? Gravity.CENTER_HORIZONTAL : Gravity.RIGHT);

                if(count != labelIndexes[Label.notes.ordinal()]) {
                    labelViews[count].setVerticalScrollBarEnabled(false);
                }

                applyBgRect(labelViews[count], bgRectColors[count], bgColors[count], margins[0], strokeWidth, cornerRadius);
                applyShadow(labelViews[count], outlineColors[count], shadowXOffset, shadowYOffset, shadowRadius);
            } else {
                lViewsParent.removeView(labelViews[count]);
                labelViews[count] = null;
            }
        }

        if (show[Label.ram.ordinal()]) {
            ramRunnable = new RamRunnable();

            memory = new ActivityManager.MemoryInfo();
            activityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
            handler.post(ramRunnable);
        }

        if(show[Label.storage.ordinal()]) {
            storageRunnable = new StorageRunnable();
            handler.post(storageRunnable);
        }

        if (show[Label.device.ordinal()]) {
            Pattern USERNAME = Pattern.compile("%u", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
            Pattern DV = Pattern.compile("%d", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

            String deviceFormat = XMLPrefsManager.get(Behavior.device_format);

            String username = XMLPrefsManager.get(Ui.username);
            String deviceName = XMLPrefsManager.get(Ui.deviceName);
            if (deviceName == null || deviceName.length() == 0) {
                deviceName = Build.DEVICE;
            }

            deviceFormat = USERNAME.matcher(deviceFormat).replaceAll(Matcher.quoteReplacement(username != null ? username : "null"));
            deviceFormat = DV.matcher(deviceFormat).replaceAll(Matcher.quoteReplacement(deviceName));
            deviceFormat = Tuils.patternNewline.matcher(deviceFormat).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));

            updateText(Label.device, Tuils.span(mContext, deviceFormat, XMLPrefsManager.getColor(Theme.device_color), labelSizes[Label.device.ordinal()]));
        }

        if(show[Label.time.ordinal()]) {
            timeRunnable = new TimeRunnable();
            handler.post(timeRunnable);
        }

        if(show[Label.battery.ordinal()]) {
            batteryUpdate = new BatteryUpdate();

            mediumPercentage = XMLPrefsManager.getInt(Behavior.battery_medium);
            lowPercentage = XMLPrefsManager.getInt(Behavior.battery_low);

            Tuils.registerBatteryReceiver(context, batteryUpdate);
        } else {
            batteryUpdate = null;
        }

        if(show[Label.network.ordinal()]) {
            networkRunnable = new NetworkRunnable();
            handler.post(networkRunnable);
        }

        final TextView notesView = getLabelView(Label.notes);
        notesManager = new NotesManager(context, notesView);
        if(show[Label.notes.ordinal()]) {
            notesRunnable = new NotesRunnable();
            handler.post(notesRunnable);

            notesView.setMovementMethod(new LinkMovementMethod());

            notesMaxLines = XMLPrefsManager.getInt(Ui.notes_max_lines);
            if(notesMaxLines > 0) {
                notesView.setMaxLines(notesMaxLines);
                notesView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
//                notesView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
//                notesView.setVerticalScrollBarEnabled(true);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && XMLPrefsManager.getBoolean(Ui.show_scroll_notes_message)) {
                    notesView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                        int linesBefore = Integer.MIN_VALUE;

                        @Override
                        public void onGlobalLayout() {
                            if(notesView.getLineCount() > notesMaxLines && linesBefore <= notesMaxLines) {
                                Tuils.sendOutput(Color.RED, context, R.string.note_max_reached);
                            }

                            linesBefore = notesView.getLineCount();
                        }
                    });
                }
            }
        }

        if(show[Label.weather.ordinal()]) {
            weatherRunnable = new WeatherRunnable();

            weatherColor = XMLPrefsManager.getColor(Theme.weather_color);

            String where = XMLPrefsManager.get(Behavior.weather_location);
            if(where.contains(",") || Tuils.isNumber(where)) handler.post(weatherRunnable);

            showWeatherUpdate = XMLPrefsManager.getBoolean(Behavior.show_weather_updates);
        }

        if(show[Label.unlock.ordinal()]) {
            unlockTimes = preferences.getInt(UNLOCK_KEY, 0);

            unlockColor = XMLPrefsManager.getColor(Theme.unlock_counter_color);
            unlockFormat = XMLPrefsManager.get(Behavior.unlock_counter_format);
            notAvailableText = XMLPrefsManager.get(Behavior.not_available_text);
            unlockTimeDivider = XMLPrefsManager.get(Behavior.unlock_time_divider);
            unlockTimeDivider = Tuils.patternNewline.matcher(unlockTimeDivider).replaceAll(Tuils.NEWLINE);

            String start = XMLPrefsManager.get(Behavior.unlock_counter_cycle_start);
            Pattern p = Pattern.compile("(\\d{1,2}).(\\d{1,2})");
            Matcher m = p.matcher(start);
            if(!m.find()) {
                m = p.matcher(Behavior.unlock_counter_cycle_start.defaultValue());
                m.find();
            }

            unlockHour = Integer.parseInt(m.group(1));
            unlockMinute = Integer.parseInt(m.group(2));

            unlockTimeOrder = XMLPrefsManager.getInt(Behavior.unlock_time_order);

            nextUnlockCycleRestart = preferences.getLong(NEXT_UNLOCK_CYCLE_RESTART, 0);
//            Tuils.log("set", nextUnlockCycleRestart);

            m = timePattern.matcher(unlockFormat);
            if(m.find()) {
                String s = m.group(3);
                if(s == null || s.length() == 0) s = "1";

                lastUnlocks = new long[Integer.parseInt(s)];

                for(int c = 0; c < lastUnlocks.length; c++) {
                    lastUnlocks[c] = -1;
                }

                registerLockReceiver();
                handler.post(unlockTimeRunnable);
            } else {
                lastUnlocks = null;
            }
        }

        final boolean inputBottom = XMLPrefsManager.getBoolean(Ui.input_bottom);
        int layoutId = inputBottom ? R.layout.input_down_layout : R.layout.input_up_layout;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inputOutputView = inflater.inflate(layoutId, null);
        rootView.addView(inputOutputView);

        terminalView = (TextView) inputOutputView.findViewById(R.id.terminal_view);
        terminalView.setOnTouchListener(this);
        ((View) terminalView.getParent().getParent()).setOnTouchListener(this);

        applyBgRect(terminalView, bgRectColors[OUTPUT_BGCOLOR_INDEX], bgColors[OUTPUT_BGCOLOR_INDEX], margins[OUTPUT_MARGINS_INDEX], strokeWidth, cornerRadius);
        applyShadow(terminalView, outlineColors[OUTPUT_BGCOLOR_INDEX], shadowXOffset, shadowYOffset, shadowRadius);

        final EditText inputView = (EditText) inputOutputView.findViewById(R.id.input_view);
        TextView prefixView = (TextView) inputOutputView.findViewById(R.id.prefix_view);

        applyBgRect(inputOutputView.findViewById(R.id.input_group), bgRectColors[INPUT_BGCOLOR_INDEX], bgColors[INPUT_BGCOLOR_INDEX], margins[INPUTAREA_MARGINS_INDEX], strokeWidth, cornerRadius);
        applyShadow(inputView, outlineColors[INPUT_BGCOLOR_INDEX], shadowXOffset, shadowYOffset, shadowRadius);
        applyShadow(prefixView, outlineColors[INPUT_BGCOLOR_INDEX], shadowXOffset, shadowYOffset, shadowRadius);

        applyMargins(inputView, margins[INPUTFIELD_MARGINS_INDEX]);
        applyMargins(prefixView, margins[INPUTFIELD_MARGINS_INDEX]);

        ImageView submitView = (ImageView) inputOutputView.findViewById(R.id.submit_tv);
        boolean showSubmit = XMLPrefsManager.getBoolean(Ui.show_enter_button);
        if (!showSubmit) {
            submitView.setVisibility(View.GONE);
            submitView = null;
        }

//        final ImageButton finalSubmitView = submitView;
//        inputView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            @Override
//            public boolean onPreDraw() {
//                Tuils.scaleImage(finalSubmitView, 20, 20);
//
//                inputView.getViewTreeObserver().removeOnPreDrawListener(this);
//
//                return false;
//            }
//        });

//        toolbar
        boolean showToolbar = XMLPrefsManager.getBoolean(Toolbar.show_toolbar);
        ImageButton backView = null;
        ImageButton nextView = null;
        ImageButton deleteView = null;
        ImageButton pasteView = null;

        if(!showToolbar) {
            inputOutputView.findViewById(R.id.tools_view).setVisibility(View.GONE);
            toolbarView = null;
        } else {
            backView = (ImageButton) inputOutputView.findViewById(R.id.back_view);
            nextView = (ImageButton) inputOutputView.findViewById(R.id.next_view);
            deleteView = (ImageButton) inputOutputView.findViewById(R.id.delete_view);
            pasteView = (ImageButton) inputOutputView.findViewById(R.id.paste_view);

            toolbarView = inputOutputView.findViewById(R.id.tools_view);
            hideToolbarNoInput = XMLPrefsManager.getBoolean(Toolbar.hide_toolbar_no_input);

            applyBgRect(toolbarView, bgRectColors[TOOLBAR_BGCOLOR_INDEX], bgColors[TOOLBAR_BGCOLOR_INDEX], margins[TOOLBAR_MARGINS_INDEX], strokeWidth, cornerRadius);
        }

        mTerminalAdapter = new TerminalManager(terminalView, inputView, prefixView, submitView, backView, nextView, deleteView, pasteView, context, mainPack, executer);

        if (XMLPrefsManager.getBoolean(Suggestions.show_suggestions)) {
            HorizontalScrollView sv = (HorizontalScrollView) rootView.findViewById(R.id.suggestions_container);
            sv.setFocusable(false);
            sv.setOnFocusChangeListener((v, hasFocus) -> {
                if(hasFocus) {
                    v.clearFocus();
                }
            });
            applyBgRect(sv, bgRectColors[SUGGESTIONS_BGCOLOR_INDEX], bgColors[SUGGESTIONS_BGCOLOR_INDEX], margins[SUGGESTIONS_MARGINS_INDEX], strokeWidth, cornerRadius);

            LinearLayout suggestionsView = (LinearLayout) rootView.findViewById(R.id.suggestions_group);

            suggestionsManager = new SuggestionsManager(suggestionsView, mainPack, mTerminalAdapter);

            inputView.addTextChangedListener(new SuggestionTextWatcher(suggestionsManager, (currentText, before) -> {
                if(!hideToolbarNoInput) return;

                if(currentText.length() == 0) toolbarView.setVisibility(View.GONE);
                else if(before == 0) toolbarView.setVisibility(View.VISIBLE);
            }));
        } else {
            rootView.findViewById(R.id.suggestions_group).setVisibility(View.GONE);
        }

        int drawTimes = XMLPrefsManager.getInt(Ui.text_redraw_times);
        if(drawTimes <= 0) drawTimes = 1;
        OutlineTextView.redrawTimes = drawTimes;
    }

    public static int[] getListOfIntValues(String values, int length, int defaultValue) {
        int[] is = new int[length];
        values = removeSquareBrackets(values);
        String[] split = values.split(",");
        int c = 0;
        for(; c < split.length; c++) {
            try {
                is[c] = Integer.parseInt(split[c]);
            } catch (Exception e) {
                is[c] = defaultValue;
            }
        }
        while(c < split.length) is[c] = defaultValue;

        return is;
    }

    public static String[] getListOfStringValues(String values, int length, String defaultValue) {
        String[] is = new String[length];
        String[] split = values.split(",");

        int len = Math.min(split.length, is.length);
        System.arraycopy(split, 0, is, 0, len);

        while(len < is.length) is[len++] = defaultValue;

        return is;
    }

    private static Pattern sbPattern = Pattern.compile("[\\[\\]\\s]");
    private static String removeSquareBrackets(String s) {
        return sbPattern.matcher(s).replaceAll(Tuils.EMPTYSTRING);
    }

//    0 = ext hor
//    1 = ext ver
//    2 = int hor
//    3 = int ver
    private static void applyBgRect(View v, String strokeColor, String bgColor, int[] spaces, int strokeWidth, int cornerRadius) {
        try {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.RECTANGLE);
            d.setCornerRadius(cornerRadius);

            if(!(strokeColor.startsWith("#00") && strokeColor.length() == 9)) {
                d.setStroke(strokeWidth, Color.parseColor(strokeColor));
            }

            applyMargins(v, spaces);

            d.setColor(Color.parseColor(bgColor));
            v.setBackgroundDrawable(d);
        } catch (Exception e) {
            Tuils.toFile(e);
            Tuils.log(e);
        }
    }

    private static void applyMargins(View v, int[] margins) {
        v.setPadding(margins[2], margins[3], margins[2], margins[3]);

        ViewGroup.LayoutParams params = v.getLayoutParams();
        if(params instanceof RelativeLayout.LayoutParams) {
            ((RelativeLayout.LayoutParams) params).setMargins(margins[0], margins[1], margins[0], margins[1]);
        } else if(params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).setMargins(margins[0], margins[1], margins[0], margins[1]);
        }
    }

    private static void applyShadow(TextView v, String color, int x, int y, float radius) {
        if(!(color.startsWith("#00") && color.length() == 9)) {
            v.setShadowLayer(radius, x, y, Color.parseColor(color));
            v.setTag(OutlineTextView.SHADOW_TAG);

//            if(radius > v.getPaddingTop()) v.setPadding(v.getPaddingLeft(), (int) Math.floor(radius), v.getPaddingRight(), (int) Math.floor(radius));
//            if(radius > v.getPaddingLeft()) v.setPadding((int) Math.floor(radius), v.getPaddingTop(), (int) Math.floor(radius), v.getPaddingBottom());
        }
    }

    public void dispose() {
        if(handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }

        if(suggestionsManager != null) suggestionsManager.dispose();
        if(notesManager != null) notesManager.dispose(mContext);
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).unregisterReceiver(receiver);
        Tuils.unregisterBatteryReceiver(mContext);

        Tuils.cancelFont();

        unregisterLockReceiver();
    }

    public void openKeyboard() {
        mTerminalAdapter.requestInputFocus();
        imm.showSoftInput(mTerminalAdapter.getInputView(), InputMethodManager.SHOW_IMPLICIT);
//        mTerminalAdapter.scrollToEnd();
    }

    public void closeKeyboard() {
        imm.hideSoftInputFromWindow(mTerminalAdapter.getInputWindowToken(), 0);
    }

    public void onStart(boolean openKeyboardOnStart) {
        if(openKeyboardOnStart) openKeyboard();
    }

    public void setInput(String s) {
        if (s == null)
            return;

        mTerminalAdapter.setInput(s);
        mTerminalAdapter.focusInputEnd();
    }

    public void setHint(String hint) {
        mTerminalAdapter.setHint(hint);
    }

    public void resetHint() {
        mTerminalAdapter.setDefaultHint();
    }

    public void setOutput(CharSequence s, int category) {
        mTerminalAdapter.setOutput(s, category);
    }

    public void setOutput(int color, CharSequence output) {
        mTerminalAdapter.setOutput(color, output);
    }

    public void disableSuggestions() {
        if(suggestionsManager != null) suggestionsManager.disable();
    }

    public void enableSuggestions() {
        if(suggestionsManager != null) suggestionsManager.enable();
    }

    public void onBackPressed() {
        mTerminalAdapter.onBackPressed();
    }

    public void focusTerminal() {
        mTerminalAdapter.requestInputFocus();
    }

    public void pause() {
        closeKeyboard();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return v.onTouchEvent(event);
    }

    public OnRedirectionListener buildRedirectionListener() {
        return new OnRedirectionListener() {
            @Override
            public void onRedirectionRequest(final RedirectCommand cmd) {
                ((Activity) mContext).runOnUiThread(() -> {
                    mTerminalAdapter.setHint(mContext.getString(cmd.getHint()));
                    disableSuggestions();
                });
            }

            @Override
            public void onRedirectionEnd(RedirectCommand cmd) {
                ((Activity) mContext).runOnUiThread(() -> {
                    mTerminalAdapter.setDefaultHint();
                    enableSuggestions();
                });
            }
        };
    }

    private BroadcastReceiver lockReceiver = null;
    private void registerLockReceiver() {
        if(lockReceiver != null) return;

        final IntentFilter theFilter = new IntentFilter();

        theFilter.addAction(Intent.ACTION_SCREEN_ON);
        theFilter.addAction(Intent.ACTION_SCREEN_OFF);
        theFilter.addAction(Intent.ACTION_USER_PRESENT);

        lockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String strAction = intent.getAction();

                KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                if(strAction.equals(Intent.ACTION_USER_PRESENT) || strAction.equals(Intent.ACTION_SCREEN_OFF) || strAction.equals(Intent.ACTION_SCREEN_ON)  )
                    if(myKM.inKeyguardRestrictedInputMode()) onLock();
                    else onUnlock();
            }
        };

        mContext.getApplicationContext().registerReceiver(lockReceiver, theFilter);
    }

    private void unregisterLockReceiver() {
        if(lockReceiver != null) mContext.getApplicationContext().unregisterReceiver(lockReceiver);
    }

    private void onLock() {
        if(clearOnLock) {
            mTerminalAdapter.clear();
        }
    }

    private final long A_DAY = (1000 * 60 * 60 * 24);

    private int unlockColor, unlockTimeOrder;

    private int unlockTimes, unlockHour, unlockMinute, cycleDuration = (int) A_DAY;
    private long lastUnlockTime = -1, nextUnlockCycleRestart;
    private String unlockFormat, notAvailableText, unlockTimeDivider;

    private final int UP_DOWN = 1;

    public static String UNLOCK_KEY = "unlockTimes", NEXT_UNLOCK_CYCLE_RESTART = "nextUnlockRestart";

//    last unlocks are stored here in this way
//    0 - the first
//    1 - the second
//    2 - ...
    private long[] lastUnlocks;

    private void onUnlock() {
        if(System.currentTimeMillis() - lastUnlockTime < 1000 || lastUnlocks == null) return;
        lastUnlockTime = System.currentTimeMillis();

        unlockTimes++;

        System.arraycopy(lastUnlocks, 0, lastUnlocks, 1, lastUnlocks.length - 1);
        lastUnlocks[0] = lastUnlockTime;

        preferences.edit()
                .putInt(UNLOCK_KEY, unlockTimes)
                .apply();

        invalidateUnlockText();
    }

    final int UNLOCK_RUNNABLE_DELAY = cycleDuration / 24;
//    this invalidates the text and checks the time values
    Runnable unlockTimeRunnable = new Runnable() {
        @Override
        public void run() {
//            Tuils.log("run");
            long delay = nextUnlockCycleRestart - System.currentTimeMillis();
//            Tuils.log("nucr", nextUnlockCycleRestart);
//            Tuils.log("now", System.currentTimeMillis());
//            Tuils.log("delay", delay);
            if(delay <= 0) {
                unlockTimes = 0;

                if(lastUnlocks != null) {
                    for(int c = 0; c < lastUnlocks.length; c++) {
                        lastUnlocks[c] = -1;
                    }
                }

                Calendar now = Calendar.getInstance();
//                Tuils.log("nw", now.toString());

                int hour = now.get(Calendar.HOUR_OF_DAY), minute = now.get(Calendar.MINUTE);
                if(unlockHour < hour || (unlockHour == hour && unlockMinute <= minute)) {
                    now.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR) + 1);
                }
                Calendar nextRestart = now;
                nextRestart.set(Calendar.HOUR_OF_DAY, unlockHour);
                nextRestart.set(Calendar.MINUTE, unlockMinute);
                nextRestart.set(Calendar.SECOND, 0);
//                Tuils.log("nr", nextRestart.toString());

                nextUnlockCycleRestart = nextRestart.getTimeInMillis();
//                Tuils.log("new setted", nextUnlockCycleRestart);

                preferences.edit()
                        .putLong(NEXT_UNLOCK_CYCLE_RESTART, nextUnlockCycleRestart)
                        .putInt(UNLOCK_KEY, 0)
                        .apply();

                delay = nextUnlockCycleRestart - System.currentTimeMillis();
                if(delay < 0) delay = 0;
            }

            invalidateUnlockText();

            delay = Math.min(delay, UNLOCK_RUNNABLE_DELAY);
//            Tuils.log("with delay", delay);
            handler.postDelayed(this, delay);
        }
    };

    Pattern unlockCount = Pattern.compile("%c", Pattern.CASE_INSENSITIVE);
    Pattern advancement = Pattern.compile("%a(\\d+)(.)");
//    Pattern timePattern = Pattern.compile("(%t\\d*)(?:\\((?:(\\d+)([^\\)]*))\\)|\\((?:([^\\)]*)(\\d+))\\))?");
    Pattern timePattern = Pattern.compile("(%t\\d*)(?:\\(([^\\)]*)\\))?(\\d+)?");
    Pattern indexPattern = Pattern.compile("%i", Pattern.CASE_INSENSITIVE);
    String whenPattern = "%w";

    private void invalidateUnlockText() {
        String cp = new String(unlockFormat);

        cp = unlockCount.matcher(cp).replaceAll(String.valueOf(unlockTimes));
        cp = Tuils.patternNewline.matcher(cp).replaceAll(Tuils.NEWLINE);

        Matcher m = advancement.matcher(cp);
        if(m.find()) {
            int denominator = Integer.parseInt(m.group(1));
            String divider = m.group(2);

            long lastCycleStart = nextUnlockCycleRestart - cycleDuration;

            int elapsed = (int) (System.currentTimeMillis() - lastCycleStart);
            int numerator = denominator * elapsed / cycleDuration;

            cp = m.replaceAll(numerator + divider + denominator);
        }

        CharSequence s = Tuils.span(mContext, cp, unlockColor, labelSizes[Label.unlock.ordinal()]);

        Matcher timeMatcher = timePattern.matcher(cp);
        if(timeMatcher.find()) {
            String timeGroup = timeMatcher.group(1);
            String text = timeMatcher.group(2);
            if(text == null) text = whenPattern;

            CharSequence cs = Tuils.EMPTYSTRING;

            int c, change;
            if(unlockTimeOrder == UP_DOWN) {
                c = 0;
                change = +1;
            } else {
                c = lastUnlocks.length - 1;
                change = -1;
            }

            for(int counter = 0; counter < lastUnlocks.length; counter++, c += change) {
                String t = text;
                t = indexPattern.matcher(t).replaceAll(String.valueOf(c + 1));

                cs = TextUtils.concat(cs, t);

                CharSequence time;
                if(lastUnlocks[c] > 0) time = TimeManager.instance.getCharSequence(timeGroup, lastUnlocks[c]);
                else time = notAvailableText;

                if(time == null) continue;

                cs = TextUtils.replace(cs, new String[] {whenPattern}, new CharSequence[] {time});

                if(counter != lastUnlocks.length - 1) cs = TextUtils.concat(cs, unlockTimeDivider);
            }

            s = TextUtils.replace(s, new String[] {timeMatcher.group(0)}, new CharSequence[] {cs});
        }

        updateText(Label.unlock, s);
    }
}

