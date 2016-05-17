package ohi.andre.consolelauncher.managers;

import android.annotation.SuppressLint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ohi.andre.consolelauncher.tuils.Tuils;

public class PreferencesManager {

    public static final String SETTINGS_FILENAME = "settings.txt";
    public static final int SETTINGS = 10;
    //	  skin values
    public static final String DEVICE = "deviceColor";
    public static final String INPUT = "inputColor";
    public static final String OUTPUT = "outputColor";
    public static final String BG = "backgroundColor";
    public static final String USE_SYSTEMFONT = "useSystemFont";
    public static final String FONTSIZE = "fontSize";
    public static final String RAM = "ramColor";
    public static final String INPUTFIELD_BOTTOM = "inputFieldBottom";
    public static final String USERNAME = "username";
    public static final String SHOWUSERNAME = "showUsername";
    public static final String SHOWSUBMIT = "showSubmit";
    public static final String DEVICENAME = "deviceName";
    public static final String SHOWRAM = "showRam";
    public static final String SHOWDEVICE = "showDevice";
    public static final String SUGGESTION_BG = "suggestionBg";
    public static final String SUGGESTION_COLOR = "suggestionColor";
    public static final String TRANSPARENT_SUGGESTIONS = "transparentSuggestions";
    //	  other values
    public static final String DOUBLETAP = "closeOnDbTap";
    public static final String SHOWSUGGESTIONS = "showSuggestions";
    //	  music values
    public static final String PLAY_RANDOM = "playRandom";
    public static final String SONGSFOLDER = "songsFolder";
    //	  preload values
    public static final String USE_SYSTEMWP = "useSystemWallpaper";
    public static final String FULLSCREEN = "fullscreen";
    public static final String NOTIFICATION = "keepAliveWithNotification";
    public static final String OPEN_KEYBOARD = "openKeyboardOnStart";

    public static final String ALIAS_FILENAME = "alias.txt";
    public static final int ALIAS = 11;

    //    settings version
    private static final String SETTINGS_VERSION = "settingsVersion";
    private List<String> settings;
    private List<String> aliases;
    private File folder;
    private InputStream rawSettingsFile;
    private InputStream rawAliasFile;

    public PreferencesManager(InputStream rawSettings, InputStream rawAlias, File folder) throws IOException {
        this.folder = folder;
        this.rawAliasFile = rawAlias;
        this.rawSettingsFile = rawSettings;

        refresh(SETTINGS);
        refresh(ALIAS);
    }

    @SuppressLint("DefaultLocale")
    private List<String> read(int n) throws IOException {

        List<String> list = new ArrayList<>();

        File f = open(n);
        FileInputStream fis = new FileInputStream(f);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

        String line = reader.readLine();
        while (line != null) {
            if (!line.startsWith("//") && !line.startsWith("\n") && line.length() > 0) {
//                can't replace all spaces in aliases
                if (n != ALIAS)
                    line = line.replaceAll("\\s+", "");
                else
                    line = Tuils.trimSpaces(line);
                list.add(line);
            }

            line = reader.readLine();
        }

        fis.close();
        reader.close();

        return list;
    }

    private File open(final int n) throws IOException {
        String name;
        switch (n) {
            case PreferencesManager.SETTINGS:
                name = SETTINGS_FILENAME;
                break;
            case PreferencesManager.ALIAS:
                name = ALIAS_FILENAME;
                break;
            default:
                return null;
        }

        File file = new File(folder, name);
        if (createOrUpdateFile(file, n))
            return file;
        return null;
    }

    private boolean createOrUpdateFile(File file, int type) throws IOException {
        List<String> newValues;
//        transfer alias from an old version of assoc.txt
        if (type == ALIAS) {
//            return true if alias.txt exists
            if (file.exists())
                return true;

//            if doesn't exist, use the template, and check if there are alias in alias.txt
            newValues = readAllInput(rawAliasFile);
//        verify if the file already exists, and get old values
        } else if (type == SETTINGS) {
            List<String> oldValues = null;
            if (file.exists()) {
                InputStream old = new FileInputStream(file);
                oldValues = readAllInput(old);
            }

            newValues = readAllInput(rawSettingsFile);

            if (oldValues != null) { //=> settings.txt exists
                String oldVersionString = getValue(oldValues, SETTINGS_VERSION);
                String versionString = getValue(newValues, SETTINGS_VERSION);

                float oldVersion = oldVersionString == null ? 0.0f : Float.parseFloat(oldVersionString);
                float currentVersion = Float.parseFloat(versionString);

                if (oldVersion < currentVersion) {
                    file.delete();
                    file.createNewFile();

                    transferValues(newValues, oldValues);
                } else
                    return true;
            } else { // settings.txt doesn't exist
                file.createNewFile();
            }
        } else
            return false;

//        FROM HERE: write values to the file
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(file);
        } catch (Exception e) {
            return false;
        }

        try {
            stream.write(Tuils.toPlanString(newValues, "\n").getBytes());
            stream.flush();
            stream.close();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void transferValues(List<String> newValues, List<String> oldValues) {
        String key;
        for (String s : oldValues) {
            key = obtainKey(s);
            if (key == null || key.equals(SETTINGS_VERSION))
                continue;

            int indexOfKey = Tuils.findPrefix(newValues, key);
            if (indexOfKey == -1)
                continue;

            newValues.set(indexOfKey, setValue(newValues.get(indexOfKey), getValue(oldValues, key)));
        }
    }

    public String getLine(int n, int index) {
        if (n == PreferencesManager.ALIAS && index < aliases.size())
            return aliases.get(index);
        if (n == PreferencesManager.SETTINGS && index < settings.size())
            return settings.get(index);
        return null;
    }

    private String setValue(String line, String value) {
        int equalsIndex = line.indexOf("=");
        if (equalsIndex == -1)
            return null;

        return line.substring(0, equalsIndex + 1).concat(value);
    }

    //    default method for settings
    public String getValue(String key) {
        return getValue(PreferencesManager.SETTINGS, key);
    }

    public String getValue(int n, String key) {
        return getValue(n == PreferencesManager.SETTINGS ? settings : aliases, key);
    }

    @SuppressLint("DefaultLocale")
    private String getValue(List<String> values, String key) {
        for (String s : values) {
            String k = obtainKey(s);
            if (k != null && k.equals(key))
                return obtainValue(s);
        }

        return null;
    }

    private List<String> readAllInput(InputStream i) {
        BufferedReader br = null;

        List<String> list = new ArrayList<>();
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(i));
            while ((line = br.readLine()) != null)
                list.add(line);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return list;
    }

    private String obtainKey(String line, int equalsIndex) {
        if (equalsIndex == -1)
            return null;
        return line.substring(0, equalsIndex);
    }

    public String obtainKey(String line) {
        return obtainKey(line, line.indexOf("="));
    }

    @SuppressLint("DefaultLocale")
    private String obtainValue(String line, int equalsIndex) {
        if (equalsIndex == -1)
            return null;
        return line.substring(equalsIndex + 1).toLowerCase();
    }

    public String obtainValue(String line) {
        return obtainValue(line, line.indexOf("="));
    }

    public int getLength(int n) {
        return n == PreferencesManager.SETTINGS ? settings.size() : aliases.size();
    }

    public void refresh(int n) {
        try {
            if (n == ALIAS)
                aliases = read(ALIAS);
            else if (n == SETTINGS)
                settings = read(SETTINGS);
        } catch (IOException e) {
        }
    }

}
