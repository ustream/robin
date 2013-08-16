/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.activity;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openqa.selenium.support.ui.FluentWait;
import org.safs.sockets.RemoteException;
import org.safs.sockets.ShutdownInvocationException;
import org.testng.Assert;
import org.testng.TestException;

import com.google.common.base.Function;
import com.jayway.android.robotium.remotecontrol.solo.RemoteSoloException;
import com.jayway.android.robotium.remotecontrol.solo.Solo;

import com.robin.BaseFunctionality;
import com.robin.reporter.Reporter;
import com.robin.uielements.Locator;
import com.robin.uielements.StringLocator;
import com.robin.uielements.TextLocator;
import com.robin.uielements.TranslationFile;

/**
 * Base Page for all pages.
 * @author Ferenc Lombai
 */
public class BaseActivity extends BaseFunctionality
{
    protected Solo solo;

    protected int indexOfSolo = 0;

    protected TranslationFile elements;

    private RobinBaseActivityFunctions core;

    private RobinBaseActivityReporting log;

    private RobinBaseActivityChecks checks;

    private RobinBaseActivityActions actions;

    private RobinBaseActivityReadings getters;

    public BaseActivity(final String elementsFileName, final int... soloIndex)
    {
        if (soloIndex.length > 0)
        {
            indexOfSolo = soloIndex[0];
        }
        solo = test().solo(indexOfSolo);
        elements = core().getTranslationsByName(elementsFileName);
    }

    protected void addUIPageToElements(final String elementsFileName)
    {
        elements =
            elements.mergeUIPages(core().getTranslationsByName(
                elementsFileName));
    }

    /**
     * This class contains functions that reflect user actions, like clicking,
     * typing, selecting from drop-down etc. Using these functions will ensure
     * proper reporting of the action in the log file and also checking the
     * visibility of Views involved in the action.
     */
    public class RobinBaseActivityActions extends RobinBaseActions
    {
        public void rotateToLandscape()
        {
            rotateToLandscape(indexOfSolo);
        }

        public void rotateToPortrait()
        {
            rotateToPortrait(indexOfSolo);
        }

