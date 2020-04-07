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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArraySet;
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

import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import ohi.andre.consolelauncher.tuils.LongClickableSpan;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.tuixt.TuixtActivity;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.RegexManager;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.TimeManager;
import ohi.andre.consolelauncher.managers.TuiLocationManager;
import ohi.andre.consolelauncher.notifications.KeeperService;
import ohi.andre.consolelauncher.notifications.NotificationManager;
import ohi.andre.consolelauncher.notifications.NotificationMonitorService;
import ohi.andre.consolelauncher.notifications.NotificationService;
import ohi.andre.consolelauncher.settings.SettingsManager;
import ohi.andre.consolelauncher.settings.options.Behavior;
import ohi.andre.consolelauncher.settings.options.Notifications;
import ohi.andre.consolelauncher.settings.options.Theme;
import ohi.andre.consolelauncher.settings.options.Ui;
import ohi.andre.consolelauncher.suggestions.SuggestionsManager;
import ohi.andre.consolelauncher.tuils.Assist;
import ohi.andre.consolelauncher.tuils.PrivateIOReceiver;
import ohi.andre.consolelauncher.tuils.PublicIOReceiver;
import ohi.andre.consolelauncher.ui.UIManager;

import static ohi.andre.consolelauncher.tuils.Tuils.hasNotificationAccess;
import static ohi.andre.consolelauncher.tuils.Tuils.identity3;
import static ohi.andre.consolelauncher.tuils.Tuils.identityPredicate;
import static ohi.andre.consolelauncher.tuils.Tuils.invertedIdentityPredicate;
import static ohi.andre.consolelauncher.tuils.Tuils.log;
import static ohi.andre.consolelauncher.tuils.Tuils.sendInput;
import static ohi.andre.consolelauncher.tuils.Tuils.sendOutput;
import static ohi.andre.consolelauncher.tuils.Tuils.span;
import static ohi.andre.consolelauncher.tuils.Tuils.toFile;

