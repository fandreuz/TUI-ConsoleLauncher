package ohi.andre.consolelauncher.commands.raw;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiManager;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecInfo;
import ohi.andre.consolelauncher.tuils.Tuils;

public class bluetooth implements CommandAbstraction {

    @Override
    public String exec(ExecInfo info) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter.getState() == BluetoothAdapter.STATE_ON) {
            adapter.disable();
            return info.context.getString(R.string.output_bluetooth) + Tuils.SPACE + "false";
        } else {
            adapter.enable();
            return info.context.getString(R.string.output_bluetooth) + Tuils.SPACE + "true";
        }
    }

    @Override
    public int helpRes() {
        return R.string.help_bluetooth;
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
    public String onNotArgEnough(ExecInfo info, int nArgs) {
        return null;
    }

    @Override
    public int notFoundRes() {
        return 0;
    }

}