        public void clickText(final Locator locator)
        {
            String message =
                "Clicking on " + log().elementStyleString(locator.getName())
                + " text.";
            String style = Reporter.CLICK_EVENT_STYLE;
            log().line(message, style);
            String uId =
                check().waitForText(locator, style);
            try
            {
                solo.clickOnView(uId);
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        public void clickText(final String textToClick)
        {
            String message =
                "Clicking on " + log().valueStyleString(textToClick)
                    + " text.";
            String style = Reporter.CLICK_EVENT_STYLE;
            log().line(message, style);
            check().waitForTextVisible(textToClick);
            try
            {
                solo.clickOnText(textToClick);
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        public void clickButton(final Locator locator)
        {
            String message =
                "Clicking on " + log().elementStyleString(locator.getName())
                + " button.";
            String style = Reporter.CLICK_EVENT_STYLE;
            log().line(message, style);
            String uId =
                check().waitForButton(locator, style);
            try
            {
                solo.clickOnView(uId);
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        public void dragAndDropBy(final Locator locator,
            final String locatorName, final int horisMove, final int vertMove)
        {
            final String message =
                "Dragging " + log().elementStyleString(locatorName)
                    + " element and dropping of at ["
                    + log().valueStyleString("" + horisMove) + ","
                    + log().valueStyleString("" + vertMove) + "] pixels.";
            final String style = Reporter.CLICK_EVENT_STYLE;
            log().line(message, style);
            String uId = check().waitForText(locator, style);
            Rectangle location;
            boolean success = false;
            try
            {
                location = solo.getViewLocation(uId);
                final int stepCount = 10;
                success =
                    solo.drag(
                        (float) location.getCenterX(),
                        (float) location.getCenterX() + horisMove,
                        (float) location.getCenterY(),
                        (float) location.getCenterY() + vertMove,
                        stepCount);
            } catch (Exception e)
            {
                throw new TestException(e);
            }
            if (!success)
            {
                throw new TestException(
                    "Could not complete drag&drop operation on '"
                        + locator.getName() + "' element from ["
                        + location.getCenterX() + "," + location.getCenterY()
                        + "] to [" + location.getCenterX() + vertMove + ","
                        + location.getCenterY() + horisMove + "]. ");
            }
        }

        public void clearTextEdit(final Locator locator)
        {
            final String style = Reporter.TYPE_EVENT_STYLE;
            String message =
                "Clearing "
                    + log().elementStyleString(locator.getName()) + " content.";
            log().line(message, style);
            final String uID =
                check().waitForText(locator, style);
            try
            {
                solo.clearEditText(uID);
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        public void type(final Locator locator, final String textToType)
        {
            final String uID = preTypeEvents(locator, textToType);
            try
            {
                solo.typeText(uID, textToType);
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        private String preTypeEvents(final Locator locator,
            final String textToType)
        {
            final String style = Reporter.TYPE_EVENT_STYLE;
            String message =
                "Type " + textToType + " into "
                    + log().elementStyleString(locator.getName()) + ".";
            log().line(message, style);
            return check().waitForText(locator, style);
        }

        public void pressMenuButton()
        {
            String message =
                "Pressing the " + log().elementStyleString("MENU")
                + " button.";
            String style = Reporter.CLICK_EVENT_STYLE;
            log().line(message, style);
            try
            {
                solo.sendKey(Solo.MENU);
            }  catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        public void pressBackbutton()
        {
            String message =
                "Pressing the " + log().elementStyleString("BACK")
                + " button.";
            String style = Reporter.CLICK_EVENT_STYLE;
            log().line(message, style);
            try
            {
                solo.goBack();
            }  catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        public void setDateOnDatePicker(final Locator locator,
            final int dateYear, final int dateMonth, final int dateDay)
        {
            final String uID =
                presetDateEvent(locator, dateYear, dateMonth, dateDay);
            try
            {
                solo.setDatePicker(uID, dateYear, dateMonth, dateDay);
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        private String presetDateEvent(final Locator locator,
            final int dateYear, final int dateMonth, final int dateDay)
        {
            final String style = Reporter.SELECT_EVENT_STYLE;
            String message =
                "Setting the date " + dateYear + "-" + (dateMonth + 1) + "-"
                    + dateDay + "on "
                    + log().elementStyleString(locator.getName())
                    + " date picker.";
            log().line(message, style);
            final boolean onlyVisible = true;
            final String errorMessage =
                "The " + locator.getName() + " did not become visible within "
                    + defaultElementTimeOut + " ms.";

            final String uID;
            try
            {
                uID = solo.getView(Integer.parseInt(locator.getValue()));
                if (uID == null)
                {
                    throw new TestException("Could not get uID for "
                        + locator.getName() + ".");
                }
                if (!solo.waitForViewUID(
                    uID,
                    defaultElementTimeOut,
                    onlyVisible))
                {
                    throw new TestException(errorMessage);
                }
            } catch (Exception e)
            {
                throw new TestException(e);
            }
            return uID;
        }

        public ArrayList<?> clickInList(final int line)
        {
            String message = "Clicking on the (" + line + "). line of a list";
            final String style = Reporter.CLICK_EVENT_STYLE;
            log().line(message, style);
            ArrayList<?> textViews = null;
            try
            {
                textViews = solo.clickInList(line);
            } catch (Exception e)
            {
                throw new TestException(e);
            }
            return textViews;
        }

        public void clickTextViewInList(final int line,
            final int indexOfTextView)
        {
            String message =
                "Clicking on the (" + indexOfTextView
                    + "). TextView in the selected list.";
            final String style = Reporter.CLICK_EVENT_STYLE;
            log().line(message, style);
            ArrayList<?> textViews = clickInList(line);
            try
            {
                solo.clickOnView((String) textViews.get(indexOfTextView));
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        public void clickTextViewInList(final int line, final int listIndex,
            final int indexOfTextView)
        {
            String message =
                "Clicking on the (" + indexOfTextView
                    + "). TextView in the selected list.";
            final String style = Reporter.CLICK_EVENT_STYLE;
            log().line(message, style);
            ArrayList<?> textViews = clickInList(line, listIndex);
            try
            {
                solo.clickOnView((String) textViews.get(indexOfTextView));
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        public ArrayList<?> clickInList(final int line, final int listIndex)
        {
            String message =
                "Clicking on the (" + line + "). line of the (" + listIndex
                    + "). list";
            final String style = Reporter.CLICK_EVENT_STYLE;
            log().line(message, style);
            ArrayList<?> listLineTextViews = null;
            try
            {
                listLineTextViews = solo.clickInList(line, listIndex);
            } catch (Exception e)
            {
                throw new TestException(e);
            }
            return listLineTextViews;
        }

        public void clickOnCheckBox(final Locator locator)
        {
            final int uID = preCheckBoxEvent(locator);

            try
            {
                Assert.assertTrue(
                    solo.clickOnCheckBox(uID),
                    "Could not click on checkbox.");
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        private int preCheckBoxEvent(final Locator locator)
        {
            final String style = Reporter.CLICK_EVENT_STYLE;
            String message =
                "Clicking on  " + log().elementStyleString(locator.getName())
                    + "' checkbox.";
            log().line(message, style);

            final boolean onlyVisible = true;
            final String errorMessage =
                "The " + locator.getName() + " did not become visible within "
                    + defaultElementTimeOut + " ms.";

            final String uID;
            try
            {
                uID = solo.getView(Integer.parseInt(locator.getValue()));
                if (uID == null)
                {
                    throw new TestException("Could not get uID for "
                        + locator.getName() + ".");
                }
                if (!solo.waitForViewUID(
                    uID,
                    defaultElementTimeOut,
                    onlyVisible))
                {
                    throw new TestException(errorMessage);
                }
            } catch (Exception e)
            {
                throw new TestException(e);
            }
            return Integer.parseInt(uID);
        }

        public void scrollScreenTop()
        {
            final String style = Reporter.SCROLLING_EVENT_STYLE;
            String message = "Scrolling screen to top...";
            log().line(message, style);

            try
            {
                Assert.assertTrue(
                    solo.scrollToTop(),
                    "Could not scrool to top.");
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        public void scrollScreenBottom()
        {
            final String style = Reporter.SCROLLING_EVENT_STYLE;
            String message = "Scrolling screen to bottom...";
            log().line(message, style);
            try
            {

                Assert.assertTrue(
                    solo.scrollToBottom(),
                    "Could not scrool to bottom.");
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        private boolean scrollToSide(final int side)
        {
            final String style = Reporter.SCROLLING_EVENT_STYLE;
            String message =
                "Scrolling screen to "
                    + log().valueStyleString(
                        side == Solo.RIGHT ? "RIGHT" : "LEFT") + " side...";
            log().line(message, style);
            boolean isScroolled;
            try
            {
                isScroolled = solo.scrollToSide(side);
            } catch (Exception e)
            {
                throw new TestException(e);
            }
            return isScroolled;
        }

        public void scrollScreenLeft()
        {
            Assert.assertTrue(
                scrollToSide(Solo.LEFT),
                "Could not scroll to LEFT.");
        }

        public void scrollScreenRight()
        {
            Assert.assertTrue(
                scrollToSide(Solo.RIGHT),
                "Could not scroll to RIGHT.");
        }

        public void scrollViewToLeft(final Locator locator)
        {
            Assert.assertTrue(scrollViewToSide(locator, Solo.LEFT));
        }

        public void scrollViewToRight(final Locator locator)
        {
            Assert.assertTrue(scrollViewToSide(locator, Solo.RIGHT));
        }

        private boolean
            scrollViewToSide(final Locator locator, final int side)
        {
            final String style = Reporter.SCROLLING_EVENT_STYLE;
            String message =
                "Scrolling "
                    + log.elementStyleString(locator.getName())
                    + " view to "
                    + log().valueStyleString(
                        side == Solo.RIGHT ? "RIGHT" : "LEFT") + " side...";
            log().line(message, style);
            String uid = check().waitForText(locator, style);
            boolean isScroolled = false;
            try
            {
                isScroolled = solo.scrollViewToSide(uid, side);
            } catch (IllegalThreadStateException e)
            {
                e.printStackTrace();
            } catch (RemoteException e)
            {
                e.printStackTrace();
            } catch (TimeoutException e)
            {
                e.printStackTrace();
            } catch (ShutdownInvocationException e)
            {
                e.printStackTrace();
            } catch (RemoteSoloException e)
            {
                e.printStackTrace();
            }
            return isScroolled;
        }

        public void enterText(final int index, final String textToType)
        {
            String message =
                "Enter " + log().valueStyleString(textToType) + " text into ("
                    + index + "). EditText field.";
            log().line(message, Reporter.TYPE_EVENT_STYLE);
            try
            {
                solo.enterText(index, textToType);
            } catch (IllegalThreadStateException e)
            {
                e.printStackTrace();
            } catch (RemoteException e)
            {
                e.printStackTrace();
            } catch (TimeoutException e)
            {
                e.printStackTrace();
            } catch (ShutdownInvocationException e)
            {
                e.printStackTrace();
            }
        }

        public void sendKey(final int keyCode)
        {
            final String style = Reporter.TYPE_EVENT_STYLE;
            String message =
                "Type '" + (char) keyCode + "' into the actual selected view.";
            log().line(message, style);
            try
            {
                solo.sendKey(keyCode);
            } catch (IllegalThreadStateException e)
            {
                e.printStackTrace();
            } catch (RemoteException e)
            {
                e.printStackTrace();
            } catch (TimeoutException e)
            {
                e.printStackTrace();
            } catch (ShutdownInvocationException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public RobinBaseActivityActions act()
    {
        if (actions == null)
        {
            actions = new RobinBaseActivityActions();
        }
        return actions;
    }

    /**
     * This class contains functions that can be considered as information
     * gathering methods. Mostly those functions collected here which provide
     * such information that a user should be able collect from the activity
     * being
     * watched. Such functions are, reading the text of the part of the view,
     * counting the number of elements from a certain kind or determining the
     * presence of some elements. There are also some functions that collect
     * user available information indirectly, like css- or attribute values.
     */
    public class RobinBaseActivityReadings
    {
        public String activityName()
        {
            log().firstInLine(
                "Reading the activity ... ",
                Reporter.READ_EVENT_STYLE);
            String actActivity;
            try {
                actActivity = solo.getCurrentActivity();
            } catch (Exception e) {
                throw new TestException(e);
            }
            log().lastInLine(
                log().valueStyleString(Reporter.getHtmlLink(actActivity)),
                Reporter.READ_EVENT_STYLE);
            return actActivity;
        }

        public String text(final Locator locator)
        {
            String message =
                "Reading text from "
                    + log().elementStyleString(locator.getName()) + ".";
            String style = Reporter.READ_EVENT_STYLE;
            log().line(message, style);
            String uid =
                check().waitForView(locator, style);
            String result = null;
            try
            {
                result = solo.getTextViewValue(uid);
            } catch (IllegalThreadStateException e)
            {
                e.printStackTrace();
            } catch (RemoteException e)
            {
                e.printStackTrace();
            } catch (TimeoutException e)
            {
                e.printStackTrace();
            } catch (ShutdownInvocationException e)
            {
                e.printStackTrace();
            }
            return result;
        }

        public boolean textIsVisible(final String textToSearch)
        {
            String message =
                "Get " + log().valueStyleString(textToSearch)
                    + " text is visible in the current activity.";
            log().line(message, Reporter.CHECKING_EVENT_STYLE);
            boolean onlyVisible = true;
            boolean isVisible = false;
            try
            {
                isVisible = solo.searchText(textToSearch, onlyVisible);
            } catch (IllegalThreadStateException e)
            {
                e.printStackTrace();
            } catch (RemoteException e)
            {
                e.printStackTrace();
            } catch (TimeoutException e)
            {
                e.printStackTrace();
            } catch (ShutdownInvocationException e)
            {
                e.printStackTrace();
            } catch (RemoteSoloException e)
            {
                e.printStackTrace();
            }
            return isVisible;
        }
    }

    public RobinBaseActivityReadings get()
    {
        if (getters == null)
        {
            getters = new RobinBaseActivityReadings();
        }
        return getters;
    }

    public class RobinBaseActivityChecks extends RobinBaseChecks
    {
        public void dialogClosed()
        {
            String style = Reporter.CHECKING_EVENT_STYLE;
            log().firstInLine(
                "Checking the disappearing of the dialog view ...",
                style);
            log().tic();
            try
            {
                if (!solo.waitForDialogToClose(defaultElementTimeOut))
                {
                    throw new TestException(
                        "The dialog view did not close within "
                            + defaultElementTimeOut + " ms.");
                }
            } catch (Exception e)
            {
                throw new TestException(e);
            }
            log().toc(style);
        }

        // TODO: Investigate how to check text visibility inside a Fragment.
        // e.g.: Visibility is true in a ViewPager's fragment when the fragment
        // is not on the screen.
        private void waitForTextVisible(final String textToSearch)
        {
            String message =
                "Waiting for " + log().valueStyleString(textToSearch)
                    + " to be visible in the current activity... ";
            log().firstInLine(message, Reporter.WAIT_EVENT_STYLE);
            log().tic();
            test().decreaseVerbosity();
            try
            {
                test().solo(indexOfSolo).waitForText(textToSearch);
            } catch (IllegalThreadStateException e)
            {
                e.printStackTrace();
            } catch (RemoteException e)
            {
                e.printStackTrace();
            } catch (TimeoutException e)
            {
                e.printStackTrace();
            } catch (ShutdownInvocationException e)
            {
                e.printStackTrace();
            } catch (RemoteSoloException e)
            {
                e.printStackTrace();
            }
        }

        public void textVisible(final String expectedText)
        {
            String message =
                "Checking the visibility of "
                    + log().valueStyleString(expectedText)
                    + " text in the current activity.";
            log().line(message, Reporter.CHECKING_EVENT_STYLE);
            waitForTextVisible(expectedText);
        }

        public void textVisible(final Locator locator,
            final String expectedText)
        {
            String message =
                "Checking the visibility of "
                    + log().valueStyleString(expectedText) + " text in "
                    + log().elementStyleString(locator.getName());
            log().line(message, Reporter.CHECKING_EVENT_STYLE);
            Assert.assertEquals(get().text(locator), expectedText);
        }

        public void textVisible(final Locator locator)
        {
            String message =
                "Checking the visibility of "
                    + log().elementStyleString(locator.getName())
                    + " text.";
            String style = Reporter.CHECKING_EVENT_STYLE;
            log().line(message, style);
            waitForText(locator, Reporter.CHECKING_EVENT_STYLE);
        }

        public void buttonVisible(final Locator locator)
        {
            String message =
                "Checking the visibility of "
                    + log().elementStyleString(locator.getName())
                    + " button.";
            String style = Reporter.CHECKING_EVENT_STYLE;
            log().line(message, style);
            waitForButton(locator, Reporter.CHECKING_EVENT_STYLE);
        }

        public void textNotVisible(final Locator locator)
        {
            String message =
                "Checking the disappear of "
                    + log().elementStyleString(locator.getName())
                    + " text.";
            String style = Reporter.CHECKING_EVENT_STYLE;
            log().line(message, style);
            waitForNoText(locator, Reporter.CHECKING_EVENT_STYLE);
        }

        private String
        waitForText(final Locator locator, final String style)
        {
            final String actStyle = style + " " + Reporter.WAIT_EVENT_STYLE;
            log().firstInLine(
                "Waiting for " + log().elementStyleString(locator.getName())
                + " text to be visible... ",
                actStyle);
            final String locatorMessage =
                locator.getType().name() + " type '" + locator.getName()
                + "' (" + locator.getValue() + ") text";
            final String errorMessage =
                "The " + locatorMessage + " did not become visible within "
                    + defaultElementTimeOut + " ms.";
            log().tic();
            String uID = null;
            final boolean onlyVisible = true;
            final boolean scrollToView = true;
            try
            {
                switch (locator.getType())
                {
                    case INDEX:
                        uID =
                        solo.getEditText(Integer.parseInt(locator
                            .getValue()));
                        if (uID == null)
                        {
                            throw new TestException("Could not get uID for "
                                + locatorMessage + ".");
                        }
                        break;
                    case STRING:
                        String value =
                        core().getString((StringLocator) locator);
                        if (value == null)
                        {
                            throw new TestException("Could not get string for "
                                + locatorMessage + ".");
                        }
                        if (!solo.waitForText(
                            value,
                            1,
                            defaultElementTimeOut,
                            scrollToView,
                            onlyVisible))
                        {
                            throw new TestException(errorMessage);
                        }
                        uID = solo.getText(value, onlyVisible);
                        if (uID == null)
                        {
                            throw new TestException("Could not get uID for "
                                + locatorMessage + ".");
                        }
                        break;
                    case TEXT:
                        FluentWait<Solo> waitForText =
                            new FluentWait<Solo>(solo)
                                .withTimeout(
                                    defaultElementTimeOut,
                                    TimeUnit.MILLISECONDS)
                                .pollingEvery(
                                    defaultPollingInterval,
                                    TimeUnit.MILLISECONDS)
                                .withMessage(
                                    "The " + locatorMessage
                                        + " element did not became visible.");
                        uID = waitForText.until(new Function<Solo, String>()
                        {
                            @Override
                            public String apply(final Solo solo)
                            {
                                try
                                {
                                    return solo.getText(
                                        locator.getValue(),
                                        onlyVisible);
                                } catch (Exception e)
                                {
                                    return null;
                                }
                            }
                        });
                        break;
                    case ID:
                    default:
                        uID =
                        solo.getView(Integer.parseInt(locator.getValue()));
                        if (uID == null)
                        {
                            throw new TestException("Could not get uID for "
                                + locatorMessage + ".");
                        }
                        if (!solo.waitForViewUID(
                            uID,
                            defaultElementTimeOut,
                            onlyVisible))
                        {
                            throw new TestException(errorMessage);
                        }
                        break;
                }
            } catch (TestException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new TestException(e);
            }
            log().toc(actStyle);
            return uID;
        }

        public void viewVisible(final Locator locator)
        {
            String message =
                "Checking the visibility of "
                    + log().elementStyleString(locator.getName()) + " view.";
            String style = Reporter.CHECKING_EVENT_STYLE;
            log().line(message, style);
            waitForView(locator, style);
        }

        private String waitForView(final Locator locator, final String style)
        {
            final String actStyle = style + " " + Reporter.WAIT_EVENT_STYLE;
            log().firstInLine(
                "Waiting for " + log().elementStyleString(locator.getName())
                + " View to be visible... ",
                actStyle);
            final String locatorMessage =
                locator.getType().name() + " type '" + locator.getName()
                + "' (" + locator.getValue() + ") text";
            final String errorMessage =
                "The " + locatorMessage + " did not become visible within "
                    + defaultElementTimeOut + " ms.";
            log().tic();
            final String uID;
            final boolean onlyVisible = true;
            final boolean scrollToView = true;
            try
            {
                switch (locator.getType())
                {
                    case INDEX:
                        ArrayList<?> viewInActivity = solo.getCurrentViews();
                        uID =
                            (String) viewInActivity.get(Integer
                                .getInteger(locator.getValue()));
                        FluentWait<Solo> waitForView =
                            new FluentWait<Solo>(solo)
                                .withTimeout(
                                    defaultElementTimeOut,
                                    TimeUnit.MILLISECONDS)
                                .pollingEvery(
                                    defaultPollingInterval,
                                    TimeUnit.MILLISECONDS)
                                .withMessage(
                                    "The " + locatorMessage
                                        + " element did not became visible.");
                        waitForView.until(new Function<Solo, Boolean>()
                        {
                            @Override
                            public Boolean apply(final Solo solo)
                            {
                                try
                                {
                                    return solo.waitForViewUID(uID);
                                } catch (Exception e)
                                {
                                    return null;
                                }
                            }
                        });
                        break;
                    case STRING:
                        String value =
                        core().getString((StringLocator) locator);
                        if (value == null)
                        {
                            throw new TestException("Could not get string for "
                                + locatorMessage + ".");
                        }
                        if (!solo.waitForText(
                            value,
                            1,
                            defaultElementTimeOut,
                            scrollToView,
                            onlyVisible))
                        {
                            throw new TestException(errorMessage);
                        }
                        uID = solo.getText(value, onlyVisible);
                        if (uID == null)
                        {
                            throw new TestException("Could not get uID for "
                                + locatorMessage + ".");
                        }
                        break;
                    case TEXT:
                        FluentWait<Solo> waitForText =
                            new FluentWait<Solo>(solo)
                                .withTimeout(
                                    defaultElementTimeOut,
                                    TimeUnit.MILLISECONDS)
                                .pollingEvery(
                                    defaultPollingInterval,
                                    TimeUnit.MILLISECONDS)
                                .withMessage(
                                    "The " + locatorMessage
                                        + " element did not became visible.");
                        uID = waitForText.until(new Function<Solo, String>()
                        {
                            @Override
                            public String apply(final Solo solo)
                            {
                                try
                                {
                                    return solo.getText(
                                        locator.getValue(),
                                        onlyVisible);
                                } catch (Exception e)
                                {
                                    return null;
                                }
                            }
                        });
                        break;
                    case ID:
                    default:
                        uID =
                        solo.getView(Integer.parseInt(locator.getValue()));
                        if (uID == null)
                        {
                            throw new TestException("Could not get uID for "
                                + locatorMessage + ".");
                        }
                        if (!solo.waitForViewUID(
                            uID,
                            defaultElementTimeOut,
                            onlyVisible))
                        {
                            throw new TestException(errorMessage);
                        }
                        break;
                }
            } catch (TestException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new TestException(e);
            }
            log().toc(actStyle);
            return uID;
        }

        private String
        waitForButton(final Locator locator, final String style)
        {
            final String actStyle = style + " " + Reporter.WAIT_EVENT_STYLE;
            log().firstInLine(
                "Waiting for " + log().elementStyleString(locator.getName())
                + " button to be visible: ",
                actStyle);
            final String locatorMessage =
                locator.getType().name() + " type '" + locator.getName()
                + "' (" + locator.getValue() + ") button";
            final String errorMessage =
                "The " + locatorMessage + " did not become visible within "
                    + defaultElementTimeOut + " ms.";
            log().tic();
            String uID = null;
            final boolean onlyVisible = true;
            final boolean scrollToView = true;
            try
            {
                switch (locator.getType())
                {
                    case STRING:
                        String value =
                        core().getString((StringLocator) locator);
                        if (!solo.waitForText(
                            value,
                            1,
                            defaultElementTimeOut,
                            scrollToView,
                            onlyVisible))
                        {
                            throw new TestException(errorMessage);
                        }
                        uID = solo.getButton(value, onlyVisible);
                        if (uID == null)
                        {
                            throw new TestException("Could not get uID for "
                                + locatorMessage + ".");
                        }
                        break;
                    case TEXT:
                        uID = solo.getButton(locator.getValue(), onlyVisible);
                        if (uID == null)
                        {
                            throw new TestException("Could not get uID for "
                                + locatorMessage + ".");
                        }
                        break;
                    case ID:
                    default:
                        uID =
                        solo.getView(Integer.parseInt(locator.getValue()));
                        if (uID == null)
                        {
                            throw new TestException("Could not get uID for "
                                + locatorMessage + ".");
                        }
                        if (!solo.waitForViewUID(
                            uID,
                            defaultElementTimeOut,
                            onlyVisible))
                        {
                            throw new TestException(errorMessage);
                        }
                        break;
                }
            } catch (TestException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new TestException(e);
            }
            log().toc(actStyle);
            return uID;
        }

        public void textDisappear(final Locator locator)
        {
            String message =
                "Checking the non-visibility of "
                    + log().elementStyleString(locator.getName()) + ".";
            String style = Reporter.CHECKING_EVENT_STYLE;
            log().line(message, style);
            waitForNoText(locator, Reporter.CHECKING_EVENT_STYLE);
        }

        private void waitForNoText(final Locator locator,
            final String style)
        {
            final String actStyle = style + " " + Reporter.WAIT_EVENT_STYLE;
            log().firstInLine(
                "Waiting for " + log().elementStyleString(locator.getName())
                + " element to fade away... ",
                actStyle);
            log().tic();
            final boolean onlyVisible = true;
            final boolean scrollToView = false;
            FluentWait<Solo> waitForDisappear =
                new FluentWait<Solo>(solo)
                .withTimeout(defaultElementTimeOut, TimeUnit.MILLISECONDS)
                .pollingEvery(
                    defaultPollingInterval,
                    TimeUnit.MILLISECONDS)
                    .withMessage(
                        "The " + locator.getType().name() + " type '"
                            + locator.getName() + "' (" + locator.getValue()
                            + ") text element did not fade away.");
            waitForDisappear.until(new Function<Solo, Boolean>()
                {
                @Override
                public Boolean apply(final Solo solo)
                {
                    boolean isVisible = false;
                    try
                    {
                        switch (locator.getType())
                        {
                            case STRING:
                                isVisible =
                                solo.waitForText(
                                    core().getString(
                                        (StringLocator) locator),
                                        1,
                                        defaultPollingInterval,
                                        scrollToView,
                                        onlyVisible);
                                break;
                            case TEXT:
                                isVisible =
                                solo.waitForText(
                                    locator.getValue(),
                                    1,
                                    defaultPollingInterval,
                                    scrollToView,
                                    onlyVisible);
                                break;
                            case ID:
                            default:
                                String uID =
                                solo.getView(Integer.parseInt(locator
                                    .getValue()));
                                isVisible =
                                    solo.waitForViewUID(
                                        uID,
                                        defaultPollingInterval,
                                        scrollToView);
                                break;
                        }
                    } catch (Exception e)
                    {
                        throw new TestException(e);
                    }
                    return !isVisible;
                }
                });
            log().toc(actStyle);
        }

        public void activityLoaded(final Class<?> activityClass)
        {
            activityLoaded(activityClass, indexOfSolo);
        }

        public void checkBoxChecked(final Locator locator)
        {
            final String uID = preCheckBoxCheckEvent(locator);
            try
            {
                Assert.assertTrue(
                    solo.isCheckBoxChecked(uID),
                    "Checkbox is not checked.");
            } catch (Exception e)
            {
                throw new TestException(e);
            }
        }

        private String preCheckBoxCheckEvent(final Locator locator)
        {
            final String style = Reporter.CHECKING_EVENT_STYLE;
            String message =
                "Checking if " + log().elementStyleString(locator.getName())
                    + " is checked...";
            log().line(message, style);
            return check().waitForText(locator, style);
        }
    }

    @Override
    public RobinBaseActivityChecks check()
    {
        if (checks == null)
        {
            checks = new RobinBaseActivityChecks();
        }
        return checks;
    }

    /**
     * This class collects those functions that are robotium specific, and
     * necessary to drive the test but not considered to be a user interaction.
     */
    public class RobinBaseActivityFunctions extends RobinBaseFunctions
    {
        public Solo getSolo()
        {
            return solo;
        }

        public String getTranslationText(final Locale language,
            final String elementName)
        {
            return elements.getTranslation(language, elementName);
        }

        public TextLocator getTranslationLocator(final Locale language,
            final String elementName)
        {
            return new TextLocator(elementName, elements.getTranslation(
                language,
                elementName));
        }

        public String getString(final StringLocator locator)
        {
            String value;
            try
            {

                value = solo.getString(locator.getValue());
            } catch (Exception e)
            {
                throw new TestException(e);
            }
            if (value == null)
            {
                throw new TestException("Could not get string for "
                    + locator.getName() + " (" + locator.getValue() + ").");
            }
            return value;
        }
    }

    @Override
    public RobinBaseActivityFunctions core()
    {
        if (core == null)
        {
            core = new RobinBaseActivityFunctions();
        }
        return core;
    }

    /**
     * This class is collects those functions that are used for logging actions
     * performed by page functions.
     */
    public class RobinBaseActivityReporting extends RobinBaseReporting
    {
        public void line(final String message, final String style)
        {
            line(message, style, indexOfSolo);
        }

        public void firstInLine(final String message, final String style)
        {
            firstInLine(message, style, indexOfSolo);
        }
    }

    @Override
    public RobinBaseActivityReporting log()
    {
        if (log == null)
        {
            log = new RobinBaseActivityReporting();
        }
        return log;
    }

}
