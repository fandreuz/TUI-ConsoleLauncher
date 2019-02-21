package ohi.andre.consolelauncher.managers.suggestions;

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

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Suggestions;

public class SuggestionRunnable implements Runnable {

    private LinearLayout.LayoutParams suggestionViewParams;

    private ViewGroup suggestionsView;
    private HorizontalScrollView scrollView;
    private Runnable scrollRunnable = new Runnable() {
        @Override
        public void run() {
            scrollView.fullScroll(View.FOCUS_LEFT);
        }
    };

    private int n;
    private TextView[] toRecycle;
    private TextView[] toAdd;

    private List<SuggestionsManager.Suggestion> suggestions;

    private boolean interrupted;

    MainPack pack;

    private boolean transparentSuggestions;
    private int suggAppBg, suggAliasBg, suggCmdBg, suggContactBg, suggFileBg, suggSongBg, suggDefaultBg;
    private int suggAppText, suggAliasText, suggCmdText, suggContactText, suggFileText, suggSongText, suggDefaultText;

    private int[] spaces;

    public SuggestionRunnable(MainPack pack, ViewGroup suggestionsView, LinearLayout.LayoutParams suggestionViewParams, HorizontalScrollView parent, int[] spaces) {
        this.suggestionsView = suggestionsView;
        this.suggestionViewParams = suggestionViewParams;
        this.scrollView = parent;
        this.pack = pack;

        transparentSuggestions = XMLPrefsManager.getBoolean(Suggestions.transparent_suggestions);
        if(!transparentSuggestions) {
            suggAppBg = XMLPrefsManager.getColor(Suggestions.apps_bg_color);
            suggAliasBg = XMLPrefsManager.getColor(Suggestions.alias_bg_color);
            suggCmdBg = XMLPrefsManager.getColor(Suggestions.cmd_bg_color);
            suggContactBg = XMLPrefsManager.getColor(Suggestions.contact_bg_color);
            suggFileBg = XMLPrefsManager.getColor(Suggestions.file_bg_color);
            suggSongBg = XMLPrefsManager.getColor(Suggestions.song_bg_color);
            suggDefaultBg = XMLPrefsManager.getColor(Suggestions.default_bg_color);
        }

        suggAppText = XMLPrefsManager.getColor(Suggestions.apps_text_color);
        suggAliasText = XMLPrefsManager.getColor(Suggestions.alias_text_color);
        suggCmdText = XMLPrefsManager.getColor(Suggestions.cmd_text_color);
        suggContactText = XMLPrefsManager.getColor(Suggestions.contact_text_color);
        suggDefaultText = XMLPrefsManager.getColor(Suggestions.default_text_color);
        suggFileText = XMLPrefsManager.getColor(Suggestions.file_text_color);
        suggSongText = XMLPrefsManager.getColor(Suggestions.song_text_color);

        this.spaces = spaces;

        suggestionViewParams.setMargins(spaces[0], spaces[1], spaces[0], spaces[1]);

        reset();
    }

    public void setN(int n) {
        this.n = n;
    }

    public void setToRecycle(TextView[] toRecycle) {
        this.toRecycle = toRecycle;
    }

    public void setToAdd(TextView[] toAdd) {
        this.toAdd = toAdd;
    }

