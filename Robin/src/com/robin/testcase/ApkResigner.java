/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.testcase;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.safs.android.auto.lib.AndroidTools;
import org.safs.android.auto.lib.Process2;
import org.testng.Assert;

import com.robin.BaseFunctionality;
import com.robin.reporter.Reporter;
import com.robin.utilities.config.ConfigParams;

import de.troido.resigner.controll.ResignerLogic;

class ApkResigner
{
    static synchronized void resignAUT(final File autFile)
    {
        String signedAutFullPath = getReSignedAUT(autFile).getAbsolutePath();
        if (!reSignedApkExists(signedAutFullPath))
        {
            String apkAbsolutePath = autFile.getAbsolutePath();
            String normalizedPath = FilenameUtils.normalize(apkAbsolutePath);
            Reporter.log("Resign '" + normalizedPath + "', to create '"
                + signedAutFullPath + "'.", true);
            // Set environment for Resigner class.
            ResignerLogic.checkEnvironment();
            try
            {
                ResignerLogic.resign(apkAbsolutePath, signedAutFullPath);
            } catch (Exception e)
            {
                Assert.fail("FAILED TO RESIGN " + normalizedPath);
            }
        } else
        {
            Reporter.log("Resigned '" + signedAutFullPath + "' found.", true);
        }
    }

    private static boolean reSignedApkExists(final String signedAutFullPath)
    {
        return new File(signedAutFullPath).exists();
    }

    private static String getReSignedAutName(final File autFile)
    {
        String autName = autFile.getName();
        String autFileCanonical = null;
        try
        {
            autFileCanonical = autFile.getCanonicalPath();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        String newAutNameFolderPart = getUnderLinedPath(autFileCanonical);
        String newAutNameFileNamePart = FilenameUtils.getBaseName(autName);
        String newAutName = newAutNameFolderPart + "_" + newAutNameFileNamePart;
        String newAutNameWithExtension =
            newAutName + "_" + getAutVersion(autFile) + "."
                + FilenameUtils.getExtension(autName);
        return newAutNameWithExtension;
    }

    private static String getUnderLinedPath(final String autFileCanonical)
    {
        return FilenameUtils
            .getPathNoEndSeparator(autFileCanonical)
            .replaceAll("[\\\\/]", "_");
    }

    private static String getResignedAutFolder()
    {
        File resignedAutFolder =
            new File(BaseFunctionality.config().getValue(
                ConfigParams.RESIGNED_AUT_PATH));
        return resignedAutFolder.getAbsolutePath();
    }

    static synchronized File getReSignedAUT(final File autFile)
    {
        return new File(getResignedAutFolder() + File.separator
            + getReSignedAutName(autFile));
    }

    private static String getAutVersion(final File autFile)
    {
        AndroidTools androidTools = AndroidTools.get();
        String versionCode = "";
        Process2 process = null;
        try
        {
            StringBuffer strBuffer = new StringBuffer();
            process =
                androidTools
                    .aapt("dumb", "badging", autFile.getAbsolutePath());
            process.connectStdout(strBuffer).waitForSuccess();

            int versionCodePosition = strBuffer.indexOf("versionCode");
            int versionControlEndPosition =
                strBuffer.indexOf(" ", versionCodePosition);
            versionCode =
                splitVersionCode(strBuffer.substring(
                    versionCodePosition,
                    versionControlEndPosition));
        } catch (InterruptedException e)
        {
            versionCode = "NotSet";
        } catch (IOException e)
        {
            versionCode = "NotSet";
        }
        return versionCode;
    }

    private static String splitVersionCode(final String substring)
    {
        String[] versionCodeParts = substring.split("'");
        return versionCodeParts[1];
    }
}
