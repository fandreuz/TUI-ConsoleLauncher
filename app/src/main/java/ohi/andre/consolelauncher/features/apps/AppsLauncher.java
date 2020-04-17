package ohi.andre.consolelauncher.features.apps;

import android.content.Context;
import android.content.Intent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class AppsLauncher {
    public static final String LAUNCH_TIMES_PREFS_NAME = "launchTimes";
    
    private final Context context;
    private final BlockingQueue<InstalledApplication> updatedLaunchedTimesQueue = new LinkedBlockingDeque<>();
    private final ExecutorService worker;
    
    public AppsLauncher (Context context) {
        this.context = context;
    
        worker = Executors.newSingleThreadExecutor();
        worker.execute(() -> {
            while(true) {
                if(updatedLaunchedTimesQueue.size() == 0) updatedLaunchedTimesQueue.notify();
                
                try {
                    updatedLaunchedTimesQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    // launches the application and updates the number of launched times
    public void launch(InstalledApplication application) {
        updateLaunchedTimes(application);
        
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(application.componentName)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);
    }
    
    private void updateLaunchedTimes(InstalledApplication application) {
        application.increaseLaunchTimes();
        updatedLaunchedTimesQueue.add(application);
    }
    
    private void dispose() {
        // don't do this in UI thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // wait for the queue to be empty
                updatedLaunchedTimesQueue.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // I don't want to shutdown the worker when the queue is not empty
            worker.shutdownNow();
        });
    }
}
