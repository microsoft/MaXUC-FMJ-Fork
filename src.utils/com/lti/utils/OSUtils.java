// Portions (c) Microsoft Corporation. All rights reserved.
package com.lti.utils;

import java.util.logging.*;

/**
 * Helper functions to determine which OS we are running.
 *
 * @author Ken Larson
 *
 */
public final class OSUtils
{
    @SuppressWarnings("deprecation")
    private static final Logger logger = Logger.global;

    public static final boolean isMacOSX()
    {
        return System.getProperty("os.name").equals("Mac OS X");
    }

    public static final boolean isWindows()
    {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private OSUtils()
    {
        super();
        logger.fine("OS: " + System.getProperty("os.name"));
    }
}
