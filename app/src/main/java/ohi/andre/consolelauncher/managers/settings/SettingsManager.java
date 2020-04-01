package ohi.andre.consolelauncher.managers.settings;

import android.content.Context;
import android.graphics.Color;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

import io.reactivex.rxjava3.core.Observer;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.settings.options.Behavior;
import ohi.andre.consolelauncher.managers.settings.options.Cmd;
import ohi.andre.consolelauncher.managers.settings.options.Suggestions;
import ohi.andre.consolelauncher.managers.settings.options.Theme;
import ohi.andre.consolelauncher.managers.settings.options.Toolbar;
import ohi.andre.consolelauncher.managers.settings.options.Ui;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.xml.XMLUtils;

public class SettingsManager {

    public static final String VALUE_ATTRIBUTE = "value";

    private static SettingsManager instance;

    private final XMLUtils xmlUtils;

    // holds previously generated instances of the settings files, retained to improve the performance
    private final HashMap<String, File> settingsFiles;

    public static SettingsManager getInstance() {
        if (instance == null) instance = new SettingsManager();
        return instance;
    }

    private SettingsManager() {
        xmlUtils = new XMLUtils();
        settingsFiles = new HashMap<>();
    }

    // let SettingsManager know that there's someone who wants to be updated when a SettingsEntry
    // changes.
    // valueClass is the class of the requested value
    // important: users should unsubscribe themselves when needed
    public void requestUpdates(SettingsOption option, Observer observer, Class<?> valueClass) {
        option.entry().subscribe(valueClass, observer);
    }

    // make sure to call this method from a non-UI thread.
    // loads the settings file and stores the values
    public void loadSettings(Context context) {
        File tuiFolder = Tuils.getFolder();
        if (tuiFolder == null) {
            Tuils.sendOutput(Color.RED, context, R.string.tuinotfound_xmlprefs);
            return;
        }

        for (SettingsFile settingsFile : SettingsFiles.values()) {
            HashMap<String, SettingsEntry> options = settingsFile.entriesCopy();

            File file = settingsFiles.get(settingsFile.path());
            if(file == null) file = new File(tuiFolder, settingsFile.path());

            if (!file.exists()) {
                xmlUtils.resetFile(file, settingsFile.label());
            }

            Object[] pack;
            try {
                pack = xmlUtils.buildDocument(file, settingsFile.label());
                if (pack == null) {
                    Tuils.sendXMLParseError(context, settingsFile.path());
                    return;
                }
            } catch (SAXParseException e) {
                Tuils.sendXMLParseError(context, settingsFile.path(), e);
                continue;
            } catch (Exception e) {
                Tuils.log(e);
                return;
            }

            Document d = (Document) pack[0];
            Element root = (Element) pack[1];

            // cache the list of nodes to be deleted
            String[] toDelete = settingsFile.delete();

            // a flag to remember whether there has been changed that should
            // be written to the file
            boolean changesFlag = false;

            NodeList nodes = root.getElementsByTagName("*");

            for (int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);
                String nodeName = node.getNodeName();

                String value;
                try {
                    value = node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue();
                } catch (Exception e) {
                    continue;
                }

                // options is only a copy of the original map, but we are still able to
                // call set() on SettingsEntry.
                SettingsEntry entry = options.remove(nodeName);

                if (entry != null) {
                    entry.set(value);
                } else if(toDelete != null) {
                    // check whether this node must be deleted
                    int index = Tuils.find(nodeName, toDelete);
                    if (index != -1) {
                        toDelete[index] = null;
                        Element e = (Element) node;
                        root.removeChild(e);

                        changesFlag = true;
                    }
                }
            }

            // if there are options left, it means that these options are not present inside
            // the file. we need to manually set their value to the default one.
            // then we have to add the options to the file.
            for (SettingsEntry entry : options.values()) {
                String value = entry.option.defaultValue();
                // set the default value
                entry.set(value);

                Element em = d.createElement(entry.option.label());
                em.setAttribute(VALUE_ATTRIBUTE, value);
                root.appendChild(em);

                changesFlag = true;
            }

            if(changesFlag) xmlUtils.write(d, file);
        }
    }

    public void dispose() {
        instance = null;

        for (SettingsFile file : SettingsFiles.values()) {
            file.clear();
        }

        settingsFiles.clear();
    }

    public void write(SettingsOption option, String value) {
        xmlUtils.set(cachedOrNewFile(option.parent().path()), option.label(), new String[]{VALUE_ATTRIBUTE}, new String[]{value});
    }

    private File cachedOrNewFile(String path) {
        File f = settingsFiles.get(path);
        return f != null ? f : new File(Tuils.getFolder(), path);
    }

    // a convenient method to convert the given string to the given type
    public static Object transform(String s, Class<?> c) {
        if (s == null) throw new IllegalArgumentException("s is null");

        if (c == int.class) return Integer.parseInt(s);
        if (c == Color.class) return Color.parseColor(s);
        if (c == boolean.class) return Boolean.parseBoolean(s);
        if (c == String.class) return s;
        if (c == float.class) return Float.parseFloat(s);
        if (c == double.class) return Double.parseDouble(s);
        if (c == File.class) {
            if (s.length() == 0) return null;

            File file = new File(s);
            if (!file.exists()) throw new UnsupportedOperationException();

            return file;
        }

        return Tuils.getDefaultValue(c);
    }

    public enum SettingsFiles implements SettingsFile {

        THEME(Theme.values()) {
            @Override
            public String[] delete() {
                return new String[]{};
            }
        },
        CMD(Cmd.values()) {
            @Override
            public String[] delete() {
                return new String[]{};
            }
        },
        TOOLBAR(Toolbar.values()) {
            @Override
            public String[] delete() {
                return new String[]{};
            }
        },
        UI(Ui.values()) {
            @Override
            public String[] delete() {
                return new String[]{};
            }
        },
        BEHAVIOR(Behavior.values()) {
            @Override
            public String[] delete() {
                return new String[]{};
            }
        },
        SUGGESTIONS(Suggestions.values()) {
            @Override
            public String[] delete() {
                return new String[]{"app_suggestions_minrate", "contact_suggestions_minrate", "song_suggestions_minrate", "file_suggestions_minrate"};
            }
        };

//        notifications
//        apps
//        alias

        public final String path;
        private final SettingsEntriesContainer values;

        SettingsFiles(SettingsOption[] options) {
            this.values = new SettingsEntriesContainer(options);
            this.path = this.name().toLowerCase() + ".xml";
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public void clear() {
            values.clear();
        }

        @Override
        public String label() {
            return name();
        }

        @Override
        public Set<String> options() {
            return values.options();
        }

        @Override
        public SettingsEntry entry(String option) {
            return values.entry(option);
        }

        @Override
        public HashMap<String, SettingsEntry> entriesCopy() {
            return values.entryCopy();
        }
    }
}
