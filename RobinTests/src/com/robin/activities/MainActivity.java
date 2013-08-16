package com.robin.activities;

import com.robin.uielements.IndexLocator;
import com.robin.uielements.Locator;
import com.robin.uielements.TextLocator;

public class MainActivity extends RobinTestBaseActivity
{
    private Locator textEdit1 = new IndexLocator("First number text edit", 0);
    private Locator textEdit2 = new IndexLocator("First number text edit", 1);
    private Locator multiplyButton = new TextLocator(
        "Multiply button",
        "Multiply");

    public MainActivity(final int... indexOfSolo)
    {
        super(indexOfSolo);
    }

    public MainActivity enterFirstNumber(final String num)
    {
        act().clearTextEdit(textEdit1);
        act().type(textEdit1, num);
        return this;
    }

    public MainActivity enterSecondNumber(final String num)
    {
        act().clearTextEdit(textEdit2);
        act().type(textEdit2, num);
        return this;
    }

    public MainActivity pressMultiply()
    {
        act().clickButton(multiplyButton);
        return this;
    }

    public MainActivity checkResult(final String expectedResult)
    {
        Locator result = new TextLocator("Result text", expectedResult);
        check().textVisible(result);
        return this;
    }

}
