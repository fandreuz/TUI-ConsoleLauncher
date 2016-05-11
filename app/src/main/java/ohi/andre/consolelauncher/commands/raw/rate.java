package ohi.andre.consolelauncher.commands.raw;

import android.content.Intent;
import android.net.Uri;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;

public class rate implements CommandAbstraction {

    @Override
    public String exec(final ExecInfo info) {
        new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    sleep(300);
                } catch (InterruptedException e) {
                }

                try {
                    info.context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" +
                            info.context.getPackageName())));
                } catch (android.content.ActivityNotFoundException anfe) {
                    info.context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" +
                            info.context.getPackageName())));
                }
            }
        }.start();


        return info.res.getString(R.string.output_rate);
    }

    @Override
    public int helpRes() {
        return R.string.help_rate;
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
        return 3;
    }

    @Override
    public String[] parameters() {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return null;
    }

    @Override
    public int notFoundRes() {
        return 0;
    }

}
