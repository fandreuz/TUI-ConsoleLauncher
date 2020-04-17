package ohi.andre.consolelauncher.features.apps;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.Process;
import android.support.v4.util.ArraySet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.reactivex.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.reactivex.schedulers.Schedulers;
import ohi.andre.consolelauncher.tuils.Tuils;

// load apps and detect new installs
public class AppsLoader {
    private static final String HIDDEN_APPS_PREFS = "hidden_apps";
    
    private final Context context;
    
    // emits an item when an app is installed (true) or uninstalled (false)
    private final Subject<Boolean> applicationStateChangedSubject = PublishSubject.create();
    
    private final BroadcastReceiver applicationStateChangedListener = new BroadcastReceiver() {
        @Override
        public void onReceive (Context context, Intent intent) {
            if (intent.getAction()
                    .equals(Intent.ACTION_PACKAGE_ADDED)) applicationStateChangedSubject.onNext(true);
            else if (intent.getAction()
                    .equals(Intent.ACTION_PACKAGE_REMOVED)) applicationStateChangedSubject.onNext(false);
        }
    };
    
    public AppsLoader (Context context) {
        this.context = context;
        registerApplicationStateChangedReceiver();
    }
    
    // the first emitted item is the list of apps at launch time.
    // then installed/uninstalled apps will trigger an update
    // the list is sorted
    public Observable<Tuils.Pack3<List<InstalledApplication>, List<InstalledApplication>, Set<InstalledApplication>>> installedAppsObservable () {
        return applicationStateChangedSubject
                .startWith(Observable.just(false))
                // todo: take different actions
                .map(installed -> loadApps(context))
                .filter(pack -> pack != null)
                .doOnNext(pack -> {
                    Collections.sort(pack.object1);
                    Collections.sort(pack.object2);
                });
    }
    
    // this method returns a pack of three collections: {all, visible, hidden}
    @SuppressLint({"NewApi", "CheckResult"})
    private Tuils.Pack3<List<InstalledApplication>, List<InstalledApplication>, Set<InstalledApplication>> loadApps (Context context) {
        final List<InstalledApplication> applications = new ArrayList<>();
        final List<InstalledApplication> visible = new ArrayList<>();
        final Set<InstalledApplication> hidden = new ArraySet<>();
        
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        
        PackageManager mgr = context.getPackageManager();
        
        List<ResolveInfo> main;
        try {
            main = mgr.queryIntentActivities(i, 0);
        } catch (Exception e) {
            return new Tuils.Pack3<>(applications, visible, hidden);
        }
        
        SharedPreferences launchedTimePrefs = context.getSharedPreferences(AppsLauncher.LAUNCH_TIMES_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences hiddenAppsPrefs = context.getSharedPreferences(AppsManager2.HIDDEN_APPS_PREFS, Context.MODE_PRIVATE);
        
        // if t-ui is the default launcher, we can try to get app shortcuts. otherwise revert to the old method for gathering apps
        boolean getShortcuts = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && Tuils.isMyLauncherDefault(mgr);
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        
        Flowable.fromIterable(main)
                .observeOn(Schedulers.io())
                .parallel()
                .runOn(Schedulers.io())
                .map(resolveInfo -> {
                    // attempt to get ShortcutInfo
                    List<ShortcutInfo> shortcutInfos = null;
                    
                    // if t-ui is the default launcher AND we're on Marshmallow
                    if (getShortcuts) {
                        try {
                            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
                            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST | LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC);
                            query.setPackage(resolveInfo.activityInfo.packageName);
                            shortcutInfos = launcherApps.getShortcuts(query, Process.myUserHandle());
                        } catch (Throwable e) {
                            // t-ui is not the default launcher
                            Tuils.log(e);
                        }
                    }
                    
                    return new InstalledApplication(resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name, resolveInfo.loadLabel(mgr)
                            .toString(), shortcutInfos);
                })
                .doOnNext(application -> application.setLaunchedTimes(launchedTimePrefs.getInt(application.getKey(), 0)))
                .doOnNext(application -> application.setHidden(hiddenAppsPrefs.getBoolean(application.getKey(), false)))
                .doOnNext(applications::add)
                .doOnNext(application -> {
                    if (application.isHidden()) hidden.add(application);
                    else visible.add(application);
                })
                .sequential()
                .subscribe(application -> {}, error -> {}, applications::notify);
        
        try {
            // todo: blocking the thread here will allow the execution of the observable?
            applications.wait();
            return new Tuils.Pack3<>(applications, visible, hidden);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // register the receiver for new installed/uninstalled apps
    private void registerApplicationStateChangedReceiver () {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        
        this.context.registerReceiver(applicationStateChangedListener, intentFilter);
    }
    
    public void dispose () {
        context.unregisterReceiver(applicationStateChangedListener);
    }
}
