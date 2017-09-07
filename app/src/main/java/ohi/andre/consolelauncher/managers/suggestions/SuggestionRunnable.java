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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;

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

    private SuggestionsManager.Suggestion[] suggestions;

    private boolean interrupted;

    public SuggestionRunnable(ViewGroup suggestionsView, LinearLayout.LayoutParams suggestionViewParams, HorizontalScrollView parent) {
        this.suggestionsView = suggestionsView;
        this.suggestionViewParams = suggestionViewParams;
        this.scrollView = parent;

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

    public void setSuggestions(SuggestionsManager.Suggestion[] suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public void run() {
        if (n < 0) {
            for (int count = toRecycle.length; count < suggestionsView.getChildCount(); count++) {
                suggestionsView.removeViewAt(count--);
            }
        }

        if (interrupted) {
            return;
        }

        for (int count = 0; count < suggestions.length; count++) {
            if (interrupted) {
                return;
            }

            String s = suggestions[count].text;
            if (toRecycle != null && count < toRecycle.length) {
                toRecycle[count].setTag(R.id.suggestion_id, suggestions[count]);

                toRecycle[count].setText(s);

//                bg and fore
                int bgColor = Integer.MAX_VALUE;
                int foreColor = Integer.MAX_VALUE;
                if(suggestions[count].type == SuggestionsManager.Suggestion.TYPE_APP) {

                    Object o = suggestions[count].object;
                    if(o != null && o instanceof AppsManager.LaunchInfo) {
                        AppsManager.LaunchInfo i = (AppsManager.LaunchInfo) o;

                        for(AppsManager.Group g : AppsManager.groups) {
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

                if(bgColor != Integer.MAX_VALUE) toRecycle[count].setBackgroundColor(bgColor);
                else toRecycle[count].setBackgroundDrawable(getSuggestionBg(suggestions[count].type));
                if(foreColor != Integer.MAX_VALUE) toRecycle[count].setTextColor(foreColor);
                else toRecycle[count].setTextColor(getSuggestionTextColor(suggestions[count].type));
//                end bg and fore

                if(suggestions[count].type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
                    toRecycle[count].setLongClickable(true);
                    ((Activity) toRecycle[count].getContext()).registerForContextMenu(toRecycle[count]);
                } else {
                    ((Activity) toRecycle[count].getContext()).unregisterForContextMenu(toRecycle[count]);
                }

            } else {
                int space = suggestions.length - (count + 1);
                if (toAdd != null && space < toAdd.length) {
                    toAdd[space].setTag(R.id.suggestion_id, suggestions[count]);

                    toAdd[space].setText(s);

//                    bg and fore
                    int bgColor = Integer.MAX_VALUE;
                    int foreColor = Integer.MAX_VALUE;
                    if(suggestions[count].type == SuggestionsManager.Suggestion.TYPE_APP) {

                        Object o = suggestions[count].object;
                        if(o != null && o instanceof AppsManager.LaunchInfo) {
                            AppsManager.LaunchInfo i = (AppsManager.LaunchInfo) o;

                            for(AppsManager.Group g : AppsManager.groups) {
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

                    if(bgColor != Integer.MAX_VALUE) toAdd[space].setBackgroundColor(bgColor);
                    else toAdd[space].setBackgroundDrawable(getSuggestionBg(suggestions[count].type));
                    if(foreColor != Integer.MAX_VALUE) toAdd[space].setTextColor(foreColor);
                    else {
                        toAdd[space].setTextColor(getSuggestionTextColor(suggestions[count].type));
                    }
//                    end bg and fore

                    if(toAdd[space].getParent() == null) {
                        suggestionsView.addView(toAdd[space], suggestionViewParams);
                    }

                    if(suggestions[count].type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
                        toAdd[space].setLongClickable(true);
                        ((Activity) toAdd[space].getContext()).registerForContextMenu(toAdd[space]);
                    } else {
                        ((Activity) toAdd[space].getContext()).unregisterForContextMenu(toAdd[space]);
                    }
                } else {
//                    throw new UnsupportedOperationException("no views enough");
                    break;
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

    static boolean transparentSuggestions, bgLoad = false, textLoad = false;
    static int suggAppBg, suggAliasBg, suggCmdBg, suggContactBg, suggFileBg, suggSongBg, suggDefaultBg;
    static int suggAppText, suggAliasText, suggCmdText, suggContactText, suggFileText, suggSongText, suggDefaultText;

    public static Drawable getSuggestionBg(int type) {
        if(!bgLoad) {
            bgLoad = true;

            transparentSuggestions = XMLPrefsManager.get(boolean.class, XMLPrefsManager.Suggestions.transparent_suggestions);
            if(!transparentSuggestions) {
                suggAppBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.apps_bg_color);
                suggAliasBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.alias_bg_color);
                suggCmdBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.cmd_bg_color);
                suggContactBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.contact_bg_color);
                suggFileBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.file_bg_color);
                suggSongBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.song_bg_color);
                suggDefaultBg = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.default_bg_color);
            }
        }

        if(transparentSuggestions) {
            return new ColorDrawable(Color.TRANSPARENT);
        } else {
            switch (type) {
                case SuggestionsManager.Suggestion.TYPE_APP:
                    return new ColorDrawable(suggAppBg);
                case SuggestionsManager.Suggestion.TYPE_ALIAS:
                    return new ColorDrawable(suggAliasBg);
                case SuggestionsManager.Suggestion.TYPE_COMMAND:
                    return new ColorDrawable(suggCmdBg);
                case SuggestionsManager.Suggestion.TYPE_CONTACT:
                    return new ColorDrawable(suggContactBg);
                case SuggestionsManager.Suggestion.TYPE_FILE:
                    return new ColorDrawable(suggFileBg);
                case SuggestionsManager.Suggestion.TYPE_SONG:
                    return new ColorDrawable(suggSongBg);
                default:
                    return new ColorDrawable(suggDefaultBg);
            }
        }
    }

    public static int getSuggestionTextColor(int type) {
        if(!textLoad) {
            textLoad = true;

            suggAppText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.apps_text_color);
            suggAliasText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.alias_text_color);
            suggCmdText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.cmd_text_color);
            suggContactText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.contact_text_color);
            suggDefaultText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.default_text_color);
            suggFileText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.file_text_color);
            suggSongText = XMLPrefsManager.getColor(XMLPrefsManager.Suggestions.song_text_color);
        }

        int chosen;

        switch (type) {
            case SuggestionsManager.Suggestion.TYPE_APP:
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
            case SuggestionsManager.Suggestion.TYPE_FILE:
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
