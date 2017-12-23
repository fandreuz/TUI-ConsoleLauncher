package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Context;
import android.net.wifi.WifiManager;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;

public class wifi implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;
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
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return null;
    }

}
