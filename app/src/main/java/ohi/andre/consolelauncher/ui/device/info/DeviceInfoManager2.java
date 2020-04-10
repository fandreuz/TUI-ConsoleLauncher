package ohi.andre.consolelauncher.ui.device.info;

import android.content.Context;

import java.util.Arrays;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Observable;
import ohi.andre.consolelauncher.managers.settings.SettingsManager;
import ohi.andre.consolelauncher.managers.settings.options.Behavior;

public class DeviceInfoManager2 {
    
    private final StorageManager            storageManager = new StorageManager();
    private final RAMManager                ramManager;
    private final WirelessConnectionManager connectionManager;
    private final BatteryManager            batteryManager;
    
    private final TextFormatter formatter = new TextFormatter();
    
    public DeviceInfoManager2 (Context context) {
        ramManager        = new RAMManager(context);
        connectionManager = new WirelessConnectionManager(context);
        batteryManager    = new BatteryManager(context);
    }
    
    // check that the order is consistent with the order of the Observables in combineLatest
    private final Pattern[] patterns = new Pattern[]{
            Pattern.compile("%total-ext-storage"),
            Pattern.compile("%available-ext-storage"),
            Pattern.compile("%total-int-storage"),
            Pattern.compile("%available-int-storage"),
            Pattern.compile("%battery-percentage"),
            Pattern.compile("%battery-charging"),
            Pattern.compile("%bluetooth-on"),
            Pattern.compile("%mobile-on"),
            Pattern.compile("%wifi-on"),
            Pattern.compile("available-ram"),
            Pattern.compile("%total-ram"),
    };
    
    public Observable<CharSequence> deviceInfoObservable () {
        return Observable.combineLatest(Arrays.asList(
                SettingsManager.getInstance()
                        .requestUpdates(Behavior.device_info_format, String.class),
                storageManager.totalExternalStorageObservable(), storageManager.availableExternalStorageObservable(),
                storageManager.totalInternalStorageObservable(), storageManager.availableInternalStorageObservable(),
                batteryManager.batteryPercentageObservable(), batteryManager.isChargingObservable(),
                connectionManager.bluetoothON(), connectionManager.mobileDataON(), connectionManager.wifiON(),
                ramManager.availableRAMObservable(), ramManager.totalRAMObservable()
                ),
                // associate the corresponding Pattern to each value
                objects -> objects)
                .map(objects -> {
                    String format = (String) objects[0];
                    
                    for (int i = 1; i < objects.length; i++) {
                        format = patterns[i].matcher(format)
                                .replaceAll(objects[i].toString());
                    }
                    
                    return format;
                })
                .flatMapSingle(formatter::evaluateExpressions)
                .map(formatter::optionals)
                .map(formatter::style);
    }
}
