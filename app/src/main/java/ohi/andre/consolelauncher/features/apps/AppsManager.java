package ohi.andre.consolelauncher.features.apps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import it.andreuzzi.comparestring2.StringableObject;
import ohi.andre.consolelauncher.MainManager;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.features.TimeManager;
import ohi.andre.consolelauncher.features.settings.SettingsEntriesContainer;
import ohi.andre.consolelauncher.features.settings.SettingsManager;
import ohi.andre.consolelauncher.features.settings.SettingsOption;
import ohi.andre.consolelauncher.features.settings.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.features.settings.options.Apps;
import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.ui.UIManager;

import static ohi.andre.consolelauncher.features.settings.SettingsManager.VALUE_ATTRIBUTE;
import static ohi.andre.consolelauncher.features.settings.SettingsManager.resetFile;
import static ohi.andre.consolelauncher.features.settings.SettingsManager.set;
import static ohi.andre.consolelauncher.features.settings.SettingsManager.writeTo;

public class AppsManager implements XMLPrefsElement {

    public static final int SHOWN_APPS = 10;
    public static final int HIDDEN_APPS = 11;

    public static final String PATH = "apps.xml";
    private final String NAME = "APPS";
    private File file;

    private final String SHOW_ATTRIBUTE = "show", APPS_ATTRIBUTE = "apps", BGCOLOR_ATTRIBUTE = "bgColor", FORECOLOR_ATTRIBUTE = "foreColor";
    public static final String APPS_SEPARATOR = ";";
    
    // the name of the SharedPreferences file which holds the launch times of each application
    public static final String APPS_LAUNCHTIMES_FILENAME = "apps_launchtimes";

    private Context context;

    private AppsHolder                 appsHolder;
    private List<InstalledApplication> hiddenApps;

    private final String PREFS = "apps";

    public static XMLPrefsElement instance = null;

    private SettingsEntriesContainer prefsList;

    public List<Group> groups;

    @Override
    public String[] delete() {
        return null;
    }

