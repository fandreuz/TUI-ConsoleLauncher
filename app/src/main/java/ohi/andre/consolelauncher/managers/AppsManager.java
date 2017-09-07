package ohi.andre.consolelauncher.managers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.MainManager;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.Compare;
import ohi.andre.consolelauncher.tuils.TimeManager;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Suggester;

import static ohi.andre.consolelauncher.managers.XMLPrefsManager.VALUE_ATTRIBUTE;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.resetFile;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.set;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.writeTo;

public class AppsManager implements XMLPrefsManager.XmlPrefsElement {

    public static final int SHOWN_APPS = 10;
    public static final int HIDDEN_APPS = 11;

    public static final String PATH = "apps.xml";
    private final String NAME = "APPS";
    private File file;

    private final String SHOW_ATTRIBUTE = "show";
    private final String APPS_ATTRIBUTE = "apps";
    private final String BGCOLOR_ATTRIBUTE = "bgColor";
    private final String FORECOLOR_ATTRIBUTE = "foreColor";
    private static final String APPS_SEPARATOR = ";";

    private Context context;

    private AppsHolder appsHolder;
    private List<LaunchInfo> hiddenApps;

    private final String PREFS = "apps";
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private static XMLPrefsManager.XmlPrefsElement instance = null;

    private XMLPrefsManager.XMLPrefsList defaultApps;

    public static List<Group> groups;

    private Pattern pp, pl, pn;
    private String appInstalledFormat, appUninstalledFormat;
    int appInstalledColor, appUninstalledColor;

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

        public static final String MOST_USED = "most_used";
        public static final String NULL = "null";

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
    public String[] deleted() {
        return new String[0];
    }

