package ohi.andre.consolelauncher.managers;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;

public class AppsManager {

    public static final int SHOWN_APPS = 10;
    public static final int HIDDEN_APPS = 11;

    public static final int MIN_RATE = 5;
    public static final boolean USE_SCROLL_COMPARE = false;

    private final int SUGGESTED_APPS_LENGTH = 5;

    private final String APPS_PREFERENCES = "appsPreferences";

    private Context context;
    private SharedPreferences.Editor prefsEditor;
    private PackageManager mgr;

    private AppsHolder appsHolder;
    private List<AppInfo> hiddenApps;

    private Outputable outputable;

    private boolean useCompareString;

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

    public AppsManager(Context context, boolean useCompareString, Outputable outputable) {
        this.context = context;
        this.mgr = context.getPackageManager();
        this.useCompareString = useCompareString;

        this.outputable = outputable;

        SharedPreferences preferences = context.getSharedPreferences(APPS_PREFERENCES, Context.MODE_PRIVATE);
        prefsEditor = preferences.edit();
        fill(preferences);

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
        Map<String, AppInfo> map = createAppMap(mgr);
        List<AppInfo> shownApps = new ArrayList<>();
        hiddenApps = new ArrayList<>();

        Map<String, ?> values;
        try {
            values = preferences.getAll();
        } catch (Exception e) {
            for(Map.Entry<String, AppInfo> entry : map.entrySet()) {
                shownApps.add(entry.getValue());
            }
            appsHolder = new AppsHolder(shownApps);
            return;
        }

        for(Map.Entry<String, ?> entry : values.entrySet()) {
            if(entry.getValue() instanceof Boolean) {
                if((Boolean) entry.getValue()) {
                    AppInfo info = map.get(entry.getKey());
                    hiddenApps.add(info);
                    map.remove(entry.getKey());
                }
            } else {
                AppInfo info = map.get(entry.getKey());
                if(info == null) {
                    continue;
                } else {
                    info.launchedTimes = (Integer) entry.getValue();
                }
            }
        }

        for (Map.Entry<String, AppInfo> stringAppInfoEntry : map.entrySet()) {
            AppInfo app = stringAppInfoEntry.getValue();
            shownApps.add(app);
        }

        appsHolder = new AppsHolder(shownApps);
        AppUtils.checkEquality(hiddenApps);
    }

    private Map<String, AppInfo> createAppMap(PackageManager mgr) {
        Map<String, AppInfo> map = new HashMap<>();

        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = mgr.queryIntentActivities(i, 0);

        for (ResolveInfo info : infos) {
            AppInfo app = new AppInfo(info.activityInfo.packageName, info.loadLabel(mgr).toString());
            map.put(info.activityInfo.packageName, app);
        }

        return map;
    }

    private void add(String packageName) {
        try {
            ApplicationInfo info = mgr.getApplicationInfo(packageName, 0);
            AppInfo app = new AppInfo(packageName, info.loadLabel(mgr).toString(), 0);
            appsHolder.add(app);
            outputable.onOutput(context.getString(R.string.app_installed) + Tuils.SPACE + packageName);
        } catch (NameNotFoundException e) {}
    }

    private void remove(String packageName) {
        appsHolder.remove(packageName);
    }

    public String findPackage(String name) {
        List<AppInfo> apps = appsHolder.getApps();
        if(apps != null) {
            apps.addAll(hiddenApps);
            return findPackage(apps, null, name);
        }
        return null;
    }

    public String findPackage(List<AppInfo> appList, List<String> labels, String name) {
        name = Compare.removeSpaces(name).toLowerCase();
        if(labels == null) {
            labels = AppUtils.labelList(appList);
        }

        if(useCompareString) {
            String label = Compare.similarString(labels, name, MIN_RATE, USE_SCROLL_COMPARE);
            if (label == null) {
                return null;
            }

            for(AppInfo info : appList) {
                if (info.publicLabel.equals(name)) {
                    return info.packageName;
                }
            }
        } else {
            for(AppInfo info : appList) {
                if(name.equals(Compare.removeSpaces(info.publicLabel.toLowerCase()))) {
                    return info.packageName;
                }
            }
        }

        return null;
    }

    public String findPackage(String name, int type) {
        List<AppInfo> appList;
        List<String> labelList;
        if(type == SHOWN_APPS) {
            appList = appsHolder.getApps();
            labelList = appsHolder.getAppLabels();
        } else {
            appList = hiddenApps;
            labelList = AppUtils.labelList(appList);
        }

        return findPackage(appList, labelList, name);
    }

    public Intent getIntent(String packageName) {
        AppInfo info = AppUtils.findAppInfo(packageName, appsHolder.getApps());
        if(info == null) {
            return null;
        }

        info.launchedTimes++;
        appsHolder.updateSuggestion(info);

        prefsEditor.putInt(packageName, info.launchedTimes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            applyPrefs();
        } else {
            prefsEditor.commit();
        }

        return mgr.getLaunchIntentForPackage(packageName);
    }

