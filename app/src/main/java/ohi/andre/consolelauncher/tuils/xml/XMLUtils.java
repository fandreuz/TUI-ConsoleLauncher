package ohi.andre.consolelauncher.tuils.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import ohi.andre.consolelauncher.tuils.Tuils;

public class XMLUtils {
    public final String XML_DEFAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    private DocumentBuilderFactory factory;
    private DocumentBuilder builder;

    public XMLUtils() {
        factory = DocumentBuilderFactory.newInstance();
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    final Pattern p1 = Pattern.compile(">");
    final Pattern p3 = Pattern.compile("\n\n");
    final String p1s = ">" + "\n";
    final String p3s = "\n";

    public String fixNewlines(String s) {
        s = p1.matcher(s).replaceAll(p1s);
//        s = p2.matcher(s).replaceAll(p2s);
        s = p3.matcher(s).replaceAll(p3s);
        return s;
    }

    //    rootName is needed in order to rebuild the file if it's corrupted
//    [0] = document
//    [1] = root
    public Object[] buildDocument(File file, String rootName) throws Exception {
        if (!file.exists()) {
            resetFile(file, rootName);
        }

        Document d;
        try {
            d = builder.parse(file);
        } catch (Exception e) {
            Tuils.log(e);

            int nOfBytes = Tuils.nOfBytes(file);
            if (nOfBytes == 0 && rootName != null) {
                resetFile(file, rootName);
                d = builder.parse(file);
            } else return null;
        }

        Element r = d.getDocumentElement();
        return new Object[]{d, r};
    }

    public void write(Document d, File f) {
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

    public String add(File file, String elementName, String[] attributeNames, String[] attributeValues) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, null);
                if (o == null) return "";
            } catch (Exception e) {
                Tuils.log(e);
                return e.toString();
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            Element element = d.createElement(elementName);
            for (int c = 0; c < attributeNames.length; c++) {
                if (attributeValues[c] == null) continue;
                element.setAttribute(attributeNames[c], attributeValues[c]);
            }
            root.appendChild(element);

            write(d, file);
        } catch (Exception e) {
            Tuils.log(e);
            return e.toString();
        }
        return null;
    }

    public String set(File file, String elementName, String[] attributeNames, String[] attributeValues) {
        return set(file, elementName, null, null, attributeNames, attributeValues, true);
    }

    public String set(File file, String elementName, String[] thatHasThose, String[] forValues, String[] attributeNames, String[] attributeValues, boolean addIfNotFound) {
        String[][] values = new String[1][attributeValues.length];
        values[0] = attributeValues;

        return setMany(file, new String[]{elementName}, thatHasThose, forValues, attributeNames, values, addIfNotFound);
    }

    public String setMany(File file, String elementNames[], String[] attributeNames, String[][] attributeValues) {
        return setMany(file, elementNames, null, null, attributeNames, attributeValues, true);
    }

    public String setMany(File file, String elementNames[], String[] thatHasThose, String[] forValues, String[] attributeNames, String[][] attributeValues, boolean addIfNotFound) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, null);
                if (o == null) return "";
            } catch (Exception e) {
                Tuils.log(e);
                return e.toString();
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            if (d == null || root == null) {
                return "";
            }

            int nFound = 0;

            Main:
            for (int c = 0; c < elementNames.length; c++) {
                NodeList nodes = root.getElementsByTagName(elementNames[c]);

                Nodes:
                for (int j = 0; j < nodes.getLength(); j++) {
                    Node n = nodes.item(j);
                    if (n.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) n;

                        if (!checkAttributes(e, thatHasThose, forValues, false)) {
                            continue Nodes;
                        }

                        nFound++;

                        for (int a = 0; a < attributeNames.length; a++) {
                            e.setAttribute(attributeNames[a], attributeValues[c][a]);
                        }

                        elementNames[c] = null;

                        continue Main;
                    }
                }
            }

            if (nFound < elementNames.length) {
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

            write(d, file);

            if (nFound == 0) return "";
            return null;
        } catch (Exception e) {
            Tuils.log(e);
            Tuils.toFile(e);
            return e.toString();
        }
    }

    //    return "" if node not found, null if all good
    public String removeNode(File file, String nodeName) {
        return removeNode(file, nodeName, null, null);
    }

    public String removeNode(File file, String nodeName, String[] thatHasThose, String[] forValues) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, null);
                if (o == null) return "";
            } catch (Exception e) {
                return e.toString();
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            Node n = findNode(root, nodeName, thatHasThose, forValues);
            if (n == null) return "";

            root.removeChild(n);
            write(d, file);

            return null;
        } catch (Exception e) {
            return e.toString();
        }
    }

    public String removeNode(File file, String[] thatHasThose, String[] forValues) {
        return removeNode(file, thatHasThose, forValues, false, false);
    }

    public String removeNode(File file, String[] thatHasThose, String[] forValues, boolean alsoNotFound, boolean all) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, null);
                if (o == null) return "";
            } catch (Exception e) {
                return e.toString();
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            NodeList list = root.getElementsByTagName("*");

            boolean check = false;

            for (int c = 0; c < list.getLength(); c++) {
                Node n = list.item(c);

                if (!(n instanceof Element)) continue;
                Element e = (Element) n;

                if (checkAttributes(e, thatHasThose, forValues, alsoNotFound)) {
                    check = true;
                    root.removeChild(n);
                    if (!all) break;
                }
            }

            write(d, file);

            return check ? null : "";
        } catch (Exception e) {
            return e.toString();
        }
    }

    public Node findNode(File file, String nodeName) {
        return findNode(file, nodeName, null, null);
    }

    public Node findNode(File file, String nodeName, String[] thatHasThose, String[] forValues) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, null);
                if (o == null) return null;
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
    public Node findNode(Element root, String nodeName, String[] thatHasThose, String[] forValues) {
        NodeList nodes = root.getElementsByTagName(nodeName);
        for (int j = 0; j < nodes.getLength(); j++)
            if (checkAttributes((Element) nodes.item(j), thatHasThose, forValues, false))
                return nodes.item(j);
        return null;
    }

    public Node findNode(Element root, String nodeName) {
        return findNode(root, nodeName, null, null);
    }

    public List<Node> findNodes(Element root, String nodeName, String[] thatHasThose, String[] forValue) {
        NodeList nodes = root.getElementsByTagName(nodeName != null ? nodeName : "*");

        List<Node> nodeList = new ArrayList<>();

        for (int c = 0; c < nodes.getLength(); c++) {
            Node n = nodeList.get(c);

            if (!(n instanceof Element)) continue;
            Element e = (Element) n;

            if (checkAttributes(e, thatHasThose, forValue, false)) {
                nodeList.add(n);
            }
        }

        return nodeList;
    }

    public List<Node> findNodes(Element root, String[] thatHasThose, String[] forValue) {
        return findNodes(root, null, thatHasThose, forValue);
    }

    public String attrValue(File file, String nodeName, String attrName) {
        return attrValue(file, nodeName, null, null, attrName);
    }

    public String attrValue(File file, String nodeName, String[] thatHasThose, String[] forValues, String attrName) {
        String[] vs = attrValues(file, nodeName, thatHasThose, forValues, new String[]{attrName});
        if (vs != null && vs.length > 0) return vs[0];
        return null;
    }

    public String[] attrValues(File file, String nodeName, String[] attrNames) {
        return attrValues(file, nodeName, null, null, attrNames);
    }

    public String[] attrValues(File file, String nodeName, String[] thatHasThose, String[] forValues, String[] attrNames) {
        try {
            Object[] o;
            try {
                o = buildDocument(file, null);
                if (o == null) return null;
            } catch (Exception e) {
                return null;
            }

            Element root = (Element) o[1];
            NodeList nodes = root.getElementsByTagName(nodeName);

            for (int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);
                Element e = (Element) node;

                if (!checkAttributes(e, thatHasThose, forValues, false)) continue;

                String[] values = new String[attrNames.length];
                for (int c = 0; c < attrNames.length; c++)
                    values[count] = e.getAttribute(attrNames[c]);

                return values;
            }
        } catch (Exception e) {
        }

        return null;
    }

    private boolean checkAttributes(Element e, String[] thatHasThose, String[] forValues, boolean alsoIfAttributeNotFound) {
        if (thatHasThose != null && forValues != null && thatHasThose.length == forValues.length) {
            for (int a = 0; a < thatHasThose.length; a++) {
                if (!e.hasAttribute(thatHasThose[a])) return alsoIfAttributeNotFound;
                if (!forValues[a].equals(e.getAttribute(thatHasThose[a]))) return false;
            }
        }
        return true;
    }

    public boolean resetFile(File f, String name) {
        try {
            if (f.exists()) f.delete();

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

    public String getStringAttribute(Element e, String attribute) {
        return e.hasAttribute(attribute) ? e.getAttribute(attribute) : null;
    }

    public long getLongAttribute(Element e, String attribute) {
        String value = getStringAttribute(e, attribute);
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return -1;
        }
    }

    public boolean getBooleanAttribute(Element e, String attribute) {
        String s = getStringAttribute(e, attribute);
        return s != null && Boolean.parseBoolean(s);

    }

    public int getIntAttribute(Element e, String attribute) {
        try {
            return Integer.parseInt(getStringAttribute(e, attribute));
        } catch (Exception ex) {
            return -1;
        }
    }

    public float getFloatAttribute(Element e, String attribute) {
        try {
            return Float.parseFloat(getStringAttribute(e, attribute));
        } catch (Exception ex) {
            return -1;
        }
    }

    public class IdValue {
        public String value;
        public int id;

        public IdValue(String value, int id) {
            this.value = value;
            this.id = id;
        }
    }
}
