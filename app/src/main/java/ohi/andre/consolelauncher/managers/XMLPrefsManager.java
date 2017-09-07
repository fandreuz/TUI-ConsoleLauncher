package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.tuils.Tuils;

public class XMLPrefsManager {

    public static final String XML_DEFAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    public static final String VALUE_ATTRIBUTE = "value";

    private static DocumentBuilderFactory factory;
    private static DocumentBuilder builder;

    static {
        factory = DocumentBuilderFactory.newInstance();
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {}
    }

    public interface XMLPrefsSave {
        String defaultValue();
        String label();
        XmlPrefsElement parent();
        boolean is(String s);
    }

    public enum Theme implements XMLPrefsSave {

        input_color {
            @Override
            public String defaultValue() {
                return "#ff00ff00";
            }
        },
        output_color {
            @Override
            public String defaultValue() {
                return "#ffffffff";
            }
        },
        bg_color {
            @Override
            public String defaultValue() {
                return "#ff000000";
            }
        },
        device_color {
            @Override
            public String defaultValue() {
                return "#ffff9800";
            }
        },
        battery_color_high {
            @Override
            public String defaultValue() {
                return "#4CAF50";
            }
        },
        battery_color_medium {
            @Override
            public String defaultValue() {
                return "#FFEB3B";
            }
        },
        battery_color_low {
            @Override
            public String defaultValue() {
                return "#FF5722";
            }
        },
        time_color {
            @Override
            public String defaultValue() {
                return "#03A9F4";
            }
        },
        storage_color {
            @Override
            public String defaultValue() {
                return "#9C27B0";
            }
        },
        ram_color {
            @Override
            public String defaultValue() {
                return "#fff44336";
            }
        },
        toolbar_bg {
            @Override
            public String defaultValue() {
                return "#00000000";
            }
        },
        toolbar_color {
            @Override
            public String defaultValue() {
                return "#ffff0000";
            }
        },
        enter_color {
            @Override
            public String defaultValue() {
                return "#ffffffff";
            }
        },
        overlay_color {
            @Override
            public String defaultValue() {
                return "#80000000";
            }
        },
        alias_content_color {
            @Override
            public String defaultValue() {
                return "#1DE9B6";
            }
        },
        statusbar_color {
            @Override
            public String defaultValue() {
                return "#000000";
            }
        },
        navigationbar_color {
            @Override
            public String defaultValue() {
                return "#000000";
            }
        },
        app_installed_color {
            @Override
            public String defaultValue() {
                return "#FF7043";
            }
        },
        app_uninstalled_color {
            @Override
            public String defaultValue() {
                return "#FF7043";
            }
        },
        hint_color {
            @Override
            public String defaultValue() {
                return "#4CAF50";
            }
        };

        @Override
        public XmlPrefsElement parent() {
            return XMLPrefsRoot.THEME;
        }

        @Override
        public String label() {
            return name();
        }

        @Override
        public boolean is(String s) {
            return name().equals(s);
        }
    }

    public enum Ui implements XMLPrefsSave {

