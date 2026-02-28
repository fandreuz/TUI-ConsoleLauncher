package ohi.andre.consolelauncher.tuils;

import android.content.Context;
import android.graphics.Canvas;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

public class OutlineTextView extends androidx.appcompat.widget.AppCompatTextView {

    public static String SHADOW_TAG = "hasShadow";

    public static int redrawTimes = 1;

    private int drawTimes = -1;

    public OutlineTextView(Context context) {
        super(context);
    }

    public OutlineTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OutlineTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void draw(Canvas canvas) {
        if(drawTimes == -1) {
            drawTimes = getTag() == null ? 1 : redrawTimes;
        }

        for(int c = 0; c < drawTimes; c++) super.draw(canvas);
    }
}
