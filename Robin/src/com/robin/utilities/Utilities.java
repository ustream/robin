/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.utilities;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.RecipientStringTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

import org.apache.commons.lang.time.DateUtils;
import org.openqa.selenium.support.ui.FluentWait;
import org.testng.Assert;

import com.google.common.base.Function;

import com.robin.reporter.Reporter;
import com.robin.utilities.email.EMail;

public final class Utilities
{

    /**
     * A utility that waits for a gmail mailbox to receive a new message which
     * subject contains a specific string.
     * @param username the mailbox owner's user name (no @gmail.com required).
     * @param pass the user password protecting this mailbox
     * @param subject the string that must be contained in the new mail
     * @param timeout The maximum amount of time to wait in milliseconds.
     * @return a last from the new messages thats subject contains the specified
     *         string
     */
    public EMail waitForMailSubjectContains(final String username,
        final String pass, final String subject, final long timeout)
    {
        SearchTerm st = new SubjectTerm(subject);
        return waitForMailWithSearchTerm(
            username,
            pass,
            st,
            "No new mail arrived to " + username
                + "@gmail.com with subject containing '" + subject + "'.",
            timeout);
    }

    /**
     * A utility that waits for a gmail mailbox to receive a new message which
     * subject contains a specific string and the TO recipient is also given.
     * @param username the mailbox owner's user name (no @gmail.com required).
     * @param pass the user password protecting this mailbox
     * @param recepient the recipient given as a TO type
     * @param subject the string that must be contained in the new mail
     * @param timeout The maximum amount of time to wait in milliseconds.
     * @return a last from the new messages thats subject contains the specified
     *         string
     */
    public EMail waitForMailToAndSubjectContains(final String username,
        final String pass, final String recepient, final String subject,
        final long timeout)
    {
        SearchTerm st =
            new AndTerm(
                new RecipientStringTerm(RecipientType.TO, recepient),
                new SubjectTerm(subject));
        return waitForMailWithSearchTerm(
            username,
            pass,
            st,
            "No new mail arrived to '" + recepient
                + "' with subject containing '" + subject + "'.",
            timeout);
    }

    /**
     * A utility that waits for a gmail mailbox to receive a new message with
     * according to a given SearchTerm. Only "Not seen" messages are searched,
     * and all match is set as SEEN, but just the first occurrence of the
     * matching message is returned.
     * @param username the mailbox owner's user name (no @gmail.com required).
     * @param pass the user password protecting this mailbox
     * @param st the SearchTerm built to filter messages
     * @param timeoutMessage the message to show when no such mail found within
     *            timeout
     * @param timeout The maximum amount of time to wait in milliseconds.
     * @return a last from the new messages thats match the st conditions
     */
    public EMail waitForMailWithSearchTerm(final String username,
        final String pass, final SearchTerm st, final String timeoutMessage,
        final long timeout)
    {
        String host = "imap.gmail.com";
        final long retryTime = 1000;

        Properties props = System.getProperties();
        props.setProperty("mail.store.protocol", "imaps");
        props.put("mail.imaps.ssl.trust", "*");
        EMail email = null;
        Session session = Session.getDefaultInstance(props, null);
        try
        {
            Store store = session.getStore("imaps");
            store.connect(host, username, pass);
            Folder inbox = store.getFolder("Inbox");
            inbox.open(Folder.READ_WRITE);
            FluentWait<Folder> waitForMail =
                new FluentWait<Folder>(inbox)
                    .withTimeout(timeout, TimeUnit.MILLISECONDS)
                    .pollingEvery(retryTime, TimeUnit.MILLISECONDS)
                    .withMessage(timeoutMessage);
            email = waitForMail.until(new Function<Folder, EMail>()
            {
                @Override
                public EMail apply(final Folder inbox)
                {
                    EMail email = null;
                    FlagTerm ft =
                        new FlagTerm(new Flags(Flags.Flag.SEEN), false);
                    SearchTerm sst = new AndTerm(ft, st);
                    try
                    {
                        inbox.getMessageCount();
                        Message[] messages = inbox.search(sst);
                        for (Message message : messages)
                        {
                            message.setFlag(Flag.SEEN, true);
                        }
                        if (messages.length > 0)
                        {
                            return new EMail(messages[0]);
                        }
                    } catch (MessagingException e)
                    {
                        Assert.fail(e.getMessage());
                    }
                    return email;
                }
            });
            inbox.close(false);
            store.close();
        } catch (MessagingException e)
        {
            Assert.fail(e.getMessage());
        }
        return email;
    }

