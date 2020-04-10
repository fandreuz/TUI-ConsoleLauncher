package ohi.andre.consolelauncher.managers.settings;

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.settings.options.Behavior;
import ohi.andre.consolelauncher.managers.settings.options.Cmd;
import ohi.andre.consolelauncher.managers.settings.options.Suggestions;
import ohi.andre.consolelauncher.managers.settings.options.Theme;
import ohi.andre.consolelauncher.managers.settings.options.Toolbar;
import ohi.andre.consolelauncher.managers.settings.options.Ui;
import ohi.andre.consolelauncher.tuils.Color;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.xml.XMLUtils;

public class SettingsManager {
    
    public static final String VALUE_ATTRIBUTE = "value";
    
    private static SettingsManager instance;
    
    private final XMLUtils xmlUtils;
    
    // holds previously generated instances of the settings files, retained to improve the performance
    private final HashMap<String, File> settingsFiles;
    
    public static SettingsManager getInstance () {
        if (instance == null) instance = new SettingsManager();
        return instance;
    }
    
    private SettingsManager () {
        xmlUtils      = new XMLUtils();
        settingsFiles = new HashMap<>();
    }
    
    // let SettingsManager know that there's someone who wants to be updated when a SettingsEntry
    // changes.
    // valueClass is the class of the requested value
    // this is an Hot observable
    public <T> Observable<T> requestUpdates (SettingsOption option, Class<T> valueClass) {
        return option
                .entry()
                .subscribe(valueClass)
                .share();
    }
    
    // build an Observable which retrieves a list of T from a String
    public <T> Observable<T[]> requestList (SettingsOption option, Class<T> valueClass, T defaultValue, String divider) {
        Function<String, T> map = buildFunction(valueClass);
        
        Function<String[], T[]> f = ss -> {
            Object[] ts = new Object[ss.length];
            for (int i = 0; i < ss.length; i++) {
                try {
                    ts[i] = map.apply(ss[i]);
                } catch (Exception e) {
                    ts[i] = defaultValue;
                }
            }
            return (T[]) ts;
        };
        
        return requestUpdates(option, String.class)
                .map(s -> s.split(divider))
                .map(f);
    }
    
    public <T> Observable<T[]> requestList (SettingsOption option, Class<T> valueClass, T defaultValue) {
        return requestList(option, valueClass, defaultValue, ",");
    }
    
    // build a function which maps String -> T
    private static <T> Function<String, T> buildFunction (Class<T> clazz) {
        if (clazz == String.class) return (string) -> (T) string;
            // I don't know why this works and (T) Integer.parseInt doesn't...
        else if (clazz == int.class) return s -> (T) (Object) Integer.parseInt(s);
        else if (clazz == float.class) return s -> (T) (Object) Float.parseFloat(s);
        else if (clazz == Color.class)
            return s -> (T) new Color(android.graphics.Color.parseColor(s));
        else throw new IllegalArgumentException(clazz.toString() + " is not supported yet");
    }
    
    // loads the settings file and stores the values.
    // multi-threaded by default
    public void loadSettings (Context context) {
        File tuiFolder = Tuils.getFolder();
        if (tuiFolder == null) {
            Tuils.sendOutput(android.graphics.Color.RED, context, R.string.tuinotfound_xmlprefs);
            return;
        }
        
        // todo: mancano reply, apps, ...
        Flowable.fromArray(SettingsGroup.values())
                .parallel()
                .runOn(Schedulers.io())
                // build the xml pack
                .map(settingsFile -> XMLPack.build(settingsFile, getOrUpdateFile(settingsFile.path), xmlUtils))
                .doOnNext(pack -> {
                    if (pack == null) Tuils.sendXMLParseError(context, pack.settingsFile.path());
                })
                .filter(pack -> pack != null)
                .doOnNext(pack -> readSettingsFromPack(pack, xmlUtils))
                .sequential()
                .subscribe();
    }
    
    // lookup the given path in settingsFile, and create a new file if it's not already there
    private File getOrUpdateFile (String path) {
        // check if the file is already in the map
        File file;
        synchronized (settingsFiles) {
            file = settingsFiles.get(path);
        }
        
        // if not, put it
        if (file == null) {
            file = new File(Tuils.getFolder(), path);
            
            // update the map
            synchronized (settingsFiles) {
                settingsFiles.put(path, file);
            }
        }
        
        return file;
    }
    
