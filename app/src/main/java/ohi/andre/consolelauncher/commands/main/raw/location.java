package ohi.andre.consolelauncher.commands.main.raw;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.APICommand;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 10/05/2017.
 */

public class location extends APICommand {

    @Override
    public String exec(final ExecutePack pack) throws Exception {
        final Context context = ((MainPack) pack).context;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LauncherActivity.COMMAND_REQUEST_PERMISSION);
            return context.getString(R.string.output_waitingpermission);
        }

        final MainPack main = ((MainPack) pack);
        if(main.locationManager == null) {
            main.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Tuils.sendOutput(context, "Lat: " + location.getLatitude() + "; Long: " + location.getLongitude());
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            boolean gpsStatus = main.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkStatus = main.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!gpsStatus && !networkStatus) {
                return context.getString(R.string.location_off);
            }

            main.locationManager.requestSingleUpdate(gpsStatus ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER, locationListener, Looper.getMainLooper());
            return null;
        }
        return null;
    }

    @Override
    public int minArgs() {
        return 0;
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_location;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return null;
    }

    @Override
    public boolean willWorkOn(int api) {
        return api >= Build.VERSION_CODES.GINGERBREAD;
    }
}
