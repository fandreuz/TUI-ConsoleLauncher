package ohi.andre.consolelauncher.features.contacts;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public class Contact implements Comparable<Contact> {
    // the full name of this contact
    protected final String fullName;
    
    // the phone numbers which belong to this command.
    // the default number is the first one
    private List<String> phoneNumbers;
    
    public Contact (String name, List<String> numbers) {
        this.fullName     = name;
        this.phoneNumbers = numbers;
    }
    
    protected String getPhoneNumber (int index) {
        return phoneNumbers.get(index);
    }
    
    protected int getPhoneNumbersCount() {
        return phoneNumbers.size();
    }
    
    protected String deletePhoneNumber(int index) {
        return phoneNumbers.remove(index);
    }
    
    protected List<String> getPhoneNumbers() {
        return Collections.unmodifiableList(phoneNumbers);
    }
    
    @Override
    public int compareTo (@NonNull Contact o) {
        return fullName.compareTo(o.fullName);
    }
}