package ohi.andre.consolelauncher.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.IBinder;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.features.apps.AppsManager;
import ohi.andre.consolelauncher.features.TimeManager;
import ohi.andre.consolelauncher.features.settings.SettingsManager;
import ohi.andre.consolelauncher.features.settings.options.Behavior;
import ohi.andre.consolelauncher.features.settings.options.Theme;
import ohi.andre.consolelauncher.features.settings.options.Ui;
import ohi.andre.consolelauncher.tuils.LongClickMovementMethod;
import ohi.andre.consolelauncher.tuils.LongClickableSpan;
import ohi.andre.consolelauncher.tuils.PrivateIOReceiver;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;

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

// manages the incoming and outgoing stream of commands/outputs between the terminal UI
// (input field + output field) and the underlying shell
public class TerminalUIManager {
    
    private final int SCROLL_DELAY  = 200;
    private final int CMD_LIST_SIZE = 40;
    
    public static final int CATEGORY_INPUT = 10, CATEGORY_OUTPUT = 11, CATEGORY_NO_COLOR = 20;
    
    public static int NO_COLOR = Integer.MAX_VALUE;
    
    private long lastEnter;
    
    private String prefix;
    private String suPrefix;
    
    private ScrollView mScrollView;
    private TextView   mTerminalView;
    private EditText   mInputView;
    
    private TextView mPrefix;
    private boolean  suMode;
    
    private List<String> cmdList = new ArrayList<>(CMD_LIST_SIZE);
    private int          howBack = -1;
    
