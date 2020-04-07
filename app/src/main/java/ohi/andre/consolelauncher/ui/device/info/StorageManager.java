package ohi.andre.consolelauncher.ui.device.info;

import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import ohi.andre.consolelauncher.settings.SettingsManager;
import ohi.andre.consolelauncher.settings.options.Behavior;

public class StorageManager {

    public Observable<Integer> totalInternalStorage(String unit) {
        return Observable.just(getTotalSpace(Environment.getDataDirectory()))
                .map(mb -> formatSize(mb, unit))
                .map(sz -> (int) Math.ceil(sz));
    }

    public Observable<Integer> totalExternalStorage(String unit) {
        return SettingsManager.getInstance().requestUpdates(Behavior.external_storage_path, File.class)
                .map(StorageManager::getTotalSpace)
                .map(mb -> formatSize(mb, unit))
                .map(size -> (int) Math.ceil(size));
    }

    public Observable<Integer> availableInternalStorageObservable(String unit) {
        return Observable.interval(3, TimeUnit.SECONDS)
                .map(x -> getAvailableSpace(Environment.getDataDirectory()))
                .map(mb -> formatSize(mb, unit))
                .map(size -> (int) Math.ceil(size));
    }

    // mb
    public Observable<Integer> availableExternalStorage(String unit) {
        return SettingsManager.getInstance().requestUpdates(Behavior.external_storage_path, File.class)
                .map(StorageManager::getAvailableSpace)
                .map(mb -> formatSize(mb, unit))
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
