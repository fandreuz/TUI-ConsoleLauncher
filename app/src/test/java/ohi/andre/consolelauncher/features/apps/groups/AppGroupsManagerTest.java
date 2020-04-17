package ohi.andre.consolelauncher.features.apps.groups;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import ohi.andre.consolelauncher.features.apps.InstalledApplication;
import ohi.andre.consolelauncher.tuils.xml.XMLUtils;

@RunWith(MockitoJUnitRunner.class)
public class AppGroupsManagerTest {
    
    @Mock
    AppGroupsManager appGroupsManager;
    
    @InjectMocks
    Map<String, InstalledApplication> applicationMap = new HashMap<>();
    @InjectMocks
    XMLUtils                          xmlUtils       = new XMLUtils();
    
    @Before
    public void init () {
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
    }
    
    @Test
    public void deleteGroup () {
        appGroupsManager.deleteGroup("group1");
        
        // test delete twice
        Assert.assertEquals(AppGroupsManager.RESULT_GROUP_ALREADY_DELETED, appGroupsManager.deleteGroup("group1"));
    }
}