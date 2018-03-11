package ohi.andre.consolelauncher.managers.suggestions;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

public class SuggestionTextWatcher implements TextWatcher {

    SuggestionsManager suggestionsManager;

    public SuggestionTextWatcher(SuggestionsManager suggestionsManager) {
        this.suggestionsManager = suggestionsManager;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int st, int b, int c) {
        suggestionsManager.requestSuggestion(s.toString());
    }

    @Override
    public void afterTextChanged(Editable s) {}
}
