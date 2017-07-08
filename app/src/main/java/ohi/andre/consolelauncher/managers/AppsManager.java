package ohi.andre.consolelauncher.managers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.format.Time;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;

import static ohi.andre.consolelauncher.managers.XMLPrefsManager.VALUE_ATTRIBUTE;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.resetFile;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.set;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.writeTo;

public class AppsManager implements XMLPrefsManager.XmlPrefsElement {

    public static final int SHOWN_APPS = 10;
    public static final int HIDDEN_APPS = 11;

    public static final boolean USE_SCROLL_COMPARE = false;

    public static final String PATH = "apps.xml";
    private final String NAME = "APPS";

    private final String SHOW_ATTRIBUTE = "show";

    private Context context;
    private File folder;

    private AppsHolder appsHolder;
    private List<AppInfo> hiddenApps;

    private Outputable outputable;

    private final String PREFS = "apps";
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private static XMLPrefsManager.XmlPrefsElement instance = null;

    public enum Options implements XMLPrefsManager.XMLPrefsSave {

        default_app_n1 {
            @Override
            public String defaultValue() {
                return MOST_USED;
            }
        },
        default_app_n2 {
            @Override
            public String defaultValue() {
                return MOST_USED;
            }
        },
        default_app_n3 {
            @Override
            public String defaultValue() {
                return "com.android.vending";
            }
        },
        default_app_n4 {
            @Override
            public String defaultValue() {
                return NULL;
            }
        },
        default_app_n5 {
            @Override
            public String defaultValue() {
                return NULL;
            }
        };

        static final String MOST_USED = "most_used";
        static final String NULL = "null";

        @Override
        public String label() {
            return name();
        }

        @Override
        public XMLPrefsManager.XmlPrefsElement parent() {
            return instance;
        }

        @Override
        public boolean is(String s) {
            return name().equals(s);
        }
    }

    @Override
    public void write(XMLPrefsManager.XMLPrefsSave save, String value) {
        set(new File(Tuils.getFolder(), PATH), NAME, save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
    }

    @Override
    public XMLPrefsManager.XMLPrefsList getValues() {
        return null;
    }

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

    public AppsManager(Context context, Outputable outputable) {
        instance = this;

        this.context = context;
        this.outputable = outputable;

        this.preferences = context.getSharedPreferences(PREFS, 0);
        this.editor = preferences.edit();

        this.folder = Tuils.getFolder();

        fill();

        initAppListener(context);
    }

    private void initAppListener(Context c) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");

        c.registerReceiver(appsBroadcast, intentFilter);
    }