    public String hideApp(String packageName) {
        AppInfo info = AppUtils.findAppInfo(packageName, appsHolder.getApps());
        if(info == null) {
            return null;
        }

        appsHolder.remove(packageName);
        hiddenApps.add(info);
        AppUtils.checkEquality(hiddenApps);

        prefsEditor.putBoolean(packageName, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            applyPrefs();
        } else {
            prefsEditor.commit();
        }

        return info.publicLabel;
    }

    public String unhideApp(String packageName) {

        AppInfo info = AppUtils.findAppInfo(packageName, hiddenApps);
        if(info == null) {
            return null;
        }

        hiddenApps.remove(info);
        appsHolder.add(info);

        prefsEditor.putBoolean(packageName, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            applyPrefs();
        } else {
            prefsEditor.commit();
        }

        return info.publicLabel;
    }

    public List<String> getAppLabels() {
        return appsHolder.getAppLabels();
    }

    public String[] getSuggestedApps() {
//        workaround
        String[] temp = appsHolder.getSuggestedApps();
        if(temp == null || temp.length == 0) {
            return null;
        }

        String[] apps = new String[temp.length];
        for(int countOnTemp = temp.length - 1, countOnApps = 0; countOnTemp >= 0 && countOnApps < apps.length; countOnApps++, countOnTemp--) {
            apps[countOnApps] = temp[countOnTemp];
        }

        return apps;
    }

