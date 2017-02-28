package ohi.andre.consolelauncher.tuils;

import android.app.Application;
import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.Command;

/**
 * Created by francescoandreuzzi on 17/02/2017.
 */

public class Animator {

    private static final int[] PATTERN = {20,60,30,80};
    private static final int FADE_DURATION = 300;

    private static final int USEFAST_LIMIT = 30;
    private static final int FAST_DURATION = 10;

    private static void animate(final TextView textView, final String text, final Animator chained) {

        Runnable runnable = new Runnable() {

            final Animator a = chained;
            int index = 0;
            int patternIndex = 0;
            char[] array = text.toCharArray();

            @Override
            public void run() {
                if(patternIndex == PATTERN.length) {
                    patternIndex = 0;
                }

                if(index < array.length) {
                    textView.append(array[index++] + "");
                    textView.postDelayed(this, array.length >= USEFAST_LIMIT ? FAST_DURATION : PATTERN[patternIndex++]);
                } else {
                    if(a != null) a.animate();
                }
            }
        };

        runnable.run();
    }

    private static void animate(View view, final Animator chained) {
        AlphaAnimation animation = new AlphaAnimation(0f, 1f);
        animation.setDuration(FADE_DURATION);
        animation.setFillAfter(true);
        if (chained != null) {
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    chained.animate();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
        view.startAnimation(animation);
    }

//    class

    View view;
    Object param;
    Animator chained;

    public Animator(TextView textView, String text) {
        view = textView;
        param = text;
    }

    public Animator(View view) {
        this.view = view;

        AlphaAnimation hide = new AlphaAnimation(1f, 0f);
        hide.setFillAfter(true);
        hide.setDuration(0);
        view.startAnimation(hide);
    }

    public Animator setChained(Animator a) {
        chained = a;
        return this;
    }

    public void animate() {
        if(view instanceof TextView && param instanceof String) {
            Animator.animate((TextView) view, (String) param, chained);
        } else {
            Animator.animate(view, chained);
        }
    }
}
