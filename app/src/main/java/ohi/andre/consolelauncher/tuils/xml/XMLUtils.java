package ohi.andre.consolelauncher.tuils.xml;

import android.support.v4.util.ArraySet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    
    private final DocumentBuilderFactory factory;
    private final DocumentBuilder        builder;
    
    public XMLUtils () {
        DocumentBuilder builder;
        
        factory = DocumentBuilderFactory.newInstance();
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            builder = null;
            e.printStackTrace();
        }
        
        this.builder = builder;
    }
    
    final Pattern p1  = Pattern.compile(">");
    final Pattern p3  = Pattern.compile("\n\n");
    final String  p1s = ">" + "\n";
    final String  p3s = "\n";
    
    public String fixNewlines (String s) {
        s = p1.matcher(s)
                .replaceAll(p1s);
        s = p3.matcher(s)
                .replaceAll(p3s);
        return s;
    }
    
    // Build an array like [Document, Element (root)]
    // rootName is needed in order to recreate the file if it's corrupted
    public Object[] buildDocument (File file, String rootName) throws Exception {
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
    
    // write the given Document to the given file
    // this is synchronous
    public void write (Document d, File f) throws Exception {
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
    }
    
    public void newRootNode (File file, String nodeName) throws Exception {
        newRootNode(file, new XMLMatcher.Builder().setNodeName(nodeName)
                .build());
    }
    
    // add a new node to the root of the given file, and make this new node satisfy the given matcher.
    // matcher.nodeName can't be null
    public void newRootNode (File file, XMLMatcher newNodeDefinition) throws Exception {
        Object[] o;
        o = buildDocument(file, null);
        
        Document d = (Document) o[0];
        Element root = (Element) o[1];
        
        newChildNode(file, d, root, newNodeDefinition);
    }
    
    public void newChildNode (File file, Document document, Element parent, XMLMatcher newNodeDefinition) throws Exception {
        Element element = document.createElement(newNodeDefinition.nodeName);
        
        // set the attributes
        if (newNodeDefinition.attributeConstraints != null && newNodeDefinition.attributeConstraints.size() > 0) {
            for (Map.Entry<String, String> entry : newNodeDefinition.attributeConstraints.entrySet()) {
                element.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        
        // add the child to the root element
        parent.appendChild(element);
        
        // write the file
        write(document, file);
    }
    
    // an helper class used to construct matches over XML nodes.
    // if you don't need to check a field (nodeName, attributes) pass null to the constructor.
    public static class XMLMatcher {
        public static int OR  = 10;
        public static int AND = 11;
        
        // a check on nodeName relates to the other checks with an AND relationship
        public final String nodeName;
        
        public final Map<String, String> attributeConstraints;
        public final int                 attributesMatchMode;
        
        // if true, an attribute not found won't be considered as a false comparison
        public final boolean forgiveAttributeNotFound;
        
        // pass a null parameter if you don't need to check a field
        private XMLMatcher (String nodeName, Map<String, String> attributeConstraints,
                            int matchMode, boolean forgiveNotFound) {
            this.nodeName                 = nodeName;
            this.attributeConstraints     = attributeConstraints;
            this.attributesMatchMode      = matchMode;
            this.forgiveAttributeNotFound = forgiveNotFound;
        }
        
        // ignores null fields
        public boolean matches (Element element) {
            if (nodeName != null && !nodeName.equals(element.getNodeName())) return false;
            
            for (Map.Entry<String, String> constraint : attributeConstraints.entrySet()) {
                String value = element.getAttribute(constraint.getKey());
                
                if (value == null) {
                    if (!forgiveAttributeNotFound) {
                        // then consider this like a false comparison
                        if (attributesMatchMode == AND) return false;
                    }
                } else {
                    if (value.equals(constraint.getValue())) {
                        if (attributesMatchMode == OR) return true;
                    } else {
                        // if the match isn't OK, we can stop. the return value can't be true
                        if (attributesMatchMode == AND) return false;
                    }
                }
            }
            
            return true;
        }
        
        public static class Builder {
            private String nodeName;
            
            private Map<String, String> attributeConstraints;
            private int                 attributesMatchMode = AND;
            
            private boolean forgiveAttributeNotFound = false;
            
            public Builder setNodeName (String nodeName) {
                this.nodeName = nodeName;
                return this;
            }
            
            public Builder setAttributeConstraints (Map<String, String> attributeConstraints) {
                this.attributeConstraints = attributeConstraints;
                return this;
            }
            
            public Builder addAttributeConstraints (String key, String value) {
                if (this.attributeConstraints == null) this.attributeConstraints = new HashMap<>();
                this.attributeConstraints.put(key, value);
                return this;
            }
            
            public Builder setAttributesMatchMode (int attributesMatchMode) {
                this.attributesMatchMode = attributesMatchMode;
                return this;
            }
            
            public Builder setForgiveAttributeNotFound (boolean forgiveAttributeNotFound) {
                this.forgiveAttributeNotFound = forgiveAttributeNotFound;
                return this;
            }
            
            public XMLMatcher build () {
                return new XMLMatcher(nodeName, attributeConstraints, attributesMatchMode,
                        forgiveAttributeNotFound);
            }
        }
    }
    
    // set the attributes of the nodes which satisfy the given matcher.
    // if createNodeIfZeroMatches is true and matcher.nodeName != null, a new node will be created
    // (matching the given constraints) in case no node which matches the given XMLMatcher is found
    public void setAttributes (File file, XMLMatcher matcher, Map<String, String> values,
                               boolean addAttributeIfNotFound, boolean createNodeIfZeroMatches)
            throws Exception {
        Object[] o;
        o = buildDocument(file, null);
        
        Document d = (Document) o[0];
        Element root = (Element) o[1];
        
        Set<Element> matches = findNodes(root, matcher, Integer.MAX_VALUE);
        for (Element element : matches) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                // if we can attribute even it's not already there, no need to check if
                // the attribute is already there
                if (addAttributeIfNotFound || element.getAttribute(entry.getKey()) != null) {
                    element.setAttribute(entry.getKey(), entry.getValue());
                }
            }
        }
        
        if (matches.size() == 0 && createNodeIfZeroMatches && matcher.nodeName != null) {
            Element element = d.createElement(matcher.nodeName);
            
            // apply constraints before, because the user may request a certain value
            // to override it thereafter
            for (Map.Entry<String, String> entry : matcher.attributeConstraints.entrySet()) {
                element.setAttribute(entry.getKey(), entry.getValue());
            }
            
            for (Map.Entry<String, String> entry : values.entrySet()) {
                element.setAttribute(entry.getKey(), entry.getValue());
            }
            
            root.appendChild(element);
        }
        
        write(d, file);
    }
    
    // this was removed because potentially dangerous, since I want to reduce the use of
    // important values in the tag name (use <group name="group_name"> instead of
    // <group_name>
    /*public void removeNode (File file, String nodeName) throws Exception {
        removeNode(file, new XMLMatcher.Builder().setNodeName(nodeName)
                .build(), true);
    }*/
    
    // remove a root node which satisfies the given matcher. if all is true, all the matching
    // nodes will be removed.
    // return true if successful, false otherwise
    public void removeRootNode (File file, XMLMatcher matcher, boolean all) throws Exception {
        Object[] o;
        o = buildDocument(file, null);
        
        Document d = (Document) o[0];
        Element root = (Element) o[1];
        
        removeChildNode(root, d, file, matcher, all);
    }
    
    // document and file are used to write the result to the given file
    public void removeChildNode(Element parent, Document document, File file, XMLMatcher matcher,
                                boolean all) throws Exception {
        Set<Element> matches = findNodes(parent, matcher, all ? Integer.MAX_VALUE : 1);
        for (Element element : matches) {
            parent.removeChild(element);
        }
    
        if (matches.size() > 0) write(document, file);
    }
    
    // search the DIRECT children of parent which satisfy the given matcher
    // if the number of found nodes is < take, then the returned array will contain null indexes
    public Set<Element> findNodes (Element parent, XMLMatcher matcher, int take) {
        Set<Element> matches = new ArraySet<>();
        
        NodeList nodes;
        // todo: if we check for the name with getElementsByTagName we should avoid to check the name in matcher.matches
        if (matcher.nodeName != null) nodes = parent.getElementsByTagName(matcher.nodeName);
        else nodes = parent.getChildNodes();
        
        int matchCounter = 0;
        for (int j = 0; j < nodes.getLength() && matchCounter < take; j++) {
            Element element = (Element) nodes.item(j);
            
            if (matcher.matches(element)) {
                matches.add(element);
                matchCounter++;
            }
        }
        
        return matches;
    }
    
    public Set<Element> findRootNodes (File file, XMLMatcher matcher, int take) throws Exception {
        Object[] o;
        o = buildDocument(file, null);
        if (o == null) return null;
        
        Element root = (Element) o[1];
        
        return findNodes(root, matcher, take);
    }
    
    // find ONE node
    public Element findNode (Element parent, XMLMatcher matcher) {
        Set<Element> groupElementSet = findNodes(parent, matcher, 1);
        if (groupElementSet.size() == 0) return null;
        else return (Element) groupElementSet.toArray()[0];
    }
    
    public void resetFile (File f, String name) throws Exception {
        if (f.exists()) f.delete();
        
        FileOutputStream stream = new FileOutputStream(f);
        stream.write(XML_DEFAULT.getBytes());
        stream.write(("<" + name + ">\n").getBytes());
        stream.write(("</" + name + ">\n").getBytes());
        stream.flush();
        stream.close();
    }
    
    public String getStringAttribute (Element e, String attribute) {
        return e.hasAttribute(attribute) ? e.getAttribute(attribute) : null;
    }
    
    public long getLongAttribute (Element e, String attribute) {
        String value = getStringAttribute(e, attribute);
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return -1;
        }
    }
    
    public boolean getBooleanAttribute (Element e, String attribute) {
        String s = getStringAttribute(e, attribute);
        return s != null && Boolean.parseBoolean(s);
        
    }
    
    public int getIntAttribute (Element e, String attribute) {
        try {
            return Integer.parseInt(getStringAttribute(e, attribute));
        } catch (Exception ex) {
            return -1;
        }
    }
    
    public float getFloatAttribute (Element e, String attribute) {
        try {
            return Float.parseFloat(getStringAttribute(e, attribute));
        } catch (Exception ex) {
            return -1;
        }
    }
}
