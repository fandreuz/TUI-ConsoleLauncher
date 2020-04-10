package ohi.andre.consolelauncher.ui.device.info;

import android.os.Build;
import android.text.Html;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Single;

public class TextFormatter {
    private final Pattern optionalPattern = Pattern.compile("%\\(((?:%(?!\\()|[^?%])+)\\?([^:]*):([^)]*)\\)");
    private final BooleanExpressionParser booleanExpressionParser = new BooleanExpressionParser();
    
    // %{echo 3}
    // waits for the output to be printed
    public Single<String> evaluateExpressions (String string) {
        return null;
    }
    
    // %(boolExpression ?ifTrue:ifFalse)
    // for instance : Wi-Fi is %(%wifi-on?ON:OFF)
    // can be nested
    // evaluates boolean expressions and updates the given string
    // regex: https://regex101.com/r/1gr5LF/1
    public String optionals (String string) {
        String next;
        // hold the (successful) result of the application of applyOptionalPattern in string
        // it follows that string is never null
        while ((next = applyOptionalRegex(string)) != null) string = next;
        
        return string;
    }
    
    // returns null if no successful matches occurred
    private String applyOptionalRegex (String string) {
        Matcher matcher = optionalPattern.matcher(string);
        
        // we need this flag to check if at least a successful match occurred
        boolean flag = false;
        while (matcher.find()) {
            flag = true;
            
            String booleanExpression = matcher.group(1);
            String ifTrue = matcher.group(2);
            String ifFalse = matcher.group(3);
            
            string = matcher.replaceFirst(booleanExpressionParser.evaluateBooleanExpression(booleanExpression) ?
                    ifTrue : ifFalse);
        }
        
        return flag ? string : null;
    }
    
    // check https://medium.com/@imstudio/android-best-practice-for-text-on-android-part-3-html-tags-826ad4a0b0fe
    public CharSequence style (String string) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(string, 0);
        } else {
            return Html.fromHtml(string);
        }
    }
}
