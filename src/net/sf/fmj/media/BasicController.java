// Portions (c) Microsoft Corporation. All rights reserved.
package net.sf.fmj.media;

import java.util.*;

import javax.media.*;

import net.sf.fmj.media.util.*;

/**
 * Media Controller implements the basic functionalities of a
 * java.media.Controller. These include:<
 * <ul>
 * <li>The clock calculations using the BasicClock helper class.
 * <li>The RealizeWorkThread and PrefetchWorkThread to implement realize() and
 * prefetch() in the correct unblocking manner.
 * <li>The ListenerList to maintain the list of ControllerListener.
 * <li>Two ThreadedEventQueues for incoming and outgoing ControllerEvents.
 * </ul>
 * <p>
 *
 * @version 1.9, 98/11/19
 */
public abstract class BasicController implements Controller, Duration
{
    private int targetState = Unrealized;
    protected int state = Unrealized;
    private Vector<ControllerListener> listenerList = null;
    private SendEventQueue sendEvtQueue;
    private ConfigureWorkThread configureThread = null;
    private RealizeWorkThread realizeThread = null;
    private PrefetchWorkThread prefetchThread = null;
    protected String processError = null;
    private Clock clock; // Use the BasicClock to keep track of time
    // and for some calculations.
    private TimedStartThread startThread = null;
    private StopTimeThread stopTimeThread = null;
    private boolean interrupted = false;
    private Object interruptSync = new Object();

    static final int Configuring = Processor.Configuring;
    static final int Configured = Processor.Configured;

    protected boolean stopThreadEnabled = true;

    /**
     * Set the timebase used by the controller. i.e. all media-time to
     * time-base-time will be computed with the given time base. The subclass
     * should implement and further specialized this method to do the real work.
     * But it should also invoke this method to maintain the correct states.
     *
     * @param tb
     *            the time base to set to.
     * @exception IncompatibleTimeBaseException
     *                is thrown if the Controller cannot accept the given time
     *                base.
     */
    static String TimeBaseError = "Cannot set time base on an unrealized controller.";

    /**
     * Start the controller. Invoke clock.start() to maintain the clock states.
     * It starts a wait thread to wait for the given tbt. At tbt, it will wake
     * up and call doStart(). A subclass should implement the doStart() method
     * to do the real work.
     *
     * @param tbt
     *            the timebase time to start the controller.
     */
    static String SyncStartError = "Cannot start the controller before it has been prefetched.";

    /**
     * Set the stop time. Invoke clock.setStopTime() to maintain the clock
     * states. The subclass should implement and further specialized this method
     * to do the real work. But it should also invoke this method to maintain
     * the correct states.
     *
     * @param t
     *            the time to stop.
     */
    static String StopTimeError = "Cannot set stop time on an unrealized controller.";

    /**
     * Set the media time. Invoke clock.setMediaTime() to maintain the clock
     * states. The subclass should implement and further specialized this method
     * to do the real work. But it should also invoke this method to maintain
     * the correct states.
     *
     * @param now
     *            the media time to set to.
     */
    static String MediaTimeError = "Cannot set media time on a unrealized controller";

    /**
     * Get the current time base.
     *
     * @return the current time base.
     */
    static String GetTimeBaseError = "Cannot get Time Base from an unrealized controller";

    /**
     * Set the rate of presentation: 1.0: normal, 2.0: twice the speed. -2.0:
     * twice the speed in reverse. Note that not all rates are supported.
     * Invokes clock.setRate() to maintain the clock states. The subclass
     * SHOULDN'T in general override this class. If necessary, it should
     * override the behavior using the doSetRate method. By overriding the
     * doSetRate method, subclass Conrollers can support negative rates,
     * fractional rates etc., but they should guard against illegal rates from
     * going into the clock calculations.
     *
     * @param factor
     *            the rate to set to.
     * @return the actual rate used.
     */
    static String SetRateError = "Cannot set rate on an unrealized controller.";

    /**
     * Returns the start latency. Don't know until the particular node is
     * implemented.
     *
     * @return the start latency.
     */
    static String LatencyError = "Cannot get start latency from an unrealized controller";

    /**
     * Release the resouces held by the controller. It obeys strict rules as
     * specified in the spec. Implement the abortRealize and abortPrefetch
     * methods to actually do the work.
     *
     * This is a blocking call. It returns only when the Controller has done
     * deallocating the resources. This should be called from an external thread
     * outside of the controller. Take caution if this is being call from inside
     * the Controller. It may cause deadlock.
     */
    static String DeallocateError = "deallocate cannot be used on a started controller.";

