package ohi.andre.consolelauncher.commands.main.raw;

import java.util.ArrayList;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by lanmonster on 04/06/2017.
 */

public class todo implements CommandAbstraction {

    private final String ADD_ITEM_PARAM = "-a";
    private final String REMOVE_ITEM_PARAM = "-r";

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;
        String param = info.get(String.class, 0);
        String item = info.get(String.class, 1);
        if (ADD_ITEM_PARAM.equals(param)) {
            //ADD
            Tuils.getTodoList().add(item.trim());
            return info.res.getString(R.string.add_success);
        } else if (REMOVE_ITEM_PARAM.equals(param)) {
            //REMOVE
            try {
                int index = Integer.parseInt(item.trim());
                if (index < Tuils.getTodoList().size()) {
                    Tuils.getTodoList().remove(index);
                    return info.res.getString(R.string.remove_success);
                } else {
                    return info.res.getString(R.string.index_out_of_bounds);
                }
            } catch (NumberFormatException e) {
                return info.res.getString(R.string.remove_example);
            }
        } else {
            return info.res.getString(R.string.todo_no_such_param);
        }
    }

    @Override
    public int minArgs() {
        return 2;
    }

    @Override
    public int maxArgs() {
        return 2;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.PARAM, CommandAbstraction.PLAIN_TEXT};
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String[] parameters() {
        return new String[] {
                ADD_ITEM_PARAM,
                REMOVE_ITEM_PARAM
        };
    }

    @Override
    public int helpRes() {
        return R.string.help_todo;
    }

    @Override
    public String onArgNotFound(ExecutePack info) {
        MainPack pack = (MainPack) info;
        return pack.res.getString(R.string.todo_no_such_param);
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        MainPack pack = (MainPack) info;
        if (nArgs > 0) {
            if (ADD_ITEM_PARAM.equals(pack.get(String.class, 0))) {
                return pack.res.getString(R.string.add_example);
            } else if (REMOVE_ITEM_PARAM.equals(pack.get(String.class, 0))) {
                return pack.res.getString(R.string.remove_example);
            }
        } else {
            String todo = printTodoList();
            if (todo.equals(Tuils.EMPTYSTRING)) {
                return pack.res.getString(R.string.empty_todo);
            } else {
                return todo;
            }
        }
        return null;
    }

    public String printTodoList() {
        ArrayList<String> list = (ArrayList) Tuils.getTodoList();
        int index = 0;
        String todo = Tuils.EMPTYSTRING;
        for (String s : list) {
            todo += index++ + ": " + s + "\n";
        }
        return todo;
    }
}