    @Override
    public void write(SettingsOption save, String value) {
        set(new File(Tuils.getFolder(), PATH), save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
    }

    @Override
    public String path() {
        return PATH;
    }

    @Override
    public SettingsEntriesContainer getValues() {
        return prefsList;
    }

    public AppsManager(final Context context) {
        this.context = context;

        File root = Tuils.getFolder();
        if(root == null) this.file = null;
        else this.file = new File(root, PATH);

        this.groups = new ArrayList<>();

        initAppListener();
    
        Executor loaderThread = Executors.newSingleThreadExecutor();
        loaderThread.execute(() -> {
            fill();
            LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(new Intent(UIManager.ACTION_UPDATE_SUGGESTIONS));
        });
    }

    public InstalledApplication findLaunchInfoWithLabel(String label, int type) {
        if(appsHolder == null) return null;

        List<InstalledApplication> appList;
        if(type == SHOWN_APPS) {
            appList = appsHolder.getApps();
        } else {
            appList = hiddenApps;
        }

        if(appList == null) return null;

        InstalledApplication i = AppUtils.findLaunchInfoWithLabel(appList, label);
        if(i != null) {
            return i;
        }

        List<InstalledApplication> is = AppUtils.findLaunchInfosWithPackage(label, appList);
        if(is == null || is.size() == 0) return null;
        return is.get(0);
    }

    public void writeLaunchTimes(InstalledApplication info) {
        editor.putInt(info.write(), info.launchedTimes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }

        if(appsHolder != null) appsHolder.update(true);
    }

    public Intent getIntent(final InstalledApplication info) {
        info.launchedTimes++;
        new StoppableThread() {
            @Override
            public void run() {
                super.run();

                appsHolder.requestSuggestionUpdate(info);
                writeLaunchTimes(info);
            }
        }.start();

       return new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(info.componentName)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }

    public String hideActivity(InstalledApplication info) {
        set(file, info.write(), new String[] {SHOW_ATTRIBUTE}, new String[] {false + Tuils.EMPTYSTRING});

        appsHolder.remove(info);
        appsHolder.update(true);
        hiddenApps.add(info);
        AppUtils.checkEquality(hiddenApps);

        return info.publicLabel;
    }

    public String showActivity(InstalledApplication info) {
        set(file, info.write(), new String[]{SHOW_ATTRIBUTE}, new String[]{true + Tuils.EMPTYSTRING});

        hiddenApps.remove(info);
        appsHolder.add(info);
        appsHolder.update(false);

        return info.publicLabel;
    }

    public String createGroup(String name) {
        int index = Tuils.find(name, groups);
        if(index == -1) {
            groups.add(new Group(name));
            return SettingsManager.set(file, name, new String[]{APPS_ATTRIBUTE}, new String[]{Tuils.EMPTYSTRING});
        }

        return context.getString(R.string.output_groupexists);
    }

    public String groupBgColor(String name, String color) {
        int index = Tuils.find(name, groups);
        if(index == -1) {
            return context.getString(R.string.output_groupnotfound);
        }

        groups.get(index).setBgColor(Color.parseColor(color));
        return SettingsManager.set(file, name, new String[]{BGCOLOR_ATTRIBUTE}, new String[]{color});
    }

    public String groupForeColor(String name, String color) {
        int index = Tuils.find(name, groups);
        if(index == -1) {
            return context.getString(R.string.output_groupnotfound);
        }

        groups.get(index).setForeColor(Color.parseColor(color));
        return SettingsManager.set(file, name, new String[]{FORECOLOR_ATTRIBUTE}, new String[]{color});
    }

    public String removeGroup(String name) {
        String output = SettingsManager.removeNode(file, name);

        if(output == null) return null;
        if(output.length() == 0) return context.getString(R.string.output_groupnotfound);

        int index = Tuils.find(name, groups);
        if(index != -1) groups.remove(index);

        return output;
    }

    public String addAppToGroup(String group, InstalledApplication app) {
        Object[] o;
        try {
            o = SettingsManager.buildDocument(file, null);
            if(o == null) {
                Tuils.sendXMLParseError(context, PATH);
                return null;
            }
        } catch (Exception e) {
            return e.toString();
        }

        Document d = (Document) o[0];
        Element root = (Element) o[1];

        Node node = SettingsManager.findNode(root, group);
        if(node == null) return context.getString(R.string.output_groupnotfound);

        Element e = (Element) node;
        String apps = e.getAttribute(APPS_ATTRIBUTE);

        if(apps != null && app.isInside(apps)) return null;

        apps = apps + APPS_SEPARATOR + app.write();
        if(apps.startsWith(APPS_SEPARATOR)) apps = apps.substring(1);

        e.setAttribute(APPS_ATTRIBUTE, apps);

        SettingsManager.writeTo(d, file);

        int index = Tuils.find(group, groups);
        if(index != -1) groups.get(index).add(app, true);

        return null;
    }

    public String removeAppFromGroup(String group, InstalledApplication app) {
        Object[] o;
        try {
            o = SettingsManager.buildDocument(file, null);
            if(o == null) {
                Tuils.sendXMLParseError(context, PATH);
                return null;
            }
        } catch (Exception e) {
            return e.toString();
        }

        Document d = (Document) o[0];
        Element root = (Element) o[1];

        Node node = SettingsManager.findNode(root, group);
        if(node == null) return context.getString(R.string.output_groupnotfound);

        Element e = (Element) node;

        String apps = e.getAttribute(APPS_ATTRIBUTE);
        if(apps == null) return null;

        if(!app.isInside(apps)) return null;

        String temp = apps.replaceAll(app.write(), Tuils.EMPTYSTRING);
        if(temp.length() < apps.length()) {
            apps = temp;
            apps = apps.replaceAll(APPS_SEPARATOR + APPS_SEPARATOR, APPS_SEPARATOR);
            if(apps.startsWith(APPS_SEPARATOR)) apps = apps.substring(1);
            if(apps.endsWith(APPS_SEPARATOR)) apps = apps.substring(0, apps.length() - 1);

            e.setAttribute(APPS_ATTRIBUTE, apps);

            SettingsManager.writeTo(d, file);

            int index = Tuils.find(group, groups);
            if(index != -1) groups.get(index).remove(app);
        }

        return null;
    }

//    public String removeAppFromGroup(String group, String app) {
//        Object[] o;
//        try {
//            o = XMLPrefsManager.buildDocument(file, NAME);
//        } catch (Exception e) {
//            return e.toString();
//        }
//
//        Document d = (Document) o[0];
//        Element root = (Element) o[1];
//
//        Node node = XMLPrefsManager.findNode(root, group);
//        if(node == null) return context.getString(R.string.output_groupnotfound);
//
//        Element e = (Element) node;
//
//        String apps = e.getAttribute(APPS_ATTRIBUTE);
//        if(apps == null) return null;
//
//        if(!apps.contains(app)) return null;
//
//        String temp = Pattern.compile(app.replaceAll(".", "\\.") + "(" + LaunchInfo.COMPONENT_SEPARATOR + "[^\\" + APPS_SEPARATOR + "]+)?").matcher(apps).replaceAll(Tuils.EMPTYSTRING);
//        if(temp.length() < apps.length()) {
//            apps = temp;
//
//            apps = apps.replaceAll(APPS_SEPARATOR + APPS_SEPARATOR, APPS_SEPARATOR);
//            if(apps.startsWith(APPS_SEPARATOR)) apps = apps.substring(1);
//            if(apps.endsWith(APPS_SEPARATOR)) apps = apps.substring(0, apps.length() - 1);
//
//            e.setAttribute(APPS_ATTRIBUTE, apps);
//
//            XMLPrefsManager.writeTo(d, file);
//
//            int index = Tuils.find(group, groups);
//            if(index != -1) {
//                Group g = groups.get(index);
//                g.remove(app);
//            }
//        }
//
//        return null;
//    }

    public String listGroup(String group) {
        Object[] o;
        try {
            o = SettingsManager.buildDocument(file, null);
            if(o == null) {
                Tuils.sendXMLParseError(context, PATH);
                return null;
            }
        } catch (Exception e) {
            return e.toString();
        }

        Element root = (Element) o[1];

        Node node = SettingsManager.findNode(root, group);
        if(node == null) return context.getString(R.string.output_groupnotfound);

        Element e = (Element) node;

        String apps = e.getAttribute(APPS_ATTRIBUTE);
        if(apps == null) return "[]";

        String labels = Tuils.EMPTYSTRING;

        PackageManager manager = context.getPackageManager();
        String[] split = apps.split(APPS_SEPARATOR);
        for(String s : split) {
            if(s.length() == 0) continue;

            String label;

            ComponentName name = InstalledApplication.componentInfo(s);
            if(name == null) {
                try {
                    label = manager.getApplicationInfo(s, 0).loadLabel(manager).toString();
                } catch (Exception e1) {
                    continue;
                }
            } else {
                try {
                    label = manager.getActivityInfo(name, 0).loadLabel(manager).toString();
                } catch (Exception e1) {
                    continue;
                }
            }

            labels = labels + Tuils.NEWLINE + label;
        }

        return labels.trim();
    }

    public String listGroups() {
        Object[] o;
        try {
            o = SettingsManager.buildDocument(file, null);
            if(o == null) {
                Tuils.sendXMLParseError(context, PATH);
                return null;
            }
        } catch (Exception e) {
            return e.toString();
        }

        Element root = (Element) o[1];

        String groups = Tuils.EMPTYSTRING;

        NodeList list = root.getElementsByTagName("*");
        for(int count = 0; count < list.getLength(); count++) {
            Node node = list.item(count);
            if(! (node instanceof Element)) continue;

            Element e = (Element) node;
            if(!e.hasAttribute(APPS_ATTRIBUTE)) continue;

            groups = groups + Tuils.NEWLINE + e.getNodeName();
        }

        if(groups.length() == 0) return "[]";
        return groups.trim();
    }

    public List<InstalledApplication> shownApps() {
        if(appsHolder == null) return new ArrayList<>();
        return appsHolder.getApps();
    }

    public List<InstalledApplication> hiddenApps() {
        return hiddenApps;
    }

    public InstalledApplication[] getSuggestedApps() {
        if(appsHolder == null) return new InstalledApplication[0];
        return appsHolder.getSuggestedApps();
    }

    public String printApps(int type) {
        return printNApps(type, -1);
    }

    public String printApps(int type, String text) {
        boolean ok;
        int length = 0;
        try {
            length = Integer.parseInt(text);
            ok = true;
        } catch (NumberFormatException exc) {
            ok = false;
        }

        if(ok) {
            return printNApps(type, length);
        } else {
            return printAppsThatBegins(type, text);
        }
    }

    private String printNApps(int type, int n) {
        try {
            List<String> labels = AppUtils.labelList(type == SHOWN_APPS ? appsHolder.getApps() : hiddenApps, true);

            if(n >= 0) {
                int toRemove = labels.size() - n;
                if(toRemove <= 0) return "[]";

                for(int c = 0; c < toRemove; c++) {
                    labels.remove(labels.size() - 1);
                }
            }

            return AppUtils.printApps(labels);
        } catch (NullPointerException e) {
            return "[]";
        }
    }

    private String printAppsThatBegins(int type, String with) {
        try {
            List<String> labels = AppUtils.labelList(type == SHOWN_APPS ? appsHolder.getApps() : hiddenApps, true);

            if(with != null && with.length() > 0) {
                with = with.toLowerCase();

                Iterator<String> it = labels.iterator();
                while(it.hasNext()) {
                    if(!it.next().toLowerCase().startsWith(with)) it.remove();
                }
            }

            return AppUtils.printApps(labels);
        } catch (NullPointerException e) {
            return "[]";
        }
    }

    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(appsBroadcast);
    }

