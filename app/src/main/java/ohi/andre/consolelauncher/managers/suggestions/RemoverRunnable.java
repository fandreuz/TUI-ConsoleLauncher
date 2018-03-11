package ohi.andre.consolelauncher.managers.suggestions;

import android.widget.LinearLayout;

import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 11/03/2018.
 */

public class RemoverRunnable implements Runnable {

    public boolean stop = false, isGoingToRun = false;

    public LinearLayout suggestionsView;

    public RemoverRunnable(LinearLayout suggestionsView) {
        this.suggestionsView = suggestionsView;
    }

    @Override
    public void run() {
        Tuils.log("run");

        if(stop) {
            stop = false;
        } else suggestionsView.removeAllViews();

        isGoingToRun = false;

        Tuils.log("now childs", suggestionsView.getChildCount());
    }
}
