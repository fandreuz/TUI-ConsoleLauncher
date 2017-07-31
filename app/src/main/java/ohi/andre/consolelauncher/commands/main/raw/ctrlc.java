package ohi.andre.consolelauncher.commands.main.raw;

import android.os.Environment;

import ohi.andre.consolelauncher.MainManager;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.libsuperuser.Shell;
import ohi.andre.consolelauncher.tuils.libsuperuser.StreamGobbler;

/**
 * Created by francescoandreuzzi on 26/07/2017.
 */

public class ctrlc implements CommandAbstraction {

    @Override
    public String exec(final ExecutePack pack) throws Exception {
        new Thread() {
            @Override
            public void run() {
                super.run();

                MainManager.interactive.kill();
                MainManager.interactive.close();
                MainManager.interactive = null;

                MainManager.interactive = new Shell.Builder()
                        .setOnSTDOUTLineListener(new StreamGobbler.OnLineListener() {
                            @Override
                            public void onLine(String line) {
                                ((MainPack) pack).outputable.onOutput(line);
                            }
                        })
                        .setOnSTDERRLineListener(new StreamGobbler.OnLineListener() {
                            @Override
                            public void onLine(String line) {
                                ((MainPack) pack).outputable.onOutput(line);
                            }
                        })
                        .open();

                MainManager.interactive.addCommand("cd " + Environment.getExternalStorageDirectory().getAbsolutePath());
            }
        }.start();

        ((MainPack) pack).rooter.onStandard();

        return null;
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
        return new int[0];
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_ctrlc;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return null;
    }
}
