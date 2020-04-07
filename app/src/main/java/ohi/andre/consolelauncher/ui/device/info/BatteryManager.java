package ohi.andre.consolelauncher.ui.device.info;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;

// monitors the battery status
public class BatteryManager {

    // this is a sticky intent, no need to dispose
    private final Intent batteryIntent;

    public BatteryManager(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryIntent = context.registerReceiver(null, ifilter);
    }

    // generates an observable which delivers the current battery level
    public Observable<Integer> batteryPercentageObservable() {
        return Observable.interval(3, TimeUnit.SECONDS)
                .map(x -> batteryPercentage())
                .distinctUntilChanged()
                .share();
    }

    public int batteryPercentage() {
        int level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level * 100 / (float)scale;

        return (int) Math.ceil(batteryPct);
    }

    // generates an observable which delivers the current battery status
    public Observable<Boolean> isChargingObservable() {
        return Observable.interval(1, TimeUnit.SECONDS)
                .map(x -> isCharging())
                .distinctUntilChanged()
                .share();
    }

    public boolean isCharging() {
        int status = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
        return status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL;
    }
}
