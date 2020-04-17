package ohi.andre.consolelauncher.features.apps.groups;

import android.graphics.Color;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import ohi.andre.consolelauncher.MainManager;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.features.apps.InstalledApplication;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.xml.XMLUtils;

// a group of applications. settable foreground/background color, settable sort mode
public class AppGroup implements MainManager.Group<InstalledApplication> {
    // the tag which represents an app group persisted in an XML file
    public static final String APP_GROUP_NODE_NAME = "group";
    
    public static final  String ATTRIBUTE_NAME                  = "name";
    public static final String ATTRIBUTE_SUGGESTION_TEXT_COLOR = "suggestionTextColor";
    public static final String ATTRIBUTE_SUGGESTION_BG_COLOR   = "suggestionBgColor";
    public static final String ATTRIBUTE_SORT_MODE             = "sortMode";
    
    // the name of the member nodes under the main tag
    public static final String MEMBER_NODE_NAME = "member";
    public static final String ATTRIBUTE_MEMBER_KEY = "key";
    
    public static final int SORT_MODE_LEXICOGRAPHIC_DESCENDING = 10;
    public static final int SORT_MODE_LEXICOGRAPHIC_ASCENDING  = 11;
    public static final int SORT_MODE_MOST_USED_DESCENDING     = 12;
    public static final int SORT_MODE_MOST_USED_ASCENDING      = 13;
    // last used means the last time an application has been launched (from t-ui)
    public static final int SORT_MODE_LAST_USED_DESCENDING     = 14;
    public static final int SORT_MODE_LAST_USED_ASCENDING      = 15;
    
    private final String name;
    
    private int                              sortMode;
    private Comparator<InstalledApplication> membersComparator;
    
    private int suggestionTextColor, suggestionBgColor;
    
    private List<InstalledApplication> members;
    
    private AppGroup (String name, int sortMode, int suggestionBgColor, int suggestionTextColor, List<InstalledApplication> members) {
        this.name                = name;
        this.members             = members;
        this.suggestionBgColor   = suggestionBgColor;
        this.suggestionTextColor = suggestionTextColor;
        
        setSortMode(sortMode);
        Collections.sort(members, membersComparator);
    }
    
    private static Comparator<InstalledApplication> buildComparator (int sortMode) {
        switch (sortMode) {
            case SORT_MODE_LEXICOGRAPHIC_ASCENDING:
                return (app1, app2) -> sortMode * app1.publicLabel.compareTo(app2.publicLabel);
            case SORT_MODE_LEXICOGRAPHIC_DESCENDING:
                return (app1, app2) -> -(sortMode * app1.publicLabel.compareTo(app2.publicLabel));
            case SORT_MODE_MOST_USED_ASCENDING:
                return (app1, app2) -> Integer.compare(app1.getLaunchedTimes(), app2.getLaunchedTimes());
            case SORT_MODE_MOST_USED_DESCENDING:
                return (app1, app2) -> -(Integer.compare(app1.getLaunchedTimes(), app2.getLaunchedTimes()));
            // an app which is "more last used" (i.e. used more recently) will be listed before
            case SORT_MODE_LAST_USED_DESCENDING:
                return (app1, app2) -> Long.compare(app1.getLastLaunched(), app2.getLastLaunched());
            case SORT_MODE_LAST_USED_ASCENDING:
                return (app1, app2) -> -(Long.compare(app1.getLastLaunched(), app2.getLastLaunched()));
            default:
                return null;
        }
    }
    
    // build a group from the given XML tag
    public static AppGroup fromXMLTag (Map<String, InstalledApplication> applications, Element xmlTag) {
        String name = xmlTag.getAttribute(ATTRIBUTE_NAME);
        int sortMode = Tuils.parseIntOrDefault(xmlTag.getAttribute(ATTRIBUTE_SORT_MODE), SORT_MODE_MOST_USED_DESCENDING);
        int suggestionTextColor = Tuils.parseColorOrDefault(xmlTag.getAttribute(ATTRIBUTE_SUGGESTION_TEXT_COLOR), Color.BLACK);
        int suggestionBgColor = Tuils.parseColorOrDefault(xmlTag.getAttribute(ATTRIBUTE_SUGGESTION_BG_COLOR), Color.WHITE);
        
        List<InstalledApplication> members = new ArrayList<>();
        
        NodeList children = xmlTag.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                InstalledApplication app = applications.get(
                        ((Element) children.item(i)).getAttribute(ATTRIBUTE_MEMBER_KEY)
                );
                
                if (app != null) members.add(app);
            }
            
        }
        
        return new AppGroup(name, sortMode, suggestionBgColor, suggestionTextColor, members);
    }
    
    // a convenient method to construct an XMLMatcher for this group
    public XMLUtils.XMLMatcher buildMatcher() {
        return new XMLUtils.XMLMatcher.Builder()
                .setNodeName(AppGroup.APP_GROUP_NODE_NAME)
                .addAttributeConstraints(AppGroup.ATTRIBUTE_NAME, this.name)
                .build();
    }
    
    public int getSortMode () {
        return sortMode;
    }
    
    protected void setSortMode (int sortMode) {
        this.sortMode          = sortMode;
        this.membersComparator = buildComparator(sortMode);
    }
    
    public int getSuggestionTextColor () {
        return suggestionTextColor;
    }
    
    protected void setSuggestionTextColor (int suggestionTextColor) {
        this.suggestionTextColor = suggestionTextColor;
    }
    
    public int getSuggestionBgColor () {
        return suggestionBgColor;
    }
    
    protected void setSuggestionBgColor (int suggestionBgColor) {
        this.suggestionBgColor = suggestionBgColor;
    }
    
    protected boolean containsMember (InstalledApplication application) {
        return members.contains(application);
    }
    
    protected boolean addMember (InstalledApplication application) {
        if (members.contains(application)) return false;
        
        members.add(application);
        Collections.sort(members, membersComparator);
        
        return true;
    }
    
    protected boolean removeMember (InstalledApplication application) {
        return members.remove(application);
    }
    
    @Override
    public List<InstalledApplication> members () {
        return Collections.unmodifiableList(members);
    }
    
    @Override
    public boolean use (ExecutePack mainPack, String input) {
        return false;
    }
    
    @Override
    public String name () {
        return null;
    }
    
    public static class Builder {
        private String name;
        
        private int sortMode;
        private int suggestionTextColor, suggestionBgColor;
        
        private List<InstalledApplication> members;
        
        public Builder setName (String name) {
            this.name = name;
            return this;
        }
        
        public Builder setSortMode (int sortMode) {
            this.sortMode = sortMode;
            return this;
        }
        
        public Builder setSuggestionTextColor (int suggestionTextColor) {
            this.suggestionTextColor = suggestionTextColor;
            return this;
        }
        
        public Builder setSuggestionBgColor (int suggestionBgColor) {
            this.suggestionBgColor = suggestionBgColor;
            return this;
        }
        
        public Builder setMembers (List<InstalledApplication> members) {
            this.members = members;
            return this;
        }
        
        public AppGroup build () {
            return new AppGroup(name, sortMode, suggestionBgColor, suggestionTextColor, members);
        }
    }
}