        show_enter_button {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        system_font {
            @Override
            public String defaultValue() {
                return "false";
            }
        },
        ram_size {
            @Override
            public String defaultValue() {
                return "13";
            }
        },
        battery_size {
            @Override
            public String defaultValue() {
                return "13";
            }
        },
        device_size {
            @Override
            public String defaultValue() {
                return "13";
            }
        },
        time_size {
            @Override
            public String defaultValue() {
                return "13";
            }
        },
        storage_size {
            @Override
            public String defaultValue() {
                return "13";
            }
        },
        input_output_size {
            @Override
            public String defaultValue() {
                return "13";
            }
        },
        input_bottom {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        show_ram {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        show_device_name {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        show_battery {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        show_storage_info {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        enable_battery_status {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        show_time {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        username {
            @Override
            public String defaultValue() {
                return "user";
            }
        },
        deviceName {
            @Override
            public String defaultValue() {
                return Build.DEVICE;
            }
        },
        system_wallpaper {
            @Override
            public String defaultValue() {
                return "false";
            }
        },
        fullscreen {
            @Override
            public String defaultValue() {
                return "false";
            }
        },
        device_index {
            @Override
            public String defaultValue() {
                return "0";
            }
        },
        ram_index {
            @Override
            public String defaultValue() {
                return "1";
            }
        },
        battery_index {
            @Override
            public String defaultValue() {
                return "2";
            }
        },
        time_index {
            @Override
            public String defaultValue() {
                return "3";
            }
        },
        storage_index {
            @Override
            public String defaultValue() {
                return "4";
            }
        },
        input_prefix {
            @Override
            public String defaultValue() {
                return "$";
            }
        },
        input_root_prefix {
            @Override
            public String defaultValue() {
                return "#";
            }
        },
        left_margin_mm {
            @Override
            public String defaultValue() {
                return "0";
            }
        },
        right_margin_mm {
            @Override
            public String defaultValue() {
                return "0";
            }
        },
        top_margin_mm {
            @Override
            public String defaultValue() {
                return "0";
            }
        },
        bottom_margin_mm {
            @Override
            public String defaultValue() {
                return "0";
            }
        },
        ignore_bar_color {
            @Override
            public String defaultValue() {
                return "false";
            }
        },
        show_app_installed {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        show_app_uninstalled {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        show_session_info {
            @Override
            public String defaultValue() {
                return "true";
            }
        };

        @Override
        public XmlPrefsElement parent() {
            return XMLPrefsRoot.UI;
        }

        @Override
        public String label() {
            return name();
        }

        @Override
        public boolean is(String s) {
            return name().equals(s);
        }
    }

    public enum Toolbar implements XMLPrefsSave {

        show_toolbar {
            @Override
            public String defaultValue() {
                return "true";
            }
        };

        @Override
        public XmlPrefsElement parent() {
            return XMLPrefsRoot.TOOLBAR;
        }

        @Override
        public String label() {
            return name();
        }

        @Override
        public boolean is(String s) {
            return name().equals(s);
        }
    }

    public enum Suggestions implements XMLPrefsSave {

        show_suggestions {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        transparent_suggestions {
            @Override
            public String defaultValue() {
                return "false";
            }
        },
        default_text_color {
            @Override
            public String defaultValue() {
                return "#000000";
            }
        },
        default_bg_color {
            @Override
            public String defaultValue() {
                return "#ffffff";
            }
        },
        apps_text_color {
            @Override
            public String defaultValue() {
                return "";
            }
        },
        apps_bg_color {
            @Override
            public String defaultValue() {
                return "#00897B";
            }
        },
        alias_text_color {
            @Override
            public String defaultValue() {
                return "";
            }
        },
        alias_bg_color {
            @Override
            public String defaultValue() {
                return "#FF5722";
            }
        },
        cmd_text_color {
            @Override
            public String defaultValue() {
                return "";
            }
        },
        cmd_bg_color {
            @Override
            public String defaultValue() {
                return "#76FF03";
            }
        },
        song_text_color {
            @Override
            public String defaultValue() {
                return "";
            }
        },
        song_bg_color {
            @Override
            public String defaultValue() {
                return "#EEFF41";
            }
        },
        contact_text_color {
            @Override
            public String defaultValue() {
                return "";
            }
        },
        contact_bg_color {
            @Override
            public String defaultValue() {
                return "#64FFDA";
            }
        },
        file_text_color {
            @Override
            public String defaultValue() {
                return "";
            }
        },
        file_bg_color {
            @Override
            public String defaultValue() {
                return "#03A9F4";
            }
        },
        suggest_alias_default {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        suggest_appgp_default {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        click_to_launch {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        suggestions_size {
            @Override
            public String defaultValue() {
                return "12";
            }
        };

        @Override
        public XmlPrefsElement parent() {
            return XMLPrefsRoot.SUGGESTIONS;
        }

        @Override
        public String label() {
            return name();
        }

        @Override
        public boolean is(String s) {
            return name().equals(s);
        }
    }

    public enum Behavior implements XMLPrefsSave {

        double_tap_lock {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        double_tap_cmd {
            @Override
            public String defaultValue() {
                return "";
            }
        },
        random_play {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        songs_folder {
            @Override
            public String defaultValue() {
                return "";
            }
        },
        songs_from_mediastore {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        tui_notification {
            @Override
            public String defaultValue() {
                return "false";
            }
        },
        auto_show_keyboard {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        auto_scroll {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        show_hints {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        show_alias_content {
            @Override
            public String defaultValue() {
                return "false";
            }
        },
        show_launch_history {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        clear_after_cmds {
            @Override
            public String defaultValue() {
                return "-1";
            }
        },
        clear_after_seconds {
            @Override
            public String defaultValue() {
                return "-1";
            }
        },
        max_lines {
            @Override
            public String defaultValue() {
                return "-1";
            }
        },
        time_format {
            @Override
            public String defaultValue() {
                return "%m/%d/%y %H.%M";
            }
        },
        battery_medium {
            @Override
            public String defaultValue() {
                return "50";
            }
        },
        battery_low {
            @Override
            public String defaultValue() {
                return "15";
            }
        },
        device_format {
            @Override
            public String defaultValue() {
                return "%d: %u";
            }
        },
        ram_format {
            @Override
            public String defaultValue() {
                return "Available RAM: %avgb GB of %totgb GB (%av%%)";
            }
        },
        battery_format {
            @Override
            public String defaultValue() {
                return "%v%";
            }
        },
        storage_format {
            @Override
            public String defaultValue() {
                return "Internal Storage: %iavgb GB / %itotgb GB (%iav%%)";
            }
        },
        input_format {
            @Override
            public String defaultValue() {
                return "[%t] %p %i";
            }
        },
        output_format {
            @Override
            public String defaultValue() {
                return "%o";
            }
        },
        session_info_format {
            @Override
            public String defaultValue() {
                return "%u@%d:%p";
            }
        },
        enable_app_launch {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        app_launch_format {
            @Override
            public String defaultValue() {
                return "--> %a";
            }
        },
        time_format_separator {
            @Override
            public String defaultValue() {
                return "@";
            }
        },
        alias_param_marker {
            @Override
            public String defaultValue() {
                return "%";
            }
        },
        alias_param_separator {
            @Override
            public String defaultValue() {
                return ",";
            }
        },
        multiple_cmd_separator {
            @Override
            public String defaultValue() {
                return ";";
            }
        },
        alias_content_format {
            @Override
            public String defaultValue() {
                return "%a --> [%v]";
            }
        },
        external_storage_path {
            @Override
            public String defaultValue() {
                String path = System.getenv("SECONDARY_STORAGE");
                if(path == null) return Tuils.EMPTYSTRING;

                File file = new File(path);
                if(file != null && file.exists()) return file.getAbsolutePath();

                return Tuils.EMPTYSTRING;
            }
        },
        home_path {
            @Override
            public String defaultValue() {
                return Environment.getExternalStorageDirectory().getAbsolutePath();
            }
        },
        app_installed_format {
            @Override
            public String defaultValue() {
                return "App installed: %p";
            }
        },
        app_uninstalled_format {
            @Override
            public String defaultValue() {
                return "App uninstalled: %p";
            }
        },
        enable_music {
            @Override
            public String defaultValue() {
                return "true";
            }
        };

        @Override
        public XmlPrefsElement parent() {
            return XMLPrefsRoot.BEHAVIOR;
        }

        @Override
        public String label() {
            return name();
        }

        @Override
        public boolean is(String s) {
            return name().equals(s);
        }
    }

    public enum Cmd implements XMLPrefsSave {

        default_search {
            @Override
            public String defaultValue() {
                return "-gg";
            }
        };

        @Override
        public XmlPrefsElement parent() {
            return XMLPrefsRoot.CMD;
        }

        @Override
        public String label() {
            return name();
        }

        @Override
        public boolean is(String s) {
            return name().equals(s);
        }
    }

    public enum XMLPrefsRoot implements XmlPrefsElement {

        THEME("theme.xml", Theme.values()) {
            @Override
            public String[] deleted() {
                return new String[0];
            }
        },
        CMD("cmd.xml", Cmd.values()) {
            @Override
            public String[] deleted() {
                return new String[] {"time_format"};
            }
        },
        TOOLBAR("toolbar.xml", Toolbar.values()) {
            @Override
            public String[] deleted() {
                return new String[] {"enabled"};
            }
        },
        UI("ui.xml", Ui.values()) {
            @Override
            public String[] deleted() {
                return new String[] {"show_timestamp_before_cmd", "linux_like", "show_username_ssninfo", "show_ssninfo", "show_path_ssninfo", "show_devicename_ssninfo", "show_alias_suggestions", "transparent_bars"};
            }
        },
        BEHAVIOR("behavior.xml", Behavior.values()) {
            @Override
            public String[] deleted() {
                return new String[] {"double_tap_closes", "donation_message"};
            }
        },
        SUGGESTIONS("suggestions.xml", Suggestions.values()) {
            @Override
            public String[] deleted() {
                return new String[] {"transparent", "enabled"};
            }
        };

//        notifications
//        apps
//        alias

        public String path;
        XMLPrefsList values;
        List<XMLPrefsSave> enums;
        public List<XMLPrefsSave> copy;

        XMLPrefsRoot(String path, XMLPrefsSave[] en) {
            this.path = path;
            this.values = new XMLPrefsList();

            if(en == null) return;
            this.enums = new ArrayList<>(Arrays.asList(en));
            this.copy = new ArrayList<>(enums);
        }

        @Override
        public void write(XMLPrefsSave save, String value) {
            set(new File(Tuils.getFolder(), path), name(), save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
        }

        public XMLPrefsList getValues() {
            return values;
        }
    }

    public interface XmlPrefsElement {
        XMLPrefsList getValues();
        void write(XMLPrefsSave save, String value);
        String[] deleted();
    }

    public static class XMLPrefsEntry {

        public String key, value;

        public XMLPrefsEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof XMLPrefsEntry) return this == obj;
            else if(obj instanceof XMLPrefsSave) return this.key.equals(((XMLPrefsSave) obj).label());
            return obj.equals(key);
        }

        @Override
        public String toString() {
            return key + " --> " + value;
        }
    }

    public static class XMLPrefsList {

        public List<XMLPrefsEntry> list = new ArrayList<>();

        public void add(XMLPrefsEntry entry) {
            list.add(entry);
        }

        public void add(String key, String value) {
            list.add(new XMLPrefsEntry(key, value));
        }

        public XMLPrefsEntry get(Object o) {
            if(o instanceof Integer) return at((Integer) o);

            for(XMLPrefsEntry e : list) if(e.equals(o)) return e;
            return null;
        }

        public XMLPrefsEntry at(int index) {
            return list.get(index);
        }

        public int size() {
            return list.size();
        }

        public List<String> values() {
            List<String> vs = new ArrayList<>();
            for(XMLPrefsEntry entry : list) vs.add(entry.key + "=" + entry.value);
            return vs;
        }
    }

    private XMLPrefsManager() {}

    static boolean called = false;
    public static void create(Context context) throws Exception {
        if(called) return;
        called = true;

        File folder = Tuils.getFolder();

        for(XMLPrefsRoot element : XMLPrefsRoot.values()) {
            File file = new File(folder, element.path);
            if(!file.exists()) {
                resetFile(file, element.name());
            }

            Object[] o;
            try {
                o = buildDocument(file, element.name());
            } catch (Exception e) {
                Tuils.sendOutput(Color.RED, context, context.getString(R.string.output_xmlproblem1) + Tuils.SPACE + element.path + context.getString(R.string.output_xmlproblem2) +
                        Tuils.NEWLINE + context.getString(R.string.output_errorlabel) + e.toString());
                continue;
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            List<XMLPrefsSave> enums = element.enums;
            if(enums == null) continue;

            String[] deleted = element.deleted();
            boolean needToWrite = false;

            if(root == null) {
                resetFile(file, element.name());
                d = builder.parse(file);
                root = (Element) d.getElementsByTagName(element.name()).item(0);
            }
            NodeList nodes = root.getElementsByTagName("*");

            for(int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);

                String nn = node.getNodeName();
                element.values.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());

                for(int en = 0; en < enums.size(); en++) {
                    if(enums.get(en).label().equals(nn)) {
                        enums.remove(en);
                        break;
                    } else if(deleted != null) {
                        int index = Tuils.find(nn, deleted);
                        if(index != -1) {
                            deleted[index] = null;
                            Element e = (Element) node;
                            root.removeChild(e);

                            needToWrite = true;
                        }
                    }
                }
            }

            if(enums.size() == 0) {
                if(needToWrite) writeTo(d, file);
                continue;
            }

            for(XMLPrefsSave s : enums) {
                String value = s.defaultValue();

                Element em = d.createElement(s.label());
                em.setAttribute(VALUE_ATTRIBUTE, value);
                root.appendChild(em);

                element.values.add(s.label(), value);
            }

            writeTo(d, file);
        }
    }

    public static Object transform(String s, Class<?> c) throws Exception {
        if(s == null) throw new UnsupportedOperationException();

        if(c == int.class) return Integer.parseInt(s);
        if(c == Color.class) return Color.parseColor(s);
        if(c == boolean.class) return Boolean.parseBoolean(s);
        if(c == String.class) return s;
        if(c == File.class) {
            if(s.length() == 0) return null;

            File file = new File(s);
            if(!file.exists()) throw new UnsupportedOperationException();

            return file;
        }

        return Tuils.getDefaultValue(c);
    }

    static final Pattern p1 = Pattern.compile(">");
//    static final Pattern p2 = Pattern.compile("</");
    static final Pattern p3 = Pattern.compile("\n\n");
    static final String p1s = ">" + Tuils.NEWLINE;
//    static final String p2s = "\n</";
    static final String p3s = Tuils.NEWLINE;

    public static String fixNewlines(String s) {
        s = p1.matcher(s).replaceAll(p1s);
//        s = p2.matcher(s).replaceAll(p2s);
        s = p3.matcher(s).replaceAll(p3s);
        return s;
    }

//    [0] = document
//    [1] = root
    public static Object[] buildDocument(File file, String root) throws Exception {
        Document d = builder.parse(file);
        Element r = (Element) d.getElementsByTagName(root).item(0);

        return new Object[] {d, r};
    }

    public static void writeTo(Document d, File f) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource source = new DOMSource(d);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);

            String s = fixNewlines(writer.toString());

            FileOutputStream stream = new FileOutputStream(f);
            stream.write(s.getBytes());

            stream.flush();
            stream.close();
        } catch (Exception e) {}
    }

    public static String add(File file, String rootName, String elementName, String[] attributeNames, String[] attributeValues) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, rootName);
            } catch (Exception e) {
                return e.toString();
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            Element element = d.createElement(elementName);
            for(int c = 0; c < attributeNames.length; c++) {
                if(attributeValues[c] == null) continue;
                element.setAttribute(attributeNames[c], attributeValues[c]);
            }
            root.appendChild(element);

            writeTo(d, file);
        } catch (Exception e) {
            return e.toString();
        }
        return null;
    }

    public static String set(File file, String rootName, String elementName, String[] attributeNames, String[] attributeValues) {
        String[][] values = new String[1][attributeValues.length];
        values[0] = attributeValues;

        return setMany(file, rootName, new String[] {elementName}, attributeNames, values);
    }

    public static String setMany(File file, String rootName, String elementNames[], String[] attributeNames, String[][] attributeValues) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, rootName);
            } catch (Exception e) {
                return e.toString();
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            NodeList nodes = root.getElementsByTagName("*");

            for(int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);

                int index = Tuils.find(node.getNodeName(), elementNames);
                if(index != -1) {
                    Element e = (Element) node;

                    for(int c = 0; c < attributeNames.length; c++) {
                        if(attributeValues[index][c] == null) continue;
                        e.setAttribute(attributeNames[c], attributeValues[index][c]);
                    }

                    writeTo(d, file);
                    return null;
                }
            }

//            it wasn't found
            for(int count = 0; count < elementNames.length; count++) {
                Element element = d.createElement(elementNames[count]);
                for(int c = 0; c < attributeNames.length; c++) {
                    if(attributeValues[count][c] == null) continue;
                    element.setAttribute(attributeNames[c], attributeValues[count][c]);
                }
                root.appendChild(element);
            }

            writeTo(d, file);
        } catch (Exception e) {
            Tuils.log(e);
            Tuils.toFile(e);
            return e.toString();
        }
        return null;
    }

    public static String removeNode(File file, String rootName, String nodeName) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, rootName);
            } catch (Exception e) {
                return e.toString();
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            Node node = findNode(root, nodeName);

            if(node == null) return Tuils.EMPTYSTRING;

            root.removeChild(node);
            writeTo(d, file);
            return null;
        } catch (Exception e) {
            return e.toString();
        }
    }

    public static Node findNode(File file, String rootName, String nodeName) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, rootName);
            } catch (Exception e) {
                return null;
            }

            Element root = (Element) o[1];

            findNode(root, nodeName);
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    public static Node findNode(Element root, String nodeName) {
        NodeList nodes = root.getElementsByTagName("*");

        for(int count = 0; count < nodes.getLength(); count++) {
            Node node = nodes.item(count);

            if(node.getNodeName().equalsIgnoreCase(nodeName)) {
                return node;
            }
        }

        return null;
    }

    public static String[] getAttrValues(File file, String rootName, String nodeName, String[] attrNames) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, rootName);
            } catch (Exception e) {
                return null;
            }

            Element root = (Element) o[1];

            NodeList nodes = root.getElementsByTagName("*");

            for(int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);

                if(node.getNodeName().equals(nodeName)) {
                    Element e = (Element) node;

                    String[] values = new String[attrNames.length];
                    for(int c = 0; c < attrNames.length; c++) values[count] = e.getAttribute(attrNames[c]);

                    return values;
                }
            }
        } catch (Exception e) {}