    public BasicController()
    {
        sendEvtQueue = new SendEventQueue(this);
        sendEvtQueue.setName(sendEvtQueue.getName() + ": SendEventQueue: "
                + this.getClass().getName());
        sendEvtQueue.start();
        clock = new BasicClock();
        Log.comment("Engine init state is: " + state);
    }

    /**
     * Called when the configure() is aborted, i.e. deallocate() was called
     * while realizing. Release all resources claimed previously by the
     * configure() call. Override this to implement subclass behavior.
     */
    protected void abortConfigure()
    {
    }

    /**
     * Called when the prefetch() is aborted, i.e. deallocate() was called while
     * prefetching. Release all resources claimed previously by the prefetch
     * call. Override this to implement subclass behavior.
     */
    protected abstract void abortPrefetch();

    /**
     * Called when the realize() is aborted, i.e. deallocate() was called while
     * realizing. Release all resources claimed previously by the realize()
     * call. Override this to implement subclass behavior.
     */
    protected abstract void abortRealize();

    // Return true if the stop time has already passed
    private boolean activateStopThread(long timeToStop)
    {
        if (getStopTime().getNanoseconds() == Long.MAX_VALUE)
            return false;

        if (stopTimeThread != null && stopTimeThread.isAlive())
        {
            stopTimeThread.abort();
            stopTimeThread = null;
        }

        if (timeToStop > 100000000)
        {
            (stopTimeThread = new StopTimeThread(this, timeToStop)).start();
            return false;
        } else
        {
            return true;
        }
    }

    /**
     * Add a listener to the listenerList. This listener will be notified of
     * this controller's event. This needs to be a synchronized method so as to
     * maintain the integrity of the listenerList.
     */
    @Override
    public final void addControllerListener(ControllerListener listener)
    {
        if (listenerList == null)
        {
            listenerList = new Vector<>();
        }

        synchronized (listenerList)
        {
            if (!listenerList.contains(listener))
            {
                listenerList.addElement(listener);
            }
        }
    }

    // Return timeToStop
    private long checkStopTime()
    {
        long stopTime = getStopTime().getNanoseconds();

        if (stopTime == Long.MAX_VALUE)
            return 1;

        return (long) ((stopTime - getMediaTime().getNanoseconds()) / getRate());
    }

    /**
     * A subclass of this implement close to stop all threads to make it
     * "finalizable", i.e., ready to be garbage collected.
     */
    @Override
    public final void close()
    {
        doClose();

        // Interrupt the controller. If it's in any of the state
        // transition threads, the threads will be interrupted and
        // that transition will be aborted.
        interrupt();

        if (startThread != null)
            startThread.abort();

        if (stopTimeThread != null)
            stopTimeThread.abort();

        if (sendEvtQueue != null)
        {
            sendEvtQueue.kill();
            sendEvtQueue = null;
        }
    }

    /**
     * Called when the controller is realized and when all the
     * ConfigureCompleteEvents from down stream Controllers have been received.
     * If a subclass wants to override this method, it should still invoke this
     * to ensure the correct events being sent to the upstream Controllers.
     */
    protected synchronized void completeConfigure()
    {
        state = setAndLogEngineChange(Configured);

        // We need any listeners to finish processing hitting the Configured state before moving on to
        // realize, so process this ConfiguredCompleteEvent synchronously.
        dispatchEvent(new ConfigureCompleteEvent(this, Configuring, Configured, getTargetState()));

        if (getTargetState() >= Realized)
        {
            realize();
        }
    }

    /**
     * Called when the controller is prefetched and when all the
     * PrefetchCompleteEvents from down stream nodes have been received. If a
     * subclass wants to override this method, it should still invoke this to
     * ensure the correct events being sent to the upstream Controllers.
     */
    protected/* synchronized */void completePrefetch()
    {
        // Send back the events to whoever is listening, most likely the
        // upstream nodes.
        clock.stop();
        state = setAndLogEngineChange(Prefetched);
        sendEvent(new PrefetchCompleteEvent(this, Prefetching, Prefetched,
                getTargetState()));
    }

