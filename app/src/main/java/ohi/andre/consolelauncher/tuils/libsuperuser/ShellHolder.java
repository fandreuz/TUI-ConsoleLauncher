package ohi.andre.consolelauncher.tuils.libsuperuser;

import android.os.Environment;

import ohi.andre.consolelauncher.tuils.interfaces.Outputable;

/**
 * Created by francescoandreuzzi on 18/08/2017.
 */

public class ShellHolder {

    private Outputable outputable;

    public ShellHolder(Outputable outputable) {
        this.outputable = outputable;
    }

    public Shell.Interactive build() {
        Shell.Interactive interactive = new Shell.Builder()
                .setOnSTDOUTLineListener(new StreamGobbler.OnLineListener() {
                    @Override
                    public void onLine(String line) {
                        outputable.onOutput(line);
                    }
                })
                .setOnSTDERRLineListener(new StreamGobbler.OnLineListener() {
                    @Override
                    public void onLine(String line) {
                        outputable.onOutput(line);
                    }
                })
                .open();
        interactive.addCommand("cd " + Environment.getExternalStorageDirectory().getAbsolutePath());
        return interactive;
    }
}
