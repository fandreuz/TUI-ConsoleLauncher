package ohi.andre.consolelauncher.managers.notifications;

/**
 * Created by francescoandreuzzi on 03/09/2017.
 */

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;

import ohi.andre.consolelauncher.tuils.Tuils;


public class NotificationMonitorService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        ensureCollectorRunning();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void ensureCollectorRunning() {
        if(Tuils.notificationServiceIsRunning(this)) toggleNotificationListenerService();
    }

    private void toggleNotificationListenerService() {
        ComponentName thisComponent = new ComponentName(this, NotificationService.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}