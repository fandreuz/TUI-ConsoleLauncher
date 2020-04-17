package ohi.andre.consolelauncher.features.apps.groups;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import ohi.andre.consolelauncher.features.apps.InstalledApplication;
import ohi.andre.consolelauncher.tuils.xml.XMLUtils;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AppGroupsManagerTest {
    
    @Mock
    AppGroupsManager appGroupsManager;
    
    @InjectMocks
    Map<String, InstalledApplication> applicationMap = new HashMap<>();
    @InjectMocks
    XMLUtils                          xmlUtils       = new XMLUtils();
    
    @Before
    public void init () {
        MockitoAnnotations.initMocks(this);
        
        doNothing().when(appGroupsManager)
                .load(any());
        try {
            doNothing().when(xmlUtils)
                    .removeNode(any(), any(), any());
            doNothing().when(xmlUtils)
                    .newNode(any(), any());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        appGroupsManager.createGroup("group1");
    }
    
    @Test
    public void nameQuality () {
        Assert.assertTrue(AppGroupsManager.nameQuality("groupnameok"));
        Assert.assertFalse(AppGroupsManager.nameQuality("groupname notok"));
        Assert.assertTrue(AppGroupsManager.nameQuality("99groupnameok12"));
    }
    
    
    @Test
    public void createGroup () {
        // check group name taken
        Assert.assertEquals(AppGroupsManager.RESULT_GROUP_NAME_ALREADY_TAKEN, appGroupsManager.createGroup("group1"));
        
        // check invalid name
        Assert.assertEquals(AppGroupsManager.RESULT_INVALID_GROUP_NAME, appGroupsManager.createGroup("group 2"));
        
        // check valid group
        appGroupsManager.createGroup("group 3");
        verify(xmlUtils, times(1)).newNode(any(), "group3");
    }
    
    @Test
    public void deleteGroup () {
        appGroupsManager.deleteGroup("group1");
        
        // verify xml call
        try {
            verify(xmlUtils, times(1)).removeNode(any(), new XMLUtils.XMLMatcher.Builder().setNodeName("group1")
                    .build(), any());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // test delete twice
        Assert.assertEquals(AppGroupsManager.RESULT_GROUP_ALREADY_DELETED, appGroupsManager.deleteGroup("group1"));
    }
}