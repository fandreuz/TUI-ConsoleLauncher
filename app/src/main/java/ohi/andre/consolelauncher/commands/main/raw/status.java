package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.lang.reflect.Method;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 05/04/16.
 */
public class status implements CommandAbstraction {

    private final String PERCENTAGE = "%";

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;

        ConnectivityManager connManager = (ConnectivityManager) info.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean wifiConnected = mWifi.isConnected();

        Intent batteryIntent = info.context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int rawlevel = batteryIntent.getIntExtra("level", -1);
        double scale = batteryIntent.getIntExtra("scale", -1);
        double level = -1;
        if (rawlevel >= 0 && scale > 0) {
            level = rawlevel / scale;
        }
        level *= 100;

        boolean mobileOn = false;
        Class cmClass;
        Method method;
        try {
            cmClass = Class.forName(connManager.getClass().getName());
            method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);

            mobileOn = (Boolean) method.invoke(connManager);
        } catch (Exception e) {}


        return info.res.getString(R.string.battery_charge) + Tuils.SPACE + (int) level + PERCENTAGE + Tuils.NEWLINE +
                info.res.getString(R.string.wifi) + Tuils.SPACE + wifiConnected + Tuils.NEWLINE +
                info.res.getString(R.string.mobile_data) + Tuils.SPACE + mobileOn;
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public int helpRes() {
        return R.string.help_status;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }
}
