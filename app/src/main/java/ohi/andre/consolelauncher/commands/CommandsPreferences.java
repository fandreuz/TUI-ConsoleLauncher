package ohi.andre.consolelauncher.commands;

import android.util.SparseArray;

import java.util.HashMap;

import ohi.andre.consolelauncher.managers.XMLPrefsManager;

/**
 * Created by francescoandreuzzi on 06/01/2017.
 */

public class CommandsPreferences {

    public static final int DEFAULT_PARAM = 10;

    private HashMap<String, Preference> preferenceHashMap;

    public CommandsPreferences() {
        preferenceHashMap = new HashMap<>();

//        search
        Preference searchP = new Preference();
        searchP.add(XMLPrefsManager.get(String.class, XMLPrefsManager.Cmd.default_search), DEFAULT_PARAM);
        preferenceHashMap.put("search", searchP);
    }

    public Preference forCommand(String cmd) {
        return preferenceHashMap.get(cmd);
    }

    public class Preference {
        SparseArray<String> prefs = new SparseArray<>();
        public void add(String pref, int id) {
            prefs.put(id, pref);
        }
        public String get(int id) {
            return prefs.get(id);
        }
    }
}