    /**
     * Called when the controller is realized and when all the
     * RealizeCompleteEvents from down stream Controllers have been received. If
     * a subclass wants to override this method, it should still invoke this to
     * ensure the correct events being sent to the upstream Controllers.
     */
    protected synchronized void completeRealize()
    {
        // Send back the events to whoever is listening, most likely the
        // upstream nodes.
        state = setAndLogEngineChange(Realized);
        sendEvent(new RealizeCompleteEvent(this, Realizing, Realized,
                getTargetState()));

        if (getTargetState() >= Prefetched)
        {
            prefetch();
        }
    }

    public synchronized void configure()
    {
        if (getTargetState() < Configured)
            setTargetState(Configured);

        switch (state)
        {
        case Configured:
        case Realizing:
        case Realized:
        case Prefetching:
        case Prefetched:
        case Started:
            sendEvent(new ConfigureCompleteEvent(this, state, state,
                    getTargetState()));
            break;
        case Configuring:
            break;
        case Unrealized:
            state = setAndLogEngineChange(Configuring);
            sendEvent(new TransitionEvent(this, Unrealized, Configuring,
                    getTargetState()));

            configureThread = new ConfigureWorkThread(this);
            configureThread.setName(configureThread.getName() + "[ " + this
                    + " ]" + " ( configureThread)");

            configureThread.start();
        }
    }

    @Override
    public final void deallocate()
    {
        int previousState = getState();
        if (state == Started)
        {
            // It's illegal to use deallocate on a started controller, but we
            // can handle this rather than falling over.
            Log.annotate(this, "Deallocate called on started controller");
            Log.dumpStack(new ClockStartedError(DeallocateError));
            this.stop();
        }

        // stop the thread even if isAlive() is false as
        // we want to kill the thread that has been created
        // but not scheduled to run

        switch (state)
        {
        case Configuring:
        case Realizing:
            interrupt();
            state = setAndLogEngineChange(Unrealized);
            break;
        case Prefetching:
            interrupt();
            state = setAndLogEngineChange(Realized);
            break;
        case Prefetched:
            abortPrefetch();
            state = setAndLogEngineChange(Realized);
            resetInterrupt();
            break;
        }

        setTargetState(state);

        // Use by subclass to add in its own behavior.
        doDeallocate();

        // Wait for the interrupt to take effect.
        synchronized (interruptSync)
        {
            while (isInterrupted())
            {
                try
                {
                    interruptSync.wait();
                } catch (InterruptedException e)
                {
                }
            }
        }

        sendEvent(new DeallocateEvent(this, previousState, state, state,
                getMediaTime()));
    }

    /**
     * For debug purposes, it's useful to log any changes of engine state so we
     * can trace the source of a configuration error. So we simply call this
     * method to log the state change and cause.
     *
     * NOTE: This method is also present in AbstractController and the two
     * should be updated together.
     *
     * @param newState The state the engine is changing to
     * @return The state the engine is changing to
     */
    protected synchronized int setAndLogEngineChange(int newState)
    {
        // Use thread information to find out what method called the change of
        // state. This should provide the necessary debug information. The third
        // line of trace is the call to this method, hence getStackTrace()[2].
        String callLocation = Thread.currentThread().getStackTrace()[2].toString();

        Log.comment("Change of engine state " + state + " to " + newState +
                     " with target state " + targetState + " at " + callLocation);
        return newState;
    }

    /**
     * An internal function to notify the listeners on the listener list the
     * given event. This gets called by the sendEvtQueue's processEvent()
     * callback. This method updates a lock on the Vector listenerList before
     * enumerating it.
     */
    protected final void dispatchEvent(ControllerEvent evt)
    {
        if (listenerList == null)
            return;

        synchronized (listenerList)
        {
            Enumeration<ControllerListener> list = listenerList.elements();
            while (list.hasMoreElements())
            {
                ControllerListener listener = (ControllerListener) list
                        .nextElement();
                listener.controllerUpdate(evt);
            }
        }
    }

    /**
     * Invoked by close() to cleanup the Controller. Override this to implement
     * subclass behavior.
     */
    protected void doClose()
    {
    }

    /**
     * The stub function to perform the steps to configure the controller. Call
     * configure() for the public method.
     */
    protected boolean doConfigure()
    {
        return true;
    }

    /**
     * Called by deallocate(). Subclasses should implement this for its specific
     * behavior.
     */
    protected void doDeallocate()
    {
    }

