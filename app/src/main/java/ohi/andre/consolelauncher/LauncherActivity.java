package ohi.andre.consolelauncher;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.tuixt.TuixtActivity;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.RegexManager;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.TimeManager;
import ohi.andre.consolelauncher.managers.TuiLocationManager;
import ohi.andre.consolelauncher.managers.notifications.KeeperService;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.managers.notifications.NotificationMonitorService;
import ohi.andre.consolelauncher.managers.notifications.NotificationService;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionsManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Notifications;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.managers.xml.options.Ui;
import ohi.andre.consolelauncher.tuils.Assist;
import ohi.andre.consolelauncher.tuils.CustomExceptionHandler;
import ohi.andre.consolelauncher.tuils.LongClickableSpan;
import ohi.andre.consolelauncher.tuils.PrivateIOReceiver;
import ohi.andre.consolelauncher.tuils.PublicIOReceiver;
import ohi.andre.consolelauncher.tuils.SimpleMutableEntry;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Inputable;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;

public class LauncherActivity extends AppCompatActivity implements Reloadable {

    public static final int COMMAND_REQUEST_PERMISSION = 10;
    public static final int STARTING_PERMISSION = 11;
    public static final int COMMAND_SUGGESTION_REQUEST_PERMISSION = 12;
    public static final int LOCATION_REQUEST_PERMISSION = 13;

    public static final int TUIXT_REQUEST = 10;

    private UIManager ui;
    private MainManager main;

    private PrivateIOReceiver privateIOReceiver;
    private PublicIOReceiver publicIOReceiver;

    private boolean openKeyboardOnStart, canApplyTheme, backButtonEnabled;

    private Set<ReloadMessageCategory> categories;
    private Runnable stopActivity = () -> {
            dispose();
            finish();

            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);

            CharSequence reloadMessage = Tuils.EMPTYSTRING;
            for (ReloadMessageCategory c : categories) {
                reloadMessage = TextUtils.concat(reloadMessage, Tuils.NEWLINE, c.text());
            }
            startMain.putExtra(Reloadable.MESSAGE, reloadMessage);

