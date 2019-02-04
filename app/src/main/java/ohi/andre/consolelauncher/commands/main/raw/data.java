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
import ohi.andre.consolelauncher.commands.main.specific.APICommand;
import ohi.andre.consolelauncher.tuils.Tuils;

public class data implements APICommand, CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;
        boolean active = toggle(info);
        return info.res.getString(R.string.output_data) + Tuils.SPACE + Boolean.toString(active);
    }

    private boolean toggle(MainPack info) {
        if (info.connectivityMgr == null) {
            try {
                init(info);
            } catch (Exception e) {}
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
        return onNotArgEnough(info, 0);
    }

    @Override
    public boolean willWorkOn(int api) {
        return api < Build.VERSION_CODES.LOLLIPOP;
    }
}
