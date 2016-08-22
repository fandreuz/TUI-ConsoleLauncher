package ohi.andre.consolelauncher.managers;

import android.app.Activity;
import android.os.IBinder;
import android.os.Looper;
import android.text.InputType;
import android.text.Layout;
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
import java.util.Map;

import ohi.andre.consolelauncher.tuils.SimpleMutableEntry;
import ohi.andre.consolelauncher.tuils.Tuils;
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

public class TerminalManager {

    private final CharSequence PREFIX = ">>";

    private final int SCROLL_DELAY = 200;

    public static final int INPUT = 10;
    public static final int OUTPUT = 11;

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

    public TerminalManager(TextView terminalView, EditText inputView, TextView prefixView, TextView submitView, SkinManager skinManager,
                           String hint, final boolean physicalEnter) {
        if (terminalView == null || inputView == null || prefixView == null || skinManager == null)
            throw new UnsupportedOperationException();

        this.mSkinManager = skinManager;

        prefixView.setTypeface(this.mSkinManager.getGlobalTypeface());
        prefixView.setTextColor(this.mSkinManager.getInputColor());
        prefixView.setTextSize(this.mSkinManager.getTextSize());
        prefixView.setText(PREFIX);

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

                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE
                        || (physicalEnter && actionId == EditorInfo.IME_ACTION_UNSPECIFIED)) {
                    onNewInput();
                    return true;
                } else
                    return false;
            }
        });
    }

    private void setupNewInput() {
        mInputView.setText(Tuils.EMPTYSTRING);
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
        if (output == null)
            return;

        writeToView(output, OUTPUT, id);
        scrollToEnd();
    }

    public void writeToView(String text, int type) {
        writeToView(text, type, mCurrentOutputId);
    }

    private void writeToView(final String text, final int type, int id) {
        if(type == INPUT || id == mCurrentOutputId) {
            if(!mTerminalView.getText().toString().endsWith(Tuils.NEWLINE)) {
                mTerminalView.append(Tuils.NEWLINE);
            }

            if(Looper.myLooper() == Looper.getMainLooper()) {
                mTerminalView.append(getSpannable(text, type));
            } else {
                ((Activity) mTerminalView.getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTerminalView.append(getSpannable(text, type));
                    }
                });
            }
        } else if(type == OUTPUT) {
            List<String> oldText = getLines(mTerminalView);
            List<Map.Entry<String, String>> wrappedOldText = splitInputOutput(oldText);

            if(wrappedOldText.size() > id) {
                SimpleMutableEntry selectedEntry = (SimpleMutableEntry) wrappedOldText.get(id);
                selectedEntry.setValue(getSpannable(text, type));

                final List<String> newText = toFlatList(wrappedOldText);

                ((Activity) mTerminalView.getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTerminalView.setText(Tuils.EMPTYSTRING);
                        for(CharSequence sequence : newText) {
                            sequence = Tuils.trimWhitespaces(sequence);
                            if(isInput(sequence)) {
                                mTerminalView.append(Tuils.NEWLINE);
                                mTerminalView.append(getSpannable(sequence.toString(), INPUT));
                                mTerminalView.append(Tuils.NEWLINE);
                            } else {
                                mTerminalView.append(getSpannable(sequence.toString(), OUTPUT));
                            }
                        }
                    }
                });
            }
        }
    }

    public void simulateEnter() {
        onNewInput();
    }

    public void setupScroller() {
        this.mTerminalView.setMovementMethod(new ScrollingMovementMethod());
    }

    private boolean isInput(CharSequence s) {
        return s.length() >= PREFIX.length() && s.subSequence(0, PREFIX.length()).toString().equals(PREFIX);
    }

    private List<String> toFlatList(List<Map.Entry<String, String>> list) {
        List<String> flatList = new ArrayList<>();

        for(Map.Entry<String, String> entry : list) {
            flatList.add(entry.getKey());
            flatList.add(entry.getValue());
        }

        return flatList;
    }

    private List<Map.Entry<String, String>> splitInputOutput(List<String> text) {
        List<Map.Entry<String, String>> list = new ArrayList<>();

        String input, output = null;
        for(int count = 0; count < text.size();) {
            if(isInput(text.get(count))) {
                input = text.get(count);
                int count2;
                for(count2 = count + 1; count2 < text.size() && !isInput(text.get(count2)); count2++) {
                    output = output == null ? text.get(count2) : output.concat(text.get(count2));
                }
                count += (count2 - count);
                list.add(new SimpleMutableEntry<>(input, output));
            }

            output = null;
        }

        return list;
    }

    private SpannableString getSpannable(String text, int type) {
        SpannableString spannableString = new SpannableString(text);
        int color;
        if(type == INPUT) {
            color = mSkinManager.getInputColor();
        } else if(type == OUTPUT) {
            color = mSkinManager.getOutputColor();
        } else {
            return null;
        }
        spannableString.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private List<String> getLines(TextView view) {
        List<String> lines = new ArrayList<>();
        Layout layout = view.getLayout();

        if (layout != null) {
            final int lineCount = layout.getLineCount();
            final CharSequence text = layout.getText();

            for (int i = 0, startIndex = 0; i < lineCount; i++) {
                final int endIndex = layout.getLineEnd(i);
                CharSequence sequence = text.subSequence(startIndex, endIndex);
                if(sequence.length() > 0 && !sequence.toString().equals(Tuils.NEWLINE)) {
                    lines.add(sequence.toString());
                }
                startIndex = endIndex;
            }
        }
        return lines;
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
        mScrollView.postDelayed(mScrollRunnable, SCROLL_DELAY);
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
        ((Activity) mTerminalView.getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTerminalView.setText(Tuils.EMPTYSTRING);
                mInputView.setText(Tuils.EMPTYSTRING);
                mCurrentOutputId = 0;
            }
        });
    }

}