            startActivity(startMain);
    };

    private Inputable in = new Inputable() {

        @Override
        public void in(String s) {
            if(ui != null) ui.setInput(s);
        }

        @Override
        public void changeHint(final String s) {
            runOnUiThread(() -> ui.setHint(s));
        }

        @Override
        public void resetHint() {
            runOnUiThread(() -> ui.resetHint());
        }
    };

    private Outputable out = new Outputable() {

        private final int DELAY = 500;

        Queue<SimpleMutableEntry<CharSequence,Integer>> textColor = new LinkedList<>();
        Queue<SimpleMutableEntry<CharSequence,Integer>> textCategory = new LinkedList<>();

        boolean charged = false;
        Handler handler = new Handler();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                if(ui == null) {
                    handler.postDelayed(this, DELAY);
                    return;
                }

                SimpleMutableEntry<CharSequence,Integer> sm;
                while ((sm = textCategory.poll()) != null) {
                    ui.setOutput(sm.getKey(), sm.getValue());
                }

                while ((sm = textColor.poll()) != null) {
                    ui.setOutput(sm.getValue(), sm.getKey());
                }

                textCategory = null;
                textColor = null;
                handler = null;
                r = null;
            }
        };

        @Override
        public void onOutput(CharSequence output) {
            if(ui != null) ui.setOutput(output, TerminalManager.CATEGORY_OUTPUT);
            else {
                textCategory.add(new SimpleMutableEntry<>(output, TerminalManager.CATEGORY_OUTPUT));

                if(!charged) {
                    charged = true;
                    handler.postDelayed(r, DELAY);
                }
            }
        }

        @Override
        public void onOutput(CharSequence output, int category) {
            if(ui != null) ui.setOutput(output, category);
            else {
                textCategory.add(new SimpleMutableEntry<>(output, category));

                if(!charged) {
                    charged = true;
                    handler.postDelayed(r, DELAY);
                }
            }
        }

        @Override
        public void onOutput(int color, CharSequence output) {
            if(ui != null) ui.setOutput(color, output);
            else {
                textColor.add(new SimpleMutableEntry<>(output, color));

                if(!charged) {
                    charged = true;
                    handler.postDelayed(r, DELAY);
                }
            }
        }

        @Override
        public void dispose() {
            if(handler != null) handler.removeCallbacksAndMessages(null);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0,0);

        if (isFinishing()) {
            return;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED  &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, LauncherActivity.STARTING_PERMISSION);
        } else {
            canApplyTheme = true;
            finishOnCreate();
        }
    }

    private void finishOnCreate() {

        Thread.currentThread().setUncaughtExceptionHandler(new CustomExceptionHandler());

        XMLPrefsManager.loadCommons(this);
        new RegexManager(LauncherActivity.this);
        new TimeManager(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PrivateIOReceiver.ACTION_INPUT);
        filter.addAction(PrivateIOReceiver.ACTION_OUTPUT);
        filter.addAction(PrivateIOReceiver.ACTION_REPLY);

        privateIOReceiver = new PrivateIOReceiver(this, out, in);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(privateIOReceiver, filter);

        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(PublicIOReceiver.ACTION_CMD);
        filter1.addAction(PublicIOReceiver.ACTION_OUTPUT);

        publicIOReceiver = new PublicIOReceiver();
        getApplicationContext().registerReceiver(publicIOReceiver, filter1);

        int requestedOrientation = XMLPrefsManager.getInt(Behavior.orientation);
        if(requestedOrientation >= 0 && requestedOrientation != 2) {
            int orientation = getResources().getConfiguration().orientation;
            if(orientation != requestedOrientation) setRequestedOrientation(requestedOrientation);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            }
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !XMLPrefsManager.getBoolean(Ui.ignore_bar_color)) {
            Window window = getWindow();

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(XMLPrefsManager.getColor(Theme.statusbar_color));
            window.setNavigationBarColor(XMLPrefsManager.getColor(Theme.navigationbar_color));
        }

        backButtonEnabled = XMLPrefsManager.getBoolean(Behavior.back_button_enabled);

        boolean showNotification = XMLPrefsManager.getBoolean(Behavior.tui_notification);
        Intent keeperIntent = new Intent(this, KeeperService.class);
        if (showNotification) {
            keeperIntent.putExtra(KeeperService.PATH_KEY, XMLPrefsManager.get(Behavior.home_path));
            startService(keeperIntent);
        } else {
            try {
                stopService(keeperIntent);
            } catch (Exception e) {}
        }

        boolean fullscreen = XMLPrefsManager.getBoolean(Ui.fullscreen);
        if(fullscreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        boolean useSystemWP = XMLPrefsManager.getBoolean(Ui.system_wallpaper);
        if (useSystemWP) {
            setTheme(R.style.Custom_SystemWP);
        } else {
            setTheme(R.style.Custom_Solid);
        }

        try {
            NotificationManager.create(this);
        } catch (Exception e) {
            Tuils.toFile(e);
        }

        boolean notifications = XMLPrefsManager.getBoolean(Notifications.show_notifications) || XMLPrefsManager.get(Notifications.show_notifications).equalsIgnoreCase("enabled");
        if(notifications) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                try {
                    ComponentName notificationComponent = new ComponentName(this, NotificationService.class);
                    PackageManager pm = getPackageManager();
                    pm.setComponentEnabledSetting(notificationComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                    if (!Tuils.hasNotificationAccess(this)) {
                        Intent i = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                        if (i.resolveActivity(getPackageManager()) == null) {
                            Toast.makeText(this, R.string.no_notification_access, Toast.LENGTH_LONG).show();
                        } else {
                            startActivity(i);
                        }
                    }

                    Intent monitor = new Intent(this, NotificationMonitorService.class);
                    startService(monitor);

                    Intent notificationIntent = new Intent(this, NotificationService.class);
                    startService(notificationIntent);
                } catch (NoClassDefFoundError er) {
                    Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
                    intent.putExtra(PrivateIOReceiver.TEXT, getString(R.string.output_notification_error) + Tuils.SPACE + er.toString());
                }
            } else {
                Tuils.sendOutput(Color.RED, this, R.string.notification_low_api);
            }
        }

        LongClickableSpan.longPressVibrateDuration = XMLPrefsManager.getInt(Behavior.long_click_vibration_duration);

        openKeyboardOnStart = XMLPrefsManager.getBoolean(Behavior.auto_show_keyboard);
        if (!openKeyboardOnStart) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        setContentView(R.layout.base_view);

        if(XMLPrefsManager.getBoolean(Ui.show_restart_message)) {
            CharSequence s = getIntent().getCharSequenceExtra(Reloadable.MESSAGE);
            if(s != null) out.onOutput(Tuils.span(s, XMLPrefsManager.getColor(Theme.restart_message_color)));
        }

        categories = new HashSet<>();

        main = new MainManager(this);

        ViewGroup mainView = (ViewGroup) findViewById(R.id.mainview);

