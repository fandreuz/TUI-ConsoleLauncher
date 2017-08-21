package ohi.andre.consolelauncher.managers;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by satyan on 8/21/17.
 */


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class WhatsAppManager extends ContactManager {
    public WhatsAppManager(Context context) {
        super(context);
    }

    @Override
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
                List<Contact> contacts = WhatsAppManager.this.contacts;

                Cursor c = context.getContentResolver().query(
                        ContactsContract.RawContacts.CONTENT_URI,
                        new String[]{
                                ContactsContract.RawContacts.SYNC1,
                                ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
                                ContactsContract.RawContacts.CONTACT_ID
                        },
                        ContactsContract.RawContacts.ACCOUNT_TYPE + "= ?",
                        new String[] { "com.whatsapp" },
                        null);
                
                if (c != null) {

                    int lastId = -1;
                    List<String> lastNumbers = new ArrayList<>();
                    List<String> nrml = new ArrayList<>();
                    int defaultNumber = 0;
                    String name = null, number;
                    int id;

                    while (c.moveToNext()) {
                        id = c.getInt(c.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
                        number = c.getString(c.getColumnIndex(ContactsContract.RawContacts.SYNC1));
                        defaultNumber = lastNumbers.size();

                        if(number == null || number.length() == 0) continue;

                        if(c.isFirst()) {
                            lastId = id;
                            name = c.getString(c.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY));
                        } else if(id != lastId || c.isLast()) {
                            lastId = id;

                            contacts.add(new Contact(name, lastNumbers, defaultNumber));

                            lastNumbers = new ArrayList<>();
                            nrml = new ArrayList<>();
                            defaultNumber = 0;

                            name = c.getString(c.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY));
                        }

                        String normalized = number.replaceAll(Tuils.SPACE, Tuils.EMPTYSTRING);
                        if(!nrml.contains(normalized)) {
                            nrml.add(normalized);
                            lastNumbers.add(number);
                        }

                        if(name != null && c.isLast()) {
                            contacts.add(new Contact(name, lastNumbers, defaultNumber));
                        }
                    }
                    c.close();
                }
                List<Contact> cp = new ArrayList<>(contacts);
                for(int count = 0; count < cp.size(); count++) {
                    if(cp.get(count).numbers.size() == 0) contacts.remove(count--);
                }

                Collections.sort(contacts);
            }
        }.start();
    }

    @Override
    public String findNumber(String name) {
        if(contacts == null) refreshContacts(context);

        for(int count = 0; count < contacts.size(); count++) {
            Contact c = contacts.get(count);
            if(c.name.toUpperCase().contains(name.toUpperCase())) {
                if(c.numbers.size() > 0){
                    return c.numbers.get(0);
                }
            }
        }

        return null;
    }

    public static final int NAME = 0;
    public static final int NUMBERS = 1;
    public static final int CONTACT_ID = 2;
    public static final int SIZE = CONTACT_ID + 1;

    @Override
    public String[] about(String phone) {

        Cursor c = context.getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts.CONTACT_ID
                },
                ContactsContract.RawContacts.ACCOUNT_TYPE + "= ?, "
                        + ContactsContract.RawContacts.SYNC1 + "= ?",
                new String[] { "com.whatsapp",  phone},
                null);

        if(c == null || c.getCount() == 0) return null;
        String[] about = new String[SIZE];

        c.moveToNext();

        String id = c.getString(c.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
        about[CONTACT_ID] = id;

        c.close();

        c = context.getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts.SYNC1,
                        ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
                        ContactsContract.RawContacts.CONTACT_ID
                },
                ContactsContract.RawContacts.CONTACT_ID + "= ?",
                new String[] {id},
                null);

        if(c == null || c.getCount() == 0) return null;
        c.moveToNext();

        about[NAME] = c.getString(c.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY));
        about[NUMBERS] = Tuils.EMPTYSTRING;

        do {
            String n = c.getString(c.getColumnIndex(ContactsContract.RawContacts.SYNC1));
            about[NUMBERS] = (about[NUMBERS].length() > 0 ? about[NUMBERS] + Tuils.NEWLINE : Tuils.EMPTYSTRING) + n;
        } while (c.moveToNext());

        return about;
    }

    @Override
    public boolean delete(String phone) {
        return false;
    }

    @Override
    public Uri fromPhone(String phone) {
        return null;
    }
}
