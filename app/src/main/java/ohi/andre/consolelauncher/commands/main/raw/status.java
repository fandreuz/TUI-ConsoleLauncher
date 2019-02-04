package ohi.andre.consolelauncher.commands.main.raw;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

import java.lang.reflect.Method;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.Tuils;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;

/**
 * Created by francescoandreuzzi on 05/04/16.
 */
public class status implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;

//        wifi
        ConnectivityManager connManager = (ConnectivityManager) info.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean wifiConnected = mWifi.isConnected();

//        battery
        Intent batteryIntent = info.context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int rawlevel = batteryIntent.getIntExtra("level", -1);
        double scale = batteryIntent.getIntExtra("scale", -1);
        double level = -1;
        if (rawlevel >= 0 && scale > 0) {
            level = rawlevel / scale;
        }
        level *= 100;

//        mobile
        boolean mobileOn = false;
        Class cmClass;
        Method method;
        try {
            cmClass = Class.forName(connManager.getClass().getName());
            method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);

            mobileOn = (Boolean) method.invoke(connManager);
        } catch (Exception e) {}

//        brightness
        ContentResolver cResolver = pack.context.getApplicationContext().getContentResolver();
        int b = 0;
        try {
            b = Settings.System.getInt(cResolver, SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {}
        b = b * 100 / 255;

        int autobrightnessState = Integer.MIN_VALUE;
        try {
            autobrightnessState = Settings.System.getInt(cResolver, SCREEN_BRIGHTNESS_MODE);
        } catch (Exception e) {}

//        location
        LocationManager lm = (LocationManager) pack.context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

//        bluetooth
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        boolean bluetoothOn;

        if(adapter == null) bluetoothOn = false;
        else bluetoothOn = adapter.isEnabled();

        StringBuilder builder = new StringBuilder();
        builder
                .append(info.res.getString(R.string.battery_label)).append(Tuils.SPACE).append(level).append("%").append(Tuils.NEWLINE)
                .append(info.res.getString(R.string.wifi_label)).append(Tuils.SPACE).append(wifiConnected).append(Tuils.NEWLINE)
                .append(info.res.getString(R.string.mobile_data_label)).append(Tuils.SPACE).append(mobileOn).append(Tuils.NEWLINE)
                .append(info.res.getString(R.string.bluetooth_label)).append(Tuils.SPACE).append(bluetoothOn).append(Tuils.NEWLINE)
                .append(info.res.getString(R.string.location_label)).append(Tuils.SPACE).append(gps_enabled || network_enabled).append(Tuils.NEWLINE)
                .append(info.res.getString(R.string.brightness_label)).append(Tuils.SPACE).append(autobrightnessState == SCREEN_BRIGHTNESS_MODE_AUTOMATIC ? "(auto) " : Tuils.EMPTYSTRING).append(b).append("%");

        return builder.toString();
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
