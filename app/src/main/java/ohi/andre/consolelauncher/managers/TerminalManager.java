package ohi.andre.consolelauncher.managers;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.IBinder;
import android.text.InputType;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
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
import ohi.andre.consolelauncher.tuils.TimeManager;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Rooter;

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

    public static final int CATEGORY_INPUT = 10;
    public static final int CATEGORY_OUTPUT = 11;
    public static final int CATEGORY_NOTIFICATION = 12;
    public static final int CATEGORY_GENERAL = 13;

    private long lastEnter;

    private String prefix;
    private String suPrefix;

    private ScrollView mScrollView;
    private TextView mTerminalView;
    private EditText mInputView;

    private TextView mPrefix;
    private boolean suMode;

    private MessagesManager messagesManager;

    private List<String> cmdList = new ArrayList<>(CMD_LIST_SIZE);
    private int howBack = -1;

    private Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {
            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            mInputView.requestFocus();
        }
    };

    private UIManager.OnNewInputListener mInputListener;
//    private UIManager.SuggestionNavigator mSuggestionNavigator;

    private MainPack mainPack;

    private boolean defaultHint = true;

    private int clearCmdsCount= 0;

    private int clearAfterCmds, clearAfterMs, maxLines;
    private Runnable clearRunnable = new Runnable() {

        @Override
        public void run() {
            clear();
            mTerminalView.postDelayed(this, clearAfterMs);
        }
    };

    private String inputFormat;
    private String outputFormat;

    private Context mContext;

    public TerminalManager(final TextView terminalView, EditText inputView, TextView prefixView, ImageButton submitView, final ImageButton backView, ImageButton nextView, ImageButton deleteView,
                           ImageButton pasteView, final Context context, MainPack mainPack) {
        if (terminalView == null || inputView == null || prefixView == null)
            throw new UnsupportedOperationException();

        this.mContext = context;

        final Typeface lucidaConsole = Typeface.createFromAsset(context.getAssets(), "lucida_console.ttf");

        this.mainPack = mainPack;

        this.clearAfterMs = XMLPrefsManager.get(int.class, XMLPrefsManager.Behavior.clear_after_seconds) * 1000;
        this.clearAfterCmds = XMLPrefsManager.get(int.class, XMLPrefsManager.Behavior.clear_after_cmds);
        this.maxLines = XMLPrefsManager.get(int.class, XMLPrefsManager.Behavior.max_lines);

        inputFormat = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.input_format);
        outputFormat = XMLPrefsManager.get(String.class, XMLPrefsManager.Behavior.output_format);

        prefix = XMLPrefsManager.get(XMLPrefsManager.Ui.input_prefix);
        suPrefix = XMLPrefsManager.get(String.class, XMLPrefsManager.Ui.input_root_prefix);

        int ioSize = XMLPrefsManager.get(int.class, XMLPrefsManager.Ui.input_output_size);

        Typeface t = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Ui.system_font) ? Typeface.DEFAULT : lucidaConsole;
        prefixView.setTypeface(t);
        prefixView.setTextColor(XMLPrefsManager.getColor(XMLPrefsManager.Theme.input_color));
        prefixView.setTextSize(ioSize);
        prefixView.setText(prefix.endsWith(Tuils.SPACE) ? prefix : prefix + Tuils.SPACE);
        this.mPrefix = prefixView;

        if (submitView != null) {
            submitView.setColorFilter(XMLPrefsManager.getColor(XMLPrefsManager.Theme.enter_color));
            submitView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNewInput();
                }
            });
        }

        if (backView != null) {
            ((View) backView.getParent()).setBackgroundColor(XMLPrefsManager.getColor(XMLPrefsManager.Theme.toolbar_bg));
            backView.setColorFilter(XMLPrefsManager.getColor(XMLPrefsManager.Theme.toolbar_color));
            backView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        if (nextView != null) {
            nextView.setColorFilter(XMLPrefsManager.getColor(XMLPrefsManager.Theme.toolbar_color));
            nextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNextPressed();
                }
            });
        }

        if (pasteView != null) {
            pasteView.setColorFilter(XMLPrefsManager.getColor(XMLPrefsManager.Theme.toolbar_color));
            pasteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String text = Tuils.getTextFromClipboard(context);
                    if(text != null && text.length() > 0) {
                        setInput(getInput() + text);
                    }
                }
            });
        }

        if (deleteView != null) {
            deleteView.setColorFilter(XMLPrefsManager.getColor(XMLPrefsManager.Theme.toolbar_color));
            deleteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setInput(Tuils.EMPTYSTRING);
                }
            });
        }

        this.mTerminalView = terminalView;
        this.mTerminalView.setTypeface(t);
        this.mTerminalView.setTextSize(ioSize);
        this.mTerminalView.setFocusable(false);
        setupScroller();

        if(clearAfterMs > 0) this.mTerminalView.postDelayed(clearRunnable, clearAfterMs);
        if(maxLines > 0) {
            this.mTerminalView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if(TerminalManager.this.mTerminalView == null) return true;

                    Layout l = terminalView.getLayout();
                    if(l == null) return true;

                    int count = l.getLineCount() - 1;

                    if(count > maxLines) {
                        int excessive = count - maxLines;

                        CharSequence text = terminalView.getText();
                        while (excessive >= 0) {
                            int index = TextUtils.indexOf(text, Tuils.NEWLINE);
                            if(index == -1) break;
                            text = text.subSequence(index + 1, text.length());
                            excessive--;
                        }

                        terminalView.setText(text);
                    }

                    return true;
                }
            });
        }

        View v = mTerminalView;
        do {
            v = (View) v.getParent();
        } while (!(v instanceof ScrollView));
        this.mScrollView = (ScrollView) v;

        this.mInputView = inputView;
        this.mInputView.setTextSize(ioSize);
        this.mInputView.setTextColor(XMLPrefsManager.getColor(XMLPrefsManager.Theme.input_color));
        this.mInputView.setTypeface(t);
        this.mInputView.setHint(Tuils.getHint(mainPack.currentDirectory.getAbsolutePath()));
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

    private void setupNewInput() {
        mInputView.setText(Tuils.EMPTYSTRING);

        if(defaultHint) {
            mInputView.setHint(Tuils.getHint(mainPack.currentDirectory.getAbsolutePath()));
        }

        requestInputFocus();
    }

    private boolean onNewInput() {
        if (mInputView == null) {
            return false;
        }

        String input = mInputView.getText().toString().trim();

        if(input.length() > 0) {
            clearCmdsCount++;

            if(clearCmdsCount != 0 && clearAfterCmds > 0 && clearCmdsCount % clearAfterCmds == 0) clear();

            if(messagesManager != null) messagesManager.onCmd();

            writeToView(input, CATEGORY_INPUT);

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

    public void setOutput(CharSequence output, int type) {
        if (output == null || output.length() == 0) return;

        if(output.equals(clear.CLEAR)) {
            clear();
            return;
        }

        writeToView(output, type);
    }

    public void setOutput(int color, CharSequence output) {
        if(output == null || output.length() == 0) return;

        if(output.equals(clear.CLEAR)) {
            clear();
            return;
        }

        if(color == Integer.MAX_VALUE) {
            color = XMLPrefsManager.getColor(XMLPrefsManager.Theme.output_color);
        }

        SpannableString si = new SpannableString(output);
        si.setSpan(new ForegroundColorSpan(color), 0, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        CharSequence s = TextUtils.concat(Tuils.NEWLINE, si);
        writeToView(s);
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

    final String FORMAT_INPUT = "%i";
    final String FORMAT_OUTPUT = "%o";
    final String FORMAT_PREFIX = "%p";
    final String FORMAT_NEWLINE = "%n";

    private void writeToView(CharSequence text, int type) {
        text = getFinalText(text, type);
        text = TextUtils.concat(Tuils.NEWLINE, text);
        writeToView(text);
    }

    private void writeToView(final CharSequence text) {
        mTerminalView.post(new Runnable() {
            @Override
            public void run() {
                mTerminalView.append(text);
                scrollToEnd();
            }
        });
    }

    private CharSequence getFinalText(CharSequence t, int type) {
        CharSequence s;
        switch (type) {
            case CATEGORY_INPUT:
                t = t.toString().trim();

                boolean su = t.toString().startsWith("su ") || suMode;

                SpannableString si = Tuils.span(inputFormat, XMLPrefsManager.getColor(XMLPrefsManager.Theme.input_color));

                s = TimeManager.replace(si,XMLPrefsManager.getColor(XMLPrefsManager.Theme.time_color));
                s = TextUtils.replace(s,
                        new String[] {FORMAT_INPUT, FORMAT_PREFIX, FORMAT_NEWLINE,
                                FORMAT_INPUT.toUpperCase(), FORMAT_PREFIX.toUpperCase(), FORMAT_NEWLINE.toUpperCase()},
                        new CharSequence[] {t, su ? suPrefix : prefix, Tuils.NEWLINE, t, su ? suPrefix : prefix, Tuils.NEWLINE});

                break;
            case CATEGORY_OUTPUT:
                t = t.toString().trim();

                SpannableString so = Tuils.span(outputFormat, XMLPrefsManager.getColor(XMLPrefsManager.Theme.output_color));

                s = TextUtils.replace(so,
                        new String[] {FORMAT_OUTPUT, FORMAT_NEWLINE, FORMAT_OUTPUT.toUpperCase(), FORMAT_NEWLINE.toUpperCase()},
                        new CharSequence[] {t, Tuils.NEWLINE, t, Tuils.NEWLINE});

                break;
            case CATEGORY_NOTIFICATION: case CATEGORY_GENERAL:
                s = t;
                break;
            default:
                return null;
        }

        return s;
    }

    public void simulateEnter() {
        onNewInput();
    }

    public void setupScroller() {
        this.mTerminalView.setMovementMethod(new ScrollingMovementMethod());
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
            mInputView.setHint(Tuils.getHint(mainPack.currentDirectory.getAbsolutePath()));
        }
    }

    public void setMessagesManager(MessagesManager msg) {
        this.messagesManager = msg;
    }

    public void setInputListener(UIManager.OnNewInputListener listener) {
        this.mInputListener = listener;
    }

//    public void setSuggestionNavigator(UIManager.SuggestionNavigator navigator) {
//        this.mSuggestionNavigator = navigator;
//    }

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
        cmdList.clear();
        clearCmdsCount = 0;
    }

    public Rooter getRooter() {
        return new Rooter() {
            @Override
            public void onRoot() {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        suMode = true;
                        mPrefix.setText(suPrefix.endsWith(Tuils.SPACE) ? suPrefix : suPrefix + Tuils.SPACE);
                    }
                });
            }

            @Override
            public void onStandard() {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        suMode = false;
                        mPrefix.setText(prefix.endsWith(Tuils.SPACE) ? prefix : prefix + Tuils.SPACE);
                    }
                });
            }
        };
    }

}
