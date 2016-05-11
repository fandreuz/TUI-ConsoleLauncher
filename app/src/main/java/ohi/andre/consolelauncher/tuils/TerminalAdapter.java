package ohi.andre.consolelauncher.tuils;

import android.app.Activity;
import android.os.IBinder;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ohi.andre.consolelauncher.managers.SkinManager;
import ohi.andre.consolelauncher.tuils.interfaces.OnNewInputListener;

/*Copyright Francesco Andreuzzi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

public class TerminalAdapter {

    private final CharSequence PREFIX = ">>";
    private final CharSequence NEWLINE = "\n";
    private final int INPUT = 10;
    private final int OUTPUT = 11;

    private int mCurrentOutputId = 0;

    private ScrollView mScrollView;
    private TextView mTerminalView;
    private EditText mInputView;
    private Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {
            boolean inputHadFocus = mInputView.hasFocus();
            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            if (inputHadFocus)
                requestInputFocus();
        }
    };
    private SkinManager mSkinManager;

    private OnNewInputListener mInputListener;

    public TerminalAdapter(TextView terminalView, EditText inputView, TextView prefixView, TextView submitView, SkinManager skinManager, String hint) {
        if (terminalView == null || inputView == null || prefixView == null || skinManager == null)
            throw new UnsupportedOperationException();

        this.mSkinManager = skinManager;

        prefixView.setTypeface(this.mSkinManager.getGlobalTypeface());
        prefixView.setTextColor(this.mSkinManager.getInputColor());
        prefixView.setTextSize(this.mSkinManager.getTextSize());
        prefixView.setText(">>");

        if (submitView != null) {
            submitView.setTextColor(this.mSkinManager.getInputColor());
            submitView.setTextSize(this.mSkinManager.getTextSize());
            submitView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNewInput();
                }
            });
        }

        this.mTerminalView = terminalView;
        this.mTerminalView.setTypeface(mSkinManager.getGlobalTypeface());
        this.mTerminalView.setTextSize(mSkinManager.getTextSize());
        setupScroller();

        this.mScrollView = (ScrollView) this.mTerminalView.getParent();

        this.mInputView = inputView;
        this.mInputView.setTextSize(mSkinManager.getTextSize());
        this.mInputView.setTextColor(mSkinManager.getInputColor());
        this.mInputView.setTypeface(mSkinManager.getGlobalTypeface());
        if (hint != null)
            this.mInputView.setHint(hint);
        this.mInputView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        this.mInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                    onNewInput();
                    return true;
                } else
                    return false;
            }
        });
    }

    private void setupNewInput() {
        mInputView.setText("");
        mCurrentOutputId++;
        requestInputFocus();
    }

    private boolean onNewInput() {
        if (mInputView == null)
            return false;

        String input = mInputView.getText().toString();
        if (input.length() == 0) {
            return false;
        }
        writeToView(PREFIX + input, INPUT);

        if (mInputListener != null) {
            mInputListener.onNewInput(input);
        }

        setupNewInput();
        return true;
    }

    public void setOutput(String output, int id) {
        if (output == null || output.length() == 0)
            return;

        writeToView(output, OUTPUT, id);
        scrollToEnd();
    }

    private void writeToView(String text, int type) {
        writeToView(text, type, mCurrentOutputId);
    }

    private void writeToView(String text, int type, int id) {
//        Log.e("andre", "---------------------");
//        Log.e("andre", "to write: " + text);
        text = text.concat(NEWLINE.toString());

        Spannable toWriteSpannable = new SpannableString(text);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(type == INPUT ? mSkinManager.getInputColor() : mSkinManager.getOutputColor());
        toWriteSpannable.setSpan(colorSpan, 0, toWriteSpannable.length(), 0);

        if (id == mCurrentOutputId) {
//            Log.e("andre", String.valueOf(System.currentTimeMillis()));
//            Log.e("andre", Arrays.toString(Thread.currentThread().getStackTrace()));
//            Log.e("andre", "==");
//            Log.e("andre", toWriteSpannable.toString());
            mTerminalView.append(toWriteSpannable);
        } else {
//            Log.e("andre", String.valueOf(System.currentTimeMillis()));
            CharSequence[] mCurrentText = Tuils.split(mTerminalView.getText(), NEWLINE, -1);
            final List<CharSequence> mNewText = new ArrayList<>();

            int count = 0;
            boolean check = false;
            List<CharSequence> output = null;
//            Log.e("andre", String.valueOf(mCurrentText.length));
//            Log.e("andre", Arrays.toString(mCurrentText));
            while (count < mCurrentText.length) {
//                Log.e("andre", String.valueOf(count) + ", " + mCurrentText[count]);

                if (isInput(mCurrentText[count])) {
                    id--;

                    if (output != null && output.size() > 0) {
                        for (CharSequence sequence : output) {
                            ForegroundColorSpan outputColorSpan = new ForegroundColorSpan(mSkinManager.getOutputColor());
                            Spannable spannable = new SpannableString(sequence.toString());
                            spannable.setSpan(outputColorSpan, 0, spannable.length(), 0);
                            mNewText.add(spannable);
                        }
//                        Log.e("andre", "output");
//                        Log.e("andre", output.toString());
                    }

                    if (output == null)
                        output = new ArrayList<>();
                    else
                        output.clear();

                    SpannableString inputSpannable = new SpannableString(mCurrentText[count] + NEWLINE.toString());
                    ForegroundColorSpan inputColorSpan = new ForegroundColorSpan(mSkinManager.getInputColor());
                    inputSpannable.setSpan(inputColorSpan, 0, inputSpannable.length(), 0);
                    mNewText.add(inputSpannable);
//                    Log.e("andre", "input");
//                    Log.e("andre", inputSpannable.toString());

                } else {
                    output.add(mCurrentText[count]);
                }

//                Log.e("andre", "id: " + id);
                if (id == -1) {
                    mNewText.add(toWriteSpannable);
//                    Log.e("andre", "towrite");
//                    Log.e("andre", toWriteSpannable.toString());
                    check = true;
                }

                count++;
            }

            if (!check)
                mNewText.add(toWriteSpannable);

            ((Activity) mTerminalView.getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTerminalView.setText(Tuils.toPlanSequence(mNewText, NEWLINE));
                }
            });
        }
    }

    public void setupScroller() {
        this.mTerminalView.setMovementMethod(new ScrollingMovementMethod());
    }

    private boolean isInput(CharSequence s) {
        return s.length() >= PREFIX.length() && s.subSequence(0, PREFIX.length()).toString().equals(PREFIX);
    }

    public String getInput() {
        return mInputView.getText().toString();
    }

    public void setInput(String input) {
        mInputView.setText(input);
    }

    public void setInputListener(OnNewInputListener listener) {
        this.mInputListener = listener;
    }

    public void focusInputEnd() {
        mInputView.setSelection(getInput().length());
    }

    public void scrollToEnd() {
        mScrollView.post(mScrollRunnable);
    }

    public void requestInputFocus() {
        mInputView.requestFocus();
    }

    public IBinder getInputWindowToken() {
        return mInputView.getWindowToken();
    }

    public View getInputView() {
        return mInputView;
    }

    public int getCurrentOutputId() {
        return mCurrentOutputId;
    }

    public void clear() {
        mTerminalView.setText("");
        mInputView.setText("");
        mCurrentOutputId = -1;
    }

}
