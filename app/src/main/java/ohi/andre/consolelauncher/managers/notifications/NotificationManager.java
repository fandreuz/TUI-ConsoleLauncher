package ohi.andre.consolelauncher.managers.notifications;

import android.annotation.TargetApi;
import android.os.Build;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    private static final String COLOR_ATTRIBUTE = "color";
    private static final String ENABLED_ATTRIBUTE = "enabled";
    private static final String ON_ATTRIBUTE = "on";

    private static final String REGEX_NODE = "regex";

    public static final String PATH = "notifications.xml";
    private static final String NAME = "NOTIFICATIONS";

    private static XMLPrefsManager.XmlPrefsElement instance = null;

    public static boolean default_app_state;
    public static String default_color;

    public enum Options implements XMLPrefsManager.XMLPrefsSave {

        enabled {
            @Override
            public String defaultValue() {
                return "false";
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
                return "#FF0000";
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
    private static List<Pattern> titleRegexs;
    private static List<Pattern> textRegexs;
    private static boolean created = false;

    private NotificationManager() {}

    public static void create() {
        instance = new NotificationManager();

        if(created) return;
        created = true;

        apps = new ArrayList<>();
        textRegexs = new ArrayList<>();
        titleRegexs = new ArrayList<>();
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
                } else if(nn.equals(REGEX_NODE)) {
                    if(node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;
                        String regex = e.hasAttribute(REGEX_NODE) ? e.getAttribute(REGEX_NODE) : null;
                        if(regex == null) continue;
                        String on = e.hasAttribute(ON_ATTRIBUTE) ? e.getAttribute(ON_ATTRIBUTE) : null;
                        if(on == null || on.equals("text") || !on.equals("title")) textRegexs.add(Pattern.compile(regex));
                        else titleRegexs.add(Pattern.compile(regex));
                    }
                } else {
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
                for(XMLPrefsManager.XMLPrefsSave s : enums) {
                    Element em = d.createElement(s.label());
                    em.setAttribute(VALUE_ATTRIBUTE, s.defaultValue());
                    root.appendChild(em);

                    values.add(s.label(), s.defaultValue());
                }

                writeTo(d, file);
            }
        } catch (Exception e) {}

        default_app_state = XMLPrefsManager.get(boolean.class, Options.default_app_state);
        default_color = XMLPrefsManager.get(String.class, Options.default_color);
    }

    public static void notificationsChangeFor(NotificatedApp app) {
        notificationsChangeFor(new ArrayList<>(Collections.singletonList(app)));
    }

    public static boolean textMatches(String text) {
        if(text == null || text.length() == 0) return false;
        for(Pattern p : textRegexs) if (p.matcher(text).matches()) return true;
        return false;
    }

    public static boolean titleMatches(String text) {
        if(text == null || text.length() == 0) return false;
        for(Pattern p : titleRegexs) if (p.matcher(text).matches()) return true;
        return false;
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

    public static void excludeRegex(String regex, String on) {
        XMLPrefsManager.set(new File(Tuils.getFolder(), PATH), NAME, REGEX_NODE, new String[] {ON_ATTRIBUTE, VALUE_ATTRIBUTE}, new String[] {on, regex});
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
}