    public void fill() {
        Map<String, AppInfo> map = createAppMap(context.getPackageManager());
        List<AppInfo> shownApps = new ArrayList<>();
        hiddenApps = new ArrayList<>();

        XMLPrefsManager.XMLPrefsList values = new XMLPrefsManager.XMLPrefsList();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            File file = new File(folder, PATH);
            if(!file.exists() && !file.createNewFile()) return;

            Document d;
            try {
                d = builder.parse(file);
            } catch (Exception e) {
                resetFile(file, NAME);
                d = builder.parse(file);
            }

            List<AppsManager.Options> enums = new ArrayList<>(Arrays.asList(AppsManager.Options.values()));

            Element root = (Element) d.getElementsByTagName(NAME).item(0);
            if(root == null) {
                resetFile(file, NAME);

                d = builder.parse(file);
                root = (Element) d.getElementsByTagName(NAME).item(0);
            }

            NodeList nodes = root.getElementsByTagName("*");
            for(int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);

                String nn = node.getNodeName();
                int nodeIndex = Tuils.find(nn, (List) enums);
                if(nodeIndex != -1) {
                    values.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());

                    for(int en = 0; en < enums.size(); en++) {
                        if(enums.get(en).label().equals(nn)) {
                            enums.remove(en);
                            break;
                        }
                    }
                } else {
                    if(node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;

                        boolean shown = !e.hasAttribute(SHOW_ATTRIBUTE) || Boolean.parseBoolean(e.getAttribute(SHOW_ATTRIBUTE));
                        if(!shown) {
                            hiddenApps.add(map.remove(nn));
                        }
                    }
                }
            }

            if(enums.size() > 0) {
                for(XMLPrefsManager.XMLPrefsSave s : enums) {
                    Element em = d.createElement(s.label());
                    em.setAttribute(VALUE_ATTRIBUTE, s.defaultValue());
                    root.appendChild(em);

                    values.add(s.label(), s.defaultValue());
                }
                writeTo(d, file);
            }
        } catch (Exception e) {}

        for(Map.Entry<String, ?> entry : this.preferences.getAll().entrySet()) {
            if (entry.getValue() instanceof Integer) {
                AppInfo info = map.get(entry.getKey());
                if(info != null) info.launchedTimes = (Integer) entry.getValue();
            }
        }

        for (Map.Entry<String, AppInfo> stringAppInfoEntry : map.entrySet()) {
            AppInfo app = stringAppInfoEntry.getValue();
            shownApps.add(app);
        }

        appsHolder = new AppsHolder(shownApps, values);
        AppUtils.checkEquality(hiddenApps);
    }

    private Map<String, AppInfo> createAppMap(PackageManager mgr) {
        Map<String, AppInfo> map = new HashMap<>();

//        Intent i = new Intent(Intent.ACTION_MAIN, null);
//        i.addCategory(Intent.CATEGORY_LAUNCHER);
//        List<ResolveInfo> infos = mgr.queryIntentActivities(i, 0);
//
//        for (ResolveInfo info : infos) {
//            AppInfo app = new AppInfo(info.activityInfo.packageName, info.loadLabel(mgr).toString());
//            map.put(info.activityInfo.packageName, app);
//        }
//
//        return map;

        for(ApplicationInfo info : mgr.getInstalledApplications(0)){
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setAction(Intent.ACTION_MAIN);
            intent.setPackage(info.packageName);
            List<ResolveInfo> list = mgr.queryIntentActivities(intent, 0);

            for(ResolveInfo rInfo:list) {
                ActivityInfo activity = rInfo.activityInfo;

                Intent i = new Intent();
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setAction(Intent.ACTION_MAIN);
                i.setPackage(activity.packageName);
                i.setComponent(new ComponentName(activity.packageName, activity.name));
                ResolveInfo in = mgr.resolveActivity(i, 0);

                ActivityInfo aInfo = in.activityInfo;

                AppInfo app = new AppInfo(aInfo.packageName, aInfo.loadLabel(mgr).toString());
                map.put(aInfo.packageName, app);
            }
        }

        return map;
    }

    private void add(String packageName) {
        try {
            PackageManager manager = context.getPackageManager();
            ApplicationInfo info = manager.getApplicationInfo(packageName, 0);
            AppInfo app = new AppInfo(packageName, info.loadLabel(manager).toString(), 0);
            appsHolder.add(app);
            outputable.onOutput(context.getString(R.string.app_installed) + Tuils.SPACE + packageName);
        } catch (NameNotFoundException e) {}
    }

    private void remove(String packageName) {
        AppInfo info = AppUtils.findAppInfo(packageName, appsHolder.getApps());
        if(info != null) {
            appsHolder.remove(info);
            appsHolder.update(true);
        }
    }

