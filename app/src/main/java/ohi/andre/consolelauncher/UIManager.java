package ohi.andre.consolelauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.RedirectCommand;
import ohi.andre.consolelauncher.managers.MessagesManager;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionRunnable;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionsManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Suggestions;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.managers.xml.options.Toolbar;
import ohi.andre.consolelauncher.managers.xml.options.Ui;
import ohi.andre.consolelauncher.tuils.AllowEqualsSequence;
import ohi.andre.consolelauncher.tuils.NetworkUtils;
import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.TimeManager;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.Hintable;
import ohi.andre.consolelauncher.tuils.interfaces.OnBatteryUpdate;
import ohi.andre.consolelauncher.tuils.interfaces.OnRedirectionListener;
import ohi.andre.consolelauncher.tuils.interfaces.Rooter;
import ohi.andre.consolelauncher.tuils.interfaces.SuggestionViewDecorer;
import ohi.andre.consolelauncher.tuils.stuff.PolicyReceiver;
import ohi.andre.consolelauncher.tuils.stuff.TrashInterfaces;

public class UIManager implements OnTouchListener {

    private enum Label {
        ram,
        device,
        time,
        battery,
        storage,
        network;

        static Label last() {
            return network;
        }
    }

    private final int RAM_DELAY = 3000;
    private final int TIME_DELAY = 1000;
    private final int STORAGE_DELAY = 60 * 1000;

    protected Context mContext;

    private DevicePolicyManager policy;
    private ComponentName component;
    private GestureDetector det;
    private MainPack info;

    private InputMethodManager imm;
    private CommandExecuter trigger;
    private TerminalManager mTerminalAdapter;

    int mediumPercentage, lowPercentage;
    String batteryFormat;
//    boolean batteryCharging;

    private String multipleCmdSeparator;

//    private boolean selectFirstSuggestionEnter = false;
    private OnNewInputListener inputListener = new OnNewInputListener() {
        @Override
        public void onNewInput(String input) {
            if(suggestionsView != null) {
//                if(suggestionsView.getChildCount() > 0 && selectFirstSuggestionEnter) {
//                    View v = suggestionsView.getChildAt(0);
//                    v.performClick();
//                    return;
//                }
                suggestionsView.removeAllViews();

            }
            trigger.exec(input);

        }
    };

    private TextView[] labelViews = new TextView[Label.last().ordinal() + 1];
    private int[] labelSizes = new int[Label.last().ordinal() + 1];
    private CharSequence[] labelTexts = new CharSequence[Label.last().ordinal() + 1];
    private float[] labelIndexes = new float[Label.last().ordinal() + 1];

