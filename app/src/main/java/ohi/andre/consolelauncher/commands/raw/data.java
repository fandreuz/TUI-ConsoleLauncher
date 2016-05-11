package ohi.andre.consolelauncher.commands.raw;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;

import java.lang.reflect.Field;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

public class data implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        boolean active = toggle(info);

        return info.res.getString(R.string.output_data) + " " + Boolean.toString(active);
    }

    private boolean toggle(ExecInfo info) {
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

    private void init(ExecInfo info) throws Exception {
        info.connectivityMgr = (ConnectivityManager) info.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Class<?> conmanClass = Class.forName(info.connectivityMgr.getClass().getName());
        Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
        iConnectivityManagerField.setAccessible(true);
        info.connectMgr = iConnectivityManagerField.get(info.connectivityMgr);
        Class<?> iConnectivityManagerClass = Class.forName(info.connectMgr.getClass().getName());
        info.setMobileDataEnabledMethod = iConnectivityManagerClass
                .getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
        info.setMobileDataEnabledMethod.setAccessible(true);
    }

    @Override
    public int helpRes() {
        return R.string.help_data;
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