//    this looks EVERYWHERE!
//    public String findPackage(String name) {
//        List<AppInfo> apps = appsHolder.getApps();
//        if(apps != null) {
//            apps.addAll(hiddenApps);
//            return findPackage(apps, null, name);
//        }
//        return null;
//    }

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

    public String findPackage(List<AppInfo> appList, List<String> labels, String name) {
        name = Compare.removeSpaces(name);
//        if(labels == null) {
//            labels = AppUtils.labelList(appList);
//        }

//        if(useCompareString) {
//            String label = Compare.similarString(labels, name, MIN_RATE, USE_SCROLL_COMPARE);
//            if (label == null) {
//                return null;
//            }
//
//            for(AppInfo info : appList) {
//                if (info.publicLabel.equals(name)) {
//                    return info.packageName;
//                }
//            }
//        } else {
            for(AppInfo info : appList) {
                if(name.equalsIgnoreCase(Compare.removeSpaces(Compare.removeSpaces(info.publicLabel)))) {
                    return info.packageName;
                }
            }
//        }

        return null;
    }

    public Intent getIntent(String packageName) {
        AppInfo info = AppUtils.findAppInfo(packageName, appsHolder.getApps());
        if(info == null) {
            return null;
        }

        info.launchedTimes++;
        appsHolder.requestSuggestionUpdate(info);

        editor.putInt(packageName, info.launchedTimes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }

        return context.getPackageManager().getLaunchIntentForPackage(packageName);
    }

    public String hideApp(String packageName) {
        AppInfo info = AppUtils.findAppInfo(packageName, appsHolder.getApps());
        if(info == null) {
            return null;
        }

        set(new File(folder, PATH), NAME, info.packageName, new String[] {SHOW_ATTRIBUTE}, new String[] {false + Tuils.EMPTYSTRING});

        appsHolder.remove(info);
        appsHolder.update(true);
        hiddenApps.add(info);
        AppUtils.checkEquality(hiddenApps);

        return info.publicLabel;
    }

    public String unhideApp(String packageName) {
        AppInfo info = AppUtils.findAppInfo(packageName, hiddenApps);
        if(info == null) {
            return null;
        }

        set(new File(folder, PATH), NAME, info.packageName, new String[] {SHOW_ATTRIBUTE}, new String[] {true + Tuils.EMPTYSTRING});

        hiddenApps.remove(info);
        appsHolder.add(info);
        appsHolder.update(false);

        return info.publicLabel;
    }

    public List<String> getAppLabels() {
        return appsHolder.getAppLabels();
    }

    public List<String> getHiddenAppsLabels() {
        return AppUtils.labelList(hiddenApps);
    }

    public String[] getSuggestedApps() {
        if(appsHolder == null) return new String[0];
        return appsHolder.getSuggestedApps();
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
                return (this.packageName == null && i.packageName == null) || (this.packageName != null && i.packageName != null && this.packageName.equals(i.packageName));
            } else if(o instanceof String) return this.packageName != null && this.packageName.equals(o);
            return false;
        }

        @Override
        public String toString() {
            return packageName + " - " + publicLabel + ", n=" + launchedTimes;
        }

        @Override
        public int hashCode() {
            return packageName.hashCode();
        }
    }

    private class AppsHolder {

        final int MOST_USED = 10, NULL = 11, USER_DEFINIED = 12;

        private List<AppInfo> infos;
        private List<String> appLabels;
        private XMLPrefsManager.XMLPrefsList values;

        private SuggestedAppMgr suggestedAppMgr;

        private class SuggestedAppMgr {
            private List<SuggestedApp> suggested;
            private int lastWriteable = -1;

            public SuggestedAppMgr(XMLPrefsManager.XMLPrefsList values) {
                suggested = new ArrayList<>();

                final String PREFIX = "default_app_n";
                for(int count = 0; count < Options.values().length; count++) {
                    String vl = values.get(Options.valueOf(PREFIX + (count + 1))).value;

                    if(vl.equals(Options.NULL)) continue;
                    if(vl.equals(Options.MOST_USED)) suggested.add(new SuggestedApp(MOST_USED));
                    else {
                        AppInfo info = AppUtils.findAppInfo(vl, infos);
                        if(info == null) continue;
                        suggested.add(new SuggestedApp(info, USER_DEFINIED));
                    }
                }

                sort();

                handler.postDelayed(runnable, 1000 * 60 * 5);
            }

            public int size() {
                return suggested.size();
            }

            private void sort() {
                Collections.sort(suggested);
                for(int count = 0; count < suggested.size(); count++) {
                    if(suggested.get(count).type != MOST_USED) {
                        lastWriteable = count - 1;
                        return;
                    }
                }
                lastWriteable = suggested.size() - 1;
            }

            public SuggestedApp get(int index) {
                return suggested.get(index);
            }

            public void set(int index, AppInfo info) {
                suggested.get(index).change(info);
            }

            public int indexOf(AppInfo info) {
                return suggested.indexOf(info);
            }

            public void attemptInsertSuggestion(AppInfo info) {
//                Log.e("andre", "attempt: " + info.toString());

                if (info.launchedTimes == 0 || lastWriteable == -1) {
                    return;
                }

//                Log.e("andre", String.valueOf(lastWriteable));

                int i = Tuils.find(info, suggested);
                if(i == -1) {
                    int index = indexOf(info);
                    if (index == -1) {
                        for (int count = 0; count <= lastWriteable; count++) {
//                            Log.e("andre", "loop: " + count);

                            SuggestedApp app = get(count);
//                            Log.e("andre", app.toString());

                            if (app.app == null || info.launchedTimes > app.app.launchedTimes) {
//                                Log.e("andre", "yes");
                                SuggestedApp s = suggested.get(count);

//                                Log.e("andre", "before it was: " + s.toString());
//                                Log.e("andre", suggested.toString());

                                AppInfo before = s.app;
                                s.change(info);

//                                Log.e("andre", suggested.toString());

                                if(before != null) {
//                                    Log.e("andre", "rec");
                                    attemptInsertSuggestion(before);
                                }

                                break;
                            }
                        }
                    }
                }
//                else {
//                    Log.e("andre", "index == -1");
//                    Log.e("andre", suggested.toString());
//                }
                sort();
//                Log.e("andre", suggested.toString());
            }

//            public void updateSuggestion(AppInfo info) {
//                int index = indexOf(info);
//
//                if(index == -1) {
//                    attemptInsertSuggestion(info);
//                } else if(index == 0) {
//                    return;
//                } else {
//                    for(int count = 0; count < index; count++) {
//                        if(get(count).type == NULL ) {
//                            if(count == lastNull()) {
//                                suggestedApps[count] = info;
//                                return;
//                            }
//                        } else if(suggestedApps[count].launchedTimes < info.launchedTimes) {
//
//                            System.arraycopy(suggestedApps, count, suggestedApps, count + 1, index - count);
//                            suggestedApps[count] = info;
//
//                            return;
//                        }
//                    }
//                }
//            }

            public List<String> labels() {
                List<AppInfo> list = new ArrayList<>();
                for(int count = 0; count < suggested.size(); count++) {
                    SuggestedApp app = suggested.get(count);
                    if(app.type != NULL && app.app != null) list.add(app.app);
                }
                return AppUtils.labelList(list);
            }

            private Handler handler = new Handler();
            private Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if(duplicates()) {
                        fillSuggestions();
                    }
                    handler.postDelayed(runnable, 1000 * 60 * 2);
                }

                private boolean duplicates() {
                    for (int count =0; count < size(); count++)
                        for (int count2 = count+1 ; count2 < size(); count2++)
                            if (count != count2 && get(count) == get(count2))
                                return true;
                    return false;
                }
            };

            private class SuggestedApp implements Comparable {
                int type;
                AppInfo app;

                public SuggestedApp(int type) {
                    this(null, type);
                }

                public SuggestedApp(AppInfo info, int type) {
                    this.app = info;
                    this.type = type;
                }

                public SuggestedApp change(AppInfo info) {
                    this.app = info;
                    return this;
                }

                @Override
                public boolean equals(Object o) {
                    if(o instanceof SuggestedApp) {
                        try {
                            return (app == null && ((SuggestedApp) o).app == null) || app.equals(((SuggestedApp) o).app);
                        } catch (NullPointerException e) {
                            return false;
                        }
                    } else if(o instanceof AppInfo) {
                        if(app == null) return false;
                        return app.equals(o);
                    }
                    return false;
                }

                @Override
                public int compareTo(@NonNull Object o) {
                    SuggestedApp other = (SuggestedApp) o;

                    if(this.type == USER_DEFINIED || other.type == USER_DEFINIED) {
                        if(this.type == USER_DEFINIED && other.type == USER_DEFINIED) return other.app.launchedTimes - this.app.launchedTimes;
                        if(this.type == USER_DEFINIED) return 1;
                        return -1;
                    }

//                    most_used
                    if(this.app == null || other.app == null) {
                        if(this.app == null && other.app == null) return 0;
                        if(this.app == null) return 1;
                        return -1;
                    }
                    return this.app.launchedTimes - other.app.launchedTimes;
                }

                @Override
                public String toString() {
                    switch (type) {
                        case USER_DEFINIED:
                            return "userdef " + (app != null ? app.packageName : "");
                        case MOST_USED:
                            return "most used " + (app != null ? app.packageName : "");
                        case NULL:
                            return "null";
                    }

                    return null;
                }
            }
        }

        Comparator<AppInfo> mostUsedComparator = new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                return rhs.launchedTimes > lhs.launchedTimes ? -1 : rhs.launchedTimes == lhs.launchedTimes ? 0 : 1;
            }
        };

        public AppsHolder(List<AppInfo> infos, XMLPrefsManager.XMLPrefsList values) {
            this.infos = infos;
            this.values = values;
            update(true);
        }

        public void add(AppInfo info) {
            if(! infos.contains(info) ) {
                infos.add(info);
                update(false);
            }
        }

        public void remove(AppInfo info) {
            infos.remove(info);
            update(true);
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
            suggestedAppMgr = new SuggestedAppMgr(values);
            for(AppInfo info : infos) {
                suggestedAppMgr.attemptInsertSuggestion(info);
            }
        }

        public void requestSuggestionUpdate(AppInfo info) {
            suggestedAppMgr.attemptInsertSuggestion(info);
        }

