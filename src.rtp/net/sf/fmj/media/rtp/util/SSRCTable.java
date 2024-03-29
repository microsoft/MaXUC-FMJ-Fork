// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media.rtp.util;

import java.util.*;

public class SSRCTable<T>
{
    static final int INCR = 16;
    int[] ssrcList;
    Object[] objList;
    int total;

    public SSRCTable()
    {
        ssrcList = new int[16];
        objList = new Object[16];
        total = 0;
    }

    public synchronized Enumeration<T> elements()
    {
        return new Enumeration<T>()
        {
            private int next = 0;

            @Override
            public boolean hasMoreElements()
            {
                return next < total;
            }

            @Override
            public T nextElement()
            {
                synchronized (SSRCTable.this)
                {
                    if (next < total)
                    {
                        @SuppressWarnings("unchecked")
                        T t = (T) objList[next++];

                        return t;
                    }
                }
                throw new NoSuchElementException("SSRCTable Enumeration");
            }
        };
    }

    public synchronized T get(int ssrc)
    {
        int i = indexOf(ssrc);

        if (i < 0)
        {
            return null;
        }
        else
        {
            @SuppressWarnings("unchecked")
            T t = (T) objList[i];

            return t;
        }
    }

    public synchronized int getSSRC(T obj)
    {
        for (int i = 0; i < total; i++)
            if (objList[i] == obj)
                return ssrcList[i];

        return 0;
    }

    private int indexOf(int ssrc)
    {
        if (total <= 3)
        {
            if (total > 0 && ssrcList[0] == ssrc)
                return 0;
            if (total > 1 && ssrcList[1] == ssrc)
                return 1;
            return total <= 2 || ssrcList[2] != ssrc ? -1 : 2;
        }
        if (ssrcList[0] == ssrc)
            return 0;
        if (ssrcList[total - 1] == ssrc)
            return total - 1;
        int i = 0;
        int j = total - 1;
        do
        {
            int x = (j - i) / 2 + i;
            if (ssrcList[x] == ssrc)
                return x;
            if (ssrc > ssrcList[x])
                i = x + 1;
            else if (ssrc < ssrcList[x])
                j = x;
        } while (i < j);
        return -1;
    }

    public boolean isEmpty()
    {
        return total == 0;
    }

    public synchronized void put(int ssrc, T obj)
    {
        if (total == 0)
        {
            ssrcList[0] = ssrc;
            objList[0] = obj;
            total = 1;
            return;
        }
        int i;
        for (i = 0; i < total; i++)
        {
            if (ssrcList[i] < ssrc)
                continue;
            if (ssrcList[i] == ssrc)
            {
                objList[i] = obj;
                return;
            }
            break;
        }

        int[] sl = ssrcList;
        Object[] ol = objList;

        if (total == ssrcList.length)
        {
            sl = new int[ssrcList.length + INCR];
            ol = new Object[objList.length + INCR];
        }

        if (ssrcList != sl && i > 0)
        {
            System.arraycopy(ssrcList, 0, sl, 0, i);
            System.arraycopy(objList, 0, ol, 0, i);
        }

        if (i < total)
        {
            System.arraycopy(ssrcList, i, sl, i + 1, total - i);
            System.arraycopy(objList, i, ol, i + 1, total - i);
        }

        ssrcList = sl;
        objList = ol;

        ssrcList[i] = ssrc;
        objList[i] = obj;
        total++;
    }

    public synchronized T remove(int ssrc)
    {
        int i;
        if ((i = indexOf(ssrc)) < 0)
            return null;

        @SuppressWarnings("unchecked")
        T res = (T) objList[i];

        for (; i < total - 1; i++)
        {
            ssrcList[i] = ssrcList[i + 1];
            objList[i] = objList[i + 1];
        }

        ssrcList[total - 1] = 0;
        objList[total - 1] = null;
        total--;
        return res;
    }

    public synchronized void removeAll()
    {
        for (int i = 0; i < total; i++)
        {
            ssrcList[i] = 0;
            objList[i] = null;
        }

        total = 0;
    }

    public synchronized void removeObj(T obj)
    {
        if (obj == null)
            return;
        int i;
        for (i = 0; i < total; i++)
            if (objList[i] == obj)
                break;

        if (i >= total)
            return;
        for (; i < total - 1; i++)
        {
            ssrcList[i] = ssrcList[i + 1];
            objList[i] = objList[i + 1];
        }

        ssrcList[total - 1] = 0;
        objList[total - 1] = null;
        total--;
    }

    public int size()
    {
        return total;
    }
}