    private OnBatteryUpdate batteryUpdate = new OnBatteryUpdate() {

//        %(charging:not charging)

        final Pattern optionalCharging = Pattern.compile("%\\(([^\\/]*)\\/([^)]*)\\)", Pattern.CASE_INSENSITIVE);
        final Pattern newline = Pattern.compile("%n", Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
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
            cp = newline.matcher(cp).replaceAll(Tuils.NEWLINE);

            int i = Label.battery.ordinal();

            labelTexts[i] = Tuils.span(mContext, cp, color, labelSizes[i]);
            UIManager.this.update(labelIndexes[i]);
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

    private Runnable storageRunnable = new Runnable() {

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

                storagePatterns.add(Pattern.compile("%n", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

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

            int i = Label.storage.ordinal();

            labelTexts[i] = Tuils.span(mContext, copy, color, labelSizes[i]);
            update(labelIndexes[i]);
            labelViews[(int) labelIndexes[i]].postDelayed(this, STORAGE_DELAY);
        }
    };

    private Runnable timeRunnable = new Runnable() {

        boolean active;
        int color;

        @Override
        public void run() {
            if(!active) {
                active = true;
                color = XMLPrefsManager.getColor(Theme.time_color);
            }

            int i = Label.time.ordinal();

            labelTexts[i] = TimeManager.replace(mContext, labelSizes[i], "%t0", color);
            update(labelIndexes[i]);
            labelViews[(int) labelIndexes[i]].postDelayed(this, TIME_DELAY);
        }
    };

    private ActivityManager.MemoryInfo memory;
    private ActivityManager activityManager;

    private Runnable ramRunnable = new Runnable() {

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

                ramPatterns.add(Pattern.compile("%n", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
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

            int i = Label.ram.ordinal();

            labelTexts[i] = Tuils.span(mContext, copy, color, labelSizes[i]);
            update(labelIndexes[i]);
            labelViews[(int) labelIndexes[i]].postDelayed(this, RAM_DELAY);
        }
    };

    private Runnable networkRunnable = new Runnable() {

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
        final Pattern nl = Pattern.compile("%n", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern ip4 = Pattern.compile("%ip4", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern ip6 = Pattern.compile("%ip6", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

        final Pattern optionalWifi = Pattern.compile("%\\(([^/]*)/([^)]*)\\)", Pattern.CASE_INSENSITIVE);
        final Pattern optionalData = Pattern.compile("%\\[([^/]*)/([^\\]]*)\\]", Pattern.CASE_INSENSITIVE);
        final Pattern optionalBluetooth = Pattern.compile("%\\{([^/]*)/([^}]*)\\}", Pattern.CASE_INSENSITIVE);

        String format;
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
            if(format == null) {
                format = XMLPrefsManager.get(Behavior.network_info_format);
                color = XMLPrefsManager.getColor(Theme.network_info_color);
                maxDepth = XMLPrefsManager.getInt(Behavior.max_optional_depth_network_info);

                updateTime = XMLPrefsManager.getInt(Behavior.network_info_update_ms);
                if(updateTime < 1000) updateTime = Integer.parseInt(Behavior.network_info_update_ms.defaultValue());

                connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                try {
                    cmClass = Class.forName(connectivityManager.getClass().getName());
                    method = cmClass.getDeclaredMethod("getMobileDataEnabled");
                    method.setAccessible(true);
                } catch (Exception e) {
                    cmClass = null;
                    method = null;
                }
            }

//            mobile data
            boolean mobileOn = false;
            try {
                mobileOn = method != null && connectivityManager != null && (Boolean) method.invoke(connectivityManager);
            } catch (Exception e) {}

//            wifi
            boolean wifiOn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
            String wifiName = null;
            if(wifiOn) {
                WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null) {
                    wifiName = connectionInfo.getSSID();
                }
            }

//            bluetooth
            boolean bluetoothOn = mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();

            String copy = format;

            if(maxDepth > 0) {
                copy = apply(1, copy, new boolean[] {wifiOn, mobileOn, bluetoothOn}, optionalWifi, optionalData, optionalBluetooth);
                copy = apply(1, copy, new boolean[] {mobileOn, wifiOn, bluetoothOn}, optionalData, optionalWifi, optionalBluetooth);
                copy = apply(1, copy, new boolean[] {bluetoothOn, wifiOn, mobileOn}, optionalBluetooth, optionalWifi, optionalData);
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
            copy = nl.matcher(copy).replaceAll(Tuils.NEWLINE);

            int i = Label.network.ordinal();

            labelTexts[i] = Tuils.span(mContext, copy, color, labelSizes[i]);
            update(labelIndexes[i]);
            labelViews[i].postDelayed(this, updateTime);
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
    };

    private void update(float line) {
        int base = (int) line;

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

        labelViews[base].setText(sequence);
    }

    private boolean showSuggestions;
    private LinearLayout suggestionsView;
    private SuggestionViewDecorer suggestionViewDecorer;
    private SuggestionRunnable suggestionRunnable;
    private LinearLayout.LayoutParams suggestionViewParams;
    private SuggestionsManager suggestionsManager;
//    private boolean navigatingWithSpace = false;

    private TextView terminalView;
    private Thread lastSuggestionThread;
    private Handler handler = new Handler();
    private Runnable removeAllSuggestions = new Runnable() {
        @Override
        public void run() {
            suggestionsView.removeAllViews();
        }
    };

    private String doubleTapCmd;
    private boolean lockOnDbTap;

    protected TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int st, int b, int c) {
            requestSuggestion(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SuggestionsManager.Suggestion suggestion = (SuggestionsManager.Suggestion) v.getTag(R.id.suggestion_id);
            boolean execOnClick = suggestion.exec;

            String text = suggestion.getText();
            String input = mTerminalAdapter.getInput();

            if(suggestion.type == SuggestionsManager.Suggestion.TYPE_PERMANENT) {
                mTerminalAdapter.setInput(input + text);
            } else {
                if(multipleCmdSeparator.length() > 0) {
                    String[] split = input.split(multipleCmdSeparator);
                    if(split.length == 0) return;
                    if(split.length == 1) mTerminalAdapter.setInput(text + (suggestion.type == SuggestionsManager.Suggestion.TYPE_FILE ? Tuils.EMPTYSTRING : Tuils.SPACE));
                    else {
                        split[split.length - 1] = Tuils.EMPTYSTRING;

                        String beforeInputs = Tuils.EMPTYSTRING;
                        for(int count = 0; count < split.length - 1; count++) {
                            beforeInputs = beforeInputs + split[count] + multipleCmdSeparator;
                        }

                        mTerminalAdapter.setInput(beforeInputs + text + (suggestion.type == SuggestionsManager.Suggestion.TYPE_FILE ? Tuils.EMPTYSTRING : Tuils.SPACE));
                    }
                } else {
                    mTerminalAdapter.setInput(text + (suggestion.type == SuggestionsManager.Suggestion.TYPE_FILE ? Tuils.EMPTYSTRING : Tuils.SPACE));
                }
            }

            if (execOnClick) {
                mTerminalAdapter.simulateEnter();
            } else {
                mTerminalAdapter.focusInputEnd();
            }
        }
    };

    public void requestSuggestion(final String input) {

        if (suggestionsView == null || suggestionsManager == null || !showSuggestions) {
            return;
        }

        if (suggestionViewParams == null) {
            suggestionViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            suggestionViewParams.setMargins(15, 0, 15, 0);
            suggestionViewParams.gravity = Gravity.CENTER_VERTICAL;
        }

        if(suggestionRunnable == null) {
            suggestionRunnable = new SuggestionRunnable(suggestionsView, suggestionViewParams, (HorizontalScrollView) suggestionsView.getParent());
        }

        if (lastSuggestionThread != null) {
            lastSuggestionThread.interrupt();
            suggestionRunnable.interrupt();
            if(handler != null) {
                handler.removeCallbacks(suggestionRunnable);
            }
        }

        lastSuggestionThread = new StoppableThread() {
            @Override
            public void run() {
                super.run();

                String before, lastWord;
                String lastInput;
                if(multipleCmdSeparator.length() > 0) {
                    String[] split = input.split(multipleCmdSeparator);
                    if(split.length == 0) lastInput = input;
                    else lastInput = split[split.length - 1];
                } else {
                    lastInput = input;
                }

                int lastSpace = lastInput.lastIndexOf(Tuils.SPACE);
                if(lastSpace == -1) {
                    before = Tuils.EMPTYSTRING;
                    lastWord = lastInput;
                } else {
                    before = lastInput.substring(0,lastSpace);
                    lastWord = lastInput.substring(lastSpace + 1,lastInput.length());
                }


                final SuggestionsManager.Suggestion[] suggestions;
                try {
                    suggestions = suggestionsManager.getSuggestions(info, before, lastWord);
                } catch (Exception e) {
                    Tuils.log(e);
                    Tuils.toFile(e);
                    return;
                }

                if(suggestions.length == 0) {
                    ((Activity) mContext).runOnUiThread(removeAllSuggestions);
                    return;
                }

                if (Thread.interrupted()) {
                    suggestionRunnable.interrupt();
                    return;
                }

                final TextView[] existingViews = new TextView[suggestionsView.getChildCount()];
                for (int count = 0; count < existingViews.length; count++) {
                    existingViews[count] = (TextView) suggestionsView.getChildAt(count);
                }

                if (Thread.interrupted()) {
                    suggestionRunnable.interrupt();
                    return;
                }

                int n = suggestions.length - existingViews.length;
                TextView[] toAdd = null;
                TextView[] toRecycle = null;
                if (n == 0) {
                    toRecycle = existingViews;
                    toAdd = null;
                } else if (n > 0) {
                    toRecycle = existingViews;
                    toAdd = new TextView[n];
                    for (int count = 0; count < toAdd.length; count++) {
                        toAdd[count] = suggestionViewDecorer.getSuggestionView(mContext);
                    }
                } else if (n < 0) {
                    toAdd = null;
                    toRecycle = new TextView[suggestions.length];
                    System.arraycopy(existingViews, 0, toRecycle, 0, toRecycle.length);
                }

                if (Thread.interrupted()) {
                    suggestionRunnable.interrupt();
                    return;
                }

                suggestionRunnable.setN(n);
                suggestionRunnable.setSuggestions(suggestions);
                suggestionRunnable.setToAdd(toAdd);
                suggestionRunnable.setToRecycle(toRecycle);
                suggestionRunnable.reset();
                ((Activity) mContext).runOnUiThread(suggestionRunnable);
            }
        };

        try {
            lastSuggestionThread.start();
        } catch (InternalError e) {}
    }

    protected UIManager(ExecutePack info, final Context context, final ViewGroup rootView, final CommandExecuter tri, MainPack mainPack, boolean canApplyTheme) {

        policy = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        component = new ComponentName(context, PolicyReceiver.class);

        multipleCmdSeparator = XMLPrefsManager.get(Behavior.multiple_cmd_separator);
//        selectFirstSuggestionEnter = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.enter_first_suggestion);

        mContext = context;
        this.info = (MainPack) info;

        trigger = tri;

        imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (!XMLPrefsManager.getBoolean(Ui.system_wallpaper) || !canApplyTheme) {
            rootView.setBackgroundColor(XMLPrefsManager.getColor(Theme.bg_color));
        } else {
            rootView.setBackgroundColor(XMLPrefsManager.getColor(Theme.overlay_color));
        }

//        scrolllllll
        if(XMLPrefsManager.getBoolean(Behavior.auto_scroll)) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
                    if (heightDiff > Tuils.dpToPx(context, 200)) { // if more than 200 dp, it's probably a keyboard...
                        if(mTerminalAdapter != null) mTerminalAdapter.scrollToEnd();
                    }
                }
            });
        }

        int rightMM, leftMM, topMM, bottomMM;
        rightMM = XMLPrefsManager.getInt(Ui.right_margin_mm);
        leftMM = XMLPrefsManager.getInt(Ui.left_margin_mm);
        topMM = XMLPrefsManager.getInt(Ui.top_margin_mm);
        bottomMM = XMLPrefsManager.getInt(Ui.bottom_margin_mm);

        labelSizes[Label.time.ordinal()] = XMLPrefsManager.getInt(Ui.time_size);
        labelSizes[Label.ram.ordinal()] = XMLPrefsManager.getInt(Ui.ram_size);
        labelSizes[Label.battery.ordinal()] = XMLPrefsManager.getInt(Ui.battery_size);
        labelSizes[Label.storage.ordinal()] = XMLPrefsManager.getInt(Ui.storage_size);
        labelSizes[Label.network.ordinal()] = XMLPrefsManager.getInt(Ui.network_size);

        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        rootView.setPadding(Tuils.mmToPx(metrics, leftMM), Tuils.mmToPx(metrics, topMM), Tuils.mmToPx(metrics, rightMM), Tuils.mmToPx(metrics, bottomMM));

        labelViews = new TextView[] {
                (TextView) rootView.findViewById(R.id.tv0),
                (TextView) rootView.findViewById(R.id.tv1),
                (TextView) rootView.findViewById(R.id.tv2),
                (TextView) rootView.findViewById(R.id.tv3),
                (TextView) rootView.findViewById(R.id.tv4),
                (TextView) rootView.findViewById(R.id.tv5)
        };

        boolean showRam = XMLPrefsManager.getBoolean(Ui.show_ram);
        boolean showStorage = XMLPrefsManager.getBoolean(Ui.show_storage_info);
        boolean showDevice = XMLPrefsManager.getBoolean(Ui.show_device_name);
        boolean showTime = XMLPrefsManager.getBoolean(Ui.show_time);
        boolean showBattery = XMLPrefsManager.getBoolean(Ui.show_battery);
        boolean showNetwork = XMLPrefsManager.getBoolean(Ui.show_network_info);

        float rIndex = showRam ? XMLPrefsManager.getFloat(Ui.ram_index) : Integer.MAX_VALUE;
        float dIndex = showDevice ? XMLPrefsManager.getFloat(Ui.device_index) : Integer.MAX_VALUE;
        float bIndex = showBattery ? XMLPrefsManager.getFloat(Ui.battery_index) : Integer.MAX_VALUE;
        float tIndex = showTime ? XMLPrefsManager.getFloat(Ui.time_index) : Integer.MAX_VALUE;
        float sIndex = showStorage ? XMLPrefsManager.getFloat(Ui.storage_index) : Integer.MAX_VALUE;
        float nIndex = showNetwork ? XMLPrefsManager.getFloat(Ui.network_index) : Integer.MAX_VALUE;

        int[] pos = {
                XMLPrefsManager.getInt(Ui.status_line0_position),
                XMLPrefsManager.getInt(Ui.status_line1_position),
                XMLPrefsManager.getInt(Ui.status_line2_position),
                XMLPrefsManager.getInt(Ui.status_line3_position),
                XMLPrefsManager.getInt(Ui.status_line4_position),
                XMLPrefsManager.getInt(Ui.status_line5_position)
        };

        AllowEqualsSequence sequence = new AllowEqualsSequence(new float[] {rIndex, dIndex, bIndex, tIndex, sIndex, nIndex},
                new Object[] {Label.ram, Label.device, Label.battery, Label.time, Label.storage, Label.network});

        for(int count = 0; count < labelViews.length; count++) {
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

                int p = pos[count];
                if(p >= 0) labelViews[count].setGravity(p == 0 ? Gravity.CENTER_HORIZONTAL : Gravity.RIGHT);
            } else {
                labelViews[count].setVisibility(View.GONE);
            }
        }

        if (showRam) {
            memory = new ActivityManager.MemoryInfo();
            activityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
            labelViews[Label.ram.ordinal()].post(ramRunnable);
        }

        if(showStorage) {
            labelViews[Label.storage.ordinal()].post(storageRunnable);
        }

        if (showDevice) {
            Pattern USERNAME = Pattern.compile("%u", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
            Pattern DV = Pattern.compile("%d", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
            Pattern NEWLINE = Pattern.compile("%n", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

            String deviceFormat = XMLPrefsManager.get(Behavior.device_format);

            String username = XMLPrefsManager.get(Ui.username);
            String deviceName = XMLPrefsManager.get(Ui.deviceName);
            if (deviceName == null || deviceName.length() == 0) {
                deviceName = Build.DEVICE;
            }

            deviceFormat = USERNAME.matcher(deviceFormat).replaceAll(Matcher.quoteReplacement(username != null ? username : "null"));
            deviceFormat = DV.matcher(deviceFormat).replaceAll(Matcher.quoteReplacement(deviceName));
            deviceFormat = NEWLINE.matcher(deviceFormat).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));

            labelTexts[Label.device.ordinal()] = Tuils.span(mContext, deviceFormat, XMLPrefsManager.getColor(Theme.device_color), XMLPrefsManager.getInt(Ui.device_size));
            update(labelIndexes[Label.device.ordinal()]);
        }

        if(showTime) {
            labelViews[Label.time.ordinal()].post(timeRunnable);
        }

        if(showBattery) {
            mediumPercentage = XMLPrefsManager.getInt(Behavior.battery_medium);
            lowPercentage = XMLPrefsManager.getInt(Behavior.battery_low);

            Tuils.registerBatteryReceiver(context, batteryUpdate);
        } else {
            batteryUpdate = null;
        }
//        batt, ram, ...

        if(showNetwork) {
            labelViews[Label.network.ordinal()].post(networkRunnable);
        }

        final boolean inputBottom = XMLPrefsManager.getBoolean(Ui.input_bottom);
        int layoutId = inputBottom ? R.layout.input_down_layout : R.layout.input_up_layout;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inputOutputView = inflater.inflate(layoutId, null);
        rootView.addView(inputOutputView);

        terminalView = (TextView) inputOutputView.findViewById(R.id.terminal_view);
        terminalView.setOnTouchListener(this);
        ((View) terminalView.getParent().getParent()).setOnTouchListener(this);

        final EditText inputView = (EditText) inputOutputView.findViewById(R.id.input_view);
        inputView.setOnTouchListener(this);

        TextView prefixView = (TextView) inputOutputView.findViewById(R.id.prefix_view);

        ImageButton submitView = (ImageButton) inputOutputView.findViewById(R.id.submit_tv);
        boolean showSubmit = XMLPrefsManager.getBoolean(Ui.show_enter_button);
        if (!showSubmit) {
            submitView.setVisibility(View.GONE);
            submitView = null;
        }

//        toolbar
        boolean showToolbar = XMLPrefsManager.getBoolean(Toolbar.show_toolbar);
        ImageButton backView = null;
        ImageButton nextView = null;
        ImageButton deleteView = null;
        ImageButton pasteView = null;

        if(!showToolbar) {
            inputOutputView.findViewById(R.id.tools_view).setVisibility(View.GONE);
        } else {
            backView = (ImageButton) inputOutputView.findViewById(R.id.back_view);
            nextView = (ImageButton) inputOutputView.findViewById(R.id.next_view);
            deleteView = (ImageButton) inputOutputView.findViewById(R.id.delete_view);
            pasteView = (ImageButton) inputOutputView.findViewById(R.id.paste_view);
        }

        if (XMLPrefsManager.getBoolean(Suggestions.show_suggestions)) {
            showSuggestions = true;

            HorizontalScrollView sv = (HorizontalScrollView) rootView.findViewById(R.id.suggestions_container);
            sv.setFocusable(false);
            sv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus) {
                        v.clearFocus();
                    }
                }
            });