    /**
     * Called when realize() has failed.
     */
    protected void doFailedConfigure()
    {
        state = setAndLogEngineChange(Unrealized);
        setTargetState(Unrealized);
        String msg = "Failed to configure";
        if (processError != null)
            msg = msg + ": " + processError;
        sendEvent(new ResourceUnavailableEvent(this, msg));
        processError = null;
    }

    /**
     * Called when the prefetch() has failed.
     */
    protected void doFailedPrefetch()
    {
        state = setAndLogEngineChange(Realized);
        setTargetState(Realized);
        String msg = "Failed to prefetch";
        if (processError != null)
            msg = msg + ": " + processError;
        sendEvent(new ResourceUnavailableEvent(this, msg));
        processError = null;
    }

    /**
     * Called when realize() has failed.
     */
    protected void doFailedRealize()
    {
        state = setAndLogEngineChange(Unrealized);
        setTargetState(Unrealized);
        String msg = "Failed to realize";
        if (processError != null)
            msg = msg + ": " + processError;
        sendEvent(new ResourceUnavailableEvent(this, msg));
        processError = null;
    }

    /**
     * The stub function to perform the steps to prefetch the controller. Call
     * prefetch for the public method. This is called from a separately running
     * thread. So do take necessary precautions to protect shared resources.
     * It's OK to put an empty stub function body here.
     * <p>
     * Return true if the prefetch is successful. Return false and set the
     * processError string if failed.
     * <p>
     * This function is not declared synchronized because first it is already
     * guaranteed by realize() not to be called more than once simultaneously.
     * Secondly if this is synchronized, then other synchronized methods,
     * deallocate() and processEvent() will be blocked since they are
     * synchronized methods. Override this to implement subclass behavior.
     *
     * @return true if successful.
     */
    protected abstract boolean doPrefetch();

    /**
     * The stub function to perform the steps to realize the controller. Call
     * realize() for the public method. This is called from a separately running
     * thread. So do take necessary precautions to protect shared resources.
     * It's OK to put an empty stub function body here.
     * <p>
     * Return true if the realize is successful. Return false and set the
     * processError string if failed.
     * <p>
     * This function is not declared synchronized because first it is already
     * guaranteed by realize() not to be called more than once simultaneously.
     * Secondly if this is synchronized, then other synchronized methods,
     * deallocate() and processEvent() will be blocked since they are
     * synchronized methods. Override this to implement subclass behavior.
     *
     * @return true if successful.
     */
    protected abstract boolean doRealize();

    protected void doSetMediaTime(Time when)
    {
    }

    // Override this to implement subclass behavior.
    // Conrollers can override this method if they
    // can support negative rates, and/or have other
    // other restrictions.
    protected float doSetRate(float factor)
    {
        return factor;
    }

    /**
     * Start immediately. Invoked from start(tbt) when the scheduled start time
     * is reached. Use the public start(tbt) method for the public interface.
     * Override this to implement subclass behavior.
     */
    protected abstract void doStart();

    /**
     * Invoked from stop(). Override this to implement subclass behavior.
     */
    protected void doStop()
    {
    }

    protected Clock getClock()
    {
        return clock;
    }

    /**
     * Get the <tt>Control</tt> that supports the class or interface specified.
     * The full class or interface name should be specified. <tt>Null</tt> is
     * returned if the <tt>Control</tt> is not supported.
     *
     * @return <tt>Control</tt> for the class or interface name.
     */
    @Override
    public Control getControl(String type)
    {
        Class<?> cls;
        try
        {
            cls = Class.forName(type);
        }
        catch (ClassNotFoundException e)
        {
            return null;
        }

        Control[] controls = getControls();
        for (Control c : controls)
        {
            if (cls.isInstance(c))
                return c;
        }
        return null;
    }

    /**
     * Return a list of <b>Control</b> objects this <b>Controller</b> supports.
     * If there are no controls, then an array of length zero is returned.
     *
     * @return list of <b>Controller</b> controls.
     */
    @Override
    public Control[] getControls()
    {
        // Not implemented $$$
        // Is this correct ? $$$
        return new Control[0];
    }

    /**
     * Return the duration of the media. It's unknown until we implement a
     * particular node.
     *
     * @return the duration of the media.
     */
    @Override
    public Time getDuration()
    {
        return Duration.DURATION_UNKNOWN;
    }

