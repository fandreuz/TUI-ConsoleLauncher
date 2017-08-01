package ohi.andre.consolelauncher.commands.specific;

import ohi.andre.consolelauncher.commands.CommandAbstraction;

/**
 * Created by francescoandreuzzi on 01/08/2017.
 */

public abstract class APICommand implements CommandAbstraction {

    public abstract boolean willWorkOn(int api);
}