    @Override
    public void write(XMLPrefsManager.XMLPrefsSave save, String value) {
        set(new File(Tuils.getFolder(), PATH), NAME, save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
    }

    @Override
    public XMLPrefsManager.XMLPrefsList getValues() {
        return defaultApps;
    }

    private BroadcastReceiver appsBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String data = intent.getData().getSchemeSpecificPart();
            if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                appInstalled(data);
            }
            else {
                appUninstalled(data);
            }
        }
    };

    public AppsManager(Context context, final Suggester s) {
        instance = this;

        this.context = context;

        appInstalledFormat = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_app_installed) ? XMLPrefsManager.get(XMLPrefsManager.Behavior.app_installed_format) : null;
        appUninstalledFormat = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.show_app_uninstalled) ? XMLPrefsManager.get(XMLPrefsManager.Behavior.app_uninstalled_format) : null;

        if(appInstalledFormat != null || appUninstalledFormat != null) {
            pp = Pattern.compile("%p", Pattern.CASE_INSENSITIVE);
            pl = Pattern.compile("%l", Pattern.CASE_INSENSITIVE);
            pn = Pattern.compile("%n", Pattern.CASE_INSENSITIVE);

            appInstalledColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.app_installed_color);
            appUninstalledColor = XMLPrefsManager.getColor(XMLPrefsManager.Theme.app_uninstalled_color);
        } else {
            pp = null;
            pl = null;
            pn = null;
        }

        this.file = new File(Tuils.getFolder(), PATH);

        this.preferences = context.getSharedPreferences(PREFS, 0);
        this.editor = preferences.edit();

        this.groups = new ArrayList<>();

        initAppListener(context);

        new Thread() {
            @Override
            public void run() {
                super.run();

                fill();
                s.requestUpdate();
            }
        }.start();
    }

    private void initAppListener(Context c) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");

        c.registerReceiver(appsBroadcast, intentFilter);
    }

    public void fill() {
        final List<LaunchInfo> allApps = createAppMap(context.getPackageManager());
        hiddenApps = new ArrayList<>();

        groups.clear();

        try {
            defaultApps = new XMLPrefsManager.XMLPrefsList();

            if(!file.exists()) {
                resetFile(file, NAME);
            }

            Object[] o;
            try {
                o = XMLPrefsManager.buildDocument(file, NAME);
            } catch (Exception e) {
                Tuils.sendOutput(Color.RED, context, context.getString(R.string.output_xmlproblem1) + Tuils.SPACE + PATH + context.getString(R.string.output_xmlproblem2) +
                                Tuils.NEWLINE + context.getString(R.string.output_errorlabel) + e.toString());
                return;
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            List<AppsManager.Options> enums = new ArrayList<>(Arrays.asList(AppsManager.Options.values()));
            NodeList nodes = root.getElementsByTagName("*");

            for (int count = 0; count < nodes.getLength(); count++) {
                final Node node = nodes.item(count);

                String nn = node.getNodeName();
                int nodeIndex = Tuils.find(nn, (List) enums);
                if (nodeIndex != -1) {
                    defaultApps.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());

                    for (int en = 0; en < enums.size(); en++) {
                        if (enums.get(en).label().equals(nn)) {
                            enums.remove(en);
                            break;
                        }
                    }
                }
//                todo support delete
                else {
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        final Element e = (Element) node;

                        if(e.hasAttribute(APPS_ATTRIBUTE)) {
                            final String name = e.getNodeName();
                            if(name.contains(Tuils.SPACE)) {
                                Tuils.sendOutput(Color.RED, context, PATH + ": " + context.getString(R.string.output_groupspace) + ": " + name);
                                continue;
                            }

                            new Thread() {
                                @Override
                                public void run() {
                                    super.run();

                                    Group g = new Group(name);

                                    String apps = e.getAttribute(APPS_ATTRIBUTE);
                                    String[] split = apps.split(APPS_SEPARATOR);

                                    List<LaunchInfo> as = new ArrayList<>(allApps);

                                    External:
                                    for(String s : split) {
                                        for(int c = 0; c < as.size(); c++) {
                                            if(as.get(c).equals(s)) {
                                                g.add(as.remove(c));
                                                continue External;
                                            }
                                        }
                                    }

                                    if(e.hasAttribute(BGCOLOR_ATTRIBUTE)) {
                                        String c = e.getAttribute(BGCOLOR_ATTRIBUTE);
                                        if(c.length() > 0) {
                                            try {
                                                g.setBgColor(Color.parseColor(c));
                                            } catch (Exception e) {
                                                Tuils.sendOutput(Color.RED, context, PATH + ": " + context.getString(R.string.output_invalidcolor) + ": " + c);
                                            }
                                        }
                                    }

                                    if(e.hasAttribute(FORECOLOR_ATTRIBUTE)) {
                                        String c = e.getAttribute(FORECOLOR_ATTRIBUTE);
                                        if(c.length() > 0) {
                                            try {
                                                g.setForeColor(Color.parseColor(c));
                                            } catch (Exception e) {
                                                Tuils.sendOutput(Color.RED, context, PATH + ": " + context.getString(R.string.output_invalidcolor) + ": " + c);
                                            }
                                        }
                                    }

                                    groups.add(g);
                                }
                            }.start();
                        } else {
                            boolean shown = !e.hasAttribute(SHOW_ATTRIBUTE) || Boolean.parseBoolean(e.getAttribute(SHOW_ATTRIBUTE));
                            if (!shown) {
                                ComponentName name = null;

                                String[] split = nn.split("-");
                                if (split.length >= 2) {
                                    name = new ComponentName(split[0], split[1]);
                                } else if (split.length == 1) {
                                    if (split[0].contains("Activity")) {
                                        for (LaunchInfo i : allApps) {
                                            if (i.componentName.getClassName().equals(split[0]))
                                                name = i.componentName;
                                        }
                                    } else {
                                        for (LaunchInfo i : allApps) {
                                            if (i.componentName.getPackageName().equals(split[0]))
                                                name = i.componentName;
                                        }
                                    }
                                }

                                if (name == null) continue;

                                LaunchInfo removed = AppUtils.findLaunchInfoWithComponent(allApps, name);
                                if (removed != null) {
                                    allApps.remove(removed);
                                    hiddenApps.add(removed);
                                }
                            }
                        }
                    }
                }
            }

            if (enums.size() > 0) {
                for (XMLPrefsManager.XMLPrefsSave s : enums) {
                    String value = s.defaultValue();

                    Element em = d.createElement(s.label());
                    em.setAttribute(VALUE_ATTRIBUTE, value);
                    root.appendChild(em);

                    defaultApps.add(s.label(), value);
                }
                writeTo(d, file);
            }

            for (Map.Entry<String, ?> entry : this.preferences.getAll().entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Integer) {
                    ComponentName name = null;

                    String[] split = entry.getKey().split("-");
                    if (split.length >= 2) {
                        name = new ComponentName(split[0], split[1]);
                    } else if (split.length == 1) {
                        if (split[0].contains("Activity")) {
                            for (LaunchInfo i : allApps) {
                                if (i.componentName.getClassName().equals(split[0]))
                                    name = i.componentName;
                            }
                        } else {
                            for (LaunchInfo i : allApps) {
                                if (i.componentName.getPackageName().equals(split[0]))
                                    name = i.componentName;
                            }
                        }
                    }

                    if (name == null) continue;

                    LaunchInfo info = AppUtils.findLaunchInfoWithComponent(allApps, name);
                    if (info != null) info.launchedTimes = (Integer) value;
                }
            }

        } catch (Exception e1) {
            Tuils.toFile(e1);
        }

        appsHolder = new AppsHolder(allApps, defaultApps);
        AppUtils.checkEquality(hiddenApps);
    }

    private List<LaunchInfo> createAppMap(PackageManager mgr) {
        List<LaunchInfo> infos = new ArrayList<>();

        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> main;
        try {
            main = mgr.queryIntentActivities(i, 0);
        } catch (Exception e) {
            return infos;
        }

        for (ResolveInfo ri : main) infos.add(new LaunchInfo(ri.activityInfo.packageName, ri.activityInfo.name, ri.loadLabel(mgr).toString()));

        return infos;
    }

    private void appInstalled(String packageName) {
        try {
            PackageManager manager = context.getPackageManager();

            PackageInfo packageInfo = manager.getPackageInfo(packageName, 0);

            if(appInstalledFormat != null) {
                String cp = appInstalledFormat;

                cp = pp.matcher(cp).replaceAll(packageName);
                if(packageInfo != null) {
                    CharSequence sequence = packageInfo.applicationInfo.loadLabel(manager);
                    if(sequence != null) cp = pl.matcher(cp).replaceAll(sequence.toString());
                } else {
                    int index = packageName.lastIndexOf(Tuils.DOT);
                    if(index == -1) cp = pl.matcher(cp).replaceAll(Tuils.EMPTYSTRING);
                    else {
                        cp = pl.matcher(cp).replaceAll(packageName.substring(index + 1));
                    }
                }

                cp = pn.matcher(cp).replaceAll(Tuils.NEWLINE);

                Tuils.sendOutput(appInstalledColor, context, cp);
            }

            Intent i = manager.getLaunchIntentForPackage(packageName);
            if(i == null) return;

            ComponentName name = i.getComponent();
            String activity = name.getClassName();
            String label = manager.getActivityInfo(name, 0).loadLabel(manager).toString();

            LaunchInfo app = new LaunchInfo(packageName, activity, label);
            appsHolder.add(app);
        } catch (Exception e) {}
    }

    private void appUninstalled(String packageName) {
        if(appsHolder == null || context == null) return;

        List<LaunchInfo> infos = AppUtils.findLaunchInfosWithPackage(packageName, appsHolder.getApps());

        if(appUninstalledFormat != null) {
            String cp = appUninstalledFormat;

            cp = pp.matcher(cp).replaceAll(packageName);
            if(infos.size() > 0) {
                cp = pl.matcher(cp).replaceAll(infos.get(0).publicLabel);
            } else {
                int index = packageName.lastIndexOf(Tuils.DOT);
                if(index == -1) cp = pl.matcher(cp).replaceAll(Tuils.EMPTYSTRING);
                else {
                    cp = pl.matcher(cp).replaceAll(packageName.substring(index + 1));
                }
            }
            cp = pn.matcher(cp).replaceAll(Tuils.NEWLINE);

            Tuils.sendOutput(appUninstalledColor, context, cp);
        }

        for(LaunchInfo i : infos) appsHolder.remove(i);
    }

    public LaunchInfo findLaunchInfoWithLabel(String label, int type) {
        List<LaunchInfo> appList;
        if(type == SHOWN_APPS) {
            appList = appsHolder.getApps();
        } else {
            appList = hiddenApps;
        }

        return AppUtils.findLaunchInfoWithLabel(appList, label);
    }

    public void writeLaunchTimes(LaunchInfo info) {
        editor.putInt(info.write(), info.launchedTimes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }

        if(appsHolder != null) appsHolder.update(true);
    }

    public Intent getIntent(LaunchInfo info) {
        info.launchedTimes++;
        appsHolder.requestSuggestionUpdate(info);

        writeLaunchTimes(info);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(info.componentName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    public String hideActivity(LaunchInfo info) {
        set(file, NAME, info.write(), new String[] {SHOW_ATTRIBUTE}, new String[] {false + Tuils.EMPTYSTRING});

        appsHolder.remove(info);
        appsHolder.update(true);
        hiddenApps.add(info);
        AppUtils.checkEquality(hiddenApps);

        return info.publicLabel;
    }

    public String showActivity(LaunchInfo info) {
        set(file, NAME, info.write(), new String[]{SHOW_ATTRIBUTE}, new String[]{true + Tuils.EMPTYSTRING});

        hiddenApps.remove(info);
        appsHolder.add(info);
        appsHolder.update(false);

        return info.publicLabel;
    }

    public String createGroup(String name) {
        int index = Tuils.find(name, groups);
        if(index == -1) {
            groups.add(new Group(name));
            return XMLPrefsManager.set(file, NAME, name, new String[]{APPS_ATTRIBUTE}, new String[]{Tuils.EMPTYSTRING});
        }

        return context.getString(R.string.output_groupexists);
    }

    public String groupBgColor(String name, String color) {
        int index = Tuils.find(name, groups);
        if(index == -1) {
            return context.getString(R.string.output_groupnotfound);
        }

        groups.get(index).setBgColor(Color.parseColor(color));
        return XMLPrefsManager.set(file, NAME, name, new String[]{BGCOLOR_ATTRIBUTE}, new String[]{color});
    }

    public String groupForeColor(String name, String color) {
        int index = Tuils.find(name, groups);
        if(index == -1) {
            return context.getString(R.string.output_groupnotfound);
        }

        groups.get(index).setForeColor(Color.parseColor(color));
        return XMLPrefsManager.set(file, NAME, name, new String[]{FORECOLOR_ATTRIBUTE}, new String[]{color});
    }

    public String removeGroup(String name) {
        String output = XMLPrefsManager.removeNode(file, NAME, name);

        if(output == null) return null;
        if(output.length() == 0) return context.getString(R.string.output_groupnotfound);

        int index = Tuils.find(name, groups);
        if(index != -1) groups.remove(index);

        return output;
    }

    public String addAppToGroup(String group, LaunchInfo app) {
        Object[] o;
        try {
            o = XMLPrefsManager.buildDocument(file, NAME);
        } catch (Exception e) {
            return e.toString();
        }

        Document d = (Document) o[0];
        Element root = (Element) o[1];

        Node node = XMLPrefsManager.findNode(root, group);
        if(node == null) return context.getString(R.string.output_groupnotfound);

        Element e = (Element) node;
        String apps = e.getAttribute(APPS_ATTRIBUTE);

        if(apps != null && app.isInside(apps)) return null;

        apps = apps + APPS_SEPARATOR + app.write();
        if(apps.startsWith(APPS_SEPARATOR)) apps = apps.substring(1);

        e.setAttribute(APPS_ATTRIBUTE, apps);

        XMLPrefsManager.writeTo(d, file);

        int index = Tuils.find(group, groups);
        if(index != -1) groups.get(index).add(app);

        return null;
    }

    public String removeAppFromGroup(String group, LaunchInfo app) {
        Object[] o;
        try {
            o = XMLPrefsManager.buildDocument(file, NAME);
        } catch (Exception e) {
            return e.toString();
        }

        Document d = (Document) o[0];
        Element root = (Element) o[1];

        Node node = XMLPrefsManager.findNode(root, group);
        if(node == null) return context.getString(R.string.output_groupnotfound);

        Element e = (Element) node;

        String apps = e.getAttribute(APPS_ATTRIBUTE);
        if(apps == null) return null;

        if(!app.isInside(apps)) return null;

        String temp = apps.replaceAll(app.write(), Tuils.EMPTYSTRING);
        if(temp.length() < apps.length()) apps = temp;
        else apps = apps.replaceAll(app.componentName.getPackageName(), Tuils.EMPTYSTRING);

        apps = apps.replaceAll(APPS_SEPARATOR + APPS_SEPARATOR, APPS_SEPARATOR);
        if(apps.startsWith(APPS_SEPARATOR)) apps = apps.substring(1);
        if(apps.endsWith(APPS_SEPARATOR)) apps = apps.substring(0, apps.length() - 1);

        e.setAttribute(APPS_ATTRIBUTE, apps);

        XMLPrefsManager.writeTo(d, file);

        int index = Tuils.find(group, groups);
        if(index != -1) groups.get(index).remove(app);

        return null;
    }

    public String listGroup(String group) {
        Object[] o;
        try {
            o = XMLPrefsManager.buildDocument(file, NAME);
        } catch (Exception e) {
            return e.toString();
        }

        Element root = (Element) o[1];

        Node node = XMLPrefsManager.findNode(root, group);
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

            ComponentName name = LaunchInfo.componentInfo(s);
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
            o = XMLPrefsManager.buildDocument(file, NAME);
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

    public List<LaunchInfo> shownApps() {
        if(appsHolder == null) return new ArrayList<>();
        return appsHolder.getApps();
    }

    public List<LaunchInfo> hiddenApps() {
        return hiddenApps;
    }

    public LaunchInfo[] getSuggestedApps() {
        if(appsHolder == null) return new LaunchInfo[0];
        return appsHolder.getSuggestedApps();
    }

    public String printApps(int type) {
        try {
            List<String> labels = AppUtils.labelList(type == SHOWN_APPS ? appsHolder.getApps() : hiddenApps, true);
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

    public static class Group implements MainManager.Group {
        List<LaunchInfo> apps;

        int bgColor = Integer.MAX_VALUE;
        int foreColor = Integer.MAX_VALUE;

        String name;

        public Group(String name) {
            this.name = name;
            apps = new ArrayList<>();
        }

        public void add(LaunchInfo info) {
            apps.add(info);
        }

        public void remove(LaunchInfo info) {
            apps.remove(info);
        }

        public boolean contains(LaunchInfo info) {
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

        public String getName() {
            return name;
        }

        @Override
        public List<? extends Compare.Stringable> members() {
            return apps;
        }

        @Override
        public boolean use(MainPack mainPack, String input) {
            LaunchInfo info = AppUtils.findLaunchInfoWithLabel(apps, input);
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
    }

    public static class LaunchInfo implements Compare.Stringable {

        private static final String COMPONENT_SEPARATOR = "-";

        public ComponentName componentName;

        public String publicLabel;
        public int launchedTimes = 0;

        public LaunchInfo(String packageName, String activityName, String label) {
            this.componentName = new ComponentName(packageName, activityName);
            this.publicLabel = label;
        }

        public boolean isInside(String apps) {
            String[] split = apps.split(AppsManager.APPS_SEPARATOR);
            for(String s : split) {
                if(is(s)) return true;
            }

            return false;
        }

        public boolean is(String app) {
            String[] split2 = app.split(COMPONENT_SEPARATOR);

            if(split2.length == 1) {
                if(componentName.getPackageName().equals(split2[0])) return true;
            } else {
                if(componentName.getPackageName().equals(split2[0]) && componentName.getClassName().equals(split2[1])) return true;
            }

            return false;
        }

        public static ComponentName componentInfo(String app) {
            String[] split2 = app.split(COMPONENT_SEPARATOR);

            if(split2.length == 1) {
                return null;
            } else {
                return new ComponentName(split2[0], split2[1]);
            }
        }

        @Override
        public boolean equals(Object o) {
            if(o == null) {
                return false;
            }

            if(o instanceof LaunchInfo) {
                LaunchInfo i = (LaunchInfo) o;
                try {
                    return this.componentName.equals(i.componentName);
                } catch (Exception e) {
                    return false;
                }
            }
            else if(o instanceof ComponentName) {
                return this.componentName.equals(o);
            }
            else if(o instanceof String) {
                return is((String) o) || this.componentName.getClassName().equals(o);
            }

            return false;
        }

        @Override
        public String toString() {
            return componentName.getPackageName() + " - " + componentName.getClassName() + " --> " + publicLabel + ", n=" + launchedTimes;
        }

        public String write() {
            return this.componentName.getPackageName() + COMPONENT_SEPARATOR + this.componentName.getClassName();
        }

        @Override
        public String getString() {
            return publicLabel;
        }
    }

    private class AppsHolder {

        final int MOST_USED = 10, NULL = 11, USER_DEFINIED = 12;

        private List<LaunchInfo> infos;
        private XMLPrefsManager.XMLPrefsList values;

        private SuggestedAppMgr suggestedAppMgr;

        private class SuggestedAppMgr {
            private List<SuggestedApp> suggested;
            private int lastWriteable = -1;

            public SuggestedAppMgr(XMLPrefsManager.XMLPrefsList values, List<LaunchInfo> apps) {
                suggested = new ArrayList<>();

                final String PREFIX = "default_app_n";
                for(int count = 0; count < Options.values().length; count++) {
                    String vl = values.get(Options.valueOf(PREFIX + (count + 1))).value;

                    if(vl.equals(Options.NULL)) continue;
                    if(vl.equals(Options.MOST_USED)) suggested.add(new SuggestedApp(MOST_USED, count + 1));
                    else {
                        ComponentName name = null;

                        String[] split = vl.split("-");
                        if(split.length >= 2) {
                            name = new ComponentName(split[0], split[1]);
                        } else if(split.length == 1) {
                            if(split[0].contains("Activity")) {
                                for(LaunchInfo i : apps) {
                                    if(i.componentName.getClassName().equals(split[0])) name = i.componentName;
                                }
                            } else {
                                for(LaunchInfo i : apps) {
                                    if(i.componentName.getPackageName().equals(split[0])) name = i.componentName;
                                }
                            }
                        }

                        if(name == null) continue;

                        LaunchInfo info = AppUtils.findLaunchInfoWithComponent(infos, name);
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

            public void set(int index, LaunchInfo info) {
                suggested.get(index).change(info);
            }

            public void attemptInsertSuggestion(LaunchInfo info) {
                if (info.launchedTimes == 0 || lastWriteable == -1) {
                    return;
                }

                int index = Tuils.find(info, suggested);
                if (index == -1) {
                    for (int count = 0; count <= lastWriteable; count++) {
                        SuggestedApp app = get(count);

                        if (app.app == null || info.launchedTimes > app.app.launchedTimes) {
                            SuggestedApp s = suggested.get(count);

                            LaunchInfo before = s.app;
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

            public List<LaunchInfo> apps() {
                List<LaunchInfo> list = new ArrayList<>();

                List<SuggestedApp> cp = new ArrayList<>(suggested);
                Collections.sort(cp, new Comparator<SuggestedApp>() {
                    @Override
                    public int compare(SuggestedApp o1, SuggestedApp o2) {
                        return o1.index - o2.index;
                    }
                });

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
                int type;
                LaunchInfo app;
                int index;

                public SuggestedApp(int type, int index) {
                    this(null, type, index);
                }

                public SuggestedApp(LaunchInfo info, int type, int index) {
                    this.app = info;
                    this.type = type;
                    this.index = index;
                }

                public SuggestedApp change(LaunchInfo info) {
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
                    } else if(o instanceof LaunchInfo) {
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

        Comparator<LaunchInfo> mostUsedComparator = new Comparator<LaunchInfo>() {
            @Override
            public int compare(LaunchInfo lhs, LaunchInfo rhs) {
                return rhs.launchedTimes > lhs.launchedTimes ? -1 : rhs.launchedTimes == lhs.launchedTimes ? 0 : 1;
            }
        };

        public AppsHolder(List<LaunchInfo> infos, XMLPrefsManager.XMLPrefsList values) {
            this.infos = infos;
            this.values = values;
            update(true);
        }

        public void add(LaunchInfo info) {
            if(!infos.contains(info) ) {
                infos.add(info);
                update(false);
            }
        }

        public void remove(LaunchInfo info) {
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
            for(LaunchInfo info : infos) {
                suggestedAppMgr.attemptInsertSuggestion(info);
            }
        }

        public void requestSuggestionUpdate(LaunchInfo info) {
            suggestedAppMgr.attemptInsertSuggestion(info);
        }

        private void update(boolean refreshSuggestions) {
            AppUtils.checkEquality(infos);
            sort();
            if(refreshSuggestions) {
                fillSuggestions();
            }
        }

        public List<LaunchInfo> getApps() {
            return infos;
        }

        public LaunchInfo[] getSuggestedApps() {
            List<LaunchInfo> apps = suggestedAppMgr.apps();
            return apps.toArray(new LaunchInfo[apps.size()]);
        }
    }

    public static class AppUtils {

        public static LaunchInfo findLaunchInfoWithComponent(List<LaunchInfo> appList, ComponentName name) {
            if(name == null) return null;

            for(LaunchInfo i : appList) {
                if(i.equals(name)) return i;
            }

            return null;
        }

        private static Pattern removeSpacePattern = Pattern.compile("\\s+");
        public static LaunchInfo findLaunchInfoWithLabel(List<LaunchInfo> appList, String label) {
            label = removeSpacePattern.matcher(label).replaceAll(Tuils.EMPTYSTRING);
            for(LaunchInfo i : appList) if(removeSpacePattern.matcher(i.publicLabel).replaceAll(Tuils.EMPTYSTRING).equalsIgnoreCase(label)) return i;
            return null;
        }

        private static List<LaunchInfo> findLaunchInfosWithPackage(String packageName, List<LaunchInfo> infos) {
            List<LaunchInfo> result = new ArrayList<>();
            for(LaunchInfo info : infos) if (info.componentName.getPackageName().equals(packageName)) result.add(info);

            return result;
        }

        public static void checkEquality(List<LaunchInfo> list) {

            for (LaunchInfo info : list) {

                if(info == null || info.publicLabel == null) {
                    continue;
                }

                for (int count = 0; count < list.size(); count++) {
                    LaunchInfo info2 = list.get(count);

                    if(info2 == null || info2.publicLabel == null) {
                        continue;
                    }

                    if(info == info2) {
                        continue;
                    }

                    if (info.publicLabel.toLowerCase().replace(Tuils.SPACE, Tuils.EMPTYSTRING).equals(info2.publicLabel.toLowerCase().replace(Tuils.SPACE, Tuils.EMPTYSTRING))) {
//                        there are two activities in the same app loadlabel gives the same result
                        if(info.componentName.getPackageName().equals(info2.componentName.getPackageName())) {
                            info.publicLabel = insertActivityName(info.publicLabel, info.componentName.getClassName());
                            info2.publicLabel = insertActivityName(info2.publicLabel, info2.componentName.getClassName());
                        }
                        else {
                            info2.publicLabel = getNewLabel(info2.publicLabel, info2.componentName.getClassName());
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

        public static String format(LaunchInfo app, PackageInfo info) {
            StringBuilder builder = new StringBuilder();

            builder.append(info.packageName).append(Tuils.NEWLINE);
            builder.append("vrs: ").append(info.versionCode).append(" - ").append(info.versionName).append(Tuils.NEWLINE).append(Tuils.NEWLINE);
            builder.append("launched_times: ").append(app.launchedTimes).append(Tuils.NEWLINE).append(Tuils.NEWLINE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                builder.append("Install: ").append(TimeManager.replace("%t0", info.firstInstallTime, Integer.MAX_VALUE)).append(Tuils.NEWLINE).append(Tuils.NEWLINE);
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

            Collections.sort(list, new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {
                    return Tuils.alphabeticCompare(lhs, rhs);
                }
            });

            Tuils.addPrefix(list, Tuils.DOUBLE_SPACE);
            Tuils.insertHeaders(list, false);
            return Tuils.toPlanString(list);
        }

        public static List<String> labelList(List<LaunchInfo> infos, boolean sort) {
            List<String> labels = new ArrayList<>();
            for (LaunchInfo info : infos) {
                labels.add(info.publicLabel);
            }
            if(sort) Collections.sort(labels);
            return labels;
        }
    }

}