    private Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run () {
            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            mInputView.requestFocus();
        }
    };
    
    private MainPack mainPack;
    
    private boolean defaultHint = true;
    
    private int clearCmdsCount = 0;
    
    private int clearAfterCmds, clearAfterMs, maxLines;
    private Runnable clearRunnable = new Runnable() {
        
        @Override
        public void run () {
            clear();
            mTerminalView.postDelayed(this, clearAfterMs);
        }
    };
    
    private String inputFormat, outputFormat;
    private int inputColor, outputColor;
    
    private boolean clickCommands, longClickCommands;
    
    public Context mContext;
    
    private CommandExecuter executer;
    
    public TerminalUIManager (final TextView terminalView, EditText inputView, TextView prefixView, ImageView submitView, final ImageView backView, ImageButton nextView, ImageButton deleteView,
                              ImageButton pasteView, final Context context, ExecutePack mainPack, CommandExecuter executer) {
        if (terminalView == null || inputView == null || prefixView == null)
            throw new UnsupportedOperationException();
        
        this.mContext = context;
        this.executer = executer;
        
        this.mainPack = mainPack;
        
        this.clickCommands     = SettingsManager.getBoolean(Behavior.click_commands);
        this.longClickCommands = SettingsManager.getBoolean(Behavior.long_click_commands);
        
        this.clearAfterMs   = SettingsManager.getInt(Behavior.clear_after_seconds) * 1000;
        this.clearAfterCmds = SettingsManager.getInt(Behavior.clear_after_cmds);
        this.maxLines       = SettingsManager.getInt(Behavior.max_lines);
        
        inputFormat  = SettingsManager.get(Behavior.input_format);
        outputFormat = SettingsManager.get(Behavior.output_format);
        
        inputColor  = SettingsManager.getColor(Theme.input_color);
        outputColor = SettingsManager.getColor(Theme.output_color);
        
        prefix   = SettingsManager.get(Ui.input_prefix);
        suPrefix = SettingsManager.get(Ui.input_root_prefix);
        
        int ioSize = SettingsManager.getInt(Ui.input_output_size);
        
        prefixView.setTypeface(Tuils.getTypeface(context));
        prefixView.setTextColor(SettingsManager.getColor(Theme.input_color));
        prefixView.setTextSize(ioSize);
        prefixView.setText(prefix.endsWith(" ") ? prefix : prefix + " ");
        this.mPrefix = prefixView;
        
        if (submitView != null) {
            submitView.setColorFilter(SettingsManager.getColor(Theme.enter_color));
            submitView.setOnClickListener(v -> onNewInput());
        }
        
        if (backView != null) {
            backView.setColorFilter(SettingsManager.getColor(Theme.toolbar_color));
            backView.setOnClickListener(v -> onBackPressed());
        }
        
        if (nextView != null) {
            nextView.setColorFilter(SettingsManager.getColor(Theme.toolbar_color));
            nextView.setOnClickListener(v -> onNextPressed());
        }
        
        if (pasteView != null) {
            pasteView.setColorFilter(SettingsManager.getColor(Theme.toolbar_color));
            pasteView.setOnClickListener(v -> {
                String text = Tuils.getTextFromClipboard(context);
                if (text != null && text.length() > 0) {
                    setInput(getInput() + text);
                }
            });
        }
        
        if (deleteView != null) {
            deleteView.setColorFilter(SettingsManager.getColor(Theme.toolbar_color));
            deleteView.setOnClickListener(v -> setInput(""));
        }
        
        this.mTerminalView = terminalView;
        this.mTerminalView.setTypeface(Tuils.getTypeface(context));
        this.mTerminalView.setTextSize(ioSize);
        this.mTerminalView.setFocusable(false);
        this.mTerminalView.setMovementMethod(LongClickMovementMethod.getInstance(SettingsManager.getInt(Behavior.long_click_duration)));
        
        int hintColor = SettingsManager.getColor(Theme.session_info_color);
        
        ColorStateList list = mTerminalView.getHintTextColors();
        try {
            Field colors = list.getClass()
                    .getDeclaredField("mColors");
            Field dColor = list.getClass()
                    .getDeclaredField("mDefaultColor");
            
            colors.setAccessible(true);
            dColor.setAccessible(true);
            
            int[] a = (int[]) colors.get(list);
            for (int c = 0; c < a.length; c++) {
                a[c] = hintColor;
            }
            
            colors.set(list, a);
            dColor.set(list, hintColor);
        } catch (Exception e) {
            Tuils.log(e);
        }
        
        if (clearAfterMs > 0) this.mTerminalView.postDelayed(clearRunnable, clearAfterMs);
        if (maxLines > 0) {
            this.mTerminalView.getViewTreeObserver()
                    .addOnPreDrawListener(() -> {
                        if (TerminalManager.this.mTerminalView == null) return true;
                        
                        Layout l = terminalView.getLayout();
                        if (l == null) return true;
                        
                        int count = l.getLineCount() - 1;
                        
                        if (count > maxLines) {
                            int excessive = count - maxLines;
                            
                            CharSequence text = terminalView.getText();
                            while (excessive >= 0) {
                                int index = TextUtils.indexOf(text, "\n");
                                if (index == -1) break;
                                text = text.subSequence(index + 1, text.length());
                                excessive--;
                            }
                            
                            terminalView.setText(text);
                        }
                        
                        return true;
                    });
        }
        
        View v = mTerminalView;
        do {
            v = (View) v.getParent();
        } while (!(v instanceof ScrollView));
        this.mScrollView = (ScrollView) v;
        
        this.mInputView = inputView;
        this.mInputView.setTextSize(ioSize);
        this.mInputView.setTextColor(SettingsManager.getColor(Theme.input_color));
        this.mInputView.setTypeface(Tuils.getTypeface(context));
        this.mInputView.setHint(Tuils.getHint(mainPack.currentDirectory.getAbsolutePath()));
        this.mInputView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        Tuils.setCursorDrawableColor(this.mInputView, SettingsManager.getColor(Theme.cursor_color));
        this.mInputView.setHighlightColor(Color.TRANSPARENT);
        this.mInputView.setOnEditorActionListener((v1, actionId, event) -> {
            if (!mInputView.hasFocus()) mInputView.requestFocus();
            
            //                physical enter
            if (actionId == KeyEvent.ACTION_DOWN) {
                if (lastEnter == 0) {
                    lastEnter = System.currentTimeMillis();
                } else {
                    long difference = System.currentTimeMillis() - lastEnter;
                    lastEnter = System.currentTimeMillis();
                    if (difference < 350) {
                        return true;
                    }
                }
            }
            
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE || actionId == KeyEvent.ACTION_DOWN) {
                onNewInput();
            }
            
            //                if (event == null && actionId == EditorInfo.IME_NULL) onNewInput();
            //                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) onNewInput();
            
            return true;
        });
        //        if(autoLowerFirstChar) {
        //            this.mInputView.setFilters(new InputFilter[] {new InputFilter() {
        //                @Override
        //                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        //                    if (dstart == 0 && dend == 0 && start == 0 && end == 1 && source.length() > 0) {
        //                        return TextUtils.concat(source.toString().toLowerCase().charAt(0) + Tuils.EMPTYSTRING, source.subSequence(1,source.length()));
        //                    }
        //
        //                    return source;
        //                }
        //            }});
        //        }
    }
    
    private void setupNewInput () {
        mInputView.setText("");
        
        if (defaultHint) {
            mInputView.setHint(Tuils.getHint(mainPack.currentDirectory.getAbsolutePath()));
        }
        
        requestInputFocus();
    }
    
    private boolean onNewInput () {
        if (mInputView == null) {
            return false;
        }
        
        CharSequence input = mInputView.getText();
        
        String cmd = input.toString()
                .trim();
        
        Object obj = null;
        try {
            obj = ((Spannable) input).getSpans(0, input.length(), AppsManager.InstalledApplication.class)[0];
        } catch (Exception e) {
            //            an error will probably be thrown everytime, but we don't need to track it
        }
        
        if (input.length() > 0) {
            clearCmdsCount++;
            if (clearCmdsCount != 0 && clearAfterCmds > 0 && clearCmdsCount % clearAfterCmds == 0) clear();
            
            writeToView(input, CATEGORY_INPUT);
            
            if (cmdList.size() == CMD_LIST_SIZE) {
                cmdList.remove(0);
            }
            cmdList.add(cmdList.size(), cmd);
            howBack = -1;
        }
        
        //        DO NOT USE THE INTENT APPROACH
        //        apps are not launching properly, when one has been launched, an other attempt will show always the same
        //
        //        Intent intent = new Intent(MainManager.ACTION_EXEC);
        //        intent.putExtra(MainManager.CMD, cmd);
        //        intent.putExtra(MainManager.CMD_COUNT, MainManager.commandCount);
        //
        //        Parcelable p = null;
        //        if(obj instanceof Parcelable) p = (Parcelable) obj;
        //        intent.putExtra(MainManager.PARCELABLE, p);
        //
        //        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(intent);
        
        executer.execute(cmd, obj);
        
        //        because it will clear suggestions without refilling them, because "aftertextchanged" wont be called
        //        if(cmd.length() > 0) LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(new Intent(UIManager.ACTION_CLEAR_SUGGESTIONS));
        
        setupNewInput();
        
        return true;
    }
    
    public void setOutput (CharSequence output, int type) {
        if (output == null || output.length() == 0) return;
        
        writeToView(output, type);
    }
    
    public void setOutput (int color, CharSequence output) {
        if (output == null || output.length() == 0) return;
        
        if (color == TerminalManager.NO_COLOR) {
            color = SettingsManager.getColor(Theme.output_color);
        }
        
        SpannableString si = new SpannableString(output);
        si.setSpan(new ForegroundColorSpan(color), 0, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        CharSequence s = TextUtils.concat("\n", si);
        writeToView(s);
    }
    
    public String getTerminalText () {
        return mTerminalView.getText()
                .toString();
    }
    
    public void onBackPressed () {
        if (cmdList.size() > 0) {
            
            if (howBack == -1) {
                howBack = cmdList.size();
            } else if (howBack == 0) {
                return;
            }
            howBack--;
            
            setInput(cmdList.get(howBack));
        }
    }
    
    public void onNextPressed () {
        if (howBack != -1 && howBack < cmdList.size()) {
            howBack++;
            
            String input;
            if (howBack == cmdList.size()) {
                input = "";
            } else {
                input = cmdList.get(howBack);
            }
            
            setInput(input);
        }
    }
    
    public static final String FORMAT_INPUT   = "%i";
    public static final String FORMAT_OUTPUT  = "%o";
    public static final String FORMAT_PREFIX  = "%p";
    public static final String FORMAT_NEWLINE = "%n";
    
    private void writeToView (CharSequence text, int type) {
        text = getFinalText(text, type);
        text = TextUtils.concat("\n", text);
        writeToView(text);
    }
    
    private void writeToView (final CharSequence text) {
        mTerminalView.post(() -> {
            mTerminalView.append(text);
            scrollToEnd();
        });
    }
    
    private CharSequence getFinalText (CharSequence t, int type) {
        CharSequence s;
        switch (type) {
            case CATEGORY_INPUT:
                t = t.toString();
                
                boolean su = t.toString()
                        .startsWith("su ") || suMode;
                
                SpannableString si = Tuils.span(inputFormat, inputColor);
                if (clickCommands || longClickCommands) si.setSpan(new LongClickableSpan(clickCommands ? t.toString() : null, longClickCommands ? t.toString() : null, PrivateIOReceiver.ACTION_INPUT), 0,
                        si.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                s = TimeManager.instance.replace(si);
                s = TextUtils.replace(s,
                        new String[]{FORMAT_INPUT, FORMAT_PREFIX, FORMAT_NEWLINE, FORMAT_INPUT.toUpperCase(), FORMAT_PREFIX.toUpperCase(), FORMAT_NEWLINE.toUpperCase()},
                        new CharSequence[]{t, su ? suPrefix : prefix, "\n", t, su ? suPrefix : prefix, "\n"});
                
                break;
            case CATEGORY_OUTPUT:
                t = t.toString();
                
                SpannableString so = Tuils.span(outputFormat, outputColor);
                
                s = TextUtils.replace(so,
                        new String[]{FORMAT_OUTPUT, FORMAT_NEWLINE, FORMAT_OUTPUT.toUpperCase(), FORMAT_NEWLINE.toUpperCase()},
                        new CharSequence[]{t, "\n", t, "\n"});
                
                break;
            //            already colored here
            case CATEGORY_NO_COLOR:
                s = t;
                break;
            default:
                return null;
        }
        
        return s;
    }
    
    public void simulateEnter () {
        onNewInput();
    }
    
    public String getInput () {
        return mInputView.getText()
                .toString();
    }
    
    public void setInput (String input, Object obj) {
        SpannableString spannable = new SpannableString(input);
        if (obj != null) {
            spannable.setSpan(obj, 0, input.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        mInputView.setText(spannable);
        focusInputEnd();
    }
    
    public void setInput (String input) {
        setInput(input, null);
    }
    
    public void setHint (String hint) {
        defaultHint = false;
        
        if (mInputView != null) {
            mInputView.setHint(hint);
        }
    }
    
    public void setDefaultHint () {
        defaultHint = true;
        
        if (mInputView != null) {
            mInputView.setHint(Tuils.getHint(mainPack.currentDirectory.getAbsolutePath()));
        }
    }
    
    public void focusInputEnd () {
        mInputView.setSelection(getInput().length());
    }
    
    public void scrollToEnd () {
        mScrollView.postDelayed(mScrollRunnable, SCROLL_DELAY);
    }
    
    public void requestInputFocus () {
        mInputView.requestFocus();
    }
    
    public IBinder getInputWindowToken () {
        return mInputView.getWindowToken();
    }
    
    public View getInputView () {
        return mInputView;
    }
    
    public void clear () {
        mTerminalView.post(() -> mTerminalView.setText(""));
        cmdList.clear();
        clearCmdsCount = 0;
    }
    
    public void onRoot () {
        ((Activity) mContext).runOnUiThread(() -> {
            suMode = true;
            mPrefix.setText(suPrefix.endsWith(" ") ? suPrefix : suPrefix + " ");
        });
    }
    
    public void onStandard () {
        ((Activity) mContext).runOnUiThread(() -> {
            suMode = false;
            mPrefix.setText(prefix.endsWith(" ") ? prefix : prefix + " ");
        });
    }
}