    public void onDestroy() {
        unregisterReceiver(context);
    }

    public static class Group implements MainManager.Group, StringableObject {

        public static final int ALPHABETIC_UP_DOWN = 0;
        public static final int ALPHABETIC_DOWN_UP = 1;
        public static final int TIME_UP_DOWN = 2;
        public static final int TIME_DOWN_UP = 3;
        public static final int MOSTUSED_UP_DOWN = 4;
        public static final int MOSTUSED_DOWN_UP = 5;

        public static int sorting;

        public static Comparator<GroupLaunchInfo> comparator = new Comparator<GroupLaunchInfo>() {
            @Override
            public int compare(GroupLaunchInfo o1, GroupLaunchInfo o2) {
                switch (sorting) {
                    case ALPHABETIC_UP_DOWN:
                        return Tuils.alphabeticCompare(o1.publicLabel, o2.publicLabel);
                    case ALPHABETIC_DOWN_UP:
                        return Tuils.alphabeticCompare(o2.publicLabel, o1.publicLabel);
                    case TIME_UP_DOWN:
                        return o1.initialIndex - o2.initialIndex;
                    case TIME_DOWN_UP:
                        return o2.initialIndex - o1.initialIndex;
                    case MOSTUSED_UP_DOWN:
                        return o2.launchedTimes - o1.launchedTimes;
                    case MOSTUSED_DOWN_UP:
                        return o1.launchedTimes - o2.launchedTimes;
                }

                return 0;
            }
        };

