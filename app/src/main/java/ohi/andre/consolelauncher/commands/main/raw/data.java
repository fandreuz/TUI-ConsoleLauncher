package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.lang.reflect.Field;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.Tuils;

public class data implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        boolean active = pack.get(boolean.class, 0);

        MainPack info = (MainPack) pack;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            active = toggle(info);
            return info.res.getString(R.string.output_data) + Tuils.SPACE + Boolean.toString(active);
        } else {
//            ShellUtils.CommandResult result = ShellUtils.execCommand("svc data " + (active ? "enable" : "disable"), true, null);
            return pack.context.getString(R.string.output_nofeature);
        }
    }

    private boolean toggle(MainPack info) {
        if (info.connectivityMgr == null) {
            try {
                init(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        boolean mobileConnected;

        if (info.wifi == null)
            info.wifi = (WifiManager) info.context.getSystemService(Context.WIFI_SERVICE);

        if (info.wifi.isWifiEnabled())
            mobileConnected = true;
        else {
            NetworkInfo mobileInfo = info.connectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            State state = mobileInfo.getState();
            mobileConnected = state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING;
        }

        try {
            info.setMobileDataEnabledMethod.invoke(info.connectMgr, !mobileConnected);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return !mobileConnected;
    }

    private void init(MainPack info) throws Exception {
        info.connectivityMgr = (ConnectivityManager) info.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Class<?> conmanClass = Class.forName(info.connectivityMgr.getClass().getName());
        Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
        iConnectivityManagerField.setAccessible(true);
        info.connectMgr = iConnectivityManagerField.get(info.connectivityMgr);
        Class<?> iConnectivityManagerClass = Class.forName(info.connectMgr.getClass().getName());
        info.setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
        info.setMobileDataEnabledMethod.setAccessible(true);
    }

    @Override
    public int helpRes() {
        return R.string.help_data;
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public int maxArgs() {
        return 1;
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.BOOLEAN};
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        MainPack pack = (MainPack) info;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            boolean active = toggle(pack);
            return pack.res.getString(R.string.mobile_data) + Tuils.SPACE + active;
        } else {
            return pack.res.getString(helpRes());
        }
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return onNotArgEnough(info, 0);
    }

}
