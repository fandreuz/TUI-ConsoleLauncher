package ohi.andre.consolelauncher.managers;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 31/08/2017.
 */

public class MessagesManager {

    final String MARKER = "---------------";

    final int REACH_THIS = 20;

    List<Message> original;
    List<Message> copy;

    int count;
    Random random;

    Context context;
    int color;

    public MessagesManager(Context context, Message... ms) {
        this.context = context;

        original = new ArrayList<>();
        for(Message m : ms) original.add(m);
        copy = new ArrayList<>(original);

        count = 0;
        random = new Random();

        color = XMLPrefsManager.getColor(XMLPrefsManager.Theme.hint_color);
    }

    public void onCmd() {
        count++;

        if(count == REACH_THIS) {
            count = 0;

            if(copy.size() == 0) {
                copy = new ArrayList<>(original);
                random = new Random();
            }

            int index = random.nextInt(copy.size());
            if(copy.size() <= index) {
                return;
            }

            Tuils.sendOutput(color, context, MARKER + Tuils.NEWLINE + copy.remove(index).msg + Tuils.NEWLINE + MARKER);
        }
    }

    public static class Message {
        String msg;
//        more coming

        public Message(String msg) {
            this.msg = msg;
        }
    }
}
