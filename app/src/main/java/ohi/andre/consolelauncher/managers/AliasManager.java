package ohi.andre.consolelauncher.managers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;

import static ohi.andre.consolelauncher.managers.XMLPrefsManager.resetFile;

public class AliasManager implements Reloadable {

    private final String NAME = "ALIAS";
    private final String PATH = "alias.xml";

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

    public int getNum() {
        return alias.size();
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
}
