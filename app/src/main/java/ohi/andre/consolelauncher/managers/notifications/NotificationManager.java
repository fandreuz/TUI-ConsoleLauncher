package ohi.andre.consolelauncher.managers.notifications;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.managers.XMLPrefsManager.VALUE_ATTRIBUTE;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.resetFile;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.set;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.setMany;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.writeTo;

/**
 * Created by francescoandreuzzi on 29/04/2017.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationManager implements XMLPrefsManager.XmlPrefsElement {

    private static int TITLE = 10;
    private static int TEXT = 11;

    private static final String COLOR_ATTRIBUTE = "color";
    private static final String ENABLED_ATTRIBUTE = "enabled";
    private static final String ID_ATTRIBUTE = "id";
    private static final String ON_ATTRIBUTE = "on";
    private static final String PACKAGE_ATTRIBUTE = "package";

    private static final String FILTER_NODE = "filter";
    private static final String APPLY_NODE = "apply";

    public static final String PATH = "notifications.xml";
    private static final String NAME = "NOTIFICATIONS";

    private static XMLPrefsManager.XmlPrefsElement instance = null;

    public static boolean default_app_state;
    public static String default_color;

    public enum Options implements XMLPrefsManager.XMLPrefsSave {

        show_notifications {
            @Override
            public String defaultValue() {
                return "false";
            }

            @Override
            public String hasReplaced() {
                return "enabled";
            }
        },
        default_app_state {
            @Override
            public String defaultValue() {
                return "true";
            }
        },
        default_color {
            @Override
            public String defaultValue() {
                return "#00FF00";
            }
        },
        notification_format {
            @Override
            public String defaultValue() {
                return "[%t] %pkg: %ttl --- %txt";
            }
        };

        @Override
        public XMLPrefsManager.XmlPrefsElement parent() {
            return instance;
        }

        @Override
        public String label() {
            return name();
        }

        @Override
        public boolean is(String s) {
            return name().equals(s);
        }

        @Override
        public String hasReplaced() {
            return null;
        }
    }

    @Override
    public String[] deleted() {
        return new String[0];
    }

    @Override
    public XMLPrefsManager.XMLPrefsList getValues() {
        return values;
    }

    @Override
    public void write(XMLPrefsManager.XMLPrefsSave save, String value) {
        set(new File(Tuils.getFolder(), PATH), NAME, save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
    }

    private static XMLPrefsManager.XMLPrefsList values;
    private static List<NotificatedApp> apps;
    private static List<FilterGroup> groups;
    private static HashMap<Integer, String> applies;
    private static boolean created = false;

    private NotificationManager() {}

    public static void create() {
        instance = new NotificationManager();

        if(created) return;
        created = true;

        apps = new ArrayList<>();
        groups = new ArrayList<>();
        applies = new HashMap<>();
        values = new XMLPrefsManager.XMLPrefsList();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            File file = new File(Tuils.getFolder(), PATH);
            if(!file.exists() && !file.createNewFile()) return;

            Document d;
            try {
                d = builder.parse(file);
            } catch (Exception e) {
                resetFile(file, NAME);

                d = builder.parse(file);
            }

            List<Options> enums = new ArrayList<>(Arrays.asList(Options.values()));

            Element root = (Element) d.getElementsByTagName(NAME).item(0);
            if(root == null) {
                resetFile(file, NAME);
                d = builder.parse(file);
                root = (Element) d.getElementsByTagName(NAME).item(0);
            }
            NodeList nodes = root.getElementsByTagName("*");

            Map<String, XMLPrefsManager.XMLPrefsSave> replacedValues = new HashMap<>();
            for(XMLPrefsManager.XMLPrefsSave s : Options.values()) {
                String r = s.hasReplaced();
                if(r != null) replacedValues.put(r, s);
            }

            Main:
            for(int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);

                String nn = node.getNodeName();
                if(Tuils.find(nn, (List) enums) != -1) {
                    values.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());

                    for(int en = 0; en < enums.size(); en++) {
                        if(enums.get(en).label().equals(nn)) {
                            enums.remove(en);
                            break;
                        }
                    }
                } else if(nn.equals(FILTER_NODE)) {
                    if(node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;

                        String regex = e.hasAttribute(VALUE_ATTRIBUTE) ? e.getAttribute(VALUE_ATTRIBUTE) : null;
                        if(regex == null) continue;

                        String on = e.hasAttribute(ON_ATTRIBUTE) ? e.getAttribute(ON_ATTRIBUTE) : null;
                        if(on == null) on = "text";

                        int id;
                        try {
                            id = e.hasAttribute(ID_ATTRIBUTE) ? Integer.parseInt(e.getAttribute(ID_ATTRIBUTE)) : -1;
                        } catch (NumberFormatException f) {
                            id = -1;
                        }

                        Filter filter = Filter.getInstance(regex, on.equals("title") ? TITLE : TEXT);
                        if(filter == null) continue;

                        if(id != -1) {
                            for(FilterGroup group : groups) {
                                if(id == group.id) {
                                    group.add(filter);
                                    continue Main;
                                }
                            }
                        }

                        FilterGroup group = new FilterGroup(id);
                        group.add(filter);
                        groups.add(group);
                    }
                } else if(nn.equals(APPLY_NODE)) {
                    if(node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;

                        int id;
                        try {
                            id = e.hasAttribute(ID_ATTRIBUTE) ? Integer.parseInt(e.getAttribute(ID_ATTRIBUTE)) : -1;
                        } catch (NumberFormatException f) {
                            continue;
                        }

                        String pkg = e.hasAttribute(PACKAGE_ATTRIBUTE) ? e.getAttribute(PACKAGE_ATTRIBUTE) : null;
                        if(pkg == null) continue;

                        applies.put(id, pkg);
                    }
                } else if(replacedValues.containsKey(nn)) {
                    XMLPrefsManager.XMLPrefsSave s = replacedValues.remove(nn);

                    Element e = (Element) node;
                    String oldValue = e.hasAttribute(VALUE_ATTRIBUTE) ? e.getAttribute(VALUE_ATTRIBUTE) : null;
                    root.removeChild(e);

                    replacedValues.put(oldValue, s);
                }
//                todo support delete
                else {
                    if(node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;

                        NotificatedApp app;

                        boolean enabled = !e.hasAttribute(ENABLED_ATTRIBUTE) || Boolean.parseBoolean(e.getAttribute(ENABLED_ATTRIBUTE));

                        String color = null;
                        if(enabled) color = e.hasAttribute(COLOR_ATTRIBUTE) ? e.getAttribute(COLOR_ATTRIBUTE) : null;

                        app = new NotificatedApp(nn, color, enabled);
                        apps.add(app);
                    }
                }
            }

            if(enums.size() > 0) {
                Set<Map.Entry<String, XMLPrefsManager.XMLPrefsSave>> es = replacedValues.entrySet();
                for(XMLPrefsManager.XMLPrefsSave s : enums) {
                    String value = null;
                    for(Map.Entry<String, XMLPrefsManager.XMLPrefsSave> e : es) {
                        if(e.getValue().equals(s)) value = e.getKey();
                    }
                    if(value == null) value = s.defaultValue();

                    Element em = d.createElement(s.label());
                    em.setAttribute(VALUE_ATTRIBUTE, value);
                    root.appendChild(em);

                    values.add(s.label(), value);
                }

                writeTo(d, file);
            }
        } catch (Exception e) {
            Log.e("andre", "", e);
        }

        default_app_state = XMLPrefsManager.get(boolean.class, Options.default_app_state);
        default_color = XMLPrefsManager.get(String.class, Options.default_color);

        Out:
        for(Map.Entry<Integer, String> e : applies.entrySet()) {
            for(FilterGroup g : groups) {
                if(g.applyTo(e.getKey(), e.getValue())) continue Out;
            }
        }
    }

    public static void notificationsChangeFor(NotificatedApp app) {
        notificationsChangeFor(new ArrayList<>(Collections.singletonList(app)));
    }

    public static boolean match(String pkg, String text, String title) {
        for(FilterGroup group : groups) {
            if(group.pkgs != null && !group.pkgs.contains(pkg)) continue;
            if(group.check(title, text)) return true;
        }
        return false;
    }

    public static String getFormat() {
        try {
            return values.get(Options.notification_format).value;
        } catch (Exception e) {

            try {
                e.printStackTrace(new PrintStream(new FileOutputStream(new File(Tuils.getFolder(), "crash.txt"), true)));
            } catch (FileNotFoundException e1) {}

            return Options.notification_format.defaultValue();
        }
    }

    public static void notificationsChangeFor(List<NotificatedApp> apps) {
        String[] names = new String[apps.size()];
        final String[] attrNames = {ENABLED_ATTRIBUTE, COLOR_ATTRIBUTE};
        String[][] values = new String[names.length][attrNames.length];

        for(int count = 0; count < apps.size(); count++) {
            NotificatedApp app = apps.get(count);
            names[count] = app.pkg;
            values[count][0] = app.enabled + Tuils.EMPTYSTRING;
            values[count][1] = app.color != null ? app.color : Tuils.EMPTYSTRING;
        }

        setMany(new File(Tuils.getFolder(), PATH), NAME, names, attrNames, values);
    }

    public static void excludeRegex(String regex, String on, int id) {
        XMLPrefsManager.add(new File(Tuils.getFolder(), PATH), NAME, FILTER_NODE, new String[] {ON_ATTRIBUTE, VALUE_ATTRIBUTE, ID_ATTRIBUTE}, new String[] {on, regex, id + Tuils.EMPTYSTRING});
    }

    public static void applyFilter(int groupId, String packageName) {
        XMLPrefsManager.add(new File(Tuils.getFolder(), PATH), NAME, APPLY_NODE, new String[] {ID_ATTRIBUTE, PACKAGE_ATTRIBUTE}, new String[] {groupId + Tuils.EMPTYSTRING, packageName});
    }

    public static int apps() {
        return apps.size();
    }

    public static NotificatedApp getAppState(String pkg) {
        int index = Tuils.find(pkg, apps);
        if(index == -1) return null;
        return apps.get(index);
    }

    public static class NotificatedApp {
        String pkg;
        String color;
        boolean enabled;

        public NotificatedApp(String pkg, String color, boolean enabled) {
            this.pkg = pkg;
            this.color = color;
            this.enabled = enabled;
        }

        @Override
        public boolean equals(Object obj) {
            return this.toString().equals(obj.toString());
        }

        @Override
        public String toString() {
            return pkg;
        }
    }

    public static class Filter {
        Pattern pattern;
        int on;

        public static Filter getInstance(String p, int on) {
            Filter filter = new Filter(p, on);
            if(filter.pattern == null) return null;
            return filter;
        }

        private Filter(String p, int on) {
            this.on = on;

            try {
                this.pattern = Pattern.compile(p);
            } catch (Exception e) {
                this.pattern = null;
            }
        }
    }

    public static class FilterGroup {
        List<Filter> brothers;
        int id;

        List<String> pkgs;

        public FilterGroup(int id) {
            this.id = id;

            brothers = new ArrayList<>();
        }

        public void add(Filter filter) {
            brothers.add(filter);
        }

        public boolean check(String title, String text) {
            for(Filter filter : brothers) {
                String s;
                if(filter.on == TITLE) s = title;
                else s = text;

                if(!filter.pattern.matcher(s).find()) return false;
            }

            return true;
        }

        public boolean applyTo(int id, String s) {
            if(this.id == id) {
                if(pkgs == null) pkgs = new ArrayList<>();

                pkgs.add(s);
                return true;
            }

            return false;
        }
    }
}