        List<GroupLaunchInfo> apps;

        int bgColor = Integer.MAX_VALUE;
        int foreColor = Integer.MAX_VALUE;

        String name, lowercaseName;

        public Group(String name) {
            this.name = name;
            this.lowercaseName = name.toLowerCase();

            apps = new ArrayList<>();
        }

        public void add(InstalledApplication info, boolean sort) {
            apps.add(new GroupLaunchInfo(info, apps.size()));

            if(sort) sort();
        }

        public void remove(InstalledApplication info) {
            Iterator<GroupLaunchInfo> iterator = apps.iterator();
            while (iterator.hasNext()) {
                if(iterator.next().componentName.equals(info.componentName)) {
                    iterator.remove();
                    return;
                }
            }
        }

        public void remove(String app) {
            Iterator<GroupLaunchInfo> iterator = apps.iterator();
            while (iterator.hasNext()) {
                if(iterator.next().componentName.getPackageName().equals(app)) {
                    iterator.remove();
                    return;
                }
            }
        }

        public void sort() {
            Collections.sort(apps, comparator);
        }

        public boolean contains(InstalledApplication info) {
            return apps.contains(info);
        }

        public int getBgColor() {
            return bgColor;
        }

        public void setBgColor(int color) {
            this.bgColor = color;
        }

