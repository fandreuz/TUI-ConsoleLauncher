package ohi.andre.consolelauncher.managers.xml;

import android.content.Context;
import android.graphics.Color;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

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
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsList;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Cmd;
import ohi.andre.consolelauncher.managers.xml.options.Suggestions;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.managers.xml.options.Toolbar;
import ohi.andre.consolelauncher.managers.xml.options.Ui;
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

    public enum XMLPrefsRoot implements XMLPrefsElement {

        THEME(Theme.values()) {
            @Override
            public String[] delete() {
                return new String[] {};
            }
        },
        CMD(Cmd.values()) {
            @Override
            public String[] delete() {
                return new String[] {};
            }
        },
        TOOLBAR(Toolbar.values()) {
            @Override
            public String[] delete() {
                return new String[] {};
            }
        },
        UI(Ui.values()) {
            @Override
            public String[] delete() {
                return new String[] {};
            }
        },
        BEHAVIOR(Behavior.values()) {
            @Override
            public String[] delete() {
                return new String[] {};
            }
        },
        SUGGESTIONS(Suggestions.values()) {
            @Override
            public String[] delete() {
                return new String[] {"app_suggestions_minrate", "contact_suggestions_minrate", "song_suggestions_minrate", "file_suggestions_minrate"};
            }
        };

//        notifications
//        apps
//        alias

        public String path;
        XMLPrefsList values;
        public List<XMLPrefsSave> enums;

        XMLPrefsRoot(XMLPrefsSave[] en) {
            this.values = new XMLPrefsList();

            this.enums = new ArrayList<>(Arrays.asList(en));
            this.path = this.name().toLowerCase() + ".xml";
        }

        @Override
        public void write(XMLPrefsSave save, String value) {
            set(new File(Tuils.getFolder(), path), save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
        }

        public XMLPrefsList getValues() {
            return values;
        }

        @Override
        public String path() {
            return path;
        }
    }

    private XMLPrefsManager() {}

    public static void dispose() {
        commonsLoaded = false;

        for(XMLPrefsRoot element : XMLPrefsRoot.values()) {
            element.values.list.clear();
        }
    }

    static boolean commonsLoaded = false;
    public static void loadCommons(Context context) {
        if(commonsLoaded) return;
        commonsLoaded = true;

        File folder = Tuils.getFolder();
        if(folder == null) {
            Tuils.sendOutput(Color.RED, context, R.string.tuinotfound_xmlprefs);
            return;
        }

        for(XMLPrefsRoot element : XMLPrefsRoot.values()) {
            File file = new File(folder, element.path);
            if(!file.exists()) {
                resetFile(file, element.name());
            }

            Object[] o;
            try {
                o = buildDocument(file, element.name());
                if(o == null) {
                    Tuils.sendXMLParseError(context, element.path);
                    return;
                }
            } catch (SAXParseException e) {
                Tuils.sendXMLParseError(context, element.path, e);
                continue;
            } catch (Exception e) {
                Tuils.log(e);
                return;
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

//            we are keeping this because maybe there are some new values to write
            List<XMLPrefsSave> enums = new ArrayList<>(element.enums);

            String[] deleted = element.delete();
            boolean needToWrite = false;

            if(root == null) {
                resetFile(file, element.name());
                try {
                    d = builder.parse(file);
                } catch (Exception e) {}
                root = (Element) d.getElementsByTagName(element.name()).item(0);
            }
            NodeList nodes = root.getElementsByTagName("*");

            for(int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);
                String nn = node.getNodeName();

                String value;
                try {
                    value = node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue();
                } catch (Exception e) {
                    continue;
                }

                boolean check = false;
                for(int en = 0; en < enums.size(); en++) {
                    XMLPrefsSave opt = enums.get(en);

                    if(opt.label().equals(nn)) {
                        XMLPrefsSave s = enums.remove(en);

                        String[] iv = s.invalidValues();
                        if(iv != null) {
                            for(String temp : iv) {
                                if(temp.equals(value)) {
                                    value = opt.defaultValue();

                                    Element em = (Element) node;
                                    em.setAttribute(VALUE_ATTRIBUTE, value);

                                    needToWrite = true;

                                    break;
                                }
                            }
                        }

                        element.values.add(nn, value);

                        check = true;
                        break;
                    }
                }

                if(!check && deleted != null) {
                    int index = Tuils.find(nn, deleted);
                    if(index != -1) {
                        deleted[index] = null;
                        Element e = (Element) node;
                        root.removeChild(e);

                        needToWrite = true;
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
        if(c == float.class) return Float.parseFloat(s);
        if(c == double.class) return Double.parseDouble(s);
        if(c == File.class) {
            if(s.length() == 0) return null;

            File file = new File(s);
            if(!file.exists()) throw new UnsupportedOperationException();

            return file;
        }

        return Tuils.getDefaultValue(c);
    }

    public static float getFloat(XMLPrefsSave prefsSave) {
        return get(float.class, prefsSave);
    }

    public static double getDouble(XMLPrefsSave prefsSave) {
        return get(double.class, prefsSave);
    }

    public static int getInt(XMLPrefsSave prefsSave) {
        return get(int.class, prefsSave);
    }

    public static boolean getBoolean(XMLPrefsSave prefsSave) {
        return get(boolean.class, prefsSave);
    }

    public static int getColor(XMLPrefsSave prefsSave) {
        if(prefsSave.parent() == null) return Integer.MAX_VALUE;

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

    public static String getString(XMLPrefsSave prefsSave) {
        return get(prefsSave);
    }

    public static <T> T get(Class<T> c, XMLPrefsRoot root, String s) {
        try {
            return (T) transform(root.getValues().get(s).value, c);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T get(Class<T> c, XMLPrefsSave prefsSave) {
        try {
//            if(prefsSave.is(Notifications.show_notifications.label())) {
//                Tuils.log("----------------");
//                Tuils.log("label", prefsSave.label());
//                Tuils.log("parent", prefsSave.parent().toString());
//                Tuils.log("values tostring", prefsSave.parent().getValues().toString());
//            }
            return (T) transform(prefsSave.parent().getValues().get(prefsSave).value, c);
        } catch (Exception e) {
            Tuils.log(e);
//            this will happen if the option is not found
            try {
                return (T) transform(prefsSave.defaultValue(), c);
            } catch (Exception e1) {
                Tuils.log(e1);
//                attempts to get a default value for the given type, as we say in italian, "the last beach"
                return Tuils.getDefaultValue(c);
            }
        }
    }

    public static String get(XMLPrefsSave prefsSave) {
        return get(String.class, prefsSave);
    }

    public static String get(XMLPrefsRoot root, String s) {
        return get(String.class, root, s);
    }

    public static boolean wasChanged(XMLPrefsSave save, boolean allowLengthZero) {
        String value = get(save);
        return (allowLengthZero || value.length() > 0) && !value.equals(save.defaultValue());
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

//    rootName is needed in order to rebuild the file if it's corrupted
//    [0] = document
//    [1] = root
    public static Object[] buildDocument(File file, String rootName) throws Exception {
        if(!file.exists()) {
            resetFile(file, rootName);
        }

        Document d;
        try {
            d = builder.parse(file);
        } catch (Exception e) {
            Tuils.log(e);

            int nOfBytes = Tuils.nOfBytes(file);
            if(nOfBytes == 0 && rootName != null) {
                XMLPrefsManager.resetFile(file, rootName);
                d = builder.parse(file);
            } else return null;
        }

        Element r = d.getDocumentElement();
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
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

//    this will only add, it won't check if there's already one
    public static String add(File file, String elementName, String[] attributeNames, String[] attributeValues) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, null);
                if(o == null) return Tuils.EMPTYSTRING;
            } catch (Exception e) {
                Tuils.log(e);
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
            Tuils.log(e);
            return e.toString();
        }
        return null;
    }

    public static String set(File file, String elementName, String[] attributeNames, String[] attributeValues) {
        return set(file, elementName, null, null, attributeNames, attributeValues, true);
    }

    public static String set(File file, String elementName, String[] thatHasThose, String[] forValues, String[] attributeNames, String[] attributeValues, boolean addIfNotFound) {
        String[][] values = new String[1][attributeValues.length];
        values[0] = attributeValues;

        return setMany(file, new String[] {elementName}, thatHasThose, forValues, attributeNames, values, addIfNotFound);
    }

    public static String setMany(File file, String elementNames[], String[] attributeNames, String[][] attributeValues) {
        return setMany(file, elementNames, null, null, attributeNames, attributeValues, true);
    }

    public static String setMany(File file, String elementNames[], String[] thatHasThose, String[] forValues, String[] attributeNames, String[][] attributeValues, boolean addIfNotFound) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, null);
                if(o == null) return Tuils.EMPTYSTRING;
            } catch (Exception e) {
                Tuils.log(e);
                return e.toString();
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            if(d == null || root == null) {
                return Tuils.EMPTYSTRING;
            }

            int nFound = 0;

            Main:
            for(int c = 0; c < elementNames.length; c++) {
                NodeList nodes = root.getElementsByTagName(elementNames[c]);

                Nodes:
                for(int j = 0; j < nodes.getLength(); j++) {
                    Node n = nodes.item(j);
                    if(n.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) n;

                        if(!checkAttributes(e, thatHasThose, forValues, false)) {
                            continue Nodes;
                        }

                        nFound++;

                        for(int a = 0; a < attributeNames.length; a++) {
                            e.setAttribute(attributeNames[a], attributeValues[c][a]);
                        }

                        elementNames[c] = null;

                        continue Main;
                    }
                }
            }

            if(nFound < elementNames.length) {
                for (int count = 0; count < elementNames.length; count++) {
                    if (elementNames[count] == null || elementNames[count].length() == 0) continue;

                    if (!addIfNotFound) continue;

                    Element element = d.createElement(elementNames[count]);
                    for (int c = 0; c < attributeNames.length; c++) {
                        if (attributeValues[count][c] == null) continue;
                        element.setAttribute(attributeNames[c], attributeValues[count][c]);
                    }
                    root.appendChild(element);
                }
            }

            writeTo(d, file);

            if(nFound == 0) return Tuils.EMPTYSTRING;
            return null;
        } catch (Exception e) {
            Tuils.log(e);
            Tuils.toFile(e);
            return e.toString();
        }
    }

//    return "" if node not found, null if all good
    public static String removeNode(File file, String nodeName) {
        return removeNode(file, nodeName, null, null);
    }

    public static String removeNode(File file, String nodeName, String[] thatHasThose, String[] forValues) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, null);
                if(o == null) return Tuils.EMPTYSTRING;
            } catch (Exception e) {
                return e.toString();
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            Node n = findNode(root, nodeName, thatHasThose, forValues);
            if(n == null) return Tuils.EMPTYSTRING;

            root.removeChild(n);
            writeTo(d, file);

            return null;
        } catch (Exception e) {
            return e.toString();
        }
    }

    public static String removeNode(File file, String[] thatHasThose, String[] forValues) {
        return removeNode(file, thatHasThose, forValues, false,false);
    }

    public static String removeNode(File file, String[] thatHasThose, String[] forValues, boolean alsoNotFound, boolean all) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, null);
                if(o == null) return Tuils.EMPTYSTRING;
            } catch (Exception e) {
                return e.toString();
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            NodeList list = root.getElementsByTagName("*");

            boolean check = false;

            for(int c = 0; c < list.getLength(); c++) {
                Node n = list.item(c);

                if(!(n instanceof Element)) continue;
                Element e = (Element) n;

                if(checkAttributes(e, thatHasThose, forValues, alsoNotFound)) {
                    check = true;
                    root.removeChild(n);
                    if(!all) break;
                }
            }

            writeTo(d, file);

            return check ? null : Tuils.EMPTYSTRING;
        } catch (Exception e) {
            return e.toString();
        }
    }

    public static Node findNode(File file, String nodeName) {
        return findNode(file, nodeName, null, null);
    }

    public static Node findNode(File file, String nodeName, String[] thatHasThose, String[] forValues) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, null);
                if(o == null) return null;
            } catch (Exception e) {
                return null;
            }

            Element root = (Element) o[1];

            return findNode(root, nodeName, thatHasThose, forValues);
        } catch (Exception e) {
            return null;
        }
    }

