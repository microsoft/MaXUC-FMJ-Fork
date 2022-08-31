package com.sun.media.controls;

import java.awt.*;
import java.awt.event.*;

import javax.media.control.*;

import com.sun.media.ui.*;

/**
 * TODO: Stub
 *
 * @author Ken Larson
 *
 */
public class BitRateAdapter implements BitRateControl, ActionListener
{
    protected int min;
    protected int max;
    protected int value;
    protected boolean settable;
    protected final TextComp textComp;

    public BitRateAdapter(int value, int min, int max, boolean settable)
    {
        super();
        this.value = value;
        this.min = min;
        this.max = max;
        this.settable = settable;
        this.textComp = new TextComp(); // TODO - implement this class
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public int getBitRate()
    {
        return value;
    }

    @Override
    public Component getControlComponent()
    {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public int getMaxSupportedBitRate()
    {
        return max;
    }

    @Override
    public int getMinSupportedBitRate()
    {
        return min;
    }

    @Override
    public int setBitRate(int bitrate)
    {
        throw new UnsupportedOperationException(); // TODO
    }
}
