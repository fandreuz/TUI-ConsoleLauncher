package ohi.andre.consolelauncher.managers;

import android.graphics.Color;

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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import ohi.andre.consolelauncher.tuils.Tuils;

public class XMLPrefsManager {

    public static final String XML_DEFAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    public static final String VALUE_ATTRIBUTE = "value";

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

        show_username_ssninfo {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        show_ssninfo {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        linux_like {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        show_path_ssninfo {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        show_devicename_ssninfo {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
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
        font_size {
            @Override
            public String defaultValue() {
                return "15";
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
                return "";
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

        enabled {
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

        enabled {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        transparent {
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

        double_tap_closes {
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
        donation_message {
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
        show_alias_suggestions {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        clear_after_cmds {
            @Override
            public String defaultValue() {
                return "20";
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
                return "100";
            }
        },
        time_format {
            @Override
            public String defaultValue() {
                return "%m/%d/%y %H.%M";
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

        THEME("theme.xml", Theme.values()),
        CMD("cmd.xml", Cmd.values()),
        TOOLBAR("toolbar.xml", Toolbar.values()),
        UI("ui.xml", Ui.values()),
        BEHAVIOR("behavior.xml", Behavior.values()),
        SUGGESTIONS("suggestions.xml", Suggestions.values());

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
            for(XMLPrefsEntry entry : list) vs.add(entry.value);
            return vs;
        }
    }

    private XMLPrefsManager() {}

    public static void create() throws Exception {
        File folder = Tuils.getFolder();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        for(XMLPrefsRoot element : XMLPrefsRoot.values()) {
            File file = new File(folder, element.path);
            if(!file.exists() && !file.createNewFile()) continue;

            Document d;
            try {
                d = builder.parse(file);
            } catch (Exception e) {
                resetFile(file, element.name());

                d = builder.parse(file);
            }

            List<XMLPrefsSave> enums = element.enums;
            if(enums == null) continue;

            Element root = (Element) d.getElementsByTagName(element.name()).item(0);
            if(root == null) {
                resetFile(file, element.name());
                d = builder.parse(file);
                root = (Element) d.getElementsByTagName(element.name()).item(0);
            }
            NodeList nodes = root.getElementsByTagName("*");

//            List<Node> customNodes = null;
//            if(element.flag == FLAG_HAS_CUSTOM_TAGS || element.flag == FLAG_CUSTOM_TAGS_ONLY) customNodes = new ArrayList<>();

            for(int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);

                String nn = node.getNodeName();
//                if(customNodes == null) {
                element.values.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());
//                } else {
//                    if(element.flag != FLAG_CUSTOM_TAGS_ONLY && element.contains(nn)) element.values.add(nn, node.getAttributes().getNamedItem("value").getNodeValue());
//                    else {
//                        customNodes.add(node);
//                        continue;
//                    }
//                }

                for(int en = 0; en < enums.size(); en++) {
                    if(enums.get(en).label().equals(nn)) {
                        enums.remove(en);
                        break;
                    }
                }
            }

            if(enums.size() == 0) continue;

            for(XMLPrefsSave s : enums) {
                Element em = d.createElement(s.label());
                em.setAttribute(VALUE_ATTRIBUTE, s.defaultValue());
                root.appendChild(em);

                element.values.add(s.label(), s.defaultValue());
            }

            writeTo(d, file);
        }
    }

    public static Object transform(String s, Class<?> c) {
        if(s == null) return null;

        try {
            if(c == int.class) return Integer.parseInt(s);
            if(c == Color.class) return Color.parseColor(s);
            if(c == boolean.class) return Boolean.parseBoolean(s);
            if(c == String.class) return s;
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    static final Pattern p1 = Pattern.compile(">");
//    static final Pattern p2 = Pattern.compile("</");
    static final Pattern p3 = Pattern.compile("\n\n");
    static final String p1s = ">\n";
//    static final String p2s = "\n</";
    static final String p3s = Tuils.NEWLINE;

    public static String fixNewlines(String s) {
        s = p1.matcher(s).replaceAll(p1s);
//        s = p2.matcher(s).replaceAll(p2s);
        s = p3.matcher(s).replaceAll(p3s);
        return s;
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

    public static String set(File file, String rootName, String elementName, String[] attributeNames, String[] attributeValues) {
        String[][] values = new String[1][attributeValues.length];
        values[0] = attributeValues;

        return setMany(file, rootName, new String[] {elementName}, attributeNames, values);
    }

    public static String setMany(File file, String rootName, String elementNames[], String[] attributeNames, String[][] attributeValues) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document d;
            try {
                d = builder.parse(file);
            } catch (Exception e) {
                return e.toString();
            }

            Element root = (Element) d.getElementsByTagName(rootName).item(0);
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
            return e.toString();
        }
        return null;
    }

    public static boolean removeNode(File file, String rootName, String nodeName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document d;
            try {
                d = builder.parse(file);
            } catch (Exception e) {
                return false;
            }

            Element root = (Element) d.getElementsByTagName(rootName).item(0);
            NodeList nodes = root.getElementsByTagName("*");

            for(int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);

                if(node.getNodeName().equalsIgnoreCase(nodeName)) {
                    root.removeChild(node);
                    writeTo(d, file);
                    return true;
                }
            }
        } catch (Exception e) {}

        return false;
    }

    public static String[] getAttrValues(File file, String rootName, String nodeName, String[] attrNames) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document d;
            try {
                d = builder.parse(file);
            } catch (Exception e) {
                return null;
            }

            Element root = (Element) d.getElementsByTagName(rootName).item(0);
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
                return (T) transform(prefsSave.defaultValue(), c);
            }
        }
        return null;
    }

    public static int getColor(XMLPrefsManager.XMLPrefsSave prefsSave) {
        try {
            return (int) transform(prefsSave.parent().getValues().get(prefsSave).value, Color.class);
        } catch (Exception e) {
            String def = prefsSave.defaultValue();
            if(def == null || def.length() == 0) {
                return SkinManager.COLOR_NOT_SET;
            }
            return (int) transform(def, Color.class);
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
}