//        private int lastNull() {
//            for(int count = suggestedApps.length - 1; count >= 0; count--) {
//                if(suggestedApps[count] == null) {
//                    return count;
//                }
//            }
//            return -1;
//        }

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
            List<String> ls = suggestedAppMgr.labels();
            return ls.toArray(new String[ls.size()]);
        }
    }

    public static class AppUtils {

        public static void checkEquality(List<AppInfo> list) {

            for (AppInfo info : list) {

                if(info == null || info.publicLabel == null) {
                    continue;
                }

                for (int count = 0; count < list.size(); count++) {
                    AppInfo info2 = list.get(count);

                    if(info2 == null || info2.publicLabel == null) {
                        continue;
                    }

                    if(info == info2) {
                        continue;
                    }

                    if (info.publicLabel.toLowerCase().replace(Tuils.SPACE, Tuils.EMPTYSTRING).equals(info2.publicLabel.toLowerCase().replace(Tuils.SPACE, Tuils.EMPTYSTRING))) {
                        list.set(count, new AppInfo(info2.packageName, getNewLabel(info2.publicLabel, info2.packageName), info2.launchedTimes));
                    }
                }
            }
        }

        public static String getNewLabel(String oldLabel, String packageName) {
            try {

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

        public static String format(PackageInfo info) {
            StringBuilder builder = new StringBuilder();

            builder.append(info.packageName).append(Tuils.NEWLINE);
            builder.append("vrs: ").append(info.versionCode).append(" - ").append(info.versionName).append(Tuils.NEWLINE).append(Tuils.NEWLINE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                Time time = new Time();
                time.set(info.firstInstallTime);
                builder.append("Install: ").append(time.format(XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.time_format))).append(Tuils.NEWLINE).append(Tuils.NEWLINE);
            }

            ActivityInfo[] a = info.activities;
            if(a != null && a.length > 0) {
                List<String> as = new ArrayList<>();
                for(ActivityInfo i : a) as.add(i.name.replace(info.packageName, Tuils.EMPTYSTRING));
                builder.append("Activities: ").append(Tuils.NEWLINE).append(Tuils.toPlanString(as, Tuils.NEWLINE)).append(Tuils.NEWLINE).append(Tuils.NEWLINE);
            }

            ServiceInfo[] s = info.services;
            if(s != null && s.length > 0) {
                List<String> ss = new ArrayList<>();
                for(ServiceInfo i : s) ss.add(i.name.replace(info.packageName, Tuils.EMPTYSTRING));
                builder.append("Services: ").append(Tuils.NEWLINE).append(Tuils.toPlanString(ss, Tuils.NEWLINE)).append(Tuils.NEWLINE).append(Tuils.NEWLINE);
            }

            ActivityInfo[] r = info.receivers;
            if(r != null && r.length > 0) {
                List<String> rs = new ArrayList<>();
                for(ActivityInfo i : r) rs.add(i.name.replace(info.packageName, Tuils.EMPTYSTRING));
                builder.append("Receivers: ").append(Tuils.NEWLINE).append(Tuils.toPlanString(rs, Tuils.NEWLINE)).append(Tuils.NEWLINE).append(Tuils.NEWLINE);
            }

            String[] p = info.requestedPermissions;
            if(p != null && p.length > 0) {
                List<String> ps = new ArrayList<>();
                for(String i : p) ps.add(i.substring(i.lastIndexOf(".") + 1));
                builder.append("Permissions: ").append(Tuils.NEWLINE).append(Tuils.toPlanString(ps, ", "));
            }

            return builder.toString();
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
            Collections.sort(labels);
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