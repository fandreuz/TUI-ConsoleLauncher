package ohi.andre.consolelauncher.commands.main;

import ohi.andre.consolelauncher.commands.ExecutePack;

/**
 * Created by francescoandreuzzi on 10/06/2017.
 */

public interface Param {

    int[] args();
    String exec(ExecutePack pack);
    String label();
}
