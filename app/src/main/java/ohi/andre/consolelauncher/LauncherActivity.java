package ohi.andre.consolelauncher;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.tuixt.TuixtActivity;
import ohi.andre.consolelauncher.managers.PreferencesManager;
import ohi.andre.consolelauncher.tuils.KeeperService;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.Inputable;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;
import ohi.andre.consolelauncher.tuils.stuff.PolicyReceiver;
import ohi.andre.consolelauncher.tuils.tutorial.TutorialActivity;

public class LauncherActivity extends Activity implements Reloadable {

    private final String FIRSTACCESS_KEY = "x1";
    private final int FILEUPDATE_DELAY = 300;

    public static final int COMMAND_REQUEST_PERMISSION = 10;
    public static final int STARTING_PERMISSION = 11;
    public static final int COMMAND_SUGGESTION_REQUEST_PERMISSION = 12;

    public static final int TUIXT_REQUEST = 10;

    View decorView;

    private UIManager ui;
    private MainManager main;

    private boolean openKeyboardOnStart, fullscreen;

    private PreferencesManager preferencesManager;

    private Intent starterIntent;

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

        SharedPreferences preferences = getPreferences(0);
        boolean firstAccess = preferences.getBoolean(FIRSTACCESS_KEY, true);
        if (firstAccess) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(FIRSTACCESS_KEY, false);
            editor.commit();

            startActivity(new Intent(this, TutorialActivity.class));
            this.finish();
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

        File tuiFolder = getFolder();
        Resources res = getResources();
        starterIntent = getIntent();

        try {
            preferencesManager = new PreferencesManager(res.openRawResource(R.raw.settings), res.openRawResource(R.raw.alias), tuiFolder);
        } catch (IOException e) {
            this.startActivity(new Intent(this, LauncherActivity.class));
            this.finish();
        }

        boolean showNotification = Boolean.parseBoolean(preferencesManager.getValue(PreferencesManager.NOTIFICATION));
        Intent keeperIntent = new Intent(this, KeeperService.class);
        if (showNotification) {
            startService(keeperIntent);
        } else {
            stopService(keeperIntent);
        }

        DevicePolicyManager policy = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName component = new ComponentName(this, PolicyReceiver.class);

        boolean useSystemWP = Boolean.parseBoolean(preferencesManager.getValue(PreferencesManager.USE_SYSTEMWP));
        if (useSystemWP) {
            setTheme(R.style.Custom_SystemWP);
        } else {
            setTheme(R.style.Custom_Solid);
        }

        openKeyboardOnStart = Boolean.parseBoolean(preferencesManager.getValue(PreferencesManager.OPEN_KEYBOARD));
        if (!openKeyboardOnStart) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        setContentView(R.layout.base_view);

        ViewGroup mainView = (ViewGroup) findViewById(R.id.mainview);
        main = new MainManager(this, in, out, preferencesManager, policy, component);
        ui = new UIManager(main.getMainPack(), this, mainView, ex, policy, component, preferencesManager, main.getMainPack());

        in.in(Tuils.EMPTYSTRING);
        ui.focusTerminal();

        System.gc();
    }

    private File getFolder() {
        final File tuiFolder = Tuils.getTuiFolder();

        while (true) {
            if (tuiFolder != null && (tuiFolder.isDirectory() || tuiFolder.mkdir())) {
                break;
            }

            try {
                Thread.sleep(FILEUPDATE_DELAY);
            } catch (InterruptedException e) {}
        }

        return tuiFolder;
    }

    @Override
    protected void onStart() {
        super.onStart();

        overridePendingTransition(0,0);

        if (ui != null && openKeyboardOnStart) {
            ui.onStart();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        overridePendingTransition(0,0);

        if (ui != null && main != null) {
            ui.pause();
            main.dispose();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    reloadOver11();
                } else {
                    finish();
                    startActivity(starterIntent);
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void reloadOver11() {
        recreate();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && ui != null) {
            ui.focusTerminal();
        }
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

        ui.scrollToEnd();
    }
}
