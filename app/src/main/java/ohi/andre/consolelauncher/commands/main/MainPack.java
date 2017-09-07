package ohi.andre.consolelauncher.commands.main;

import android.content.Context;
import android.content.res.Resources;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import java.io.File;
import java.lang.reflect.Method;

import ohi.andre.consolelauncher.commands.CommandGroup;
import ohi.andre.consolelauncher.commands.CommandsPreferences;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.managers.AliasManager;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.flashlight.TorchManager;
import ohi.andre.consolelauncher.managers.music.MusicManager2;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.Redirectator;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;
import ohi.andre.consolelauncher.tuils.interfaces.Rooter;
import ohi.andre.consolelauncher.tuils.libsuperuser.ShellHolder;

/**
 * Created by francescoandreuzzi on 24/01/2017.
 */

public class MainPack extends ExecutePack {

    //	current directory
    public File currentDirectory;

    //	resources references
    public Resources res;

    //	flashlight
//    public boolean isFlashOn = false, canUseFlash = false;
//    public Camera camera;
//    public Camera.Parameters parameters;

    //	internet
    public WifiManager wifi;

    //	3g/data
    public Method setMobileDataEnabledMethod;
    public ConnectivityManager connectivityMgr;
    public Object connectMgr;

    //	contacts
    public ContactManager contacts;

    //	music
    public MusicManager2 player;

    //	apps & assocs
    public AliasManager aliasManager;
    public AppsManager appsManager;

    //	reload field
    public Reloadable reloadable;

    public Rooter rooter;

    public CommandsPreferences cmdPrefs;

    //	execute a command
    public CommandExecuter executer;

    public LocationManager locationManager;

    public String lastCommand;

    public Redirectator redirectator;

    public ShellHolder shellHolder;

    public MainPack(Context context, CommandGroup commandGroup, AliasManager alMgr, AppsManager appmgr, MusicManager2 p,
                    ContactManager c, Reloadable r, CommandExecuter executeCommand, Redirectator redirectator, ShellHolder shellHolder) {
        super(commandGroup);

        this.currentDirectory = XMLPrefsManager.get(File.class, XMLPrefsManager.Behavior.home_path);

        this.shellHolder = shellHolder;

        this.res = context.getResources();

        this.executer = executeCommand;

        this.context = context;

        this.aliasManager = alMgr;
        this.appsManager = appmgr;

//        this.canUseFlash = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        this.cmdPrefs = new CommandsPreferences();

        this.player = p;
        this.contacts = c;

        this.reloadable = r;

        this.redirectator = redirectator;
    }

//    public void initCamera() {
//        try {
//            this.camera = Camera.open();
//            this.parameters = this.camera.getParameters();
//            List<Camera.Size> sizes = this.parameters.getSupportedPreviewSizes();
//            if(sizes != null && sizes.size() > 0) {
//                this.parameters.setPreviewSize(sizes.get(0).width, sizes.get(0).height);
//            }
//        } catch (Exception e) {
//            this.camera = null;
//            this.parameters = null;
//        }
//    }

    public void dispose() {
//        if (this.camera == null || this.isFlashOn)
//            return;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            flash.detachSurfaceTexture(null);
//        }

//        this.camera.stopPreview();
//        this.camera.release();
//        this.camera = null;
//        this.parameters = null;

        TorchManager mgr = TorchManager.getInstance();
        if(mgr.isOn()) mgr.turnOff();
    }

    public void destroy() {
        if(player != null) player.destroy();
        appsManager.onDestroy();
    }
}
