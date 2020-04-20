package ohi.andre.consolelauncher.features.contacts;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.core.Observable;
import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.tuils.Tuils;

public class ContactManager {
    
    public static String ACTION_REFRESH = BuildConfig.APPLICATION_ID + ".refresh_contacts";
    
    private final Context context;
    
    // each contact has a unique ID, which is passed around the application.
    // this is given by the system
    private HashMap<Integer, Contact> contacts;
    
    private final BroadcastReceiver refreshContactsReceiver;
    
    public ContactManager (Context context) {
        this.context = context;
        
        if (ContextCompat
                .checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            loadContacts();
        }
        
        IntentFilter refreshFilter = new IntentFilter();
        refreshFilter.addAction(ACTION_REFRESH);
        
        refreshContactsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive (Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_REFRESH)) {
                    if (ContextCompat
                            .checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        loadContacts();
                    }
                }
            }
        };
        
        LocalBroadcastManager.getInstance(context.getApplicationContext())
                .registerReceiver(refreshContactsReceiver, refreshFilter);
    }
    
    public void destroy (Context context) {
        LocalBroadcastManager.getInstance(context.getApplicationContext())
                .unregisterReceiver(refreshContactsReceiver);
    }
    
    // before calling, make sure that t-ui has contact permissions
    private void loadContacts () {
        Executors.newSingleThreadExecutor()
                .execute(() -> {
                    if (contacts == null) {
                        contacts = new HashMap<>();
                    }
                    else {
                        contacts.clear();
                    }
                    
                    Cursor phones = context.getContentResolver()
                            .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    new String[]{ContactsContract.Contacts.DISPLAY_NAME,
                                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                                            ContactsContract.Data.IS_SUPER_PRIMARY,},
                                    null, null, null);
                    
                    if (phones != null) {
                        // the default phone number will be the first of the list
                        HashMap<Integer, Tuils.BiPack<String, List<String>>> contactsMap =
                                new HashMap<>();
                        
                        while (phones.moveToNext()) {
                            int id = phones.getInt(phones
                                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                            
                            // the phone number from this record
                            String phoneNumber = phones.getString(phones
                                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            if (phoneNumber == null || phoneNumber.length() == 0) { continue; }
                            
                            String name = phones.getString(phones
                                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                            
                            Tuils.BiPack<String, List<String>> pack = contactsMap.get(id);
                            if (pack == null) {
                                pack = new Tuils.BiPack<>(name, new ArrayList<>());
                                contactsMap.put(id, pack);
                            }
                            
                            boolean isPrimaryNumber = phones.getInt(phones
                                    .getColumnIndex(ContactsContract.Data.IS_SUPER_PRIMARY)) > 0;
                            if (isPrimaryNumber) {
                                pack.object2.set(0, phoneNumber);
                            }
                            else {
                                pack.object2.add(phoneNumber);
                            }
                        }
                        
                        phones.close();
                        
                        for (Map.Entry<Integer, Tuils.BiPack<String, List<String>>> entry :
                                contactsMap.entrySet()) {
                            // skip if no phone numbers for this contact
                            if (entry.getValue().object2.size() > 0) {
                                Tuils.BiPack<String, List<String>> pack = entry.getValue();
                                
                                contacts.put(
                                        entry.getKey(),
                                        new Contact(
                                                pack.object1,
                                                pack.object2
                                        )
                                );
                            }
                        }
                    }
                });
    }
    
    // shows a list of contacts with the format
    // fullName -> phoneNumber[0]
    // ...
    // fullName -> phoneNumber[n-1]
    // the lines are nicely padded
    public String lsContacts() {
        return ls(contacts.values(), longestFullNameAndPhoneNumber(contacts.values()));
    }
    
    protected static String ls (Collection<Contact> values, Tuils.BiPack<Integer,Integer> maxPack) {
        // I need the longest fullName and phoneNumber in order to pad nicely
        int longestFullNameLength = maxPack.object1;
        int longestPhoneNumberLength = maxPack.object2;
        
        return Observable.fromIterable(values)
                .flatMap(contact -> Observable.fromIterable(contact.getPhoneNumbers())
                        .map(phoneNumber -> new Tuils.BiPack<>(
                                contact.fullName,
                                phoneNumber)
                        ))
                .map(pack -> StringUtils.rightPad(pack.object1, longestFullNameLength) +
                        ": " + StringUtils.rightPad(pack.object2, longestPhoneNumberLength)
                )
                .toList()
                .map(list -> TextUtils.join("\n", list))
                .blockingGet();
    }
    
    protected static Tuils.BiPack<Integer, Integer> longestFullNameAndPhoneNumber (Collection<Contact> contactsCollection) {
        Tuils.BiPack<Integer, Integer> seed = new Tuils.BiPack<>(0, 0);
        return Observable.fromIterable(contactsCollection)
                .flatMap(contact -> Observable.fromIterable(contact.getPhoneNumbers())
                        .map(phoneNumber -> new Tuils.BiPack<>(
                                contact.fullName.length(),
                                phoneNumber.length())
                        ))
                // maxPack is a pack containing the lengths of the longest items so far
                .reduce(seed, (pack1, pack2) -> new Tuils.BiPack<>(
                        Math.max(pack1.object1, pack2.object1),
                        Math.max(pack1.object2, pack2.object2))
                )
                .blockingGet();
    }
    
    public static final int NAME_INDEX           = 0;
    public static final int NUMBERS_INDEX        = 1;
    public static final int TIME_CONTACTED_INDEX = 2;
    public static final int LAST_CONTACTED_INDEX = 3;
    public static final int CONTACT_ID_INDEX     = 4;
    public static final int ABOUT_SIZE           = CONTACT_ID_INDEX + 1;
    
    // get info about the given contact
    public String[] about (int contactID) {
        Cursor cursor = context.getContentResolver()
                .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
                                ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED,
                                ContactsContract.CommonDataKinds.Phone.NUMBER},
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{String.valueOf(contactID)},
                        null);
        
        if (cursor == null) { return null; }
        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }
        
        String[] about = new String[ABOUT_SIZE];
        
        cursor.moveToNext();
        
        about[NAME_INDEX] = cursor.getString(cursor
                .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        
        List<String> phoneNumbers = new ArrayList<>();
        
        int timesContacted = 0;
        long lastTimeContacted = Long.MIN_VALUE;
        
        do {
            // do this for each number of the contact
            
            int times = cursor.getInt(cursor
                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED));
            long lastTime = cursor.getLong(cursor
                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED));
            
            timesContacted += times;
            if (lastTime > 0) { lastTimeContacted = Math.max(lastTime, lastTimeContacted); }
            
            String phoneNumber = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            phoneNumbers.add(phoneNumber);
        } while (cursor.moveToNext());
        
        about[TIME_CONTACTED_INDEX] = String.valueOf(timesContacted);
        about[LAST_CONTACTED_INDEX] = lastTimeContacted != Long.MIN_VALUE ?
                new Date(lastTimeContacted).toString() :
                "n/a";
        about[NUMBERS_INDEX]        = TextUtils.join("\n", phoneNumbers);
        
        cursor.close();
        
        return about;
    }
    
    private String deleteContactPhoneNumber (int contactID, int phoneIndex) {
        Contact contact = contacts.get(contactID);
        if (contact == null || contact.getPhoneNumbersCount() <= phoneIndex) { return null; }
        
        String phoneNumber = contact.getPhoneNumber(phoneIndex);
        if (deletePhoneNumber(phoneNumber)) { return phoneNumber; }
        else { return null; }
    }
    
    // delete the given phone number
    private boolean deletePhoneNumber (String phone) {
        Uri uri = fromPhone(phone);
        return uri != null && context.getContentResolver()
                .delete(uri, null, null) > 0;
    }
    
    private Uri fromPhone (String phone) {
        Cursor mCursor = context.getContentResolver()
                .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                        ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?", new String[]{phone},
                        null);
        
        if (mCursor == null || mCursor.getCount() == 0) { return null; }
        mCursor.moveToNext();
        
        String name = mCursor.getString(mCursor
                .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        mCursor.close();
        
        mCursor = context.getContentResolver()
                .query(ContactsContract.Contacts.CONTENT_URI,
                        new String[]{ContactsContract.Contacts.LOOKUP_KEY,
                                ContactsContract.Contacts._ID,
                                ContactsContract.Contacts.DISPLAY_NAME},
                        ContactsContract.Contacts.DISPLAY_NAME + " = ?", new String[]{name},
                        null);
        
        if (mCursor == null || mCursor.getCount() == 0) { return null; }
        mCursor.moveToNext();
        
        String mCurrentLookupKey = mCursor
                .getString(mCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        long mCurrentId = mCursor.getLong(mCursor.getColumnIndex(ContactsContract.Contacts._ID));
        
        mCursor.close();
        
        return ContactsContract.Contacts.getLookupUri(mCurrentId, mCurrentLookupKey);
    }
}
