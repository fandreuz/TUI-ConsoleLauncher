package ohi.andre.consolelauncher.features.alias;

import android.support.v4.util.ArraySet;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import ohi.andre.consolelauncher.features.settings.SettingsManager;
import ohi.andre.consolelauncher.features.settings.options.Behavior;
import ohi.andre.consolelauncher.tuils.Tuils;

public class AliasManager {
    
    public static final int RESULT_NOT_FOUND            = 10;
    // returned when the file doesn't contain the alias which the user is trying to delete
    public static final int RESULT_CORRUPTED_FILE       = 11;
    public static final int RESULT_ALIAS_ALREADY_EXISTS = 12;
    public static final int RESULT_FILE_SYSTEM_ERROR    = 13;
    
    public static String NAME = "name";
    
    public static final String FILE_NAME = "alias.txt";
    
    private Map<String, Alias> aliases;
    
    // the pattern used to match the separator in a string containing the parameters
    private Pattern aliasParametersSeparatorPattern;
    
    // the pattern used to match the parameters placeholders in the value of an alias
    private Pattern aliasPlaceholderPattern;
    
    private String aliasContentFormat;
    
    // matches the symbol associated with the value of an alias
    private final Pattern patternValue     = Pattern.compile("%v", Pattern.LITERAL);
    // matches the symbol associated with the name of an alias
    private final Pattern patternAliasName = Pattern.compile("%a", Pattern.LITERAL);
    
    private final CompositeDisposable disposable = new CompositeDisposable();
    
    public AliasManager () {
        SettingsManager settingsManager = SettingsManager.getInstance();
        disposable.add(settingsManager.requestUpdates(Behavior.alias_parameters_placeholder, String.class)
                .map(string -> Pattern.compile(string, Pattern.LITERAL))
                .subscribe(pattern -> aliasPlaceholderPattern = pattern)
        );
        
        disposable.add(settingsManager.requestUpdates(Behavior.alias_parameters_placeholder, String.class)
                .map(string -> Pattern.compile(string, Pattern.LITERAL))
                .subscribe(pattern -> aliasParametersSeparatorPattern = pattern)
        );
        
        disposable.add(settingsManager.requestUpdates(Behavior.alias_content_format, String.class)
                .subscribe(string -> aliasContentFormat = string));
        
        load();
    }
    
    public String printAliases () {
        return Observable.fromIterable(aliases.values())
                .map(alias -> alias.name + "=" + alias.value)
                .toList()
                .map(list -> TextUtils.join("\n", list))
                .blockingGet();
    }
    
    // pack.object1 : alias name
    // pack.object2 : residual string
    // pack.object3 : Alias
    // from the given string, we're looking for an alias from the left. we aren't interested in an alias in the center
    // of the string.
    // I loop over the spaces until I find a matching name for an alias (if allowSpaces is true)
    public Tuils.Pack3<String, String, Alias> getAlias (String string, boolean allowSpaces) {
        return Observable.just(string)
                .map(s -> s.split(" "))
                // from the given splitted string, generate all the combinations taking all the possible spaces
                .flatMap(ss -> {
                    if (allowSpaces) {
                        // index i: take ... + ss[i-1] + ss[i]
                        return Observable.range(0, ss.length)
                                // return a pack of [possibleAliasName, residualString]
                                .map(i -> new Tuils.BiPack<>(TextUtils.join(" ", Arrays.copyOfRange(ss, 0, i + 1)),
                                        TextUtils.join(" ", Arrays.copyOfRange(ss, i + 1, ss.length))));
                    } else {
                        // if the user doesn't want spaces, emit just the whole string
                        return Observable.just(new Tuils.BiPack<>(string, ""));
                    }
                })
                .map(bipack -> bipack.extend(aliases.get(bipack.object1)))
                .filter(pack3 -> pack3.object3 != null)
                .first(null)
                .blockingGet();
    }
    
    public String applyParameters (Alias alias, String params) {
        return alias.applyParameters(params, aliasPlaceholderPattern, aliasParametersSeparatorPattern);
    }
    
    private Alias getAliasByName (String name) {
        return aliases.get(name);
    }
    
    public String formatAliasContent (String aliasName, String aliasValue) {
        String returnValue = aliasContentFormat;
        returnValue = Tuils.patternNewline.matcher(returnValue)
                .replaceAll(Matcher.quoteReplacement("\n"));
        returnValue = patternValue.matcher(returnValue)
                .replaceAll(Matcher.quoteReplacement(aliasValue));
        returnValue = patternAliasName.matcher(returnValue)
                .replaceAll(Matcher.quoteReplacement(aliasName));
        return returnValue;
    }
    
    // load the aliases from the underlying file
    public void load () {
        if (aliases != null) aliases.clear();
        else aliases = new HashMap<>();
        
        Executors.newSingleThreadExecutor()
                .execute(() -> {
                    File file = new File(Tuils.getFolder(), FILE_NAME);
                    
                    try {
                        if (!file.exists()) file.createNewFile();
                        
                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                        Pattern equalPattern = Pattern.compile("=");
                        
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] split = equalPattern.split(line);
                            // we want two strings on the line, divided by "="
                            if (split.length != 2) continue;
                            
                            String name = split[0].trim();
                            
                            // recreate the value, which has been splitted by "="
                            String value = TextUtils.join("=", Arrays.copyOfRange(split, 1, split.length))
                                    .trim();
                            
                            // todo: during runtime of an alias check that value != name
                            
                            aliases.put(name, new Alias(name, value, aliasPlaceholderPattern));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }
    
    public void dispose () {
        disposable.dispose();
        disposable.clear();
    }
    
    public int newAlias (String aliasName, String aliasValue) {
        if (aliases.containsKey(aliasName)) return RESULT_ALIAS_ALREADY_EXISTS;
        else {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(new File(Tuils.getFolder(), FILE_NAME), true);
                fos.write(("\n" + aliasName + "=" + aliasValue).getBytes());
                fos.close();
                
                aliases.put(aliasName, new Alias(aliasName, aliasValue, aliasPlaceholderPattern));
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                return RESULT_FILE_SYSTEM_ERROR;
            }
        }
    }
    
    // remove the alias from the pool and from the underlying file.
    // doesn't touch the pool until the file has been properly modified.
    public int deleteAlias (String aliasName) {
        if (aliases.containsKey(aliasName)) {
            try {
                File inputFile = new File(Tuils.getFolder(), FILE_NAME);
                File tempFile = new File(Tuils.getFolder(), FILE_NAME + "2");
                
                BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
                
                boolean found = false;
                
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(aliasName)) {
                        writer.write(line + "\n");
                        found = true;
                        break;
                    }
                }
                
                writer.close();
                reader.close();
                
                inputFile.delete();
                tempFile.renameTo(inputFile);
                
                if (found) {
                    aliases.remove(aliasName);
                    return 0;
                } else {
                    // we couldn't find the alias in the file
                    return RESULT_CORRUPTED_FILE;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return RESULT_FILE_SYSTEM_ERROR;
            }
        } else {
            return RESULT_NOT_FOUND;
        }
    }
    
    public Set<Alias> getAliases () {
        return Collections.unmodifiableSet(new ArraySet<>(aliases.values()));
    }
}