    /**
     * Get the current media time in nanoseconds.
     *
     * @return the media time in nanoseconds.
     */
    @Override
    public long getMediaNanoseconds()
    {
        return clock.getMediaNanoseconds();
    }

    /**
     * Return the current media time. Uses the clock to do the computation. A
     * subclass can override this method to do the right thing for itself.
     *
     * @return the current media time.
     */
    @Override
    public Time getMediaTime()
    {
        return clock.getMediaTime();
    }

    /**
     * Get the current presentation speed.
     *
     * @return the current presentation speed.
     */
    @Override
    public float getRate()
    {
        return clock.getRate();
    }

    @Override
    public Time getStartLatency()
    {
        if (state < Realized)
        {
            throwError(new NotRealizedError(LatencyError));
        }
        return LATENCY_UNKNOWN;
    }

    /**
     * Get the current state of the controller.
     *
     * @return the current state of the controller.
     */
    @Override
    public final int getState()
    {
        return state;
    }

    /**
     * Get the preset stop time.
     *
     * @return the preset stop time.
     */
    @Override
    public Time getStopTime()
    {
        return clock.getStopTime();
    }

    /**
     * Return the Sync Time. Not yet implementated.
     */
    @Override
    public Time getSyncTime()
    {
        return new Time(0);
    }

    /**
     * Get the current target state.
     *
     * @return the current target state.
     */
    @Override
    public final int getTargetState()
    {
        return targetState;
    }

    @Override
    public TimeBase getTimeBase()
    {
        if (state < Realized)
        {
            throwError(new NotRealizedError(GetTimeBaseError));
        }

        return clock.getTimeBase();
    }

    /**
     * Interrupt the process.
     */
    protected void interrupt()
    {
        synchronized (interruptSync)
        {
            interrupted = true;
            interruptSync.notify();
        }
    }

    /**
     * Subclass should define this. If this return true, the controller will go
     * through the Configured state. For example, the Controller implementing
     * the Processor should return true.
     */
    protected abstract boolean isConfigurable();

    /**
     * Return true if the process is interrupted.
     */
    protected boolean isInterrupted()
    {
        return interrupted;
    }

    /**
     * Map the given media-time to time-base-time.
     *
     * @param t
     *            given media time.
     * @return timebase time.
     * @exception ClockStoppedException
     *                thrown if the Controller has already been stopped.
     */
    @Override
    public Time mapToTimeBase(Time t) throws ClockStoppedException
    {
        return clock.mapToTimeBase(t);
    }

    /**
     * Take the necessary steps to prefetch the controller. This is a
     * non-blocking call. It starts a work thread to do the real work. The
     * actual code to do the realizing should be written in doPrefetch(). The
     * thread is also responsible for catching all the PrefetchCompleteEvents
     * from the down stream nodes. When the steps to prefetch the controller are
     * completed and when all the PrefetchCompleteEvents from down stream nodes
     * have been received, the completePrefetch() call will be invoked.
     */
    @Override
    public final/* synchronized */void prefetch()
    {
        if (getTargetState() <= Realized)
            setTargetState(Prefetched);
        switch (state)
        {
        case Prefetched:
        case Started:
            sendEvent(new PrefetchCompleteEvent(this, state, state,
                    getTargetState()));
            break;
        case Configuring:
        case Realizing:
        case Prefetching:
            break; // $$ Nothing is done.
        case Unrealized:
        case Configured:
            // The controller is not realized yet, we have to implicitly
            // carry out a realize().
            realize();
            break;
        case Realized:
            // Put it in the prefetching state.
            // synchronized(this) {
            state = setAndLogEngineChange(Prefetching);
            sendEvent(new TransitionEvent(this, Realized, Prefetching,
                    getTargetState()));

            // Start the prefetch thread for this controller.
            prefetchThread = new PrefetchWorkThread(this);
            prefetchThread.setName(prefetchThread.getName()
                    + " ( prefetchThread)");
            prefetchThread.start();
            break;
        }
    }

