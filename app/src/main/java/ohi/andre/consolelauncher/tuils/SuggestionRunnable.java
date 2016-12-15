package ohi.andre.consolelauncher.tuils;

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

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.SkinManager;
import ohi.andre.consolelauncher.managers.SuggestionsManager;

public class SuggestionRunnable implements Runnable {

    private ViewGroup.LayoutParams suggestionViewParams;
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

    private SkinManager skinManager;

    private boolean interrupted;

    public SuggestionRunnable(SkinManager skinManager, ViewGroup suggestionsView, ViewGroup.LayoutParams suggestionViewParams, HorizontalScrollView parent) {
        this.skinManager = skinManager;
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
                toRecycle[count].setTag(R.id.exec_on_click_id, suggestions[count].exec);
                toRecycle[count].setTag(R.id.suggestion_type_id, suggestions[count].type);

                toRecycle[count].setText(s);
                toRecycle[count].setBackgroundDrawable(skinManager.getSuggestionBg(suggestions[count].type));
            } else {
                int space = suggestions.length - (count + 1);
                if (toAdd != null && space < toAdd.length) {
                    toAdd[space].setTag(R.id.exec_on_click_id, suggestions[count].exec);
                    toAdd[space].setTag(R.id.suggestion_type_id, suggestions[count].type);

                    toAdd[space].setText(s);
                    toAdd[space].setBackgroundDrawable(skinManager.getSuggestionBg(suggestions[count].type));

                    if(toAdd[space].getParent() == null) {
                        suggestionsView.addView(toAdd[space], suggestionViewParams);
                    } else {
                        throw new UnsupportedOperationException("view has a parent(?)");
                    }
                } else {
                    throw new UnsupportedOperationException("no views enough");
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
}
