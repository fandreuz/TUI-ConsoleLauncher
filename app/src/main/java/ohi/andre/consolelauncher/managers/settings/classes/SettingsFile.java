package ohi.andre.consolelauncher.managers.settings.classes;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

//

/*
Defines info about a settings file (path, values, ...)
 */
public interface SettingsFile {
    // the list of entries in this file
    SettingsEntriesContainer values();

    // write on this file
    void write(SettingsOption save, String value);

    // the list of old entries to be deleted as soon as possible
    String[] delete();

    // the path on this file (TUI-FOLDER/path())
    String path();
}