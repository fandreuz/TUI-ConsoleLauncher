package ohi.andre.consolelauncher.managers.apps;

import org.junit.Test;

import static org.junit.Assert.*;

public class InstalledApplicationTest {
    InstalledApplication application = new InstalledApplication("ohi.andre.consolelauncher",
            "ohi.andre.consolelauncher.LauncherActivity", "t-ui", null);
    
    @Test
    public void belongs () {
        assertTrue(application.belongs(application.getKey() + AppsManager.APPS_SEPARATOR + application.getKey()));
    }
}