package net.sf.fmj.media;

import javax.media.*;

/**
 * Abstract implementation of PlugIn, useful for subclassing.
 *
 * @author Ken Larson
 *
 */
public abstract class AbstractPlugIn extends AbstractControls implements PlugIn
{
    private boolean opened = false;

    @Override
    public void close()
    {
        opened = false;
    }

    @Override
    public String getName()
    {
        return getClass().getSimpleName(); // override to provide a better name
    }

    @Override
    public void open() throws ResourceUnavailableException
    {
        opened = true;
    }

    @Override
    public void reset()
    { // TODO
    }
}
