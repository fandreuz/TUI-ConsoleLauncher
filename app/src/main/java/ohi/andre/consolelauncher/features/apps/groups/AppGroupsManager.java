package ohi.andre.consolelauncher.features.apps.groups;

import android.graphics.Color;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.features.apps.InstalledApplication;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.xml.XMLUtils;

public class AppGroupsManager {
    private static String FILE_NAME     = "app_groups.xml";
    private static String XML_ROOT_NAME = "appgroups";
    
    public static final int RESULT_INVALID_GROUP_NAME       = 10;
    public static final int RESULT_XML_ERROR                = 11;
    public static final int RESULT_GROUP_ALREADY_DELETED    = 12;
    public static final int RESULT_GROUP_NAME_ALREADY_TAKEN = 13;
    public static final int RESULT_MEMBER_ALREADY_INSIDE    = 14;
    public static final int RESULT_MEMBER_NOT_INSIDE        = 15;
    
    private Map<String, AppGroup> appGroups;
    
    private final XMLUtils xmlUtils;
    private final File     groupsFile;
    
    // installedApplication.getKey() -> installedApplication
    public AppGroupsManager (HashMap<String, InstalledApplication> applicationsSnapshot) {
        this(applicationsSnapshot, new XMLUtils());
    }
    
    protected AppGroupsManager (Map<String, InstalledApplication> applicationsSnapshot, XMLUtils utils) {
        groupsFile = new File(Tuils.getFolder(), FILE_NAME);
        load(applicationsSnapshot);
        xmlUtils = utils;
    }
    
    protected void load (Map<String, InstalledApplication> applicationsSnapshot) {
        Runnable loadGroups = () -> {
            Map<String, AppGroup> map = new HashMap<>();
            
            Object[] pack;
            try {
                pack = xmlUtils.buildDocument(groupsFile, XML_ROOT_NAME);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            
            Element root = (Element) pack[1];
            
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Element groupTag = (Element) children.item(i);
                
                AppGroup group = AppGroup.fromXMLTag(applicationsSnapshot, groupTag);
                map.put(group.name(), group);
            }
            
            appGroups = map;
        };
        
        Executors.newSingleThreadExecutor()
                .submit(loadGroups);
    }
    
    public Map<String, AppGroup> getGroups () {
        if (appGroups != null) return Collections.unmodifiableMap(appGroups);
        else return new HashMap<>();
    }
    
    public AppGroup getGroup (String name) {
        return appGroups.get(name);
    }
    
    // updates the underlying file.
    // returns the error code (or 0)
    public int createGroup (String name) {
        if (nameTaken(name)) return RESULT_GROUP_NAME_ALREADY_TAKEN;
        
        if (nameQuality(name)) {
            try {
                xmlUtils.newRootNode(groupsFile, name);
                
                // reflect changes in-app only if the file was properly modified
                appGroups.put(name, new AppGroup.Builder()
                        .setName(name)
                        .build());
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                return RESULT_XML_ERROR;
            }
        } else {
            return RESULT_INVALID_GROUP_NAME;
        }
    }
    
    // checks if the name is already the name of another group
    private boolean nameTaken (String name) {
        return appGroups.containsKey(name);
    }
    
    // checks if the given name satisfies the guidelines for group names:
    // only letters+numbers
    protected static boolean nameQuality (String name) {
        Pattern pattern = Pattern.compile("[a-zA-Z0-9]]*");
        return pattern.matcher(name)
                .matches();
    }
    
    // updates the underlying file.
    // returns the error code (or 0)
    public int deleteGroup (String groupName) {
        // this may be a cached instance, to check if it has already been deleted I check
        // whether or not the same instance is in appGroups
        if (appGroups.containsKey(groupName)) {
            try {
                xmlUtils.removeRootNode(groupsFile, new XMLUtils.XMLMatcher.Builder()
                        .setNodeName(AppGroup.APP_GROUP_NODE_NAME)
                        .addAttributeConstraints(AppGroup.ATTRIBUTE_NAME, groupName)
                        .build(), false);
                appGroups.remove(groupName);
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                return RESULT_XML_ERROR;
            }
        } else {
            return RESULT_GROUP_ALREADY_DELETED;
        }
    }
    
    // returns the error code (or 0)
    public int addMember (AppGroup group, InstalledApplication application) {
        if (!group.containsMember(application)) {
            try {
                Object[] pack = xmlUtils.buildDocument(groupsFile, XML_ROOT_NAME);
                
                Document document = (Document) pack[0];
                Element root = (Element) pack[1];
                
                Element groupElement = xmlUtils.findNode(root, group.buildMatcher());
                
                Element child = document.createElement(AppGroup.MEMBER_NODE_NAME);
                child.setNodeValue(application.getKey());
                
                groupElement.appendChild(child);
                xmlUtils.write(document, groupsFile);
                
                group.addMember(application);
                
                return 0;
            } catch (Exception e) {
                return RESULT_XML_ERROR;
            }
        } else {
            return RESULT_MEMBER_ALREADY_INSIDE;
        }
    }
    
    // returns the error code (or 0)
    public int removeMember (AppGroup group, InstalledApplication application) {
        if (group.containsMember(application)) {
            try {
                Object[] pack = xmlUtils.buildDocument(groupsFile, XML_ROOT_NAME);
                
                Document document = (Document) pack[0];
                Element root = (Element) pack[1];
                
                // find the group tag
                Element groupElement = xmlUtils.findNode(root, group.buildMatcher());
                
                // remove the given member
                xmlUtils.removeChildNode(groupElement, document, groupsFile, new XMLUtils.XMLMatcher.Builder()
                                .setNodeName(AppGroup.MEMBER_NODE_NAME)
                                .addAttributeConstraints(AppGroup.ATTRIBUTE_MEMBER_KEY, application.getKey())
                                .build(),
                        false);
                
                group.removeMember(application);
                
                return 0;
            } catch (Exception e) {
                return RESULT_XML_ERROR;
            }
        } else {
            return RESULT_MEMBER_NOT_INSIDE;
        }
    }
    
    // set the group text and bg color for suggestions. take strings, because Color.parseColor can't be
    // wrote in #RRGGBB.
    // pass null if you don't want to set an attribute
    // returns the error code (or 0)
    public int setGroupColors (AppGroup group, String bgColor, String textColor) {
        Map<String, String> map = new HashMap<>();
        if (bgColor != null) map.put(AppGroup.ATTRIBUTE_SUGGESTION_BG_COLOR, bgColor);
        if (textColor != null) map.put(AppGroup.ATTRIBUTE_SUGGESTION_TEXT_COLOR, textColor);
        
        try {
            xmlUtils.setAttributes(groupsFile, group.buildMatcher(), map, true, false
            );
            
            if (textColor != null) group.setSuggestionTextColor(Color.parseColor(textColor));
            if (bgColor != null) group.setSuggestionBgColor(Color.parseColor(bgColor));
            
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return RESULT_XML_ERROR;
        }
    }
    
    public int setGroupSortMode (AppGroup group, int sortMode) {
        Map<String, String> map = new HashMap<>();
        map.put(AppGroup.ATTRIBUTE_SORT_MODE, String.valueOf(sortMode));
        
        try {
            xmlUtils.setAttributes(groupsFile, group.buildMatcher(), map, true, false);
            
            group.setSortMode(sortMode);
            
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return RESULT_XML_ERROR;
        }
    }
}
