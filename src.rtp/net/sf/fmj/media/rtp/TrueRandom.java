// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media.rtp;

import java.security.SecureRandom;
import java.util.*;

/** Wrapper around {@link SecureRandom} to save the seeding cost. */
public final class TrueRandom
{
    private static Random random;

    static
    {
        random = new SecureRandom();
    }

    public static long rand()
    {
        return random.nextLong();
    }

    public TrueRandom()
    {
    }
}