    /**
     * Take the necessary steps to realize the controller. This is a
     * non-blocking call. It starts a work thread to do the real work. The
     * actual code to do the realizing should be written in doRealize(). The
     * thread is also responsible for catching all the RealizeCompleteEvents
     * from the down stream nodes. When the steps to realize the controller are
     * completed and when all the RealizeCompleteEvents from down stream nodes
     * have been received, the completeRealize() call will be invoked.
     */
    @Override
    public final synchronized void realize()
    {
        if (getTargetState() < Realized)
            setTargetState(Realized);

        switch (state)
        {
        case Realized:
        case Prefetching:
        case Prefetched:
        case Started:
            sendEvent(new RealizeCompleteEvent(this, state, state,
                    getTargetState()));
            break;
        case Realizing:
        case Configuring:
            break; // $$ Nothing is done. Two realize() will result in one event
        case Unrealized:
            // For processors, we'll implicitly call configure.
            if (isConfigurable())
            {
                configure();
                break;
            }
        case Configured:
            // Put it in the realizing state.
            int oldState = state;
            state = setAndLogEngineChange(Realizing);
            sendEvent(new TransitionEvent(this, oldState, Realizing,
                    getTargetState()));

            // Start the realize thread for this controller.
            realizeThread = new RealizeWorkThread(this);
            realizeThread.setName(realizeThread.getName() + "[ " + this + " ]"
                    + " ( realizeThread)");

            realizeThread.start();
            break;
        }
    }

    /**
     * Remove a listener from the listener list. The listener will stop
     * receiving notification from this controller. This needs to be a
     * synchronized method so as to maintain the integrity of the listenerList.
     */
    @Override
    public final void removeControllerListener(ControllerListener listener)
    {
        if (listenerList == null)
            return;
        synchronized (listenerList)
        {
            if (listenerList != null)
            {
                listenerList.removeElement(listener);
            }
        }
    }

    /**
     * Reset the interrupted flag.
     */
    protected void resetInterrupt()
    {
        synchronized (interruptSync)
        {
            interrupted = false;
            interruptSync.notify();
        }
    }

    /**
     * Send an event to the listeners listening to my events. The event is
     * Queued in the sendEvtQueue which runs in a separate thread. This way,
     * sendEvent() won't be blocked.
     */
    protected final void sendEvent(ControllerEvent evt)
    {
        if (sendEvtQueue != null)
            sendEvtQueue.postEvent(evt);
    }

    /**
     * Subclass can use this to switch to a different default clock.
     */
    protected void setClock(Clock c)
    {
        clock = c;
    }

    protected void setMediaLength(long t)
    {
        if (clock instanceof BasicClock)
            ((BasicClock) clock).setMediaLength(t);
    }

    @Override
    public void setMediaTime(Time when)
    {
        if (state < Realized)
        {
            throwError(new NotRealizedError(MediaTimeError));
        }
        clock.setMediaTime(when);
        doSetMediaTime(when);
        sendEvent(new MediaTimeSetEvent(this, when));
    }

    @Override
    public float setRate(float factor)
    {
        if (state < Realized)
        {
            throwError(new NotRealizedError(SetRateError));
        }

        float oldRate = getRate();
        float rateSet = doSetRate(factor);
        float newRate = clock.setRate(rateSet);

        if (newRate != oldRate)
        {
            sendEvent(new RateChangeEvent(this, newRate));
        }
        return newRate;
    }

    @Override
    public void setStopTime(Time t)
    {
        if (state < Realized)
        {
            throwError(new NotRealizedError(StopTimeError));
        }
        Time oldStopTime = getStopTime();
        clock.setStopTime(t);
        boolean stopTimeHasPassed = false;
        if (state == Started)
        {
            long timeToStop;
            if (((timeToStop = checkStopTime()) < 0)
                    || (stopThreadEnabled && activateStopThread(timeToStop)))
                stopTimeHasPassed = true;
        }
        if (oldStopTime.getNanoseconds() != t.getNanoseconds())
        {
            sendEvent(new StopTimeChangeEvent(this, t));
        }
        if (stopTimeHasPassed)
        {
            stopAtTime();
        }
    }

    /**
     * Set the target state.
     */
    protected final /* synchronized */void setTargetState(int state)
    {
        targetState = state;
    }

    @Override
    public void setTimeBase(TimeBase tb) throws IncompatibleTimeBaseException
    {
        if (state < Realized)
        {
            throwError(new NotRealizedError(TimeBaseError));
        }
        clock.setTimeBase(tb);
    }

    /**
     * Stop the controller. Invoke clock.stop() to maintain the clock states.
     * The subclass should implement and further specialized this method to do
     * the real work. But it should also invoke this method to maintain the
     * correct states.
     */
    @Override
    public void stop()
    {
        if (state == Started || state == Prefetching)
        {
            stopControllerOnly();
            doStop();
        }
    }