//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !XMLPrefsManager.getBoolean(Ui.ignore_bar_color) && !XMLPrefsManager.getBoolean(Ui.statusbar_light_icons)) {
//            mainView.setSystemUiVisibility(0);
//        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !XMLPrefsManager.getBoolean(Ui.ignore_bar_color) && !XMLPrefsManager.getBoolean(Ui.statusbar_light_icons)) {
            mainView.setSystemUiVisibility(mainView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        ui = new UIManager(this, mainView, main.getMainPack(), canApplyTheme, main.executer());

        main.setRedirectionListener(ui.buildRedirectionListener());
        ui.pack = main.getMainPack();

        in.in(Tuils.EMPTYSTRING);
        ui.focusTerminal();

        if(fullscreen) Assist.assistActivity(this);

        System.gc();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ui != null) ui.onStart(openKeyboardOnStart);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(new Intent(UIManager.ACTION_UPDATE_SUGGESTIONS));
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (ui != null && main != null) {
            ui.pause();
            main.dispose();
        }
    }

    private boolean disposed = false;
    private void dispose() {
        if(disposed) return;

        try {
            LocalBroadcastManager.getInstance(this.getApplicationContext()).unregisterReceiver(privateIOReceiver);
            getApplicationContext().unregisterReceiver(publicIOReceiver);
        } catch (Exception e) {}

        try {
            stopService(new Intent(this, NotificationMonitorService.class));
        } catch (NoClassDefFoundError | Exception e) {
            Tuils.log(e);
        }

        try {
            stopService(new Intent(this, KeeperService.class));
        } catch (NoClassDefFoundError | Exception e) {
            Tuils.log(e);
        }

        try {
            Intent notificationIntent = new Intent(this, NotificationService.class);
            notificationIntent.putExtra(NotificationService.DESTROY, true);
            startService(notificationIntent);
        } catch (Throwable e) {
            Tuils.log(e);
        }

        overridePendingTransition(0,0);

        if(main != null) main.destroy();
        if(ui != null) ui.dispose();

        XMLPrefsManager.dispose();
        RegexManager.instance.dispose();
        TimeManager.instance.dispose();

        disposed = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        dispose();
    }

    @Override
    public void onBackPressed() {
        if (backButtonEnabled && main != null) {
            ui.onBackPressed();
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK)
            return super.onKeyLongPress(keyCode, event);

        if (main != null)
            main.onLongBack();
        return true;
    }

    @Override
    public void reload() {
        runOnUiThread(stopActivity);
    }

    @Override
    public void addMessage(String header, String message) {
        for(ReloadMessageCategory cs : categories) {
            Tuils.log(cs.header, header);
            if(cs.header.equals(header)) {
                cs.lines.add(message);
                return;
            }
        }

        ReloadMessageCategory c = new ReloadMessageCategory(header);
        if(message != null) c.lines.add(message);
        categories.add(c);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && ui != null) {
            ui.focusTerminal();
        }
    }

    SuggestionsManager.Suggestion suggestion;
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        suggestion = (SuggestionsManager.Suggestion) v.getTag(R.id.suggestion_id);

        if(suggestion.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
            ContactManager.Contact contact = (ContactManager.Contact) suggestion.object;

            menu.setHeaderTitle(contact.name);
            for(int count = 0; count < contact.numbers.size(); count++) {
                menu.add(0, count, count, contact.numbers.get(count));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(suggestion != null) {
            if(suggestion.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
                ContactManager.Contact contact = (ContactManager.Contact) suggestion.object;
                contact.setSelectedNumber(item.getItemId());

                Tuils.sendInput(this, suggestion.getText());

                return true;
            }
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == TUIXT_REQUEST && resultCode != 0) {
            if(resultCode == TuixtActivity.BACK_PRESSED) {
                Tuils.sendOutput(this, R.string.tuixt_back_pressed);
            } else {
                Tuils.sendOutput(this, data.getStringExtra(TuixtActivity.ERROR_KEY));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(permissions.length > 0 && permissions[0].equals(Manifest.permission.READ_CONTACTS) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(new Intent(ContactManager.ACTION_REFRESH));
        }

        try {
            switch (requestCode) {
                case COMMAND_REQUEST_PERMISSION:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        MainPack info = main.getMainPack();
                        main.onCommand(info.lastCommand, (String) null, false);
                    } else {
                        ui.setOutput(getString(R.string.output_nopermissions), TerminalManager.CATEGORY_OUTPUT);
                        main.sendPermissionNotGrantedWarning();
                    }
                    break;
                case STARTING_PERMISSION:
                    int count = 0;
                    while(count < permissions.length && count < grantResults.length) {
                        if(grantResults[count] == PackageManager.PERMISSION_DENIED) {
                            Toast.makeText(this, R.string.permissions_toast, Toast.LENGTH_LONG).show();
                            new Thread() {
                                @Override
                                public void run() {
                                    super.run();

                                    try {
                                        sleep(2000);
                                    } catch (InterruptedException e) {}

                                    runOnUiThread(stopActivity);
                                }
                            }.start();
                            return;
                        }
                        count++;
                    }
                    canApplyTheme = false;
                    finishOnCreate();
                    break;
                case COMMAND_SUGGESTION_REQUEST_PERMISSION:
                    if (grantResults.length == 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        ui.setOutput(getString(R.string.output_nopermissions), TerminalManager.CATEGORY_OUTPUT);
                    }
                    break;
                case LOCATION_REQUEST_PERMISSION:
//                    Intent i = new Intent(UIManager.ACTION_WEATHER_GOT_PERMISSION);
//                    i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, grantResults[0]);
//                    LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(i);

                    Intent i = new Intent(TuiLocationManager.ACTION_GOT_PERMISSION);
                    i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, grantResults[0]);
                    LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(i);

                    break;
            }
        } catch (Exception e) {}
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String cmd = intent.getStringExtra(PrivateIOReceiver.TEXT);
        if(cmd != null) {
            Intent i = new Intent(MainManager.ACTION_EXEC);
            i.putExtra(MainManager.CMD_COUNT, MainManager.commandCount);
            i.putExtra(MainManager.CMD, cmd);
            i.putExtra(MainManager.NEED_WRITE_INPUT, true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
