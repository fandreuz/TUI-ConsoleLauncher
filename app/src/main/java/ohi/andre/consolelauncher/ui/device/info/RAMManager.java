package ohi.andre.consolelauncher.ui.device.info;

import android.app.ActivityManager;
import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;

import static android.content.Context.ACTIVITY_SERVICE;

public class RAMManager {

    private final ActivityManager activityManager;

    public RAMManager(Context context) {
        activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
    }

    public Observable<Integer> totalRAMObservable(String unit) {
        return Observable.just(totalRam())
                .map(bytes -> formatSize(bytes, unit))
                .map(sz -> (int) Math.ceil(sz));
    }

    public Observable<Integer> availableRAM(String unit) {
        return Observable.interval(3, TimeUnit.SECONDS)
                .map(time -> freeRam())
                .map(bytes -> formatSize(bytes, unit))
                .map(sz -> (int) Math.ceil(sz));
    }

    // bytes
    private double freeRam() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();

        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }

    // bytes
    private long totalRam() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/meminfo")));

            String line;
            while((line = reader.readLine()) != null) {
                if(line.startsWith("MemTotal")) {
                    line = line.replaceAll("\\D+", "");
                    return Long.parseLong(line);
                }
            }

            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static double formatSize(double bytes, String unit) {
        switch (unit) {
            case "gb":
                return bytes / (double) (1024*1024*1024);
            case "mb":
                return bytes / (double) (1024*1024);
            case "kb":
                return bytes / (double) 1024;
            default: return -1;
        }
    }
}