            suggestionsView = (LinearLayout) rootView.findViewById(R.id.suggestions_group);

            inputView.addTextChangedListener(textWatcher);

            suggestionsManager = new SuggestionsManager();

            this.suggestionViewDecorer = new SuggestionViewDecorer() {

                final int PADDING = 15;

                @Override
                public TextView getSuggestionView(Context context) {
                    TextView textView = new TextView(mContext);
                    textView.setOnClickListener(clickListener);

                    textView.setFocusable(false);
                    textView.setLongClickable(false);
                    textView.setClickable(true);

                    textView.setTypeface(Tuils.getTypeface(context));
                    textView.setTextSize(XMLPrefsManager.getInt(Suggestions.suggestions_size));

                    textView.setPadding(PADDING, PADDING, PADDING, PADDING);

                    textView.setLines(1);
                    textView.setMaxLines(1);

                    return textView;
                }
            };
        } else {
            showSuggestions = false;
            rootView.findViewById(R.id.suggestions_group).setVisibility(View.GONE);
            this.textWatcher = null;
            this.clickListener = null;
        }

        lockOnDbTap = XMLPrefsManager.getBoolean(Behavior.double_tap_lock);
        doubleTapCmd = XMLPrefsManager.get(Behavior.double_tap_cmd);
        if(!lockOnDbTap && doubleTapCmd == null) {
            policy = null;
            component = null;
            det = null;
        } else initDetector();

        mTerminalAdapter = new TerminalManager(terminalView, inputView, prefixView, submitView, backView, nextView, deleteView, pasteView, context, mainPack);
        mTerminalAdapter.setInputListener(inputListener);

        if(XMLPrefsManager.getBoolean(Behavior.show_hints)) {
            MessagesManager messagesManager = new MessagesManager(context,
                    new MessagesManager.Message(context.getString(R.string.hint_alias)),
                    new MessagesManager.Message(context.getString(R.string.hint_appgroups)),
                    new MessagesManager.Message(context.getString(R.string.hint_clear)),
                    new MessagesManager.Message(context.getString(R.string.hint_config)),
                    new MessagesManager.Message(context.getString(R.string.hint_disable)),
                    new MessagesManager.Message(context.getString(R.string.hint_donate)),
                    new MessagesManager.Message(context.getString(R.string.hint_googlep)),
                    new MessagesManager.Message(context.getString(R.string.hint_help)),
                    new MessagesManager.Message(context.getString(R.string.hint_music)),
                    new MessagesManager.Message(context.getString(R.string.hint_notifications)),
                    new MessagesManager.Message(context.getString(R.string.hint_telegram)),
                    new MessagesManager.Message(context.getString(R.string.hint_theme)),
                    new MessagesManager.Message(context.getString(R.string.hint_theme2)),
                    new MessagesManager.Message(context.getString(R.string.hint_tutorial)),
                    new MessagesManager.Message(context.getString(R.string.hint_twitter)),
                    new MessagesManager.Message(context.getString(R.string.hint_wallpaper)),
                    new MessagesManager.Message(context.getString(R.string.hint_musicdisable)));

            mTerminalAdapter.setMessagesManager(messagesManager);
        }
    }

    public void dispose() {
        handler.removeCallbacksAndMessages(null);
    }

    public void openKeyboard() {
        mTerminalAdapter.requestInputFocus();
        imm.showSoftInput(mTerminalAdapter.getInputView(), InputMethodManager.SHOW_IMPLICIT);
        mTerminalAdapter.scrollToEnd();
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
        if(suggestionsView != null) {
            showSuggestions = false;
            suggestionsView.setVisibility(View.GONE);
        }
    }

    public void enableSuggestions() {
        if(suggestionsView != null) {
            showSuggestions = true;
            suggestionsView.setVisibility(View.VISIBLE);
        }
    }

    public void onBackPressed() {
        mTerminalAdapter.onBackPressed();
    }

    public void focusTerminal() {
        mTerminalAdapter.requestInputFocus();
    }

    public void scrollToEnd() {
        mTerminalAdapter.scrollToEnd();
    }

    public Hintable getHintable() {
        return new Hintable() {
            @Override
            public void updateHint() {
                mTerminalAdapter.setDefaultHint();
            }
        };
    }

    public Rooter getRooter() {
        return mTerminalAdapter.getRooter();
    }

    //	 init detector for double tap
    private void initDetector() {
        det = new GestureDetector(mContext, TrashInterfaces.trashGestureListener);

        det.setOnDoubleTapListener(new OnDoubleTapListener() {

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

    protected boolean verifyDoubleTap(MotionEvent event) {
        boolean b = det.onTouchEvent(event);
        return det != null && b;
    }

    //	 on pause
    public void pause() {
        closeKeyboard();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (verifyDoubleTap(event)) {
            return true;
        }

        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return v.onTouchEvent(event);

        if (v.getId() == R.id.input_view) {
            openKeyboard();
            return true;
        } else
            return v.onTouchEvent(event);
    }

    public OnRedirectionListener buildRedirectionListener() {
        return new OnRedirectionListener() {
            @Override
            public void onRedirectionRequest(final RedirectCommand cmd) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTerminalAdapter.setHint(mContext.getString(cmd.getHint()));
                        disableSuggestions();
                    }
                });
            }

            @Override
            public void onRedirectionEnd(RedirectCommand cmd) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTerminalAdapter.setDefaultHint();
                        enableSuggestions();
                    }
                });
            }
        };
    }

    public interface SuggestionNavigator {
        boolean isNavigating();
        void onEnter();
    }

    public interface OnNewInputListener {
        void onNewInput(String input);
    }

//    public class PowerConnectionReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
//            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
//
//            if(isCharging) {
//                battery.postDelayed(batteryChargingRunnable, BATTERY_CHARGING_DELAY);
//            } else {
//                battery.removeCallbacks(batteryChargingRunnable);
//            }
//        }
//    }

}

