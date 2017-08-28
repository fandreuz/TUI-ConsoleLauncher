package ohi.andre.consolelauncher.managers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.tuils.Compare;
import ohi.andre.consolelauncher.tuils.Tuils;

public class ContactManager {

    private Context context;
    private List<Contact> contacts;

    public ContactManager(Context context) {
        this.context = context;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            refreshContacts(context);
        }
    }

    public void refreshContacts(final Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_CONTACTS}, LauncherActivity.COMMAND_SUGGESTION_REQUEST_PERMISSION);
            return;
        }

        new Thread() {
            @Override
            public void run() {
                super.run();

                if(contacts == null) {
                    contacts = new ArrayList<>();
                } else {
                    contacts.clear();
                }
                List<Contact> contacts = ContactManager.this.contacts;

                Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[] {ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.Data.IS_SUPER_PRIMARY,}, null, null,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

                if (phones != null) {

                    int lastId = -1;
                    List<String> lastNumbers = new ArrayList<>();
                    List<String> nrml = new ArrayList<>();
                    int defaultNumber = 0;
                    String name = null, number;
                    int id, prim;

                    while (phones.moveToNext()) {
                        id = phones.getInt(phones.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                        number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        prim = phones.getInt(phones.getColumnIndex(ContactsContract.Data.IS_SUPER_PRIMARY));
                        if(prim > 0) {
                            defaultNumber = lastNumbers.size();
                        }

                        if(number == null || number.length() == 0) continue;

                        if(phones.isFirst()) {
                            lastId = id;
                            name = phones.getString(phones.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        } else if(id != lastId || phones.isLast()) {
                            lastId = id;

                            contacts.add(new Contact(name, lastNumbers, defaultNumber));

                            lastNumbers = new ArrayList<>();
                            nrml = new ArrayList<>();
                            name = null;
                            defaultNumber = 0;

                            name = phones.getString(phones.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        }

                        String normalized = number.replaceAll(Tuils.SPACE, Tuils.EMPTYSTRING);
                        if(!nrml.contains(normalized)) {
                            nrml.add(normalized);
                            lastNumbers.add(number);
                        }

                        if(name != null && phones.isLast()) {
                            contacts.add(new Contact(name, lastNumbers, defaultNumber));
                        }
                    }
                    phones.close();
                }

                List<Contact> cp = new ArrayList<>(contacts);
                for(int count = 0; count < cp.size(); count++) {
                    if(cp.get(count).numbers.size() == 0) contacts.remove(count--);
                }

                Collections.sort(contacts);
            }
        }.start();
    }

    public List<String> listNames() {
        if(contacts == null || contacts.size() == 0) refreshContacts(context);

        List<String> names = new ArrayList<>();
        for(Contact c : contacts) names.add(c.name);
        return names;
    }

    public List<Contact> getContacts() {
        if(contacts == null || contacts.size() == 0) refreshContacts(context);

        return contacts;
    }

    public List<String> listNamesAndNumbers() {
        if(contacts == null || contacts.size() == 0) refreshContacts(context);

        List<String> c = new ArrayList<>();

        for(int count = 0; count < contacts.size(); count++) {
            Contact cnt = contacts.get(count);

            StringBuilder b = new StringBuilder();
            b.append(cnt.name);

            for(String n : cnt.numbers) {
                b.append(Tuils.NEWLINE);
                b.append("\t");
                b.append(n);
            }

            c.add(b.toString());
        }

        return c;
    }

    public static final int NAME = 0;
    public static final int NUMBERS = 1;
    public static final int TIME_CONTACTED = 2;
    public static final int LAST_CONTACTED = 3;
    public static final int CONTACT_ID = 4;
    public static final int SIZE = CONTACT_ID + 1;

    public String[] about(String phone) {
        Cursor mCursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[] {ContactsContract.CommonDataKinds.Phone.CONTACT_ID},
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?", new String[] {phone},
                null);

        if(mCursor == null || mCursor.getCount() == 0) return null;
        String[] about = new String[SIZE];

        mCursor.moveToNext();

        String id = mCursor.getString(mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
        about[CONTACT_ID] = id;

        mCursor.close();
        mCursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED, ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED,
                        ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] {id},
                null);

        if(mCursor == null || mCursor.getCount() == 0) return null;
        mCursor.moveToNext();

        about[NAME] = mCursor.getString(mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        about[NUMBERS] = new String(Tuils.EMPTYSTRING);

        int timesContacted = -1;
        long lastContacted = Long.MAX_VALUE;
        do {
            int tempT = mCursor.getInt(mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED));
            long tempL = mCursor.getLong(mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED));

            timesContacted = tempT > timesContacted ? tempT : timesContacted;
            if(tempL > 0) lastContacted = tempL < lastContacted ? tempL : lastContacted;

            String n = mCursor.getString(mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            about[NUMBERS] = (about[NUMBERS].length() > 0 ? about[NUMBERS] + Tuils.NEWLINE : Tuils.EMPTYSTRING) + n;
        } while (mCursor.moveToNext());

        about[TIME_CONTACTED] = String.valueOf(timesContacted);
        if(lastContacted != Long.MAX_VALUE) {
            long difference = System.currentTimeMillis() - lastContacted;
            long sc = difference / 1000;
            if(sc < 60) {
                about[LAST_CONTACTED] = "sec: " + String.valueOf(lastContacted);
            } else {
                int ms = (int) (sc / 60);
                sc = ms % 60;
                if(ms < 60) {
                    about[LAST_CONTACTED] = "min: " + ms + ", sec: " + sc;
                } else {
                    int h = ms / 60;
                    ms = h % 60;
                    if(h < 24) {
                        about[LAST_CONTACTED] = "h: " + h + ", min: " + ms + ", sec: " + sc;
                    } else {
                        int days = h / 24;
                        h = days % 24;
                        about[LAST_CONTACTED] = "d: " + days + ", h: " + h + ", min: " + ms + ", sec: " + sc;
                    }
                }
            }
        }

        return about;
    }

    public String findNumber(String name) {
        if(contacts == null) refreshContacts(context);

        for(int count = 0; count < contacts.size(); count++) {
            Contact c = contacts.get(count);
            if(c.name.equalsIgnoreCase(name)) {
                if(c.numbers.size() > 0) return c.numbers.get(0);
            }
        }

        return null;
    }

    public boolean delete(String phone) {
        return context.getContentResolver().delete(fromPhone(phone), null, null) > 0;
    }

    public Uri fromPhone(String phone) {
        Cursor mCursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?", new String[] {phone},
                null);

        if(mCursor == null || mCursor.getCount() == 0) return null;
        mCursor.moveToNext();

        String name = mCursor.getString(mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        mCursor.close();

        mCursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                new String[] {ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME},
                ContactsContract.Contacts.DISPLAY_NAME + " = ?", new String[] {name},
                null);

        if(mCursor == null || mCursor.getCount() == 0) return null;
        mCursor.moveToNext();

        String mCurrentLookupKey = mCursor.getString(mCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        long mCurrentId = mCursor.getLong(mCursor.getColumnIndex(ContactsContract.Contacts._ID));

        mCursor.close();

        return ContactsContract.Contacts.getLookupUri(mCurrentId, mCurrentLookupKey);
    }

    public static class Contact implements Comparable<Contact>, Compare.Stringable {
        public String name;
        public List<String> numbers = new ArrayList<>();

        private int selectedNumber;

        public Contact(String name, List<String> numbers, int defNumber) {
            this.name = name;
            this.numbers = numbers;

            setSelectedNumber(defNumber);
        }

        public void setSelectedNumber(int s) {
            if(s >= numbers.size()) s = 0;
            this.selectedNumber = s;
        }

        public int getSelectedNumber() {
            return selectedNumber;
        }

        @Override
        public String toString() {
            return name + " : " + numbers.toString();
        }

        @Override
        public int compareTo(@NonNull Contact o) {
            char tf = name.toUpperCase().charAt(0);
            char of = o.name.toUpperCase().charAt(0);

            return tf - of;
        }

        @Override
        public String getString() {
            return name;
        }
    }
}
