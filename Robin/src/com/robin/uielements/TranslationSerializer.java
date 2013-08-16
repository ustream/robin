/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.uielements;

import java.io.File;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TranslationSerializer
{
    private static final String TEXT_ELEMENT = "Text";

    /**
     * Deserializer for the xmlTL elements.
     * @param file The xmls to deserialize
     * @param baseFolder the base folder to relativize path
     * @throws XmlReadErrorException
     * @return An serialized TLElement instance
     */
    public TranslationFile deserialize(final File baseFolder,
        final File file)
    {
        String filePath = baseFolder.toURI().relativize(file.toURI()).getPath();
        TranslationFile tlActivity = new TranslationFile();
        try
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();

            tlActivity.setFileName(file.getName());
            tlActivity.setPath(filePath);

            NodeList textElements =
                doc.getElementsByTagName(TEXT_ELEMENT);

            for (int s = 0; s < textElements.getLength(); s++)
            {
                Node textNode = textElements.item(s);
                if (textNode.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element textNodeElement = (Element) textNode;
                    String elementName = textNodeElement.getAttribute("name");
                    TranslationElement tlElement =
                        new TranslationElement(elementName);
                    NodeList languages = textNodeElement.getChildNodes();

                    for (int j = 0; j < languages.getLength(); j++)
                    {
                        Node lang = languages.item(j);
                        if (lang.getNodeType() == Node.ELEMENT_NODE)
                        {
                            String tag = lang.getNodeName();
                            String[] tagParts = tag.split("_");
                            String value = lang.getTextContent();
                            tlElement.addTranslation(new Locale(
                                tagParts[0],
                                tagParts[1]), value);
                        }
                    }
                    tlActivity.addElement(tlElement);
                }
            }
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return tlActivity;
    }
}
