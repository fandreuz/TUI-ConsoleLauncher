package ohi.andre.consolelauncher.features.alias;

import java.util.regex.Pattern;

public class Alias {
    public final String name, value;
    
    // this will be used to determin whether we can launch this alias at the first touch on its
    // suggestion, or we should wait for the user to insert his parameters and then press enter
    public final int parametersCount;
    
    public Alias (String name, String value, Pattern parameterPlaceholderPattern) {
        this.name  = name;
        this.value = value;
        
        int counter = 0;
        while (parameterPlaceholderPattern.matcher(value)
                .find()) counter++;
        parametersCount = counter;
    }
    
    // applies the given parameters to this alias, and return the complete string.
    // throws IllegalArgumentException if too much or less than expected parameters are given
    protected String applyParameters (String params, Pattern parameterPlaceholderPattern, Pattern parameterSeparatorPattern) {
        if (params == null || params.length() == 0 || parametersCount == 0) return value;
        
        String[] splitParams = parameterSeparatorPattern.split(params);
        if (splitParams.length != parametersCount)
            throw new IllegalArgumentException("No. of given arguments != No. of expected argument");
        
        String returnValue = value;
        for (int i = 0; i < splitParams.length; i++) {
            returnValue = parameterPlaceholderPattern.matcher(returnValue)
                    .replaceFirst(splitParams[i]);
        }
        
        return returnValue;
    }
}
