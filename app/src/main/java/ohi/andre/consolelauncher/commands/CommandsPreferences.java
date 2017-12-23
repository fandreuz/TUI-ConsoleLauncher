package ohi.andre.consolelauncher.commands;

import java.util.HashMap;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Cmd;

/**
 * Created by francescoandreuzzi on 06/01/2017.
 */

public class CommandsPreferences {

    private HashMap<String, String> preferenceHashMap;

    public CommandsPreferences() {
        preferenceHashMap = new HashMap<>();

        for(XMLPrefsManager.XMLPrefsSave save : Cmd.values()) {
            preferenceHashMap.put(save.label(), XMLPrefsManager.get(save));
        }
    }

    public String get(XMLPrefsManager.XMLPrefsSave save) {
        String v = preferenceHashMap.get(save.label());
        if(v == null || v.length() == 0) v = save.defaultValue();
        return v;
    }
}
