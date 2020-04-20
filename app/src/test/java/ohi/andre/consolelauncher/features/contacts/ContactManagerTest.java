package ohi.andre.consolelauncher.features.contacts;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ohi.andre.consolelauncher.tuils.Tuils;

import static org.junit.Assert.*;

public class ContactManagerTest {
    
    @Test
    public void longestFullNameAndPhoneNumber () {
        String longerName = "Piergiovanni Luigi Rossi", shorterName = "Mario Rossi";
        String longerPhone = "328462738292", shorterPhone1 = "83829298", shorterPhoen2 = "392992";
        
        List<Contact> contacts = new ArrayList<>();
        contacts.add(new Contact(longerName, Arrays.asList(shorterPhoen2)));
        contacts.add(new Contact(shorterName, Arrays.asList(shorterPhone1, longerPhone)));
    
        Tuils.BiPack<Integer,Integer> maxPack = ContactManager.longestFullNameAndPhoneNumber(contacts);
        assertEquals(longerName.length(), (int) maxPack.object1);
        assertEquals(longerPhone.length(), (int) maxPack.object2);
    }
}