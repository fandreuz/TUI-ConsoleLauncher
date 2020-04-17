package ohi.andre.consolelauncher.features.apps;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import ohi.andre.consolelauncher.features.apps.groups.AppGroupsManager;
import ohi.andre.consolelauncher.tuils.Tuils;

// this is the bridge between t-ui and the apps
public class AppsManager2 {
    public static final String HIDDEN_APPS_PREFS = "hiddenApps";
    
    public final AppsLauncher     launcher;
    public final AppsLoader       appsLoader;
    public       AppGroupsManager groups;
    
    // this will be updated everytime a new update comes from the observable in AppsLoader
    private List<InstalledApplication> installedApplications;
    private List<InstalledApplication> visibleApplications;
    private Set<InstalledApplication>  hiddenApplications;
    
    private final Context context;
    
    private final CompositeDisposable disposable = new CompositeDisposable();
    
    public AppsManager2 (Context context) {
        this.context = context;
        
        launcher   = new AppsLauncher(context);
        appsLoader = new AppsLoader(context);
        
        disposable.add(appsLoader.installedAppsObservable()
                .observeOn(Schedulers.io())
                .doOnNext(pack -> {
                    // initialize app groups
                    if (groups == null) {
                        // create the required map
                        HashMap<String, InstalledApplication> map = new HashMap<>();
                        Observable.fromIterable(pack.object1)
                                .map(application -> new Tuils.BiPack<>(application.getKey(), application))
                                .subscribe(bipack -> map.put(bipack.object1, bipack.object2));
                        
                        groups = new AppGroupsManager(map);
                    }
                })
                .subscribe(pack -> {
                    installedApplications = pack.object1;
                    visibleApplications   = pack.object2;
                    hiddenApplications    = pack.object3;
                }));
    }
    
    public List<InstalledApplication> getVisibleApps () {
        return Collections.unmodifiableList(installedApplications);
    }
    
    public Set<InstalledApplication> getHiddenApps () {
        return Collections.unmodifiableSet(hiddenApplications);
    }
    
    // we need to update visible/hidden, since we won't get an update from the observable in AppsLoader
    public void hide (InstalledApplication application) {
        if (visibleApplications != null && visibleApplications.remove(application)) {
            hiddenApplications.add(application);
            application.setHidden(true);
            
            // I can do this synchronously, since an app is hidden by a command
            SharedPreferences.Editor editor = context.getSharedPreferences(HIDDEN_APPS_PREFS, Context.MODE_PRIVATE)
                    .edit();
            editor.putBoolean(application.getKey(), true);
            editor.apply();
        }
    }
    
    // we need to update visible/hidden, since we won't get an update from the observable in AppsLoader
    public void show (InstalledApplication application) {
        if (visibleApplications != null && hiddenApplications.remove(application)) {
            visibleApplications.add(application);
            application.setHidden(false);
            Collections.sort(visibleApplications);
            
            // I can do this synchronously, since an app is hidden by a command
            SharedPreferences.Editor editor = context.getSharedPreferences(HIDDEN_APPS_PREFS, Context.MODE_PRIVATE)
                    .edit();
            editor.putBoolean(application.getKey(), false);
            editor.apply();
        }
    }
    
    public void dispose () {
        disposable.dispose();
    }
}