        public int getForeColor() {
            return foreColor;
        }

        public void setForeColor(int foreColor) {
            this.foreColor = foreColor;
        }

        @Override
        public List<? extends Object> members() {
            return apps;
        }

        @Override
        public boolean use(MainPack mainPack, String input) {
            InstalledApplication info = AppUtils.findLaunchInfoWithLabel(apps, input);
            if(info == null) return false;

            info.launchedTimes++;

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(info.componentName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            mainPack.context.startActivity(intent);

            return true;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Group) {
                return name.equals(((Group) obj).name());
            } else if(obj instanceof String) {
                return obj.equals(name);
            }

            return false;
        }

        @Override
        public String getLowercaseString() {
            return lowercaseName;
        }

        @Override
        public String getString() {
            return name();
        }

        public class GroupLaunchInfo extends InstalledApplication {

            int initialIndex;

            public GroupLaunchInfo(InstalledApplication info, int index) {
                super(info.componentName.getPackageName(), info.componentName.getClassName(), info.publicLabel);
                launchedTimes = info.launchedTimes;
                unspacedLowercaseLabel = info.unspacedLowercaseLabel;

                this.initialIndex = index;
            }
        }

    }

    private class AppsHolder {

        final int MOST_USED = 10, NULL = 11, USER_DEFINIED = 12;

        private List<InstalledApplication> infos;
        private SettingsEntriesContainer   values;

        private SuggestedAppMgr suggestedAppMgr;

        private class SuggestedAppMgr {
            private List<SuggestedApp> suggested;
            private int lastWriteable = -1;

