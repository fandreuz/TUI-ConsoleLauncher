package ohi.andre.consolelauncher.commands.raw;

import android.content.Intent;
import android.net.Uri;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

public class call implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        String number = info.get(String.class, 0);
        Uri uri = Uri.parse("tel:" + number);
        Intent intent = new Intent(Intent.ACTION_CALL, uri);

        try {
            info.context.startActivity(intent);
        } catch (SecurityException e) {
            return info.res.getString(R.string.permission_error);
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
        return info.res.getString(helpRes());
    }

    @Override
    public int notFoundRes() {
        return R.string.output_numbernotfound;
    }

}
