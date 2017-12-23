package ohi.andre.consolelauncher.tuils;

import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * Created by francescoandreuzzi on 17/11/2017.
 */

public class LongClickMovementMethod extends LinkMovementMethod {

//    private Long lastClickTime = 0l;
//    private int lastX = 0;
//    private int lastY = 0;

    private int longClickDuration, lastLine = -1;

    private abstract class WasActivatedRunnable implements Runnable {

        public boolean wasActivated = false;

        @Override
        public void run() {
            wasActivated = true;
        }
    };

    private WasActivatedRunnable runnable;

    @Override
    public boolean onTouchEvent(final TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();
//        Tuils.log("action", action);

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_CANCEL) {
            int x = (int) event.getX();
            int y = (int) event.getY();
//            lastX = x;
//            lastY = y;
//            int deltaX = Math.abs(x-lastX);
//            int deltaY = Math.abs(y-lastY);

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            final int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            final LongClickableSpan[] link = buffer.getSpans(off, off, LongClickableSpan.class);

//            Tuils.log("lastline", lastLine);
//            Tuils.log("line", line);
            if (action == MotionEvent.ACTION_UP) {
//                Tuils.log("action up");

//                    if (System.currentTimeMillis() - lastClickTime < longClickDuration) {
//                        link[0].onClick(widget);
//                    }
//                    else if (deltaX < 10 && deltaY < 10) {
//                        link[0].onLongClick(widget);
//                    }

                if(runnable != null) {
//                        long click, do nothing
                    if(runnable.wasActivated) {}
//                        single click
                    else {
                        widget.removeCallbacks(runnable);
                        if(link.length > 0) link[0].onClick(widget);
                    }

                    runnable = null;
                }

            } else if (action == MotionEvent.ACTION_DOWN) {

//                Tuils.log("action down");

//                    Selection.setSelection(buffer,
//                            buffer.getSpanStart(link[0]),
//                            buffer.getSpanEnd(link[0]));

//                    lastClickTime = System.currentTimeMillis();

                if(link.length > 0) {
                    final LongClickableSpan span = link[0];
                    runnable = new WasActivatedRunnable() {

                        @Override
                        public void run() {
                            super.run();
                            span.onLongClick(widget);
                        }
                    };
                }

                widget.postDelayed(runnable, longClickDuration);
            } else {
//                Tuils.log("action move or cancel");

//                action_move
                if(line != lastLine) {
//                    Tuils.log("line != last line");
                    widget.removeCallbacks(runnable);
                }
            }

            lastLine = line;
//            Tuils.log("updated kast line", lastLine);
//            Tuils.log("#####");

            return true;
        }

        return super.onTouchEvent(widget, buffer, event);
    }

    private static LongClickMovementMethod sInstance;
    public static MovementMethod getInstance(int longClickDuration) {
        if (sInstance == null) {
            sInstance = new LongClickMovementMethod();
            sInstance.longClickDuration = longClickDuration;
        }

        return sInstance;
    }

    public static MovementMethod getInstance() {
        return getInstance(-1);
    }
}