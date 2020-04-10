package ohi.andre.consolelauncher.ui.device.info;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.github.pwittchen.reactivenetwork.library.rx2.Connectivity;
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;

import java.util.concurrent.TimeUnit;

import hu.akarnokd.rxjava3.bridge.RxJavaBridge;
import io.reactivex.rxjava3.core.Observable;

public class WirelessConnectionManager {

    // avoid memory leaks
    private final Context applicationContext;

    public WirelessConnectionManager(Context context) {
        applicationContext = context.getApplicationContext();
    }

    public Observable<Boolean> wifiON() {
        return networkInfo()
                .map(connectivity -> connectivity.state() == NetworkInfo.State.CONNECTED
                        && connectivity.type() == ConnectivityManager.TYPE_WIFI)
                .distinctUntilChanged();
    }

    public Observable<Boolean> mobileDataON() {
        return networkInfo()
                .map(connectivity -> connectivity.state() == NetworkInfo.State.CONNECTED
                        && connectivity.type() == ConnectivityManager.TYPE_MOBILE)
                .distinctUntilChanged();
    }

    public Observable<Boolean> bluetoothON() {
        return Observable.interval(3, TimeUnit.SECONDS)
                .map(lg -> BluetoothAdapter.getDefaultAdapter().isEnabled())
                .share();
    }

    public Observable<Connectivity> networkInfo() {
        return RxJavaBridge.toV3Observable(
                ReactiveNetwork
                        .observeNetworkConnectivity(applicationContext))
                .share();
    }
}
