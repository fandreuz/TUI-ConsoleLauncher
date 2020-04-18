package ohi.andre.consolelauncher.features;

import org.junit.Test;

import java.util.regex.Pattern;

import ohi.andre.consolelauncher.features.alias.AliasManager;

import static org.junit.Assert.*;

public class AliasManagerTest {
    
    @Test
    public void testAliasClass() {
        // test number of parameters
        Pattern parameterPattern = Pattern.compile("#");
        AliasManager.Alias alias = new AliasManager.Alias("pippo", "pluto#, # sui#ciao#", parameterPattern);
        assertEquals("test number of parameters", 4, alias.parametersCount);
        
        // test parameter replacement
        String replaced = alias.applyParameters("test1,test2,test3,test4", parameterPattern, Pattern.compile(","));
        assertEquals("test parameter replacement", "plutotest1, test2 suitest3ciaotest4", replaced);
    }
}