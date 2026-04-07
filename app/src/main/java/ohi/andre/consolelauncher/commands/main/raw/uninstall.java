package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;
import android.net.Uri;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.tuils.Tuils;

public class uninstall implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;

        AppsManager.LaunchInfo launchInfo = info.getLaunchInfo();
        if (launchInfo == null || launchInfo.componentName == null) {
            return info.res.getString(R.string.output_appnotfound);
        }

        return uninstall(info, launchInfo.componentName.getPackageName());
    }

    private String uninstall(MainPack info, String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return info.res.getString(R.string.output_appnotfound);
        }

        try {
            Uri packageURI = Uri.fromParts("package", packageName, null);
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
            uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            info.context.startActivity(uninstallIntent);
        } catch (Exception e) {
            return e.toString();
        }

        return String.format("Uninstalling %s...", packageName);
    }

    @Override
    public int helpRes() {
        return R.string.help_uninstall;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.ALL_PACKAGES};
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        MainPack info = (MainPack) pack;
        return info.res.getString(helpRes());
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        MainPack info = (MainPack) pack;
        String arg = (String) pack.args[index];

        // Attempt to find the app in ALL_PACKAGES (including hidden) even if argType parsing failed
        AppsManager.LaunchInfo li = info.appsManager.findLaunchInfoWithLabel(arg, AppsManager.HIDDEN_APPS);
        if (li == null) {
            li = info.appsManager.findLaunchInfoWithLabel(arg, AppsManager.SHOWN_APPS);
        }

        if (li != null && li.componentName != null) {
            return uninstall(info, li.componentName.getPackageName());
        }

        // If still not found, assume the arg might be the package name itself
        return uninstall(info, arg);
    }

}