/*
The main activity of the launcher.
 */
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

    private boolean openKeyboardOnStart, backButtonEnabled;

    // a restart message is shown the next time the user restarts t-ui
    // note that a restart message is lost if t-ui doesn't quit properly
    // each category holds the messages sent around by the components of t-ui
    private Set<RestartMessageCategory> restartMessageCategories;

    // holds a set of disposable subscriptions to Observable
    private Set<Disposable> disposableSet;

    // build the restart message
    private CharSequence restartMessage() {
        CharSequence reloadMessage = "";
        for (RestartMessageCategory c : restartMessageCategories) {
            reloadMessage = TextUtils.concat(reloadMessage, "\n", c.text());
        }
        return reloadMessage;
    }

    // cleanup, build the restart message, and restart the activity
    private void restartActivity() {
        dispose();
        finish();

        // build a new intent
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);

        // put the restart message inside the intent
        startMain.putExtra(Reloadable.MESSAGE, restartMessage());

        startActivity(startMain);
    }

    // the bridge makes sure that the code runs on the proper thread, so UiManager doesn't need
    // to care
    private InputBridge inputBridge = new InputBridge() {

        @Override
        public void setInput(String s) {
            if (ui != null) runOnUiThread(() -> ui.setInput(s));
        }

        @Override
        public void changeHint(final String s) {
            if (ui != null) runOnUiThread(() -> ui.setHint(s));
        }

        @Override
        public void resetHint() {
            if (ui != null) runOnUiThread(() -> ui.resetHint());
        }
    };

    // the bridge makes sure that the code runs on the proper thread, so UiManager doesn't need
    // to care
    private OutputBridge outputBridge = new OutputBridge() {

        @Override
        public void sendOutput(CharSequence output) {
            ui.setOutput(output);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove fancy animations
        overridePendingTransition(0, 0);

        // detect if the activity is going to finish, may filter some unexpected behaviors
        if (isFinishing()) {
            return;
        }

        // ask storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !(ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    LauncherActivity.STARTING_PERMISSION);
        } else {
            disposableSet = new ArraySet<>();

            SettingsManager settingsManager = SettingsManager.getInstance();
            settingsManager.loadSettings(LauncherActivity.this);

            new RegexManager(this);
            new TimeManager(this);

            IntentFilter filter = new IntentFilter();
            filter.addAction(PrivateIOReceiver.ACTION_INPUT);
            filter.addAction(PrivateIOReceiver.ACTION_OUTPUT);
            filter.addAction(PrivateIOReceiver.ACTION_REPLY);

            // todo: improve
            privateIOReceiver = new PrivateIOReceiver(this, outputBridge, inputBridge);
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(privateIOReceiver, filter);

            IntentFilter filter1 = new IntentFilter();
            filter1.addAction(PublicIOReceiver.ACTION_CMD);
            filter1.addAction(PublicIOReceiver.ACTION_OUTPUT);

            // todo: improve
            publicIOReceiver = new PublicIOReceiver();
            getApplicationContext().registerReceiver(publicIOReceiver, filter1);

            // orientation
            disposableSet.add(
                    settingsManager.requestUpdates(Behavior.orientation, Integer.class)
                            // exclude invalid orientations
                            .filter(requestedOrientation -> requestedOrientation >= 0 && requestedOrientation != 2)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(requestedOrientation -> {
                                setRequestedOrientation(requestedOrientation);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                                }
                            })
            );

            // status/navbar color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                disposableSet.add(
                        Observable.combineLatest(
                                settingsManager.requestUpdates(Ui.ignore_bar_color, Boolean.class),
                                settingsManager.requestUpdates(Theme.statusbar_color, Integer.class),
                                settingsManager.requestUpdates(Theme.navigationbar_color, Integer.class),
                                identity3()
                                )
                                // go on only if ignoreBar is false
                                .filter(pack3 -> !pack3.object1)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(pack3 -> {
                                    int statusBarColor = pack3.object2;
                                    int navBarColor = pack3.object3;

                                    Window window = getWindow();

                                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                                    window.setStatusBarColor(statusBarColor);
                                    window.setNavigationBarColor(navBarColor);
                                })
                );
            }

            // back button
            disposableSet.add(
                    settingsManager.requestUpdates(Behavior.back_button_enabled, Boolean.class)
                            .subscribe(enabled -> {
                                synchronized (LauncherActivity.this) {
                                    backButtonEnabled = enabled;
                                }
                            })
            );

            // show t-ui notification
            Observable<String> homePath = settingsManager.requestUpdates(Behavior.home_path, String.class);
            Observable<Boolean> showTuiNotification = settingsManager.requestUpdates(Behavior.tui_notification, Boolean.class);

            disposableSet.add(
                    Observable.combineLatest(showTuiNotification, homePath, (show, path) -> show ? path : null)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(path -> {
                                Intent keeperIntent = new Intent(LauncherActivity.this, KeeperService.class);
                                if (path != null) {
                                    keeperIntent.putExtra(KeeperService.PATH_KEY, path);
                                    startService(keeperIntent);
                                } else {
                                    try {
                                        stopService(keeperIntent);
                                    } catch (Exception e) {
                                        log(e);
                                    }
                                }
                            })
            );

            // fullscreen
            disposableSet.add(
                    settingsManager.requestUpdates(Ui.fullscreen, Boolean.class)
                            .filter(fullscreen -> fullscreen)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(fullscreen -> {
                                Assist.assistActivity(this);
                                requestWindowFeature(Window.FEATURE_NO_TITLE);
                                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                            })
            );

            // system wp
            disposableSet.add(
                    settingsManager.requestUpdates(Ui.system_wallpaper, Boolean.class)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(useSystemWP -> {
                                if (useSystemWP) {
                                    setTheme(R.style.Custom_SystemWP);
                                } else {
                                    setTheme(R.style.Custom_Solid);
                                }
                            })
            );

            // todo: improve
            try {
                NotificationManager.create(this);
            } catch (Exception e) {
                toFile(e);
            }

            // notifications
            disposableSet.add(
                    settingsManager.requestUpdates(Notifications.show_notifications, Boolean.class)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(showNotifications -> {
                                if (showNotifications) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                        try {
                                            ComponentName notificationComponent = new ComponentName(this, NotificationService.class);
                                            PackageManager pm = getPackageManager();
                                            pm.setComponentEnabledSetting(notificationComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                                            if (!hasNotificationAccess(this)) {
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
                                            intent.putExtra(PrivateIOReceiver.TEXT, getString(R.string.output_notification_error) + " " + er.toString());
                                        }
                                    } else {
                                        sendOutput(Color.RED, this, R.string.notification_low_api);
                                    }
                                }
                            })
            );

            // long-click duration
            // todo: improve (use system default)
            disposableSet.add(
                    settingsManager.requestUpdates(Behavior.long_click_vibration_duration, Integer.class)
                            .subscribe(duration -> LongClickableSpan.longPressVibrateDuration = duration)
            );

            // open keyboard on start
            disposableSet.add(
                    settingsManager.requestUpdates(Behavior.auto_show_keyboard, Boolean.class)
                            .doOnNext(openKeyboard -> openKeyboardOnStart = openKeyboard)
                            .filter(invertedIdentityPredicate)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(openKeyboard -> getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE))
            );

            setContentView(R.layout.base_view);

            // restart message
            CharSequence restartMessage = getIntent().getCharSequenceExtra(Reloadable.MESSAGE);
            if (restartMessage != null) {
                Observable<Boolean> showRestartMessage = settingsManager.requestUpdates(Ui.show_restart_message, Boolean.class);
                Observable<Integer> restartMessageColor = settingsManager.requestUpdates(Theme.restart_message_color, Integer.class);
                disposableSet.add(
                        Observable.combineLatest(showRestartMessage, restartMessageColor, (show, color) -> show ? color : Integer.MIN_VALUE)
                                .firstOrError()
                                .filter(color -> color != Integer.MIN_VALUE)
                                .subscribe(color -> outputBridge.sendOutput(span(restartMessage, color)))
                );
            }

            restartMessageCategories = new ArraySet<>();

            main = new MainManager(this);

            ViewGroup mainView = findViewById(R.id.mainview);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Observable<Boolean> ignoreBarColor = settingsManager.requestUpdates(Ui.ignore_bar_color, Boolean.class);
                Observable<Boolean> lightIcons = settingsManager.requestUpdates(Ui.statusbar_light_icons, Boolean.class);

                disposableSet.add(
                        Observable.combineLatest(ignoreBarColor, lightIcons, (ignore, light) -> !ignore && !light)
                                .takeWhile(integer -> !isFinishing())
                                .filter(identityPredicate)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(bool -> mainView.setSystemUiVisibility(mainView.getSystemUiVisibility()
                                        | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR))
                );
            }

            ui = new UIManager(this, mainView, main.getMainPack(), main.executer());

            main.setRedirectionListener(ui.buildRedirectionListener());
            ui.pack = main.getMainPack();

            inputBridge.setInput("");
            ui.focusTerminal();

            System.gc();
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        dispose();

        overridePendingTransition(0, 0);
    }
    
    private void dispose() {
        for(Disposable d : disposableSet)
            d.dispose();
        disposableSet.clear();

        try {
            LocalBroadcastManager.getInstance(this.getApplicationContext()).unregisterReceiver(privateIOReceiver);
            getApplicationContext().unregisterReceiver(publicIOReceiver);
        } catch (Exception e) {
        }

        try {
            stopService(new Intent(this, NotificationMonitorService.class));
        } catch (NoClassDefFoundError | Exception e) {
            log(e);
        }

        try {
            stopService(new Intent(this, KeeperService.class));
        } catch (NoClassDefFoundError | Exception e) {
            log(e);
        }

        try {
            Intent notificationIntent = new Intent(this, NotificationService.class);
            notificationIntent.putExtra(NotificationService.DESTROY, true);
            startService(notificationIntent);
        } catch (Throwable e) {
            log(e);
        }
        
        if (main != null) main.destroy();
        if (ui != null) ui.dispose();

        SettingsManager.dispose();
        RegexManager.instance.dispose();
        TimeManager.instance.dispose();
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
        runOnUiThread(this::restartActivity);
    }

    @Override
    public void addMessage(String header, String message) {
        for (ReloadMessageCategory cs : categories) {
            log(cs.header, header);
            if (cs.header.equals(header)) {
                cs.lines.add(message);
                return;
            }
        }

        ReloadMessageCategory c = new ReloadMessageCategory(header);
        if (message != null) c.lines.add(message);
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

        if (suggestion.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
            ContactManager.Contact contact = (ContactManager.Contact) suggestion.object;

            menu.setHeaderTitle(contact.name);
            for (int count = 0; count < contact.numbers.size(); count++) {
                menu.add(0, count, count, contact.numbers.get(count));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (suggestion != null) {
            if (suggestion.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
                ContactManager.Contact contact = (ContactManager.Contact) suggestion.object;
                contact.setSelectedNumber(item.getItemId());

                sendInput(this, suggestion.getText());

                return true;
            }
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TUIXT_REQUEST && resultCode != 0) {
            if (resultCode == TuixtActivity.BACK_PRESSED) {
                sendOutput(this, R.string.tuixt_back_pressed);
            } else {
                sendOutput(this, data.getStringExtra(TuixtActivity.ERROR_KEY));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (permissions.length > 0 && permissions[0].equals(Manifest.permission.READ_CONTACTS) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
                    while (count < permissions.length && count < grantResults.length) {
                        if (grantResults[count] == PackageManager.PERMISSION_DENIED) {
                            Toast.makeText(this, R.string.permissions_toast, Toast.LENGTH_LONG).show();
                            new Thread() {
                                @Override
                                public void run() {
                                    super.run();

                                    try {
                                        sleep(2000);
                                    } catch (InterruptedException e) {
                                    }

                                    runOnUiThread(stopActivity);
                                }
                            }.start();
                            return;
                        }
                        count++;
                    }
                    onCreate(null);
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
                    i.putExtra(SettingsManager.VALUE_ATTRIBUTE, grantResults[0]);
                    LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(i);

                    break;
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String cmd = intent.getStringExtra(PrivateIOReceiver.TEXT);
        if (cmd != null) {
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

    /*
    An instance of InputBridge can be sent around t-ui components safely, in order
    to provide a convenient way to set the input and the hint.
     */
    public interface InputBridge {
        // set the input
        void setInput(String s);

        // set the hint
        void changeHint(String s);

        // reset the hint to the default value
        void resetHint();
    }

    /*
    An instance of InputBridge can be sent around t-ui components safely, in order
    to provide a convenient way to send an output to t-ui.
     */
    public interface OutputBridge {
        void sendOutput(CharSequence output);
    }

}
