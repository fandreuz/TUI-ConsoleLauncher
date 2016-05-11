package ohi.andre.consolelauncher.commands.raw;

import android.content.Intent;
import android.net.Uri;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

public class remove implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        info.policy.removeActiveAdmin(info.component);

        Uri packageURI = Uri.parse("package:" + "ohi.andre.consolelauncher");
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
        info.context.startActivity(uninstallIntent);

        return info.res.getString(R.string.output_removetui);
    }

    @Override
    public int minArgs() {
        return 0;
    }

    @Override
    public int maxArgs() {
        return 0;
    }

    @Override
    public int[] argType() {
        return null;
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String[] parameters() {
        return null;
    }

    @Override
    public int helpRes() {
        return R.string.help_remove;
    }

    @Override
    public int notFoundRes() {
        return 0;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return null;
    }

}
