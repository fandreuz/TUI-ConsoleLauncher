package ohi.andre.consolelauncher.ui.device.info;

import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import ohi.andre.consolelauncher.managers.settings.SettingsManager;
import ohi.andre.consolelauncher.managers.settings.options.Behavior;

public class StorageManager {

    // mb
    public Observable<Integer> totalInternalStorageObservable() {
        return Observable.just(getTotalSpace(Environment.getDataDirectory()))
                .map(sz -> (int) Math.ceil(sz));
    }

    // mb
    public Observable<Integer> totalExternalStorageObservable() {
        return SettingsManager.getInstance().requestUpdates(Behavior.external_storage_path, File.class)
                .map(StorageManager::getTotalSpace)
                .map(size -> (int) Math.ceil(size));
    }

    // mb
    public Observable<Integer> availableInternalStorageObservable() {
        return Observable.interval(3, TimeUnit.SECONDS)
                .map(x -> getAvailableSpace(Environment.getDataDirectory()))
                .map(size -> (int) Math.ceil(size));
    }

    // mb
    public Observable<Integer> availableExternalStorageObservable() {
        return SettingsManager.getInstance().requestUpdates(Behavior.external_storage_path, File.class)
                .map(StorageManager::getAvailableSpace)
                .map(size -> (int) Math.ceil(size));
    }

    private static double formatSize(double mb, String unit) {
        switch (unit) {
            case "gb":
                return mb / (double) 1024;
            case "mb":
                return mb;
            case "kb":
                return mb * (double) 1024;
            default: return -1;
        }
    }

    // mb
    private static double getAvailableSpace(File dir) {
        if(dir == null) return -1;

        StatFs statFs = new StatFs(dir.getAbsolutePath());
        long blocks = statFs.getAvailableBlocks();
        return blocks * statFs.getBlockSize() / (double) 1024;
    }

    // mb
    private static double getTotalSpace(File dir) {
        if(dir == null) return -1;

        StatFs statFs = new StatFs(dir.getAbsolutePath());
        long blocks = statFs.getBlockCount();
        return blocks * statFs.getBlockSize() / (double) 1024;
    }
}
