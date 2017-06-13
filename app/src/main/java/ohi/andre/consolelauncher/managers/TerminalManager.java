package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.graphics.Typeface;
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
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ohi.andre.consolelauncher.UIManager;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.raw.clear;
import ohi.andre.consolelauncher.tuils.Tuils;

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

    private final int SCROLL_DELAY = 200;
    private final int CMD_LIST_SIZE = 40;

    public static final int INPUT = 10;
    public static final int OUTPUT = 11;

    private int cmds = 0;

    private long lastEnter;

    private CharSequence prefix;

    private ScrollView mScrollView;
    private TextView mTerminalView;
    private EditText mInputView;

    private List<String> cmdList = new ArrayList<>(CMD_LIST_SIZE);
    private int howBack = -1;

    private Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {
            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            mInputView.requestFocus();
        }
    };

    private SkinManager mSkinManager;

    private UIManager.OnNewInputListener mInputListener;
    private UIManager.SuggestionNavigator mSuggestionNavigator;

    private List<Messager> messagers = new ArrayList<>();

    private MainPack mainPack;

    private boolean defaultHint = true;

    public TerminalManager(TextView terminalView, EditText inputView, TextView prefixView, ImageButton submitView, final ImageButton backView, ImageButton nextView, ImageButton deleteView,
                           ImageButton pasteView, SkinManager skinManager, final Context context, MainPack mainPack) {
        if (terminalView == null || inputView == null || prefixView == null || skinManager == null)
            throw new UnsupportedOperationException();

        final Typeface lucidaConsole = Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");

        this.mSkinManager = skinManager;
        this.mainPack = mainPack;

        if(skinManager.linuxAppearence) {
            prefix = "$ ";
        } else {
            prefix = ">> ";
        }
        prefixView.setTypeface(skinManager.systemFont ? Typeface.DEFAULT : lucidaConsole);
        prefixView.setTextColor(this.mSkinManager.inputColor);
        prefixView.setTextSize(this.mSkinManager.getTextSize());
        prefixView.setText(prefix);

        if (submitView != null) {
            submitView.setColorFilter(mSkinManager.inputColor);
            submitView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNewInput();
                }
            });
        }

        if (backView != null) {
            backView.setColorFilter(this.mSkinManager.inputColor);
            backView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        if (nextView != null) {
            nextView.setColorFilter(this.mSkinManager.inputColor);
            nextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNextPressed();
                }
            });
        }

        if (pasteView != null) {
            pasteView.setColorFilter(this.mSkinManager.inputColor);
            pasteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String text = Tuils.getTextFromClipboard(context);
                    if(text != null && text.length() > 0) {
                        setInput(text);
                    }
                }
            });
        }

        if (deleteView != null) {
            deleteView.setColorFilter(this.mSkinManager.inputColor);
            deleteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setInput(Tuils.EMPTYSTRING);
                }
            });
        }

        this.mTerminalView = terminalView;
        this.mTerminalView.setTypeface(skinManager.systemFont ? Typeface.DEFAULT : lucidaConsole);
        this.mTerminalView.setTextSize(mSkinManager.getTextSize());
        this.mTerminalView.setFocusable(false);
        setupScroller();

        View v = mTerminalView;
        do {
            v = (View) v.getParent();
        } while (!(v instanceof ScrollView));
        this.mScrollView = (ScrollView) v;

        this.mInputView = inputView;
        this.mInputView.setTextSize(mSkinManager.getTextSize());
        this.mInputView.setTextColor(mSkinManager.inputColor);
        this.mInputView.setTypeface(skinManager.systemFont ? Typeface.DEFAULT : lucidaConsole);
        this.mInputView.setHint(Tuils.getHint(skinManager, mainPack.currentDirectory.getAbsolutePath()));
        this.mInputView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        this.mInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(!mInputView.hasFocus()) mInputView.requestFocus();

//                physical enter
                if(actionId == KeyEvent.ACTION_DOWN) {
                    if(lastEnter == 0) {
                        lastEnter = System.currentTimeMillis();
                    } else {
                        long difference = System.currentTimeMillis() - lastEnter;
                        lastEnter = System.currentTimeMillis();
                        if(difference < 350) {
                            return true;
                        }
                    }
                }

                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE || actionId == KeyEvent.ACTION_DOWN) {
                    onNewInput();
                }