    // read all the settings inside the given XMLPack, using the given XMLUtils
    private static void readSettingsFromPack (XMLPack pack, XMLUtils xmlUtils) {
        HashMap<String, SettingsEntry> options = pack.settingsFile.entriesCopy();
        
        // a flag to remember whether there has been changed that should
        // be written to the file
        boolean changesFlag = false;
        
        for (int count = 0; count < pack.nodeList.getLength(); count++) {
            Node node = pack.nodeList.item(count);
            String nodeName = node.getNodeName();
            
            // options is only a copy of the original map, but we are still able to
            // call set() on SettingsEntry.
            SettingsEntry entry = options.remove(nodeName);
            
            if (entry == null) {
                Element e = (Element) node;
                pack.root.removeChild(e);
                
                changesFlag = true;
            } else {
                String value;
                try {
                    value = node.getAttributes()
                            .getNamedItem(VALUE_ATTRIBUTE)
                            .getNodeValue();
                } catch (Exception e) {
                    value = entry.option.defaultValue();
                    e.printStackTrace();
                }
                
                entry.set(value);
            }
        }
        
        // if there are options left, it means that these options are not present inside
        // the file. we need to manually set their value to the default one.
        // then we have to add the options to the file.
        for (SettingsEntry entry : options.values()) {
            String value = entry.option.defaultValue();
            // set the default value
            entry.set(value);
            
            Element em = pack.document.createElement(entry.option.label());
            em.setAttribute(VALUE_ATTRIBUTE, value);
            pack.root.appendChild(em);
            
            changesFlag = true;
        }
        
        if (changesFlag) xmlUtils.write(pack.document, pack.file);
    }
    
    public void dispose () {
        instance = null;
        
        for (SettingsFile file : SettingsGroup.values()) {
            file.clear();
        }
        
        settingsFiles.clear();
    }
    
    public void write (SettingsOption option, String value) {
        xmlUtils.set(cachedOrNewFile(option.parent()
                .path()), option.label(), new String[]{VALUE_ATTRIBUTE}, new String[]{value});
    }
    
    private File cachedOrNewFile (String path) {
        File f = settingsFiles.get(path);
        return f != null ? f : new File(Tuils.getFolder(), path);
    }
    
    // a convenient method to convert the given string to the given type
    public static Object transform (String s, Class<?> c) {
        if (s == null) throw new IllegalArgumentException("s is null");
        
        if (c == int.class || c == Integer.class) return Integer.parseInt(s);
        if (c == Color.class) return android.graphics.Color.parseColor(s);
        if (c == boolean.class || c == Boolean.class) return Boolean.parseBoolean(s);
        if (c == String.class) return s;
        if (c == float.class || c == Float.class) return Float.parseFloat(s);
        if (c == double.class || c == Double.class) return Double.parseDouble(s);
        if (c == File.class) {
            if (s.length() == 0) return null;
            
            File file = new File(s);
            if (!file.exists()) throw new UnsupportedOperationException();
            
            return file;
        }
        
        return Tuils.getDefaultValue(c);
    }
    
    // defines a group of settings sharing a common field of application
    public enum SettingsGroup implements SettingsFile {
        
        THEME(Theme.values()),
        CMD(Cmd.values()),
        TOOLBAR(Toolbar.values()),
        UI(Ui.values()),
        BEHAVIOR(Behavior.values()),
        SUGGESTIONS(Suggestions.values());
        
        //        notifications
        //        apps
        //        alias
        
        public final  String                   path;
        private final SettingsEntriesContainer values;
        
        SettingsGroup (SettingsOption[] options) {
            this.values = new SettingsEntriesContainer(options);
            this.path   = this.name()
                    .toLowerCase() + ".xml";
        }
        
        @Override
        public String path () {
            return path;
        }
        
        @Override
        public void clear () {
            values.clear();
        }
        
        @Override
        public String label () {
            return name();
        }
        
        @Override
        public Set<String> options () {
            return values.options();
        }
        
        @Override
        public SettingsEntry entry (String option) {
            return values.entry(option);
        }
        
        @Override
        public HashMap<String, SettingsEntry> entriesCopy () {
            return values.entryCopy();
        }
    }
    
    // used to send around info about an XML file
    private static class XMLPack {
        public final SettingsFile settingsFile;
        public final NodeList     nodeList;
        public final Document     document;
        public final Element      root;
        public final File         file;
        
        private XMLPack (SettingsFile settingsFile, NodeList nodeList, Document document, Element root, File file) {
            this.settingsFile = settingsFile;
            this.nodeList     = nodeList;
            this.document     = document;
            this.root         = root;
            this.file         = file;
        }
        
        public static XMLPack build (SettingsFile settingsFile, File file, XMLUtils xmlUtils) {
            Object[] pack;
            try {
                pack = xmlUtils.buildDocument(file, settingsFile.label());
            } catch (Exception e) {
                Tuils.log(e);
                return null;
            }
            
            if (pack == null) {
                return null;
            }
            
            Document d = (Document) pack[0];
            Element root = (Element) pack[1];
            
            NodeList nodes = root.getElementsByTagName("*");
            return new XMLPack(settingsFile, nodes, d, root, file);
        }
    }
}
