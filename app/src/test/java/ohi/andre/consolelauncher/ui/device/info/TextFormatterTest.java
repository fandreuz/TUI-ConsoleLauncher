package ohi.andre.consolelauncher.ui.device.info;

import org.junit.Test;
import org.junit.Assert;

import io.reactivex.rxjava3.core.Single;

public class TextFormatterTest {
    private TextFormatter formatter = new TextFormatter();
    
    @Test
    public void evaluateExpressions () {
        Single<String> single = formatter.evaluateExpressions("%{echo gatto}");
        single.subscribe(output -> Assert.assertEquals("gatto", output));
    
        Single<String> single2 = formatter.evaluateExpressions("ciao %{echo 1 2 4} test");
        single.subscribe(output -> Assert.assertEquals("ciao 1 2 4 test", output));
    }
    
    @Test
    public void optionals () {
        Assert.assertEquals(" yes", formatter.optionals("%(true ? yes:no)"));
        Assert.assertEquals("no", formatter.optionals("%(0 ? yes:no)"));
        Assert.assertEquals(" no^2", formatter.optionals("%(true ? %(0 ? maybe:no^2):no)"));
    }
}