            public SuggestedAppMgr(SettingsEntriesContainer values, List<InstalledApplication> apps) {
                suggested = new ArrayList<>();

                final String PREFIX = "default_app_n";
                for(int count = 0; count < 5; count++) {
                    String vl = values.get(Apps.valueOf(PREFIX + (count + 1))).value;

                    if(vl.equals(Apps.NULL)) continue;
                    if(vl.equals(Apps.MOST_USED)) suggested.add(new SuggestedApp(MOST_USED, count + 1));
                    else {
                        ComponentName name = null;

                        String[] split = vl.split("-");
                        if(split.length >= 2) {
                            name = new ComponentName(split[0], split[1]);
                        } else if(split.length == 1) {
                            if(split[0].contains("Activity")) {
                                for(InstalledApplication i : apps) {
                                    if(i.componentName.getClassName().equals(split[0])) name = i.componentName;
                                }
                            } else {
                                for(InstalledApplication i : apps) {
                                    if(i.componentName.getPackageName().equals(split[0])) name = i.componentName;
                                }
                            }
                        }

                        if(name == null) continue;

                        InstalledApplication info = AppUtils.findLaunchInfoWithComponent(infos, name);
                        if(info == null) continue;
                        suggested.add(new SuggestedApp(info, USER_DEFINIED, count + 1));
                    }
                }

                sort();
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

            public void set(int index, InstalledApplication info) {
                suggested.get(index).change(info);
            }

            public void attemptInsertSuggestion(InstalledApplication info) {
                if (info.launchedTimes == 0 || lastWriteable == -1) {
                    return;
                }

                int index = Tuils.find(info, suggested);
                if (index == -1) {
                    for (int count = 0; count <= lastWriteable; count++) {
                        SuggestedApp app = get(count);

                        if (app.app == null || info.launchedTimes > app.app.launchedTimes) {
                            SuggestedApp s = suggested.get(count);

                            InstalledApplication before = s.app;
                            s.change(info);

                            if(before != null) {
                                attemptInsertSuggestion(before);
                            }

                            break;
                        }
                    }
                }
                sort();
            }

            public List<InstalledApplication> apps() {
                List<InstalledApplication> list = new ArrayList<>();

                List<SuggestedApp> cp = new ArrayList<>(suggested);
                Collections.sort(cp, (o1, o2) -> o1.index - o2.index);

                for(int count = 0; count < cp.size(); count++) {
                    SuggestedApp app = cp.get(count);
                    if(app.type != NULL && app.app != null) list.add(app.app);
                }
                return list;
            }

//            public List<String> labels() {
//                List<LaunchInfo> list = new ArrayList<>();
//
//                List<SuggestedApp> cp = new ArrayList<>(suggested);
//                Collections.sort(cp, new Comparator<SuggestedApp>() {
//                    @Override
//                    public int compare(SuggestedApp o1, SuggestedApp o2) {
//                        return o1.index - o2.index;
//                    }
//                });
//
//                for(int count = 0; count < cp.size(); count++) {
//                    SuggestedApp app = cp.get(count);
//                    if(app.type != NULL && app.app != null) list.add(app.app);
//                }
//                return AppUtils.labelList(list, false);
//            }

            private class SuggestedApp implements Comparable {
                int                  type;
                InstalledApplication app;
                int                  index;

                public SuggestedApp(int type, int index) {
                    this(null, type, index);
                }

                public SuggestedApp(InstalledApplication info, int type, int index) {
                    this.app = info;
                    this.type = type;
                    this.index = index;
                }

                public SuggestedApp change(InstalledApplication info) {
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
                    } else if(o instanceof InstalledApplication) {
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
            }
        }

        Comparator<InstalledApplication> mostUsedComparator = (lhs, rhs) -> rhs.launchedTimes > lhs.launchedTimes ? -1 : rhs.launchedTimes == lhs.launchedTimes ? 0 : 1;

        public AppsHolder(List<InstalledApplication> infos, SettingsEntriesContainer values) {
            this.infos = infos;
            this.values = values;
            update(true);
        }

        public void add(InstalledApplication info) {
            if(!infos.contains(info) ) {
                infos.add(info);
                update(false);
            }
        }

        public void remove(InstalledApplication info) {
            infos.remove(info);
            update(true);
        }

        private void sort() {
            try {
                Collections.sort(infos, mostUsedComparator);
            } catch (NullPointerException e) {}
        }

        private void fillSuggestions() {
            suggestedAppMgr = new SuggestedAppMgr(values, getApps());
            for(InstalledApplication info : infos) {
                suggestedAppMgr.attemptInsertSuggestion(info);
            }
        }

        public void requestSuggestionUpdate(InstalledApplication info) {
            suggestedAppMgr.attemptInsertSuggestion(info);
        }

        private void update(boolean refreshSuggestions) {
            AppUtils.checkEquality(infos);
            sort();
            if(refreshSuggestions) {
                fillSuggestions();
            }
        }

        public List<InstalledApplication> getApps() {
            return infos;
        }

        public InstalledApplication[] getSuggestedApps() {
            List<InstalledApplication> apps = suggestedAppMgr.apps();
            return apps.toArray(new InstalledApplication[apps.size()]);
        }
    }

