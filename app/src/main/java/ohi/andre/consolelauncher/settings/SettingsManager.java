package ohi.andre.consolelauncher.settings;

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.settings.options.Behavior;
import ohi.andre.consolelauncher.settings.options.Cmd;
import ohi.andre.consolelauncher.settings.options.Suggestions;
import ohi.andre.consolelauncher.settings.options.Theme;
import ohi.andre.consolelauncher.settings.options.Toolbar;
import ohi.andre.consolelauncher.settings.options.Ui;
import ohi.andre.consolelauncher.tuils.Color;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.xml.XMLUtils;

public class SettingsManager {

    public static final String VALUE_ATTRIBUTE = "value";

    private static SettingsManager instance;

    private final XMLUtils xmlUtils;

    // holds previously generated instances of the settings files, retained to improve the performance
    private final HashMap<String, File> settingsFiles;

    // holds packs of NodeList associated with the corresponding SettingsFile
    // this will be used in Product/Consumer for loadSettings
    private BlockingQueue<XMLPack> nodesQueue;

    // used to host the files read by Producer
    private BlockingQueue<SettingsFile> filesQueue;

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
    // this is an Hot observable
    public <T> Observable<T> requestUpdates(SettingsOption option, Class<T> valueClass) {
        return option
                .entry()
                .subscribe(valueClass)
                .share();
    }

    // build an Observable which retrieves a list of T from a String
    public <T> Observable<T[]> requestList(SettingsOption option, Class<T> valueClass, T defaultValue, String divider) {
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

    public <T> Observable<T[]> requestList(SettingsOption option, Class<T> valueClass, T defaultValue) {
        return requestList(option, valueClass, defaultValue, ",");
    }

    // build a function which maps String -> T
    private static <T> Function<String, T> buildFunction(Class<T> clazz) {
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
    public void loadSettings(Context context) {
        File tuiFolder = Tuils.getFolder();
        if (tuiFolder == null) {
            Tuils.sendOutput(android.graphics.Color.RED, context, R.string.tuinotfound_xmlprefs);
            return;
        }

        // todo: mancano reply, apps, ...
        nodesQueue = new ArrayBlockingQueue<>(SettingsFiles.values().length);
        filesQueue = new ArrayBlockingQueue<>(SettingsFiles.values().length, false, Arrays.asList(SettingsFiles.values()));

        Executor producers = Executors.newCachedThreadPool();
        Executor consumers = Executors.newCachedThreadPool();

        final int CONSUMERS = 2, PRODUCERS = 4;

        for (int i = 0; i < PRODUCERS; i++) {
            producers.execute(new Producer(context, tuiFolder));
        }

        for (int i = 0; i < CONSUMERS; i++) {
            consumers.execute(new Consumer());
        }
    }

    private class Producer implements Runnable {
        // todo: remove context
        private final Context context;
        private final File tuiFolder;

        public Producer(Context context, File tuiFolder) {
            this.context = context;
            this.tuiFolder = tuiFolder;
        }

        @Override
        public void run() {
            while (filesQueue.size() > 0) {
                SettingsFile settingsFile = null;
                try {
                    settingsFile = filesQueue.poll(10, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }

                File file = settingsFiles.get(settingsFile.path());
                if (file == null) {
                    file = new File(tuiFolder, settingsFile.path());

                    synchronized (settingsFiles) {
                        settingsFiles.put(settingsFile.path(), file);
                    }
                }

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
                    return;
                } catch (Exception e) {
                    Tuils.log(e);
                    return;
                }

                Document d = (Document) pack[0];
                Element root = (Element) pack[1];

                NodeList nodes = root.getElementsByTagName("*");
                nodesQueue.add(new XMLPack(settingsFile, nodes, d, root));
            }
        }
    }

    private class Consumer implements Runnable {
        @Override
        public void run() {
            while (true) {
                XMLPack pack = null;
                try {
                    pack = nodesQueue.poll(3000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

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
                            value = node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue();
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

                File file;
                synchronized (settingsFiles) {
                    file = settingsFiles.get(pack.settingsFile.path());
                }

                if (changesFlag) xmlUtils.write(pack.document, file);
            }
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

    public enum SettingsFiles implements SettingsFile {

        THEME(Theme.values()),
        CMD(Cmd.values()),
        TOOLBAR(Toolbar.values()),
        UI(Ui.values()),
        BEHAVIOR(Behavior.values()),
        SUGGESTIONS(Suggestions.values());

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

    private static class XMLPack {
        public final SettingsFile settingsFile;
        public final NodeList nodeList;
        public Document document;
        public Element root;

        public XMLPack(SettingsFile settingsFile, NodeList nodeList, Document document, Element root) {
            this.settingsFile = settingsFile;
            this.nodeList = nodeList;
            this.document = document;
            this.root = root;
        }
    }
}
