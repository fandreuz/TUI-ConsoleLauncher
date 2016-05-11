package ohi.andre.consolelauncher.commands.raw;

import android.content.Intent;
import android.net.Uri;

import java.io.File;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

public class share implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        File f = info.get(File.class, 0);
        if (f.isDirectory())
            return info.res.getString(R.string.output_isdirectory);

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        Uri uri = Uri.fromFile(f);
        sharingIntent.setType("*/*");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        info.context.startActivity(Intent.createChooser(sharingIntent,
                info.res.getString(R.string.share_label)));

        return info.res.getString(R.string.sharing) + " " + f.getName();
    }

    @Override
    public int helpRes() {
        return R.string.help_share;
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public int maxArgs() {
        return CommandAbstraction.UNDEFINIED;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.FILE};
    }

    @Override
    public int priority() {
        return 3;
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
        return R.string.output_filenotfound;
    }

}
