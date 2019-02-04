package ohi.andre.consolelauncher.managers.suggestions;

import android.text.Editable;
import android.text.TextWatcher;

import ohi.andre.consolelauncher.tuils.interfaces.OnTextChanged;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

public class SuggestionTextWatcher implements TextWatcher {

    SuggestionsManager suggestionsManager;
    OnTextChanged textChanged;

    int before = Integer.MIN_VALUE;

    public SuggestionTextWatcher(SuggestionsManager suggestionsManager, OnTextChanged textChanged) {
        this.textChanged = textChanged;
        this.suggestionsManager = suggestionsManager;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int st, int b, int c) {
        suggestionsManager.requestSuggestion(s.toString());

        textChanged.textChanged(s.toString(), before);
        before = s.length();
    }

    @Override
    public void afterTextChanged(Editable s) {}
}
