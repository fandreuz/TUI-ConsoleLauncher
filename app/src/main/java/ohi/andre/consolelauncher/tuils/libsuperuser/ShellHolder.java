package ohi.andre.consolelauncher.tuils.libsuperuser;

import android.content.Context;

import java.io.File;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 18/08/2017.
 */

public class ShellHolder {

    private Context context;

    public ShellHolder(Context context) {
        this.context = context;
    }

    public Shell.Interactive build() {
        Shell.Interactive interactive = new Shell.Builder()
                .setOnSTDOUTLineListener(new StreamGobbler.OnLineListener() {
                    @Override
                    public void onLine(String line) {
                        Tuils.sendOutput(context, line);
                    }
                })
                .setOnSTDERRLineListener(new StreamGobbler.OnLineListener() {
                    @Override
                    public void onLine(String line) {
                        Tuils.sendOutput(context, line);
                    }
                })
                .open();
        interactive.addCommand("cd " + XMLPrefsManager.get(File.class, Behavior.home_path));
        return interactive;
    }
}
