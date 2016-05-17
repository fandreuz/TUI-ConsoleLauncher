package ohi.andre.consolelauncher.managers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.tuils.AppInfo;
import ohi.andre.consolelauncher.tuils.Tuils;

public class AppsManager {

    public static final int MIN_RATE = 5;
    public static final boolean USE_SCROLL_COMPARE = false;
    private static final String HIDDENAPP_KEY = "hiddenapp_";
    private static final String HIDDENAPP_N_KEY = "hiddenapp_n";
    private PackageManager mgr;

    private Set<AppInfo> apps;
    private Set<AppInfo> hiddenApps;

    private BroadcastReceiver appsBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String data = intent.getData().getSchemeSpecificPart();
            if (action.equals(Intent.ACTION_PACKAGE_ADDED))
                add(data);
            else
                remove(data);
        }
    };

    //    constructor
    public AppsManager(Context context) {
        mgr = context.getPackageManager();

        fill(((Activity) context).getPreferences(0));

        initAppListener(context);
    }

    private void initAppListener(Context c) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");

        c.registerReceiver(appsBroadcast, intentFilter);
    }

    public void fill(SharedPreferences preferences) {
        apps = Tuils.getApps(mgr);

        Set<String> hiddenPackages = readHiddenApps(preferences);
        hiddenApps = new HashSet<>();

//        remove hidden apps from apps & store coincidences in hiddenApps
        Iterator<AppInfo> it = apps.iterator();
        while (it.hasNext()) {
            AppInfo app = it.next();
            if (hiddenPackages.contains(app.packageName)) {
                hiddenApps.add(app);
                it.remove();
            }
        }

//        add company name if necessary (Google/FaceBook Messenger)
        apps = checkEquality(apps);
        hiddenApps = checkEquality(hiddenApps);
    }

    //    add a new app (onPackageAdded listener)
    private void add(String packageName) {
        try {
            ApplicationInfo info = mgr.getApplicationInfo(packageName, 0);
            AppInfo app = new AppInfo(packageName, info.loadLabel(mgr).toString());
            apps.add(app);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        apps = checkEquality(apps);
    }

    //    as below, but remove
    private void remove(String packageName) {
        Iterator<AppInfo> it = apps.iterator();

        AppInfo app;
        while (it.hasNext()) {
            app = it.next();
            if (app.packageName.equals(packageName)) {
                it.remove();
                break;
            }
        }
    }

    //    find a package using its public label
//    notice that it can be an app or an hidden app (depends on appList parameter)
    public String findPackage(Set<AppInfo> appList, String name) {
        String label = Compare.getOneSimilarString(labelSet(appList), name, MIN_RATE, USE_SCROLL_COMPARE);
        if (label == null)
            return null;

        Iterator<AppInfo> it = appList.iterator();
        AppInfo app;
        while (it.hasNext()) {
            app = it.next();
            if (app.publicLabel.equals(label))
                return app.packageName;
        }

        return null;
    }

    public String findPackage(String name) {
        Set<AppInfo> allApps = new HashSet<>(apps);
        allApps.addAll(hiddenApps);
        return findPackage(allApps, name);
    }

    //    find the Application intent from an input
    public Intent getIntent(String packageName) {
        return mgr.getLaunchIntentForPackage(packageName);
    }

    //    read hidden apps
    private Set<String> readHiddenApps(SharedPreferences preferences) {
        int n = preferences.getInt(HIDDENAPP_N_KEY, 0);

        Set<String> hiddenPackages = new HashSet<>();
        for (int count = 0; count < n; count++)
            hiddenPackages.add(preferences.getString(HIDDENAPP_KEY + count, null));
        return hiddenPackages;
    }

    //    store hidden apps in shared preferences
    private void storeHiddenApps(SharedPreferences.Editor editor, Set<String> hiddenPackages) {
        int n = hiddenPackages.size();

        editor.putInt(HIDDENAPP_N_KEY, n);

        Iterator<String> it = hiddenPackages.iterator();
        for (int count = 0; count < n; count++)
            editor.putString(HIDDENAPP_KEY + count, it.next());
    }

    //    hide an app
    public String hideApp(SharedPreferences.Editor editor, String packageName) {
        Iterator<AppInfo> it = apps.iterator();
        while (it.hasNext()) {
            AppInfo i = it.next();
            if (i.packageName.equals(packageName)) {
                it.remove();
                hiddenApps.add(i);
                hiddenApps = checkEquality(hiddenApps);

                storeHiddenApps(editor, packageSet(hiddenApps));

                return i.publicLabel;
            }
        }

        return null;
    }

    //    unhide an app
    public String unhideApp(SharedPreferences.Editor editor, String packageName) {

        Iterator<AppInfo> it = hiddenApps.iterator();
        while (it.hasNext()) {
            AppInfo i = it.next();
            if (i.packageName.equals(packageName)) {
                it.remove();
                apps.add(i);

                storeHiddenApps(editor, packageSet(hiddenApps));

                return i.publicLabel;
            }
        }

        return null;
    }

    //    return a set of labels in AppInfos
    private Set<String> labelSet(Set<AppInfo> infos) {
        Set<String> set = new HashSet<>();

        Iterator<AppInfo> it = infos.iterator();
        while (it.hasNext())
            set.add(it.next().publicLabel);

        return set;
    }

    //    return a set of packages in AppInfos
    private Set<String> packageSet(Set<AppInfo> infos) {
        Set<String> set = new HashSet<>();

        Iterator<AppInfo> it = infos.iterator();
        while (it.hasNext())
            set.add(it.next().packageName);

        return set;
    }

    public String printApps() {
        List<String> list = new ArrayList<>(labelSet(apps));

        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return Compare.alphabeticCompare(lhs, rhs);
            }
        });

        Tuils.addPrefix(list, Tuils.DOUBLE_SPACE);
        Tuils.insertHeaders(list, false);
        return Tuils.toPlanString(list);
    }

    public Set<AppInfo> getApps() {
        return apps;
    }

    public Set<String> getAppsLabels() {
        return labelSet(apps);
    }

    public String printHiddenApps() {
        List<String> list = new ArrayList<>(labelSet(hiddenApps));

        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return Compare.alphabeticCompare(lhs, rhs);
            }
        });

        Tuils.addPrefix(list, "  ");
        Tuils.insertHeaders(list, false);
        return Tuils.toPlanString(list);
    }

    private Set<AppInfo> checkEquality(Set<AppInfo> appInfoSet) {
        List<AppInfo> list = new ArrayList<>(appInfoSet);

        for (Iterator<AppInfo> iterator = appInfoSet.iterator(); iterator.hasNext(); ) {
            AppInfo info = iterator.next();

            for (int count = 0; count < list.size(); count++) {
                AppInfo info2 = list.get(count);
                if (!info.equals(info2) && info.publicLabel.equals(info2.publicLabel))
                    list.set(count, new AppInfo(info2.packageName, getNewLabel(info2.publicLabel, info2.packageName)));
            }
        }

        return new HashSet<>(list);
    }

    private String getNewLabel(String oldLabel, String packageName) {
        try {
            int firstDot = packageName.indexOf(".") + 1;
            int secondDot = packageName.substring(firstDot).indexOf(".") + firstDot;

            StringBuilder newLabel = new StringBuilder();
            if (firstDot == -1) {
                newLabel.append(packageName);
                newLabel.append(Tuils.SPACE);
                newLabel.append(oldLabel);
            } else if (secondDot == -1) {
                newLabel.append(packageName.substring(firstDot, packageName.length()));
                newLabel.append(Tuils.SPACE);
                newLabel.append(oldLabel);
            } else {
                newLabel.append(packageName.substring(firstDot, secondDot));
                newLabel.append(Tuils.SPACE);
                newLabel.append(oldLabel);
            }

            String label = newLabel.toString();
            return label.substring(0, 1).toUpperCase() + label.substring(1);
        } catch (IndexOutOfBoundsException e) {
            return packageName;
        }
    }

    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(appsBroadcast);
    }

}