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
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;

import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.Inputable;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;
import ohi.andre.consolelauncher.tuils.stuff.PolicyReceiver;
import ohi.andre.consolelauncher.managers.PreferencesManager;
import ohi.andre.consolelauncher.tuils.Tuils;

public class LauncherActivity extends Activity implements Reloadable {

    private final int FILEUPDATE_DELAY = 300;

    private final String FIRSTACCESS_KEY = "firstAccess";

    private UIManager ui;
    private MainManager main;

    private boolean hideStatusBar;
    private boolean openKeyboardOnStart;

    private PreferencesManager preferencesManager;

    private CommandExecuter ex = new CommandExecuter() {

        @Override
        public String exec(String input, int id) {
            try {
                main.onCommand(input, id);
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
        public void onOutput(String output, int id) {
            try {
                ui.setOutput(output, id);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    };
    private Runnable clearer = new Runnable() {

        @Override
        public void run() {
            ui.clear();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DevicePolicyManager policy = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName component = new ComponentName(this, PolicyReceiver.class);

        SharedPreferences preferences = getPreferences(0);
        boolean firstAccess = preferences.getBoolean(FIRSTACCESS_KEY, true);
        if (firstAccess) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(FIRSTACCESS_KEY, false);
            editor.commit();

            Tuils.showTutorial(this);
        }

        Resources res = getResources();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            checkPermission(res);
        if (isFinishing())
            return;

        File tuiFolder = getFolder();

        try {
            preferencesManager = new PreferencesManager(res.openRawResource(R.raw.settings), res.openRawResource(R.raw.alias), tuiFolder);
        } catch (IOException e) {
            this.startActivity(new Intent(this, LauncherActivity.class));
            this.finish();
        }

        boolean showNotification = Boolean.parseBoolean(preferencesManager.getValue(PreferencesManager.NOTIFICATION));
        if (showNotification) {
            Intent service = new Intent(this, KeeperService.class);
            startService(service);
        }

        boolean useSystemWP = Boolean.parseBoolean(preferencesManager.getValue(PreferencesManager.USE_SYSTEMWP));
        if (useSystemWP)
            setTheme(R.style.SystemWallpaperStyle);

        hideStatusBar = Boolean.parseBoolean(preferencesManager.getValue(PreferencesManager.FULLSCREEN));

        openKeyboardOnStart = Boolean.parseBoolean(preferencesManager.getValue(PreferencesManager.OPEN_KEYBOARD));
        if (!openKeyboardOnStart) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }

        setContentView(R.layout.main_view);

        ViewGroup mainView = (ViewGroup) findViewById(R.id.mainview);
        main = new MainManager(this, in, out, preferencesManager, policy, component, clearer);
        ui = new UIManager(main.getInfo(), this, mainView, ex, policy, component, preferencesManager);

        System.gc();
    }

    private File getFolder() {
        final File tuiFolder = Tuils.getTuiFolder();

        while (true) {
            if (tuiFolder.isDirectory() || tuiFolder.mkdir())
                break;

            try {
                Thread.sleep(FILEUPDATE_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return tuiFolder;
    }

    private void hideStatusBar() {
        if (!hideStatusBar)
            return;

        if (Build.VERSION.SDK_INT < 16)
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        else
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @TargetApi(23)
    private void checkPermission(Resources res) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            this.finish();
            Tuils.openSettingsPage(this, res.getString(R.string.permissions_toast));
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            this.finish();
            Tuils.openSettingsPage(this, res.getString(R.string.permissions_toast));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (openKeyboardOnStart)
            ui.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideStatusBar();
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

        main.destroy();
    }

    @Override
    public void onBackPressed() {
        if (main != null)
            main.onBackPressed();
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
        Intent intent = getIntent();
        startActivity(intent);
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus)
            hideStatusBar();
    }

}
