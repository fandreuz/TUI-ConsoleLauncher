package ohi.andre.consolelauncher.commands.main.raw;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.telephony.SmsManager;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.RedirectCommand;

/**
 * Created by francescoandreuzzi on 02/03/2017.
 */

public class sms extends RedirectCommand {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        MainPack info = (MainPack) pack;
        if (ContextCompat.checkSelfPermission(info.context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions((Activity) info.context, new String[]{Manifest.permission.READ_CONTACTS}, LauncherActivity.COMMAND_REQUEST_PERMISSION);
            return info.context.getString(R.string.output_waitingpermission);
        }

        beforeObjects.add(pack.getString());

        if(afterObjects.size() == 0) {
            info.redirectator.prepareRedirection(this);
        } else {
            return onRedirect(info);
        }

        return null;
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.CONTACTNUMBER};
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_sms;
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

        if (ContextCompat.checkSelfPermission(info.context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) info.context, new String[]{Manifest.permission.SEND_SMS}, LauncherActivity.COMMAND_REQUEST_PERMISSION);
            return info.context.getString(R.string.output_waitingpermission);
        }

        info.redirectator.cleanup();

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, message, null, null);

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