package ohi.andre.consolelauncher;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Queue;

import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.tuixt.TuixtActivity;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.managers.notifications.NotificationMonitorService;
import ohi.andre.consolelauncher.managers.notifications.NotificationService;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionsManager;
import ohi.andre.consolelauncher.tuils.Assist;
import ohi.andre.consolelauncher.tuils.InputOutputReceiver;
import ohi.andre.consolelauncher.tuils.KeeperService;
import ohi.andre.consolelauncher.tuils.SimpleMutableEntry;
import ohi.andre.consolelauncher.tuils.TimeManager;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.Inputable;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;
import ohi.andre.consolelauncher.tuils.interfaces.Suggester;

public class LauncherActivity extends AppCompatActivity implements Reloadable {

    private final String FIRSTACCESS_KEY = "x3";

    public static final int COMMAND_REQUEST_PERMISSION = 10;
    public static final int STARTING_PERMISSION = 11;
    public static final int COMMAND_SUGGESTION_REQUEST_PERMISSION = 12;

    public static final int TUIXT_REQUEST = 10;

    private UIManager ui;
    private MainManager main;

    private InputOutputReceiver ioReceiver;

    private boolean openKeyboardOnStart, canApplyTheme;

    private CommandExecuter ex = new CommandExecuter() {

        @Override
        public String exec(String cmd, String aliasName) {
            if(main != null) main.onCommand(cmd, aliasName);
            return null;
        }

        @Override
        public String exec(String input) {
            exec(input, false);
            return null;
        }

        @Override
        public String exec(String input, boolean needWriteInput) {
            if(ui != null && needWriteInput) ui.setOutput(input, TerminalManager.CATEGORY_INPUT);
            if(main != null) main.onCommand(input, null);
            return null;
        }

    };

    private Inputable in = new Inputable() {

        @Override
        public void in(String s) {
            if(ui != null) ui.setInput(s);
        }

        @Override
        public void changeHint(final String s) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ui.setHint(s);
                }
            });
        }

        @Override
        public void resetHint() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ui.resetHint();
                }
            });
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
    };

    private Suggester sugg = new Suggester() {
        @Override
        public void requestUpdate() {
            if(ui != null) ui.requestSuggestion(Tuils.EMPTYSTRING);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0,0);

        if (isFinishing()) {
            return;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED  &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, LauncherActivity.STARTING_PERMISSION);
        }
        else {
            canApplyTheme = true;
            finishOnCreate();
        }
    }

    private void finishOnCreate() {
        FlavorUtils.startANR();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Tuils.log(e);
                Tuils.toFile(e);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(InputOutputReceiver.ACTION_CMD);
        filter.addAction(InputOutputReceiver.ACTION_OUTPUT);

        ioReceiver = new InputOutputReceiver(ex, out);
        getApplicationContext().registerReceiver(ioReceiver, filter);

        try {
            XMLPrefsManager.create(this);
            TimeManager.create();
        } catch (Exception e) {
            Tuils.log(Tuils.getStackTrace(e));
            Tuils.toFile(e);
            return;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.ignore_bar_color)) {
            Window window = getWindow();

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(XMLPrefsManager.getColor(XMLPrefsManager.Theme.statusbar_color));
            window.setNavigationBarColor(XMLPrefsManager.getColor(XMLPrefsManager.Theme.navigationbar_color));
        }

        boolean showNotification = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.tui_notification);
        Intent keeperIntent = new Intent(this, KeeperService.class);
        if (showNotification) {
            startService(keeperIntent);
        } else {
            try {
                stopService(keeperIntent);
            } catch (Exception e) {}
        }

        boolean fullscreen = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.fullscreen);

        boolean useSystemWP = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.system_wallpaper);
        if (useSystemWP) {
            if(fullscreen) {
                setTheme(R.style.Custom_SystemWP_Fullscreen);
            } else {
                setTheme(R.style.Custom_SystemWP);
            }
        } else {
            if(fullscreen) {
                setTheme(R.style.Custom_Solid_Fullscreen);
            } else {
                setTheme(R.style.Custom_Solid);
            }
        }

        try {
            NotificationManager.create(this);
        } catch (Exception e) {
            Tuils.toFile(e);
        }


        boolean notifications = XMLPrefsManager.get(boolean.class, NotificationManager.Options.show_notifications) ||
                XMLPrefsManager.get(String.class, NotificationManager.Options.show_notifications).equalsIgnoreCase("enabled");

        if(notifications) {
            ComponentName thisComponent = new ComponentName(this, NotificationService.class);
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            if(!Tuils.hasNotificationAccess(this)) {
                Intent i = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                if(i.resolveActivity(getPackageManager()) == null) {
                    Toast.makeText(this, R.string.no_notification_access, Toast.LENGTH_LONG).show();
                } else {
                    startActivity(i);
                }
            }

            Intent monitor = new Intent(this, NotificationMonitorService.class);
            startService(monitor);

            Intent timeColorIntent = new Intent(this, NotificationService.class);
            timeColorIntent.putExtra(XMLPrefsManager.Theme.time_color.label(), XMLPrefsManager.getColor(XMLPrefsManager.Theme.time_color));
            startService(timeColorIntent);
        }

        openKeyboardOnStart = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.auto_show_keyboard);
        if (!openKeyboardOnStart) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        setContentView(R.layout.base_view);

        ViewGroup mainView = (ViewGroup) findViewById(R.id.mainview);
        main = new MainManager(this, in, out, sugg, ex);
        ui = new UIManager(main.getMainPack(), this, mainView, ex, main.getMainPack(), canApplyTheme);
        main.setRedirectionListener(ui.buildRedirectionListener());
        main.setHintable(ui.getHintable());
        main.setRooter(ui.getRooter());

        in.in(Tuils.EMPTYSTRING);
        ui.focusTerminal();

        if(fullscreen) Assist.assistActivity(this);

        SharedPreferences preferences = getPreferences(0);
        boolean firstAccess = preferences.getBoolean(FIRSTACCESS_KEY, true);
        if (firstAccess) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(FIRSTACCESS_KEY, false);
            editor.commit();

            ui.setOutput(getString(R.string.firsthelp_text), TerminalManager.CATEGORY_OUTPUT);
            ui.setInput("tutorial");
        }

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

        sugg.requestUpdate();
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

        try {
            stopService(new Intent(this, KeeperService.class));
            stopService(new Intent(this, NotificationMonitorService.class));
            getApplicationContext().unregisterReceiver(ioReceiver);
        } catch (NoClassDefFoundError | Exception e) {}

        overridePendingTransition(0,0);

        if(main != null) {
            main.destroy();
        }

        System.exit(0);
    }

    @Override
    public void onBackPressed() {
        if (main != null) {
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 1000);
            }
        });
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

                in.in(suggestion.getText());

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
                out.onOutput(getString(R.string.tuixt_back_pressed));
            } else {
                out.onOutput(data.getStringExtra(TuixtActivity.ERROR_KEY));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(permissions.length > 0 && permissions[0].equals(Manifest.permission.READ_CONTACTS) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            main.getMainPack().contacts.refreshContacts(this);
        }

        try {
            switch (requestCode) {
                case COMMAND_REQUEST_PERMISSION:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        MainPack info = main.getMainPack();
                        main.onCommand(info.lastCommand, null);
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
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            }, 2000);
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
            }
        } catch (Exception e) {}
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(ui != null) ui.scrollToEnd();
    }
}
