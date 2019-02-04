package ohi.andre.consolelauncher.managers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.tuils.Tuils;

public class TuiLocationManager {

    public static final String ACTION_GOT_PERMISSION = BuildConfig.APPLICATION_ID + ".got_location_permission";

    public static final String LATITUDE = "lat", LONGITUDE = "long", FAIL = "fail";

    private static final int MAX_DELAY = 10000;

    Context context;
    BroadcastReceiver receiver;

    LocationListener locationListener;

    Handler handler;

    public boolean locationAvailable = false;
    public double latitude, longitude;

    private List<String> actionsPool;

    private static TuiLocationManager instance;
    public static TuiLocationManager instance(Context context) {
        if(instance == null) instance = new TuiLocationManager(context);
        return instance;
    }

    private TuiLocationManager(final Context context) {
        this.context = context;
        actionsPool = new ArrayList<>();

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                clearHandler();

                latitude = location.getLatitude();
                longitude = location.getLongitude();

                locationAvailable = true;

                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context.getApplicationContext());

                for(String s : actionsPool) {
                    Intent i = new Intent(s);
                    i.putExtra(LATITUDE, location.getLatitude());
                    i.putExtra(LONGITUDE, location.getLongitude());
                    localBroadcastManager.sendBroadcast(i);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(action.equals(ACTION_GOT_PERMISSION)) {
                    if (intent.getIntExtra(XMLPrefsManager.VALUE_ATTRIBUTE, 1) == PackageManager.PERMISSION_GRANTED) {
                        register();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_GOT_PERMISSION);

        LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(receiver, filter);
    }

    private boolean registered = false;

    @SuppressLint("MissingPermission")
    private void register() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LauncherActivity.LOCATION_REQUEST_PERMISSION);
            return;
        }

        if(registered) return;
        registered = true;

        Criteria c = new Criteria();
        c.setAltitudeRequired(false);
        c.setAccuracy(Criteria.ACCURACY_COARSE);
        c.setBearingRequired(false);
        c.setCostAllowed(false);
        c.setHorizontalAccuracy(Criteria.NO_REQUIREMENT);
        c.setPowerRequirement(Criteria.POWER_LOW);
        c.setSpeedRequired(false);

        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        try {
            manager.requestLocationUpdates(XMLPrefsManager.getInt(Behavior.location_update_mintime) * 60 * 1000, XMLPrefsManager.getInt(Behavior.location_update_mindistance),
                    c, locationListener, Looper.getMainLooper());
        } catch (Exception e) {
            Tuils.log(e);
            Tuils.toFile(e);
        }

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context.getApplicationContext());

                for(String s : actionsPool) {
                    Intent i = new Intent(s);
                    i.putExtra(FAIL, true);
                    localBroadcastManager.sendBroadcast(i);
                }

                dispose();
            }
        }, MAX_DELAY);
    }

    public void add(String action) {
        actionsPool.add(action);

        register();
    }

    public void rm(String action) {
        actionsPool.remove(action);
    }

    private void dispose() {
        actionsPool.clear();
        LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(receiver);

        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if(manager != null) manager.removeUpdates(locationListener);

        clearHandler();
    }

    public static void disposeStatic() {
        if(instance != null) instance.dispose();
        instance = null;
    }

    private void clearHandler() {
        if(handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }
}
