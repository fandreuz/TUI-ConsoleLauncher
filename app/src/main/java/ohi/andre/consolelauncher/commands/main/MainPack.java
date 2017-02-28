package ohi.andre.consolelauncher.commands.main;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Parcel;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Objects;

import ohi.andre.consolelauncher.commands.CommandGroup;
import ohi.andre.consolelauncher.commands.CommandsPreferences;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.raw.flash;
import ohi.andre.consolelauncher.managers.AliasManager;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.MusicManager;
import ohi.andre.consolelauncher.managers.PreferencesManager;
import ohi.andre.consolelauncher.managers.SkinManager;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;

/**
 * Created by francescoandreuzzi on 24/01/2017.
 */

public class MainPack extends ExecutePack {

    public Outputable outputable;

    //	current directory
    public File currentDirectory;

    //	context for accessing methods
    public Context context;

    public SkinManager skinManager;

    //	resources references
    public Resources res;

    //	flashlight
    public boolean isFlashOn = false, canUseFlash = false;
    public Camera camera;
    public Camera.Parameters parameters;

    //	internet
    public WifiManager wifi;

    //	prefs
    public PreferencesManager preferencesManager;

    //	3g/data
    public Method setMobileDataEnabledMethod;
    public ConnectivityManager connectivityMgr;
    public Object connectMgr;

    //	contacts
    public ContactManager contacts;

    //	music
    public MusicManager player;

    //	apps & assocs
    public AliasManager aliasManager;
    public AppsManager appsManager;

    //	admin
    public DevicePolicyManager policy;
    public ComponentName component;

    //	reload field
    public Reloadable reloadable;

    public CommandsPreferences cmdPrefs;

    //	execute a command
    public CommandExecuter executer;
    //	uses su
    private boolean canUseSu = false;

    public String lastCommand;

    public MainPack(Context context, PreferencesManager prefsMgr, CommandGroup commandGroup, AliasManager alMgr, AppsManager appmgr, MusicManager p,
                       ContactManager c, DevicePolicyManager devicePolicyManager, ComponentName componentName,
                       Reloadable r, CommandExecuter executeCommand, Outputable outputable) {
        super(commandGroup);

        this.outputable = outputable;

        this.res = context.getResources();

        this.executer = executeCommand;

        this.preferencesManager = prefsMgr;

        this.context = context;

        this.currentDirectory = new File(Tuils.getInternalDirectoryPath());
        this.aliasManager = alMgr;
        this.appsManager = appmgr;

        this.canUseFlash = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        this.cmdPrefs = new CommandsPreferences(prefsMgr);

        this.player = p;
        this.contacts = c;

        this.policy = devicePolicyManager;
        this.component = componentName;

        this.reloadable = r;
    }

    public boolean getSu() {
        boolean su = canUseSu;
        canUseSu = false;
        return su;
    }

    public void setSu(boolean su) {
        this.canUseSu = su;
    }

    public void initCamera() {
        try {
            this.camera = Camera.open();
            this.parameters = this.camera.getParameters();
        } catch (Exception e) {
            this.camera = null;
            this.parameters = null;
        }
    }

    public void dispose() {
        if (this.camera == null || this.isFlashOn)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            flash.detachSurfaceTexture(null);
        }

        this.camera.stopPreview();
        this.camera.release();
        this.camera = null;
        this.parameters = null;
    }

    public void destroy() {
        player.destroy(this.context);
        appsManager.onDestroy();
    }

    @Override
    public void clear() {
        super.clear();
        setSu(false);
    }
}
