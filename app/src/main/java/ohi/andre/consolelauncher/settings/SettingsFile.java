package ohi.andre.consolelauncher.settings;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

//

/*
Defines info about a settings file (path, values, ...)
 */
public interface SettingsFile {
    Set<String> options();
    SettingsEntry entry(String option);
    HashMap<String, SettingsEntry> entriesCopy();

    // the path on this file (TUI-FOLDER/path())
    String path();

    String label();

    void clear();
}