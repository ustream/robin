/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.utilities.email;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import org.testng.Assert;

public class EMail
{
    /**
     * The sender address as string.
     */
    private String from;
    /**
     * The first recepient as string.
     */
    private String recepient;
    /**
     * The message subject.
     */
    private String subject;
    /**
     * The message body text.
     */
    private String bodyText;
    /**
     * Indicating whether the body is html or not.
     */
    private boolean textIsHtml = false;

    /**
     * Represents an email message down loaded from a mailbox for later
     * reference. Stores the main fields of the message to be used after the
     * connection to the mail server is closed.
     * @param message javaMail message object
     */
    public EMail(final Message message)
    {
        try
        {
            from = message.getFrom()[0].toString();
            recepient = message.getRecipients(RecipientType.TO)[0].toString();
            subject = message.getSubject();
            bodyText = getText(message);
        } catch (MessagingException e)
        {
            Assert.fail(e.getMessage());
        } catch (IOException e)
        {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Return the primary text content of the message.
     * @param p Message or its part
     * @return the main body text of the part
     * @throws MessagingException when connection problem occurs
     * @throws IOException when connection problem occurs
     */
    private String getText(final Part p) throws MessagingException,
        IOException
    {
        if (p.isMimeType("text/*"))
        {
            String s = (String) p.getContent();
            textIsHtml = p.isMimeType("text/html");
            return s;
        }

        if (p.isMimeType("multipart/alternative"))
        {
            // prefer html text over plain text
            Multipart mp = (Multipart) p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++)
            {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain"))
                {
                    if (text == null)
                    {
                        text = getText(bp);
                    }
                    continue;
                } else if (bp.isMimeType("text/html"))
                {
                    String s = getText(bp);
                    if (s != null)
                    {
                        return s;
                    }
                } else
                {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*"))
        {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++)
            {
                String s = getText(mp.getBodyPart(i));
                if (s != null)
                {
                    return s;
                }
            }
        }
        return null;
    }

    public String getFrom()
    {
        return from;
    }

    public String getRecepient()
    {
        return recepient;
    }

    public String getSubject()
    {
        return subject;
    }

    public String getBodyText()
    {
        return bodyText;
    }

    public boolean isTextHtml()
    {
        return textIsHtml;
    }
}
