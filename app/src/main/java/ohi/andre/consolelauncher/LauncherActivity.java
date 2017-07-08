package ohi.andre.consolelauncher;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.tuixt.TuixtActivity;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionsManager;
import ohi.andre.consolelauncher.tuils.Assist;
import ohi.andre.consolelauncher.tuils.KeeperService;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.Inputable;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;
import ohi.andre.consolelauncher.tuils.stuff.PolicyReceiver;

public class LauncherActivity extends AppCompatActivity implements Reloadable {

    private final String FIRSTACCESS_KEY = "x1";

    public static final int COMMAND_REQUEST_PERMISSION = 10;
    public static final int STARTING_PERMISSION = 11;
    public static final int COMMAND_SUGGESTION_REQUEST_PERMISSION = 12;

    public static final int TUIXT_REQUEST = 10;

    private UIManager ui;
    private MainManager main;

    private boolean openKeyboardOnStart, fullscreen;

    private CommandExecuter ex = new CommandExecuter() {

        @Override
        public String exec(String input, String alias) {
            try {
                main.onCommand(input, alias);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            return null;
        }
    };

    private Inputable in = new Inputable() {

        @Override
        public void in(String s) {
            try {
                ui.setInput(s);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
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

        @Override
        public void onOutput(String output) {
            try {
                ui.setOutput(output);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED  &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        LauncherActivity.STARTING_PERMISSION);
                return;
            }
        }

        finishOnCreate();
    }

    private void finishOnCreate() {

        SharedPreferences preferences = getPreferences(0);
        boolean firstAccess = preferences.getBoolean(FIRSTACCESS_KEY, true);
        if (firstAccess) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(FIRSTACCESS_KEY, false);
            editor.commit();
        }

        try {
            XMLPrefsManager.create();
        } catch (Exception e) {
            Log.e("andre", "", e);
//            this.startActivity(new Intent(this, LauncherActivity.class));
//            this.finish();
            return;
        }

        boolean showNotification = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.tui_notification);
        Intent keeperIntent = new Intent(this, KeeperService.class);
        if (showNotification) {
            startService(keeperIntent);
        } else {
            stopService(keeperIntent);
        }

        DevicePolicyManager policy = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName component = new ComponentName(this, PolicyReceiver.class);

        fullscreen = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.fullscreen);

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

        NotificationManager.create();
        boolean notifications = XMLPrefsManager.get(boolean.class, NotificationManager.Options.enabled);
        if(notifications) {
            LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));
            if(!Tuils.hasNotificationAccess(this)) {
                startActivity(new Intent(Build.VERSION.SDK_INT >= 22 ? Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS : "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        }

        openKeyboardOnStart = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Behavior.auto_show_keyboard);
        if (!openKeyboardOnStart) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        setContentView(R.layout.base_view);

        ViewGroup mainView = (ViewGroup) findViewById(R.id.mainview);
        main = new MainManager(this, in, out, policy, component);
        ui = new UIManager(main.getMainPack(), this, mainView, ex, policy, component, main.getMainPack());
        main.setRedirectionListener(ui.buildRedirectionListener());

        in.in(Tuils.EMPTYSTRING);
        ui.focusTerminal();

        if(fullscreen) Assist.assistActivity(this);

        System.gc();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ui != null) {
            ui.onStart(openKeyboardOnStart);
        }
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

        stopService(new Intent(this, KeeperService.class));

        if(main != null) {
            main.destroy();
        }
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

                Intent mStartActivity = new Intent(LauncherActivity.this, LauncherActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(LauncherActivity.this, mPendingIntentId, mStartActivity,
                        PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) LauncherActivity.this.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
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
                contact.selectedNumber = item.getItemId();

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
        try {
            switch (requestCode) {
                case COMMAND_REQUEST_PERMISSION:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        MainPack info = main.getMainPack();
                        main.onCommand(info.lastCommand, null);
                    } else {
                        ui.setOutput(getString(R.string.output_nopermissions));
                        main.sendPermissionNotGrantedWarning();
                    }
                    break;
                case STARTING_PERMISSION:
                    int count = 0;
                    while(count < permissions.length && count < grantResults.length) {
                        if( (permissions[count].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) || permissions[count].equals(Manifest.permission.READ_EXTERNAL_STORAGE))
                                && grantResults[count] == PackageManager.PERMISSION_DENIED) {
                            Toast.makeText(this, R.string.permissions_toast, Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        count++;
                    }
                    finishOnCreate();
                    break;
                case COMMAND_SUGGESTION_REQUEST_PERMISSION:
                    if (grantResults.length == 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        ui.setOutput(getString(R.string.output_nopermissions));
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

    private BroadcastReceiver onNotice = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String pack = intent.getStringExtra("package");
            String title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");
            int color = intent.getIntExtra("color", -1);

            if(ui != null) {
                ui.setOutput(pack + ": " + (title == null ? Tuils.EMPTYSTRING : title + (text == null ? Tuils.EMPTYSTRING : " --- ")) + (text == null ? Tuils.EMPTYSTRING : text),
                        color,
                        false);
            }
        }
    };

}