    public void setSuggestions(List<SuggestionsManager.Suggestion> suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public void run() {
        if (n < 0) {
            for (int count = toRecycle.length; count < suggestionsView.getChildCount(); count++) {
                suggestionsView.removeViewAt(count--);
            }
        }

        int length = suggestions.size();

        for (int count = 0; count < suggestions.size(); count++) {
            if (interrupted) {
                return;
            }

            SuggestionsManager.Suggestion s = suggestions.get(count);

            String text = s.text;

            TextView sggView = null;
            if (count < toRecycle.length) {
                sggView = toRecycle[count];
            }
            else {
                int space = length - (count + 1);
                if(space < toAdd.length) {
                    sggView = toAdd[space];

                    if(toAdd[space].getParent() == null) {
                        suggestionsView.addView(toAdd[space], suggestionViewParams);
                    }
                }
            }

            if (sggView != null) {
                sggView.setTag(R.id.suggestion_id, s);

                sggView.setText(text);

//                bg and fore
                int bgColor = Integer.MAX_VALUE;
                int foreColor = Integer.MAX_VALUE;

                if(s.type == SuggestionsManager.Suggestion.TYPE_APP || s.type == SuggestionsManager.Suggestion.TYPE_APPGP) {
                    Object o = s.object;
                    if(o != null && o instanceof AppsManager.LaunchInfo) {
                        AppsManager.LaunchInfo i = (AppsManager.LaunchInfo) o;

                        for(AppsManager.Group g : pack.appsManager.groups) {
                            if(g.contains(i)) {
                                o = g;
                                break;
                            }
                        }
                    }

                    if(o != null && o instanceof AppsManager.Group) {
                        bgColor = ((AppsManager.Group) o).getBgColor();
                        foreColor = ((AppsManager.Group) o).getForeColor();
                    }
                }

                if(bgColor != Integer.MAX_VALUE) sggView.setBackgroundColor(bgColor);
                else sggView.setBackgroundDrawable(getSuggestionBg(pack.context, s.type));
                if(foreColor != Integer.MAX_VALUE) sggView.setTextColor(foreColor);
                else sggView.setTextColor(getSuggestionTextColor(s.type));
//                end bg and fore

                if(s.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
                    sggView.setLongClickable(true);
                    ((Activity) sggView.getContext()).registerForContextMenu(sggView);
                } else {
                    ((Activity) sggView.getContext()).unregisterForContextMenu(sggView);
                }

            }
        }

        scrollView.post(scrollRunnable);
    }

    public void interrupt() {
        interrupted = true;
    }

    public void reset() {
        interrupted = false;
    }

    public Drawable getSuggestionBg(Context context, int type) {

        if(transparentSuggestions) {
            return new ColorDrawable(Color.TRANSPARENT);
        } else {
            switch (type) {
                case SuggestionsManager.Suggestion.TYPE_APP: case SuggestionsManager.Suggestion.TYPE_APPGP:
                    return new ColorDrawable(suggAppBg);
                case SuggestionsManager.Suggestion.TYPE_ALIAS:
                    return new ColorDrawable(suggAliasBg);
                case SuggestionsManager.Suggestion.TYPE_COMMAND:
                    return new ColorDrawable(suggCmdBg);
                case SuggestionsManager.Suggestion.TYPE_CONTACT:
                    return new ColorDrawable(suggContactBg);
                case SuggestionsManager.Suggestion.TYPE_FILE: case SuggestionsManager.Suggestion.TYPE_CONFIGFILE:
                    return new ColorDrawable(suggFileBg);
                case SuggestionsManager.Suggestion.TYPE_SONG:
                    return new ColorDrawable(suggSongBg);
                default:
                    return new ColorDrawable(suggDefaultBg);
            }
        }
    }

    public int getSuggestionTextColor(int type) {
        int chosen;

        switch (type) {
            case SuggestionsManager.Suggestion.TYPE_APP: case SuggestionsManager.Suggestion.TYPE_APPGP:
                chosen = suggAppText;
                break;
            case SuggestionsManager.Suggestion.TYPE_ALIAS:
                chosen = suggAliasText;
                break;
            case SuggestionsManager.Suggestion.TYPE_COMMAND:
                chosen = suggCmdText;
                break;
            case SuggestionsManager.Suggestion.TYPE_CONTACT:
                chosen = suggContactText;
                break;
            case SuggestionsManager.Suggestion.TYPE_FILE: case SuggestionsManager.Suggestion.TYPE_CONFIGFILE:
                chosen = suggFileText;
                break;
            case SuggestionsManager.Suggestion.TYPE_SONG:
                chosen = suggSongText;
                break;
            default:
                chosen = suggDefaultText;
                break;
        }

        if(chosen == Integer.MAX_VALUE) chosen = suggDefaultText;
        return chosen;
    }
}
