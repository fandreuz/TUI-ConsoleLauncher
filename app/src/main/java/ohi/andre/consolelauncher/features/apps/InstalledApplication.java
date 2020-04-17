package ohi.andre.consolelauncher.features.apps;

import android.content.ComponentName;
import android.content.pm.ShortcutInfo;
import android.support.annotation.NonNull;

import java.util.List;

import ohi.andre.consolelauncher.tuils.Tuils;

// represents an installed (or ex-installed) application on the device
public class InstalledApplication implements Comparable<InstalledApplication> {
    // a fake instance of the class, used when an instance is needed
    public static final InstalledApplication fakeInstance = new InstalledApplication();
    
    public final ComponentName componentName;
    
    // i.e. Settings, Clash of Clans, ...
    public final String publicLabel;
    
    // kept for optimization
    public final String nospacesLowercaseLabel, lowercaseLabel;
    
    private boolean hidden;
    
    // the number of times this applications has been launched from t-ui.
    // this is used for sorting purposes
    private int launchedTimes = 0;
    
    // the last instant this app was launched. used for sorting purposes
    private long lastLaunched;
    
    public final List<ShortcutInfo> shortcuts;
    
    // create a new InstalledApplication from the packageName, the name of the main activity, the public
    // label and the list of Shortcuts
    public InstalledApplication (String packageName, String mainActivityName, String publicLabel, List<ShortcutInfo> shortcutInfos) {
        this.componentName = new ComponentName(packageName, mainActivityName);
        
        // setup public label
        this.publicLabel            = publicLabel;
        this.lowercaseLabel         = publicLabel.toLowerCase();
        this.nospacesLowercaseLabel = Tuils.removeSpaces(lowercaseLabel);
        
        this.shortcuts = shortcutInfos;
    }
    
    private InstalledApplication () {
        this.componentName          = null;
        this.publicLabel            = null;
        this.nospacesLowercaseLabel = null;
        this.lowercaseLabel         = null;
        this.shortcuts              = null;
    }
    
    // this should trigger an update of the priority queue
    protected void setLaunchedTimes(int launchedTimes) {
        this.launchedTimes = launchedTimes;
    }
    
    public int getLaunchedTimes () {
        return launchedTimes;
    }
    
    protected void setLastLaunched(long when) {
        this.lastLaunched = when;
    }
    
    public long getLastLaunched () {
        return lastLaunched;
    }
    
    // check whether this InstalledApplication belongs to the list of applications. this is used to check
    // whether this instance belongs to an app group
    public boolean belongs (String apps) {
        String[] split = apps.split(AppsManager.APPS_SEPARATOR);
        for (String s : split) if (getKey().equals(s)) return true;
        return false;
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    protected void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    
    protected void increaseLaunchTimes() {
        this.launchedTimes++;
    }
    
    @Override
    public boolean equals (Object o) {
        if (o == null) return false;
        if (o instanceof InstalledApplication)
            return this == o || this.componentName.equals(((InstalledApplication) o).componentName);
        return false;
    }
    
    @Override
    public String toString () {
        return componentName.flattenToString() + " --> " + publicLabel + ", n=" + launchedTimes;
    }
    
    // returns a key which uniquely identifies this instance. this is used to persist the number
    // of launched times and for app groups
    public String getKey () {
        return this.componentName.flattenToString();
    }
    
    // this is
    @Override
    public int compareTo (@NonNull InstalledApplication o) {
        // < 0 if this has been launched MORE
        // > 0 if this has been launched LESS
        return o.launchedTimes - launchedTimes;
    }
}