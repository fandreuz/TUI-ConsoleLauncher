package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Context;
import android.os.Vibrator;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;

/**
 * Created by francescoandreuzzi on 29/04/2017.
 */

public class vibrate implements CommandAbstraction {


    @Override
    public String exec(ExecutePack pack) throws Exception {
        String text = (String) pack.args[0];

        Context context = ((MainPack) pack).context;

        int ms;
        try {
            ms = Integer.parseInt(text);

            ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(ms);
        } catch (NumberFormatException e) {
            return context.getString(R.string.output_numberformat);
        } catch (Exception e) {
            return e.toString();
        }

        return null;
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.PLAIN_TEXT};
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public int helpRes() {
        return R.string.help_vibrate;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return ((MainPack) pack).context.getString(helpRes());
    }
}
