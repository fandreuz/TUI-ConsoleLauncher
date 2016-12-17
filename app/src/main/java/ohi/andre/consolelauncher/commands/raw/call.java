package ohi.andre.consolelauncher.commands.raw;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.List;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.tuils.Tuils;

public class call implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        if (ContextCompat.checkSelfPermission(info.context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions((Activity) info.context, new String[]{Manifest.permission.READ_CONTACTS}, LauncherActivity.COMMAND_REQUEST_PERMISSION);
            return info.context.getString(R.string.output_waitingpermission);
        }

        if (ContextCompat.checkSelfPermission(info.context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions((Activity) info.context, new String[]{Manifest.permission.CALL_PHONE}, LauncherActivity.COMMAND_REQUEST_PERMISSION);
            return info.context.getString(R.string.output_waitingpermission);
        }

        String number = info.get(String.class, 0);
        Uri uri = Uri.parse("tel:" + number);
        Intent intent = new Intent(Intent.ACTION_CALL, uri);

        try {
            info.context.startActivity(intent);
        } catch (SecurityException e) {
            return info.res.getString(R.string.output_nopermissions);
        }

        return info.res.getString(R.string.calling) + " " + number;
    }

    @Override
    public int helpRes() {
        return R.string.help_call;
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
        return new int[]{CommandAbstraction.CONTACTNUMBER};
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public String[] parameters() {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        if (ContextCompat.checkSelfPermission(info.context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) info.context, new String[]{Manifest.permission.READ_CONTACTS}, LauncherActivity.COMMAND_REQUEST_PERMISSION);
            return info.context.getString(R.string.output_waitingpermission);
        }

        List<String> contacts = info.contacts.listNamesAndNumbers();
        Tuils.addPrefix(contacts, Tuils.DOUBLE_SPACE);
        Tuils.insertHeaders(contacts, false);
        return Tuils.toPlanString(contacts);
    }

    @Override
    public int notFoundRes() {
        return R.string.output_numbernotfound;
    }

}