//                if(event == null && actionId == EditorInfo.IME_NULL) onNewInput();
//                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) onNewInput();

                return true;
            }
        });
    }

    public void addMessager(Messager messager) {
        messagers.add(messager);
    }

    private void setupNewInput() {
        mInputView.setText(Tuils.EMPTYSTRING);

        if(defaultHint && mSkinManager.showPath) {
            mInputView.setHint(Tuils.getHint(mSkinManager, mainPack.currentDirectory.getAbsolutePath()));
        }

        requestInputFocus();
    }

    private boolean onNewInput() {
        if (mInputView == null) {
            return false;
        }

        String input = mInputView.getText().toString().trim();

        if(input.length() > 0) {
            writeToView((input.startsWith("su ") ? "# " : prefix) + input, INPUT);

            cmds++;
            if(cmdList.size() == CMD_LIST_SIZE) {
                cmdList.remove(0);
            }
            cmdList.add(cmdList.size(), input);
            howBack = -1;
        }


        if (mInputListener != null) {
            mInputListener.onNewInput(input);
        }

        setupNewInput();

        return true;
    }

    public void setOutput(String output) {
        if (output == null) {
            return;
        }

        output = output.trim();
        if(output.equals(Tuils.EMPTYSTRING)) {
            return;
        }

        if(output.equals(clear.CLEAR)) {
            clear();
            return;
        }

        writeToView(output, OUTPUT);

        for(Messager messager : messagers) {
            if(cmds != 0 && cmds % messager.n == 0) {
                writeToView(messager.message, OUTPUT);
            }
        }
    }

    public void setOutput(String output, int color) {
        if (output == null) {
            return;
        }

        output = output.trim();
        if(output.equals(Tuils.EMPTYSTRING)) {
            return;
        }

        if(output.equals(clear.CLEAR)) {
            clear();
            return;
        }

        writeToView(color, output);

        for(Messager messager : messagers) {
            if(cmds != 0 && cmds % messager.n == 0) {
                writeToView(messager.message, OUTPUT);
            }
        }
    }

    public void onBackPressed() {
        if(cmdList.size() > 0) {

            if(howBack == -1) {
                howBack = cmdList.size();
            } else if(howBack == 0) {
                return;
            }
            howBack--;

            setInput(cmdList.get(howBack));
        }
    }

    public void onNextPressed() {
        if(howBack != -1 && howBack < cmdList.size()) {
            howBack++;

            String input;
            if(howBack == cmdList.size()) {
                input = Tuils.EMPTYSTRING;
            } else {
                input = cmdList.get(howBack);
            }

            setInput(input);
        }
    }

    private void writeToView(final String text, final int type) {
        mTerminalView.post(new Runnable() {
            @Override
            public void run() {
                String txt = text;
                txt = Tuils.NEWLINE.concat(txt);

                SpannableString string = getSpannable(txt, type);
                mTerminalView.append(string);

                scrollToEnd();
            }
        });
    }

    private void writeToView(final int color, final String text) {
        mTerminalView.post(new Runnable() {
            @Override
            public void run() {
                String txt = text;
                txt = Tuils.NEWLINE.concat(txt);

                SpannableString string = getSpannable(color, txt);
                mTerminalView.append(string);

                scrollToEnd();
            }
        });
    }

    public void simulateEnter() {
        onNewInput();
    }

    public void setupScroller() {
        this.mTerminalView.setMovementMethod(new ScrollingMovementMethod());
    }

    private SpannableString getSpannable(String text, int type) {
        int color;
        if(type == INPUT) {
            color = mSkinManager.inputColor;
        } else if(type == OUTPUT) {
            color = mSkinManager.outputColor;
        } else {
            return null;
        }
        return getSpannable(color, text);
    }

    private SpannableString getSpannable(int color, String text) {
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new ForegroundColorSpan(color), 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableString;
    }

    public String getInput() {
        return mInputView.getText().toString();
    }

    public void setInput(String input) {
        mInputView.setText(input);
        focusInputEnd();
    }

    public void setHint(String hint) {
        defaultHint = false;

        if(mInputView != null) {
            mInputView.setHint(hint);
        }
    }

    public void setDefaultHint() {
        defaultHint = true;

        if(mInputView != null) {
            mInputView.setHint(Tuils.getHint(mSkinManager, mainPack.currentDirectory.getAbsolutePath()));
        }
    }

    public void setInputListener(UIManager.OnNewInputListener listener) {
        this.mInputListener = listener;
    }

    public void setSuggestionNavigator(UIManager.SuggestionNavigator navigator) {
        this.mSuggestionNavigator = navigator;
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

    public void clear() {
        mTerminalView.post(new Runnable() {
            @Override
            public void run() {
                mTerminalView.setText(Tuils.EMPTYSTRING);
            }
        });
        mInputView.post(new Runnable() {
            @Override
            public void run() {
                mInputView.setText(Tuils.EMPTYSTRING);
            }
        });
        cmdList.clear();
    }

    public static class Messager {

        int n;
        String message;

        public Messager(int n, String message) {
            this.n = n;
            this.message = message;
        }
    }

}