    /**
     * Stop because stop time has been reached. Subclasses should override this
     * method.
     */
    protected void stopAtTime()
    {
        stop();
        setStopTime(Clock.RESET);
        sendEvent(new StopAtTimeEvent(this, Started, Prefetched,
                getTargetState(), getMediaTime()));
    }

    /**
     * Stop the controller. Invoke clock.stop() to maintain the clock states.
     * The subclass should implement and further specialized this method to do
     * the real work. But it should also invoke this method to maintain the
     * correct states.
     */
    protected void stopControllerOnly()
    {
        if (state == Started || state == Prefetching)
        {
            clock.stop();
            state = setAndLogEngineChange(Prefetched);
            setTargetState(Prefetched);

            if (stopTimeThread != null && stopTimeThread.isAlive()
                    && Thread.currentThread() != stopTimeThread)
            {
                stopTimeThread.abort();
            }

            // If start(tbt) was invoked and it hasn't reached the
            // scheduled tbt yet, the startThread is spinning and
            // we need to abort that.
            if (startThread != null && startThread.isAlive())
                startThread.abort();
        }
    }

    @Override
    public void syncStart(final Time tbt)
    {
        if (state < Prefetched)
        {
            throwError(new NotPrefetchedError(SyncStartError));
        }
        clock.syncStart(tbt); // Will generate ClockStartedError if state is
                              // Started
        state = setAndLogEngineChange(Started);
        setTargetState(Started);
        sendEvent(new StartEvent(this, Prefetched, Started, Started,
                getMediaTime(), tbt));
        long timeToStop;
        if ((timeToStop = checkStopTime()) < 0
                || (stopThreadEnabled && activateStopThread(timeToStop)))
        {
            // If the stop-time is set to a value that the Clock
            // has already passed, the Clock immediately stops.
            stopAtTime();
            return;
        }

        // Schedule the start time.
        // startThread will wake up at the scheduled tbt and call the
        // protected doStart() method.
        startThread = new TimedStartThread(this, tbt.getNanoseconds());
        startThread.setName(startThread.getName() + " ( startThread: " + this
                + " )");
        startThread.start();
    }

    protected boolean syncStartInProgress()
    {
        return (startThread != null && startThread.isAlive());
    }

    protected void throwError(Error e)
    {
        Log.dumpStack(e);
        throw e;
    }
}

/**
 * A Thread to take care of realizing and catching of ConfigureCompleteEvents
 * from down stream nodes.
 */
class ConfigureWorkThread extends StateTransitionWorkThread
{
    // Made this method on package private class public [for jdk1.2 security]
    public ConfigureWorkThread(BasicController mc)
    {
        controller = mc;
        setName(getName() + ": " + mc);
    }

    @Override
    protected void aborted()
    {
        controller.abortConfigure();
    }

    @Override
    protected void completed()
    {
        controller.completeConfigure();
    }

    @Override
    protected void failed()
    {
        controller.doFailedConfigure();
    }

    @Override
    protected boolean process()
    {
        return controller.doConfigure();
    }
}

/**
 * A Thread to take care of prefetching and catching of PrefetchCompleteEvents
 * from down stream nodes.
 */
class PrefetchWorkThread extends StateTransitionWorkThread
{
    // Made this method on package private class public [for jdk1.2 security]
    public PrefetchWorkThread(BasicController mc)
    {
        controller = mc;
        setName(getName() + ": " + mc);
    }

    @Override
    protected void aborted()
    {
        controller.abortPrefetch();
    }

    @Override
    protected void completed()
    {
        controller.completePrefetch();
    }

    @Override
    protected void failed()
    {
        controller.doFailedPrefetch();
    }

    @Override
    protected boolean process()
    {
        return controller.doPrefetch();
    }
}

/**
 * A Thread to take care of realizing and catching of RealizeCompleteEvents from
 * down stream nodes.
 */
class RealizeWorkThread extends StateTransitionWorkThread
{
    // Made this method on package private class public [for jdk1.2 security]
    public RealizeWorkThread(BasicController mc)
    {
        controller = mc;
        setName(getName() + ": " + mc);
        Log.annotate(this, "RealizeWorkThread created, name: " + getName());
    }

    @Override
    protected void aborted()
    {
        controller.abortRealize();
    }