    public String printApps(int type) {
        List<String> labels = type == SHOWN_APPS ? appsHolder.appLabels : AppUtils.labelList(hiddenApps);
        return AppUtils.printApps(labels);
    }

    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(appsBroadcast);
    }

    public void onDestroy() {
        unregisterReceiver(context);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void applyPrefs() {
        prefsEditor.apply();
    }

    public static class AppInfo {

        public String packageName;
        public String publicLabel;
        public int launchedTimes;

        public AppInfo(String packageName, String publicLabel, int launchedTimes) {
            this.packageName = packageName;
            this.publicLabel = publicLabel;
            this.launchedTimes = launchedTimes;
        }

        public AppInfo(String packageName, String publicLabel) {
            this.packageName = packageName;
            this.publicLabel = publicLabel;
        }

        @Override
        public boolean equals(Object o) {
            if(o == null) {
                return false;
            }

            if(o instanceof AppInfo) {
                AppInfo i = (AppInfo) o;
                return this.packageName.equals(i.packageName);
            } else if(o instanceof String) {
                return this.packageName.equals(o);
            }
            return false;
        }

        @Override
        public String toString() {
            return packageName + " - " + publicLabel;
        }

        @Override
        public int hashCode() {
            return packageName.hashCode();
        }
    }

    private class AppsHolder {

        private List<AppInfo> infos;
        private List<String> appLabels;
        private AppInfo[] suggestedApps = new AppInfo[SUGGESTED_APPS_LENGTH];

        Comparator<AppInfo> mostUsedComparator = new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                return rhs.launchedTimes > lhs.launchedTimes ? -1 : rhs.launchedTimes == lhs.launchedTimes ? 0 : 1;
            }
        };

        public AppsHolder(List<AppInfo> infos) {
            this.infos = infos;
            update(true);
        }

        public void add(AppInfo info) {
            infos.add(info);
            update(false);
        }

        public void remove(String packageName) {
            infos.remove(packageName);
            update(true);
        }

        public void updateSuggestion(AppInfo app) {
            int index = suggestionIndex(app);
            if(index != -1) {
                for(int count = suggestedApps.length - 1; count > index; count--) {
                    AppInfo cycleInfo = suggestedApps[count];
                    if(cycleInfo == null) {
//                        this should not happen
                        throw new UnsupportedOperationException("suggestion is null");
                    }

                    if(app.launchedTimes > cycleInfo.launchedTimes) {
                        suggestedApps[index] = cycleInfo;
                        suggestedApps[count] = app;
                        return;
                    }
                }
            } else {
                for(int count = suggestedApps.length - 1; count >= 0; count--) {
                    AppInfo cycleInfo = suggestedApps[count];
                    if(cycleInfo == null || app.launchedTimes > cycleInfo.launchedTimes) {
                        System.arraycopy(suggestedApps, 1, suggestedApps, 0, count);
                        suggestedApps[count] = app;
                        return;
                    }
                }
            }
        }

        private int suggestionIndex(AppInfo app) {
            for(int count = 0; count < suggestedApps.length; count++) {
                if(app.equals(suggestedApps[count])) {
                    return count;
                }
            }
            return -1;
        }

        private void sort() {
            try {
                Collections.sort(infos, mostUsedComparator);
            } catch (NullPointerException e) {}
        }

        private void fillLabels() {
            appLabels = AppUtils.labelList(infos);
        }

        private void fillSuggestions() {
            for(AppInfo info : infos) {
                if(info.launchedTimes == 0){
                    continue;
                }
                for(int count = suggestedApps.length - 1; count >= 0; count--) {
                    AppInfo i = suggestedApps[count];
                    if(i == null) {
                        suggestedApps[count] = info;
                        break;
                    } else if(i.launchedTimes < info.launchedTimes) {
                        suggestedApps[count] = info;
                        if(count < suggestedApps.length - 1) {
                            suggestedApps[count + 1] = i;
                        }
                        break;
                    }
                }
            }
        }

        private void update(boolean refreshSuggestions) {
            AppUtils.checkEquality(infos);
            sort();
            fillLabels();
            if(refreshSuggestions) {
                fillSuggestions();
            }
        }

        public List<String> getAppLabels() {
            return appLabels;
        }

        public List<AppInfo> getApps() {
            return infos;
        }

        public String[] getSuggestedApps() {
            return AppUtils.labelList(suggestedApps);
        }

    }

    private static class AppUtils {

        public static void checkEquality(List<AppInfo> list) {

            First:
            for (AppInfo info : list) {

                Second:
                for (int count = 0; count < list.size(); count++) {
                    AppInfo info2 = list.get(count);

                    if(info == null || info.publicLabel == null) {
                        continue First;
                    }

                    if(info2 == null || info2.publicLabel == null) {
                        continue Second;
                    }

                    if (info.publicLabel.toLowerCase().replace(Tuils.SPACE, Tuils.EMPTYSTRING).equals(info2.publicLabel.replace(Tuils.SPACE, Tuils.EMPTYSTRING))) {
                        list.set(count, new AppInfo(info2.packageName, getNewLabel(info2.publicLabel, info2.packageName), info2.launchedTimes));
                    }
                }
            }
        }

        public static String getNewLabel(String oldLabel, String packageName) {
            try {

//                              OLD VERSION OF this method
//
//                int firstDot = packageName.indexOf(Tuils.DOT) + 1;
//                int secondDot = packageName.substring(firstDot).indexOf(Tuils.DOT) + firstDot;
//
//                StringBuilder newLabel = new StringBuilder();
//                if (firstDot == -1) {
//                    newLabel.append(packageName);
//                    newLabel.append(Tuils.SPACE);
//                    newLabel.append(oldLabel);
//                } else if (secondDot == -1) {
//                    newLabel.append(packageName.substring(firstDot, packageName.length()));
//                    newLabel.append(Tuils.SPACE);
//                    newLabel.append(oldLabel);
//                } else {
//                    newLabel.append(packageName.substring(firstDot, secondDot));
//                    newLabel.append(Tuils.SPACE);
//                    newLabel.append(oldLabel);
//                }
//
//                String label = newLabel.toString();
//                return label.substring(0, 1).toUpperCase() + label.substring(1);

                int firstDot = packageName.indexOf(Tuils.DOT);
                if(firstDot == -1) {
//                    no dots in package name (nearly impossible)
                    return packageName;
                }
                firstDot++;

                int secondDot = packageName.substring(firstDot).indexOf(Tuils.DOT);
                String prefix;
                if(secondDot == -1) {
//                    only one dot, so two words. The first is most likely to be the company name
//                    facebook.messenger
//                    is better than
//                    messenger.facebook
                    prefix = packageName.substring(0, firstDot - 1);
                    prefix = prefix.substring(0,1).toUpperCase() + prefix.substring(1).toLowerCase();
                    return prefix + Tuils.SPACE + oldLabel;
                } else {
//                    two dots or more, the second word is the company name
                    prefix = packageName.substring(firstDot, secondDot + firstDot);
                    prefix = prefix.substring(0,1).toUpperCase() + prefix.substring(1).toLowerCase();
                    return prefix + Tuils.SPACE + oldLabel;
                }

            } catch (Exception e) {
                return packageName;
            }
        }

        protected static AppInfo findAppInfo(String packageName, List<AppInfo> infos) {
            for(AppInfo info : infos) {
                if(info.packageName.equals(packageName)) {
                    return info;
                }
            }
            return null;
        }

        public static String printApps(List<String> apps) {
            if(apps.size() == 0) {
                return apps.toString();
            }

            List<String> list = new ArrayList<>(apps);

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

        public static List<String> labelList(List<AppInfo> infos) {
            List<String> labels = new ArrayList<>();
            for (AppInfo info : infos) {
                labels.add(info.publicLabel);
            }
            return labels;
        }

        public static String[] labelList(AppInfo[] infos) {
            String[] labels = new String[infos.length];
            for(int count = 0; count < infos.length; count++) {
                if(infos[count] != null) {
                    labels[count] = infos[count].publicLabel;
                }
            }
            return labels;
        }
    }

}