//    useful only if you're looking for a single node
    public static Node findNode(Element root, String nodeName, String[] thatHasThose, String[] forValues) {
        NodeList nodes = root.getElementsByTagName(nodeName);
        for(int j = 0; j < nodes.getLength(); j++) if(checkAttributes((Element) nodes.item(j), thatHasThose, forValues, false)) return nodes.item(j);
        return null;
    }

    public static Node findNode(Element root, String nodeName) {
        return findNode(root, nodeName, null, null);
    }

    public static List<Node> findNodes(Element root, String nodeName, String[] thatHasThose, String[] forValue) {
        NodeList nodes = root.getElementsByTagName(nodeName != null ? nodeName : "*");

        List<Node> nodeList = new ArrayList<>();

        for(int c = 0; c < nodes.getLength(); c++) {
            Node n = nodeList.get(c);

            if(!(n instanceof Element)) continue;
            Element e = (Element) n;

            if(checkAttributes(e, thatHasThose, forValue, false)) {
                nodeList.add(n);
            }
        }

        return nodeList;
    }

    public static List<Node> findNodes(Element root, String[] thatHasThose, String[] forValue) {
        return findNodes(root, null, thatHasThose, forValue);
    }

    public static String attrValue(File file, String nodeName, String attrName) {
        return attrValue(file, nodeName, null, null, attrName);
    }

    public static String attrValue(File file, String nodeName, String[] thatHasThose, String[] forValues, String attrName) {
        String[] vs = attrValues(file, nodeName, thatHasThose, forValues, new String[] {attrName});
        if(vs != null && vs.length > 0) return vs[0];
        return null;
    }

    public static String[] attrValues(File file, String nodeName, String[] attrNames) {
        return attrValues(file, nodeName, null, null, attrNames);
    }

    public static String[] attrValues(File file, String nodeName, String[] thatHasThose, String[] forValues, String[] attrNames) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, null);
                if(o == null) return null;
            } catch (Exception e) {
                return null;
            }

            Element root = (Element) o[1];
            NodeList nodes = root.getElementsByTagName(nodeName);

            for(int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);
                Element e = (Element) node;

                if(!checkAttributes(e, thatHasThose, forValues, false)) continue;

                String[] values = new String[attrNames.length];
                for(int c = 0; c < attrNames.length; c++) values[count] = e.getAttribute(attrNames[c]);

                return values;
            }
        } catch (Exception e) {}

        return null;
    }

    private static boolean checkAttributes(Element e, String[] thatHasThose, String[] forValues, boolean alsoIfAttributeNotFound) {
        if(thatHasThose != null && forValues != null && thatHasThose.length == forValues.length) {
            for(int a = 0; a < thatHasThose.length; a++) {
                if(!e.hasAttribute(thatHasThose[a])) return alsoIfAttributeNotFound;
                if(!forValues[a].equals(e.getAttribute(thatHasThose[a]))) return false;
            }
        }
        return true;
    }

    public static boolean resetFile(File f, String name) {
        try {
            if(f.exists()) f.delete();

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

    public static String getStringAttribute(Element e, String attribute) {
        return e.hasAttribute(attribute) ? e.getAttribute(attribute) : null;
    }

    public static long getLongAttribute(Element e, String attribute) {
        String value = getStringAttribute(e, attribute);
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return -1;
        }
    }

    public static boolean getBooleanAttribute(Element e, String attribute) {
        String s = getStringAttribute(e, attribute);
        return s != null && Boolean.parseBoolean(s);

    }

    public static int getIntAttribute(Element e, String attribute) {
        try {
            return Integer.parseInt(getStringAttribute(e, attribute));
        } catch (Exception ex) {
            return -1;
        }
    }

    public static float getFloatAttribute(Element e, String attribute) {
        try {
            return Float.parseFloat(getStringAttribute(e, attribute));
        } catch (Exception ex) {
            return -1;
        }
    }

    public static class IdValue {
        public String value;
        public int id;

        public IdValue(String value, int id) {
            this.value = value;
            this.id = id;
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
