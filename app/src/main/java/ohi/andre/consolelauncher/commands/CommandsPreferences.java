package ohi.andre.consolelauncher.commands;

import java.util.HashMap;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;
import ohi.andre.consolelauncher.managers.xml.options.Cmd;

/**
 * Created by francescoandreuzzi on 06/01/2017.
 */

public class CommandsPreferences {

    public static final String PRIORITY_SUFFIX = "_priority";

    private HashMap<String, String> preferenceHashMap;

    public CommandsPreferences() {
        preferenceHashMap = new HashMap<>();

        for(XMLPrefsSave save : Cmd.values()) {
            preferenceHashMap.put(save.label(), XMLPrefsManager.get(save));
        }
    }

    public String get(String s) {
        String v = preferenceHashMap.get(s);
        if(v == null) return XMLPrefsManager.get(XMLPrefsManager.XMLPrefsRoot.CMD, s);
        return v;
    }

    public String get(XMLPrefsSave save) {
        String v = get(save.label());
        if(v == null || v.length() == 0) v = save.defaultValue();
        return v;
    }

    public int userSetPriority(CommandAbstraction c) {
        try {
            String p = get(c.getClass().getSimpleName() + PRIORITY_SUFFIX);
            return Integer.parseInt(p);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    public int getPriority(CommandAbstraction c) {
        int priority = userSetPriority(c);
        if(priority == Integer.MAX_VALUE) return c.priority();
        return priority;
    }
}