    /**
     * A utility that waits for a specific amount of time.
     * @param timeToWait Time to wait in ms.
     */
    public static void waitTime(final long timeToWait)
    {
        try
        {
            Thread.sleep(timeToWait);
        } catch (InterruptedException e)
        {
            Reporter.log(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * A utility that generates random String with a given length of characters.
     * Duplicate entries are allowed.
     * The random string contains lowercase characters and numbers.
     * @param length the generated String's length
     * @return the generated random String
     */
    public String generateRandomString(final int length)
    {

        final String validCharacters =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final Random randomGenerator = new Random();
        int randIndex = 0;
        String randomString = "";

        for (int i = 0; i < length; i++)
        {
            randIndex = randomGenerator.nextInt(validCharacters.length());
            randomString =
                randomString
                    + validCharacters.substring(randIndex, randIndex + 1);
        }
        return randomString;
    }

    /**
     * A utility that generates random String with a given length of characters.
     * Duplicate entries are allowed.
     * The random string contains lowercase characters and numbers.
     * @param length the generated String's length
     * @param notThis a string that must not be generated
     * @return the generated random String
     */
    public String generateRandomString(final int length, final String notThis)
    {
        final int maxTry = 777;
        int numOfTry = 0;
        String randomString = notThis;
        while (notThis.startsWith(randomString) && numOfTry < maxTry)
        {
            randomString = generateRandomString(length);
            numOfTry++;
        }
        if (numOfTry >= maxTry)
        {
            Assert.fail("Could not generate " + length
                + " length random string that differs from '" + notThis
                + "' within " + numOfTry + "trials. Go and buy a lottery!");
        }

        return randomString;
    }

    /**
     * Gives the actual time date with a minute precision.
     * @return The truncated date object.
     */
    public Date now()
    {
        return now(Calendar.MINUTE);
    }

    /**
     * Gives the actual time date with a given precision.
     * @param precision the date precision (like Calendar.SECOND).
     * @return The truncated date object.
     */
    public Date now(final int precision)
    {
        return DateUtils.truncate(new Date(), precision);
    }

    /**
     * Gives the actual time in the specified format by using SimpleDateFormat
     * and Calendar.
     * @param dateFormat the format string of the returned time text. (e.g.
     *            "yyyy-MM-dd HH:mm:ss")
     * @param timeZone the time zone of the current time
     * @return The current date formatted according to the given dateFormat and
     *         time zone.
     */
    public String now(final String dateFormat, final TimeZone timeZone)
    {
        return dateToString(now(), dateFormat, timeZone);
    }

    /**
     * Formats a date to string by SimpledateFormat rules in a given time zone.
     * @param date the date to write to string
     * @param dateFormat the format string of the returned time text. (e.g.
     *            "yyyy-MM-dd HH:mm:ss")
     * @param timeZone the time zone of the current time
     * @return The date formatted according to the given dateFormat and time
     *         zone.
     */
    public String dateToString(final Date date, final String dateFormat,
        final TimeZone timeZone)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(timeZone);
        return sdf.format(date);
    }

    /**
     * Formats a date to string by SimpledateFormat rules in a given time zone
     * and the locale to use.
     * @param date the date to write to string
     * @param dateFormat the format string of the returned time text. (e.g.
     *            "yyyy-MM-dd HH:mm:ss")
     * @param timeZone the time zone of the current time
     * @param locale The locale to use.
     * @return The date formatted according to the given dateFormat and time
     *         zone.
     */
    public String dateToString(final Date date, final String dateFormat,
        final TimeZone timeZone, final Locale locale)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, locale);
        sdf.setTimeZone(timeZone);
        return sdf.format(date);
    }

    /**
     * Add time to a specific date.
     * @param date the date to add time to
     * @param calendarField The calendar field (e.g. Calendar.Day,
     *            Calendar.Hour) which increase the value.
     * @param valueToAdd the number to Add the selected calendar field.
     * @return The date object calculated
     */
    public Date datePlusTime(final Date date, final int calendarField,
        final int valueToAdd)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(calendarField, valueToAdd);
        return cal.getTime();
    }

    /**
     * Add time to actual time.
     * @param calendarField The calendar field (e.g. Calendar.Day,
     *            Calendar.Hour) which increase the value.
     * @param valueToAdd the number to Add the selected calendar field.
     * @return The date object according to the given added time
     */
    public Date nowPlusTime(final int calendarField, final int valueToAdd)
    {
        return datePlusTime(now(), calendarField, valueToAdd);
    }

    /**
     * Add time to the actual time in the specified format by using
     * SimpleDateFormat and Calendar.
     * @param dateFormat the format string of the returned time text. (e.g.
     *            "yyyy-MM-dd HH:mm:ss")
     * @param calendarField The calendar field (e.g. Calendar.Day,
     *            Calendar.Hour) which increase the value.
     * @param valueToAdd the number to Add the selected calendar field.
     * @param timeZone the time zone of the current time
     * @return The date formatted according to the given dateFormat
     */
    public String
        nowPlusTime(final String dateFormat, final int calendarField,
            final int valueToAdd, final TimeZone timeZone)
    {
        return dateToString(
            datePlusTime(now(), calendarField, valueToAdd),
            dateFormat,
            timeZone);
    }

    /**
     * Gives a time stamp in the format of 'YYMMddHHmmss'.
     * @return The time stamp string.
     */
    public String getTimeStamp()
    {
        return dateToString(
            now(Calendar.SECOND),
            "yyMMddHHmmss",
            TimeZone.getDefault());
    }

    public String generateOneRandomLetter()
    {
        final Random randomGenerator = new Random();
        final String validCharacters =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int randIndex = 0;

        randIndex = randomGenerator.nextInt(validCharacters.length());

        final String randomLetter =
            validCharacters.substring(randIndex, randIndex + 1);

        return randomLetter;
    }

    public String generateOneRandomAccent()
    {
        final Random randomGenerator = new Random();
        final String validCharacters = ",?:-;'+!%=()|<>*";
        int randIndex = 0;

        randIndex = randomGenerator.nextInt(validCharacters.length());

        final String randomAccent =
            validCharacters.substring(randIndex, randIndex + 1);

        return randomAccent;
    }

    public static void moveFileAndReplace(final File target,
        final File destination)
    {
        if (destination.exists())
        {
            Assert.assertTrue(destination.delete(), "Could not delete '"
                + destination.getAbsolutePath() + "'.");
        }
        Assert.assertTrue(
            target.renameTo(destination),
            "Could not move '" + target.getAbsolutePath() + "' to '"
                + destination.getAbsolutePath() + "'.");
    }
}
