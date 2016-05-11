package ohi.andre.consolelauncher.tuils.stuff;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class PolicyReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
    }

}   