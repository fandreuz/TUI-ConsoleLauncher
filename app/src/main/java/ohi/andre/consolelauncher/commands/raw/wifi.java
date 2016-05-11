package ohi.andre.consolelauncher.commands.raw;

import android.content.Context;
import android.net.wifi.WifiManager;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

public class wifi implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        if (info.wifi == null)
            info.wifi = (WifiManager) info.context.getSystemService(Context.WIFI_SERVICE);
        boolean active = !info.wifi.isWifiEnabled();
        info.wifi.setWifiEnabled(active);
        return info.res.getString(R.string.output_wifi) + " " + Boolean.toString(active);
    }

    @Override
    public int helpRes() {
        return R.string.help_wifi;
    }

    @Override
    public int minArgs() {
        return 0;
    }

    @Override
    public int maxArgs() {
        return 0;
    }

    @Override
    public int[] argType() {
        return null;
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String[] parameters() {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return null;
    }

    @Override
    public int notFoundRes() {
        return 0;
    }

}