    @Override
    protected void completed()
    {
        controller.completeRealize();
    }

    @Override
    protected void failed()
    {
        controller.doFailedRealize();
    }

    @Override
    protected boolean process()
    {
        return controller.doRealize();
    }
}

/**
 * The event queue to post outgoing events for the BasicController. The queue is
 * running is a separate thread. This is so the posting of the events won't
 * block the controller.
 */
class SendEventQueue extends ThreadedEventQueue
{
    private BasicController controller;

    public SendEventQueue(BasicController c)
    {
        controller = c;
    }

    /**
     * Callback from the thread when there is an event to be processed. In this
     * case, we call controller's dispatchEvent() to send the event to the
     * listening controllers.
     */
    @Override
    public void processEvent(ControllerEvent evt)
    {
        Log.info("SendEventQueue " + this +
                 " for BasicController " + controller +
                 " dispatching event " + evt);
        controller.dispatchEvent(evt);
    }
}

/**
 * An execution thread to take care of processing and waiting of completion
 * events for BasicController. Subclass this to build PrefetchWork thread for
 * example.
 */
abstract class StateTransitionWorkThread extends MediaThread
{
    BasicController controller;
    boolean allEventsArrived = false;

    StateTransitionWorkThread()
    {
        useControlPriority();
    }

    /**
     * Called if the processing is aborted in the middle.
     */
    protected abstract void aborted();

    /**
     * This will be invoked when everything is ready. i.e., the processing is
     * completed and all the events from down stream nodes have been fully
     * captured.
     */
    protected abstract void completed();

    /**
     * Called if the processing failed.
     */
    protected abstract void failed();

    /**
     * Implement this to do the real work.
     */
    protected abstract boolean process();

    /**
     * run() method for the thread. Do the work, wait for every expected events
     * to arrive, signal the completion.
     */
    @Override
    public void run()
    {
        controller.resetInterrupt();

        try
        {
            boolean success = process();

            if (controller.isInterrupted())
                aborted();
            else if (success)
                completed();
            else
                failed();

        } catch (OutOfMemoryError e)
        {
            System.err.println("Out of memory!");
        }

        controller.resetInterrupt();
    }
}

/**
 * A thread to schedule the stop time.
 */
class StopTimeThread extends TimedActionThread
{
    // Made this method on package private class public [for jdk1.2 security]
    public StopTimeThread(BasicController mc, long nanoseconds)
    {
        super(mc, nanoseconds);
        setName(getName() + ": StopTimeThread");
        wakeupTime = getTime() + nanoseconds;
    }

    @Override
    protected void action()
    {
        controller.stopAtTime();
    }

    @Override
    protected long getTime()
    {
        return controller.getMediaNanoseconds();
    }
}

/**
 * Start a countdown thread to perform an action at the specified time.
 */
abstract class TimedActionThread extends MediaThread
{
    protected BasicController controller;
    protected long wakeupTime;
    protected boolean aborted = false;

    TimedActionThread(BasicController mc, long nanoseconds)
    {
        Log.annotate(this, "TimedActionThread created with mc " + mc +
                           " and time " + nanoseconds);
        controller = mc;
        useControlPriority();
        wakeupTime = nanoseconds;
    }

    public synchronized void abort()
    {
        aborted = true;
        notify();
    }

    protected abstract void action();

    protected abstract long getTime();

    @Override
    public void run()
    {
        long sleepTime, now;

        while (true)
        {
            now = getTime();
            if (now >= wakeupTime || aborted)
                break;

            // Don't sleep more than 1 sec.
            sleepTime = wakeupTime - now;
            if (sleepTime > 1000000000L)
                sleepTime = 1000000000L;

            synchronized (this)
            {
                try
                {
                    wait(sleepTime / 1000000);
                } catch (InterruptedException e)
                {
                    break;
                }
            }
        }

        if (!aborted)
            action();
    }
}

/**
 * A Thread to schedule the start of the node.
 */
class TimedStartThread extends TimedActionThread
{
    // Made this method on package private class public [for jdk1.2 security]
    public TimedStartThread(BasicController mc, long tbt)
    {
        super(mc, tbt);
        setName(getName() + ": TimedStartThread");
    }

    @Override
    protected void action()
    {
        controller.doStart();
    }

    @Override
    protected long getTime()
    {
        return controller.getTimeBase().getNanoseconds();
    }
}