        return null;
    }

    public static <T> T get(Class<T> c, XMLPrefsManager.XMLPrefsSave prefsSave) {
        if(prefsSave != null) {
            try {
                return (T) transform(prefsSave.parent().getValues().get(prefsSave).value, c);
            } catch (Exception e) {

                try {
                    return (T) transform(prefsSave.defaultValue(), c);
                } catch (Exception e1) {
                    return Tuils.getDefaultValue(c);
                }
            }
        }

//        this won't ever happen, I think
        return null;
    }

    public static String get(XMLPrefsManager.XMLPrefsSave prefsSave) {
        return get(String.class, prefsSave);
    }

    public static int getColor(XMLPrefsManager.XMLPrefsSave prefsSave) {
        try {
            return (int) transform(prefsSave.parent().getValues().get(prefsSave).value, Color.class);
        } catch (Exception e) {
            String def = prefsSave.defaultValue();
            if(def == null || def.length() == 0) {
                return Integer.MAX_VALUE;
            }

            try {
                return (int) transform(def, Color.class);
            } catch (Exception e1) {
                return Integer.MAX_VALUE;
            }
        }
    }

    public static boolean resetFile(File f, String name) {
        try {
            FileOutputStream stream = new FileOutputStream(f);
            stream.write(XML_DEFAULT.getBytes());
            stream.write(("<" + name + ">\n").getBytes());
            stream.write(("</" + name + ">\n").getBytes());
            stream.flush();
            stream.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

//    private static HashMap<XMLPrefsSave, String> getOld(BufferedReader reader) {
//        HashMap<XMLPrefsSave, String> map = new HashMap<>();
//
//        String line;
//        try {
//            while((line = reader.readLine()) != null) {
//                String[] split = line.split("=");
//                if(split.length != 2) continue;
//
//                String name = split[0].trim();
//                String value = split[1];
//
//                XMLPrefsSave s = getCorresponding(name);
//                if(s == null) continue;
//
//                map.put(s, value);
//            }
//        } catch (IOException e) {
//            return null;
//        }
//
//        return map;
//    }

//    static final SimpleMutableEntry[] OLD = {
//            new SimpleMutableEntry("deviceColor", Theme.device_color),
//            new SimpleMutableEntry("inputColor", Theme.input_color),
//            new SimpleMutableEntry("outputColor", Theme.output_color),
//            new SimpleMutableEntry("backgroundColor", Theme.bg_color),
//            new SimpleMutableEntry("useSystemFont", Ui.system_font),
//            new SimpleMutableEntry("fontSize", Ui.font_size),
//            new SimpleMutableEntry("ramColor", Theme.ram_color),
//            new SimpleMutableEntry("inputFieldBottom", Ui.input_bottom),
//            new SimpleMutableEntry("username", Ui.username),
//            new SimpleMutableEntry("showSubmit", Ui.show_enter_button),
//            new SimpleMutableEntry("deviceName", Ui.deviceName),
//            new SimpleMutableEntry("showRam", Ui.show_ram),
//            new SimpleMutableEntry("showDevice", Ui.show_device_name),
//            new SimpleMutableEntry("showToolbar", Toolbar.show_toolbar),
//
//            new SimpleMutableEntry("suggestionTextColor", Suggestions.default_text_color),
//            new SimpleMutableEntry("transparentSuggestions", Suggestions.transparent),
//            new SimpleMutableEntry("aliasSuggestionBg", Suggestions.alias_bg_color),
//            new SimpleMutableEntry("appSuggestionBg", Suggestions.apps_bg_color),
//            new SimpleMutableEntry("commandSuggestionsBg", Suggestions.cmd_bg_color),
//            new SimpleMutableEntry("songSuggestionBg", Suggestions.song_bg_color),
//            new SimpleMutableEntry("contactSuggestionBg", Suggestions.contact_bg_color),
//            new SimpleMutableEntry("fileSuggestionBg", Suggestions.file_bg_color),
//            new SimpleMutableEntry("defaultSuggestionBg", Suggestions.default_bg_color),
//
//            new SimpleMutableEntry("useSystemWallpaper", Ui.system_wallpaper),
//            new SimpleMutableEntry("fullscreen", Ui.fullscreen),
//            new SimpleMutableEntry("keepAliveWithNotification", Behavior.tui_notification),
//            new SimpleMutableEntry("openKeyboardOnStart", Behavior.auto_show_keyboard),
//
//            new SimpleMutableEntry("fromMediastore", Behavior.songs_from_mediastore),
//            new SimpleMutableEntry("playRandom", Behavior.random_play),
//            new SimpleMutableEntry("songsFolder", Behavior.songs_folder),
//
//            new SimpleMutableEntry("closeOnDbTap", Behavior.double_tap_closes),
//            new SimpleMutableEntry("showSuggestions", Suggestions.show_suggestions),
//            new SimpleMutableEntry("showDonationMessage", Behavior.donation_message),
//            new SimpleMutableEntry("showAliasValue", Behavior.show_alias_content),
//            new SimpleMutableEntry("showAppsHistory", Behavior.show_launch_history),
//
//            new SimpleMutableEntry("defaultSearch", Cmd.default_search)
//    };
//
//    private static XMLPrefsSave getCorresponding(String old) {
//        for(SimpleMutableEntry<String, XMLPrefsSave> s : OLD) {
//            if(old.equals(s.getKey())) return s.getValue();
//        }
//        return null;
//    }
}
