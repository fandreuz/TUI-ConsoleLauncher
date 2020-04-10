package ohi.andre.consolelauncher.managers.apps;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Process;

import org.reactivestreams.Publisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Stream;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.reactivex.schedulers.Schedulers;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.settings.SettingsEntriesContainer;
import ohi.andre.consolelauncher.managers.settings.SettingsManager;
import ohi.andre.consolelauncher.managers.settings.SettingsOption;
import ohi.andre.consolelauncher.managers.settings.options.Apps;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.managers.settings.SettingsManager.VALUE_ATTRIBUTE;

// load apps and detect new installs
public class AppsLoader {
    private final Context context;
    
    // emits an item when an app is installed (true) or uninstalled (false)
    private final Subject<Boolean> applicationStateChangedSubject = PublishSubject.create();
    
    private final BroadcastReceiver applicationStateChangedListener = new BroadcastReceiver() {
        @Override
        public void onReceive (Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) applicationStateChangedSubject.onNext(true);
            else if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) applicationStateChangedSubject.onNext(false);
        }
    };
    
    public AppsLoader (Context context) {
        this.context = context;
        registerApplicationStateChangedReceiver();
    }
    
    // the first emitted item is the list of apps at launch time.
    // then installed/uninstalled apps will trigger an update
    public Observable<PriorityQueue<InstalledApplication>> installedAppsObservable () {
        return applicationStateChangedSubject
                .startWith(Observable.just(false))
                // todo: take different actions
                .flatMap(installed -> Observable.just(loadApps(context)));
    }
    
    // todo: write apps times WorkManager
    private PriorityQueue<InstalledApplication> loadApps (Context context) {
        PriorityQueue<InstalledApplication> applications = new PriorityQueue<>();
        
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        
        PackageManager mgr = context.getPackageManager();
        
        List<ResolveInfo> main;
        try {
            main = mgr.queryIntentActivities(i, 0);
        } catch (Exception e) {
            return applications;
        }
        
        SharedPreferences preferences = context.getSharedPreferences(AppsManager.APPS_LAUNCHTIMES_FILENAME, Context.MODE_PRIVATE);
        
        // if t-ui is the default launcher, we can try to get app shortcuts. otherwise revert to the old method for gathering apps
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && Tuils.isMyLauncherDefault(context.getPackageManager())) {
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            
            Flowable.fromIterable(main)
                    .parallel()
                    .runOn(Schedulers.io())
                    .map(resolveInfo -> {
                        // attempt to get ShortcutInfo
                        List<ShortcutInfo> shortcutInfos = null;
                        try {
                            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
                            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST | LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC);
                            query.setPackage(resolveInfo.activityInfo.packageName);
                            shortcutInfos = launcherApps.getShortcuts(query, Process.myUserHandle());
                        } catch (Throwable e) {
                            // t-ui is not the default launcher
                            Tuils.log(e);
                        }
                        
                        return new Tuils.BiPack<>(resolveInfo, shortcutInfos);
                    })
                    .map(biPack -> new InstalledApplication(biPack.object1.activityInfo.packageName,
                            biPack.object1.activityInfo.name, biPack.object1.loadLabel(mgr)
                            .toString(), biPack.object2))
                    .doOnNext(application -> application.setLaunchedTimes(preferences.getInt(application.getKey(), 0)))
                    .doOnNext(applications::add)
                    .sequential()
                    .subscribe();
        } else {
            Flowable.fromIterable(main)
                    .parallel()
                    .runOn(Schedulers.io())
                    .map(resolveInfo -> new InstalledApplication(resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name, resolveInfo.loadLabel(mgr)
                            .toString(), null))
                    .doOnNext(application -> application.setLaunchedTimes(preferences.getInt(application.getKey(), 0)))
                    .doOnNext(applications::add)
                    .sequential()
                    .subscribe();
        }
        
        return applications;
    }
    
    public void fill () {
        final List<InstalledApplication> allApps = createAppMap(context.getPackageManager());
        hiddenApps = new ArrayList<>();
        
        groups.clear();
        
        try {
            prefsList = new SettingsEntriesContainer();
            
            if (file != null) {
                if (!file.exists()) {
                    resetFile(file, NAME);
                }
                
                Object[] o;
                try {
                    o = SettingsManager.buildDocument(file, NAME);
                    if (o == null) {
                        Tuils.sendXMLParseError(context, PATH);
                        return;
                    }
                } catch (SAXParseException e) {
                    Tuils.sendXMLParseError(context, PATH, e);
                    return;
                } catch (Exception e) {
                    Tuils.log(e);
                    return;
                }
                
                Document d = (Document) o[0];
                Element root = (Element) o[1];
                
                List<Apps> enums = new ArrayList<>(Arrays.asList(Apps.values()));
                NodeList nodes = root.getElementsByTagName("*");
                
                for (int count = 0; count < nodes.getLength(); count++) {
                    final Node node = nodes.item(count);
                    
                    String nn = node.getNodeName();
                    int nodeIndex = Tuils.find(nn, (List) enums);
                    if (nodeIndex != -1) {
                        //                        default_app...
                        if (nn.startsWith("d")) {
                            prefsList.add(nn, node.getAttributes()
                                    .getNamedItem(VALUE_ATTRIBUTE)
                                    .getNodeValue());
                        } else {
                            prefsList.add(nn, SettingsManager.getStringAttribute((Element) node, VALUE_ATTRIBUTE));
                        }
                        
                        for (int en = 0; en < enums.size(); en++) {
                            if (enums.get(en)
                                    .label()
                                    .equals(nn)) {
                                enums.remove(en);
                                break;
                            }
                        }
                    }
                    //                todo support delete
                    else {
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            final Element e = (Element) node;
                            
                            if (e.hasAttribute(APPS_ATTRIBUTE)) {
                                final String name = e.getNodeName();
                                if (name.contains(Tuils.SPACE)) {
                                    Tuils.sendOutput(Color.RED, context, PATH + ": " + context.getString(R.string.output_groupspace) + ": " + name);
                                    continue;
                                }
                                
                                new StoppableThread() {
                                    @Override
                                    public void run () {
                                        super.run();
                                        
                                        AppsManager.Group g = new AppsManager.Group(name);
                                        
                                        String apps = e.getAttribute(APPS_ATTRIBUTE);
                                        String[] split = apps.split(APPS_SEPARATOR);
                                        
                                        List<InstalledApplication> as = new ArrayList<>(allApps);
                                        
                                        External:
                                        for (String s : split) {
                                            for (int c = 0; c < as.size(); c++) {
                                                if (as.get(c)
                                                        .equals(s)) {
                                                    g.add(as.remove(c), false);
                                                    continue External;
                                                }
                                            }
                                        }
                                        
                                        g.sort();
                                        
                                        if (e.hasAttribute(BGCOLOR_ATTRIBUTE)) {
                                            String c = e.getAttribute(BGCOLOR_ATTRIBUTE);
                                            if (c.length() > 0) {
                                                try {
                                                    g.setBgColor(Color.parseColor(c));
                                                } catch (Exception e) {
                                                    Tuils.sendOutput(Color.RED, context, PATH + ": " + context.getString(R.string.output_invalidcolor) + ": " + c);
                                                }
                                            }
                                        }
                                        
                                        if (e.hasAttribute(FORECOLOR_ATTRIBUTE)) {
                                            String c = e.getAttribute(FORECOLOR_ATTRIBUTE);
                                            if (c.length() > 0) {
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
                                            for (InstalledApplication i : allApps) {
                                                if (i.componentName.getClassName()
                                                        .equals(split[0]))
                                                    name = i.componentName;
                                            }
                                        } else {
                                            for (InstalledApplication i : allApps) {
                                                if (i.componentName.getPackageName()
                                                        .equals(split[0]))
                                                    name = i.componentName;
                                            }
                                        }
                                    }
                                    
                                    if (name == null) continue;
                                    
                                    InstalledApplication removed = AppsManager.AppUtils.findLaunchInfoWithComponent(allApps, name);
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
                    for (SettingsOption s : enums) {
                        String value = s.defaultValue();
                        
                        Element em = d.createElement(s.label());
                        em.setAttribute(VALUE_ATTRIBUTE, value);
                        root.appendChild(em);
                        
                        prefsList.add(s.label(), value);
                    }
                    writeTo(d, file);
                }
            } else {
                Tuils.sendOutput(Color.RED, context, R.string.tuinotfound_app);
            }
            
            for (Map.Entry<String, ?> entry : this.preferences.getAll()
                    .entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Integer) {
                    ComponentName name = null;
                    
                    String[] split = entry.getKey()
                            .split("-");
                    if (split.length >= 2) {
                        name = new ComponentName(split[0], split[1]);
                    } else if (split.length == 1) {
                        if (split[0].contains("Activity")) {
                            for (InstalledApplication i : allApps) {
                                if (i.componentName.getClassName()
                                        .equals(split[0]))
                                    name = i.componentName;
                            }
                        } else {
                            for (InstalledApplication i : allApps) {
                                if (i.componentName.getPackageName()
                                        .equals(split[0]))
                                    name = i.componentName;
                            }
                        }
                    }
                    
                    if (name == null) continue;
                    
                    InstalledApplication info = AppsManager.AppUtils.findLaunchInfoWithComponent(allApps, name);
                    if (info != null) info.launchedTimes = (Integer) value;
                }
            }
            
        } catch (Exception e1) {
            Tuils.toFile(e1);
        }
        
        appsHolder = new AppsManager.AppsHolder(allApps, prefsList);
        AppsManager.AppUtils.checkEquality(hiddenApps);
        
        AppsManager.Group.sorting = SettingsManager.getInt(Apps.app_groups_sorting);
        for (AppsManager.Group g : groups) g.sort();
        Collections.sort(groups, (o1, o2) -> Tuils.alphabeticCompare(o1.name(), o2.name()));
    }
    
    // register the receiver for new installed/uninstalled apps
    private void registerApplicationStateChangedReceiver () {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        
        this.context.registerReceiver(applicationStateChangedListener, intentFilter);
    }
    
    public void dispose() {
        context.unregisterReceiver(applicationStateChangedListener);
    }
}
