package ohi.andre.consolelauncher.managers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;

import static ohi.andre.consolelauncher.managers.XMLPrefsManager.resetFile;
import static ohi.andre.consolelauncher.managers.XMLPrefsManager.set;

public class AliasManager implements Reloadable {

    public static final String NAME = "ALIAS";
    public static final String PATH = "alias.xml";

    private final String VALUE_ATTRIBUTE = "value";

    private XMLPrefsManager.XMLPrefsList alias;

    public AliasManager() {
        reload();
    }

    public String printAliases() {
        List<XMLPrefsManager.XMLPrefsEntry> list = alias.list;

        String output = Tuils.EMPTYSTRING;
        for (XMLPrefsManager.XMLPrefsEntry entry : list) {
            output = output.concat(entry.key + " --> " + entry.value + Tuils.NEWLINE);
        }

        return output.trim();
    }

    public String getAlias(String s) {
        XMLPrefsManager.XMLPrefsEntry entry = alias.get(s);
        if(entry != null) return entry.value;
        return null;
    }

    @Override
    public void reload() {
        if(alias != null) alias.list.clear();
        alias = new XMLPrefsManager.XMLPrefsList();

        File file = new File(Tuils.getFolder(), PATH);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return;
        }

        Document d;
        try {
            d = builder.parse(file);
        } catch (Exception e) {
            resetFile(file, NAME);

            try {
                d = builder.parse(file);
            } catch (Exception e1) {return;}
        }

        Element root = (Element) d.getElementsByTagName(NAME).item(0);
        if(root == null) {
            resetFile(file, NAME);

            try {
                d = builder.parse(file);
            } catch (Exception e) {
                return;
            }

            root = (Element) d.getElementsByTagName(NAME).item(0);
        }
        NodeList nodes = root.getElementsByTagName("*");

        for(int count = 0; count < nodes.getLength(); count++) {
            Node node = nodes.item(count);

            String nn = node.getNodeName();
            alias.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());
        }
    }

    public String add(String name, String value) {
        return set(new File(Tuils.getFolder(), PATH), NAME, name, new String[] {VALUE_ATTRIBUTE}, new String[] {value});
    }

    public boolean remove(String name) {
        return XMLPrefsManager.removeNode(new File(Tuils.getFolder(), PATH), NAME, name);
    }

    public List<String> getAliases() {
        List<String> labels = new ArrayList<>();
        if(alias == null) return labels;

        for(int count = 0; count < alias.size(); count++) labels.add(alias.at(count).key);
        return labels;
    }
}
