package ohi.andre.consolelauncher.ui.device.info;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class BooleanExpressionParserTest {
    private BooleanExpressionParser booleanExpressionParser = new BooleanExpressionParser();
    
    @Test
    public void evaluateBooleanExpression () {
        // ignore case
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("true"));
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("TRUE"));
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("True"));
        Assert.assertFalse(booleanExpressionParser.evaluateBooleanExpression("false"));
        Assert.assertFalse(booleanExpressionParser.evaluateBooleanExpression("FALSE"));
        Assert.assertFalse(booleanExpressionParser.evaluateBooleanExpression("False"));
    
        // 0,1 are boolean values
        Assert.assertFalse(booleanExpressionParser.evaluateBooleanExpression("0"));
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("1"));
        
        // number comparison
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("2 == 2"));
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("2.0 == 2.0"));
        Assert.assertFalse(booleanExpressionParser.evaluateBooleanExpression("2.1 == 2"));
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("2 != 2.1"));
    
        // <,>,<=, ...
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("2.1 > 2"));
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("2.1 >= 2"));
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("2 < 2.9"));
    
        // string comparison
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("on == on"));
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("off != on"));
    
        // AND, OR, NOT
        Assert.assertFalse(booleanExpressionParser.evaluateBooleanExpression("!(true)"));
        Assert.assertTrue(booleanExpressionParser.evaluateBooleanExpression("!(true) && 1"));
        Assert.assertFalse(booleanExpressionParser.evaluateBooleanExpression("false || !(true && 1)"));
    }
}