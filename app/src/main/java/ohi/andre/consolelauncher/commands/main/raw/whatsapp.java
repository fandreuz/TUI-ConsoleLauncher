package ohi.andre.consolelauncher.commands.main.raw;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.specific.RedirectCommand;

/**
 *
 * SNIPPET GET WHATSAPP NUMBERS
 Cursor c = context.getContentResolver().query(
 ContactsContract.RawContacts.CONTENT_URI,
 new String[]{ContactsContract.RawContacts.SYNC1,ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY},
 ContactsContract.RawContacts.ACCOUNT_TYPE + "= ?",
 new String[] { "com.whatsapp" },
 null);

 int nameColumn = c.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY);
 int numColumn = c.getColumnIndex(ContactsContract.RawContacts.SYNC1);
 while (c.moveToNext())
 {
 // You can also read RawContacts.CONTACT_ID to read the
 // ContactsContract.Contacts table or any of the other related ones.
 String name = c.getString(nameColumn);
 String num = c.getString(numColumn);
 List<String> nums = new ArrayList<>();
 nums.add(num);
 contacts.add(new Contact("WhatsApp:"+name, nums, 0));
 }

 */

public class whatsapp extends RedirectCommand {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        MainPack info = (MainPack) pack;
        if (ContextCompat.checkSelfPermission(info.context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions((Activity) info.context, new String[]{Manifest.permission.READ_CONTACTS}, LauncherActivity.COMMAND_REQUEST_PERMISSION);
            return info.context.getString(R.string.output_waitingpermission);
        }

        beforeObjects.add(pack.get(String.class, 0));

        if(afterObjects.size() == 0) {
            info.redirectator.prepareRedirection(this);
        } else {
            return onRedirect(info);
        }

        return null;
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public int maxArgs() {
        return 1;
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.WHATSAPPNUMBER};
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_whatsapp;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return info.context.getString(helpRes());
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        MainPack info = (MainPack) pack;
        return info.res.getString(R.string.output_numbernotfound);
    }

    @Override
    public String onRedirect(ExecutePack pack) {
        MainPack info = (MainPack) pack;

        String number = (String) beforeObjects.get(0);
        String message = (String) afterObjects.get(0);
        if(message.length() == 0) {
            info.redirectator.cleanup();
            return info.res.getString(R.string.output_smsnotsent);
        }

        info.redirectator.cleanup();

        try {
            Uri uri = Uri.parse("whatsapp://send?text="+ message +"&phone="+number);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            pack.context.startActivity(intent);
            cleanup();

            return info.res.getString(R.string.output_smssent);
        } catch (Exception ex) {
            cleanup();

            return ex.toString();
        }
    }

    @Override
    public int getHint() {
        return R.string.sms_hint;
    }

    @Override
    public boolean isWaitingPermission() {
        return beforeObjects.size() == 1 && afterObjects.size() == 1;
    }
}