    public static class AppUtils {

        public static InstalledApplication findLaunchInfoWithComponent(List<InstalledApplication> appList, ComponentName name) {
            if(name == null) return null;

            for(InstalledApplication i : appList) {
                if(i.equals(name)) return i;
            }

            return null;
        }

        public static InstalledApplication findLaunchInfoWithLabel(List<? extends InstalledApplication> appList, String label) {
            label = Tuils.removeSpaces(label);
            for(InstalledApplication i : appList) if(i.unspacedLowercaseLabel.equalsIgnoreCase(label)) return i;
            return null;
        }

        private static List<InstalledApplication> findLaunchInfosWithPackage(String packageName, List<InstalledApplication> infos) {
            List<InstalledApplication> result = new ArrayList<>();
            for(InstalledApplication info : infos) if (info.componentName.getPackageName().equals(packageName)) result.add(info);
            return result;
        }

        public static void checkEquality(List<InstalledApplication> list) {

            for (InstalledApplication info : list) {

                if(info == null || info.publicLabel == null) {
                    continue;
                }

                for (int count = 0; count < list.size(); count++) {
                    InstalledApplication info2 = list.get(count);

                    if(info2 == null || info2.publicLabel == null) {
                        continue;
                    }

                    if(info == info2) {
                        continue;
                    }

                    if (info.unspacedLowercaseLabel.equals(info2.unspacedLowercaseLabel)) {
//                        there are two activities in the same app loadlabel gives the same result
                        if(info.componentName.getPackageName().equals(info2.componentName.getPackageName())) {
                            info.setLabel(insertActivityName(info.publicLabel, info.componentName.getClassName()));
                            info2.setLabel(insertActivityName(info2.publicLabel, info2.componentName.getClassName()));
                        }
                        else {
                            info2.setLabel(getNewLabel(info2.publicLabel, info2.componentName.getClassName()));
                        }
                    }
                }
            }
        }

        static Pattern activityPattern = Pattern.compile("activity", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        public static String insertActivityName(String oldLabel, String activityName) {
            String name;

            int lastDot = activityName.lastIndexOf(".");
            if(lastDot == -1) {
                name = activityName;
            } else {
                name = activityName.substring(lastDot + 1);
            }

            name = activityPattern.matcher(name).replaceAll(Tuils.EMPTYSTRING);
            name = name.substring(0,1).toUpperCase() + name.substring(1);
            return oldLabel + Tuils.SPACE + "-" + Tuils.SPACE + name;
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

        public static String format(InstalledApplication app, PackageInfo info) {
            StringBuilder builder = new StringBuilder();

            builder.append(info.packageName).append(Tuils.NEWLINE);
            builder.append("vrs: ").append(info.versionCode).append(" - ").append(info.versionName).append(Tuils.NEWLINE).append(Tuils.NEWLINE);
            builder.append("launched_times: ").append(app.launchedTimes).append(Tuils.NEWLINE).append(Tuils.NEWLINE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                builder.append("Install: ").append(TimeManager.instance.replace("%t0", info.firstInstallTime, Integer.MAX_VALUE)).append(Tuils.NEWLINE).append(Tuils.NEWLINE);
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

        public static String printApps(List<String> apps) {
            if(apps.size() == 0) {
                return apps.toString();
            }

            List<String> list = new ArrayList<>(apps);

            Collections.sort(list, Tuils::alphabeticCompare);

            Tuils.addPrefix(list, Tuils.DOUBLE_SPACE);
            Tuils.insertHeaders(list, false);
            return Tuils.toPlanString(list);
        }

        public static List<String> labelList(List<InstalledApplication> infos, boolean sort) {
            List<String> labels = new ArrayList<>();
            for (InstalledApplication info : infos) {
                labels.add(info.publicLabel);
            }
            if(sort) Collections.sort(labels);
            return labels;
        }
    }

}