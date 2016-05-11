package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import ohi.andre.comparestring.Compare;

public class ContactManager {

    public static final boolean USE_SCROLL_COMPARE = false;

    private Context context;

    public ContactManager(Context context) {
        this.context = context;
    }

    private Map<String, String> getContacts() {
        Map<String, String> contacts = new TreeMap<>();

        Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (phones != null && phones.moveToNext()) {
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            contacts.put(name, phoneNumber);
        }
        if (phones != null)
            phones.close();

        return contacts;
    }

    public Set<String> names() {
        return getContacts().keySet();
    }

    public ArrayList<String> listNamesAndNumbers() {
        ArrayList<String> values = new ArrayList<>();

        Set<Entry<String, String>> set = getContacts().entrySet();
        for (Entry<String, String> entry : set)
            values.add(entry.getKey() + "\t:\t" + entry.getValue());

        return values;
    }

    public String findNumber(String name, int minRate) {
        Map<String, String> contacts = getContacts();
        Set<String> names = contacts.keySet();

        String mostSuitable = Compare.getOneSimilarString(names, name, minRate, USE_SCROLL_COMPARE);
        return mostSuitable == null ? null : contacts.get(mostSuitable);
    }
}
