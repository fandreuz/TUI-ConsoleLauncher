package ohi.andre.consolelauncher.commands.main.raw;

import android.bluetooth.BluetoothAdapter;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.Tuils;

public class bluetooth implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;
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
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return null;
    }

}
