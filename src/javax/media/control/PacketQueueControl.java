package javax.media.control;

import javax.media.Control;

/**
 * Control for the packet queue
 *
 * @author Boris Grozev
 */
public interface PacketQueueControl extends Control
{
    /**
     * @return the current approximate delay in milliseconds that the queue
     * introduces.
     */
    public int getCurrentDelayMs();

    /**
     * @return the current approximate delay in number of packets that the queue
     * introduces.
     */
    public int getCurrentDelayPackets();

    /**
     * @return the number of elements currently in the queue
     */
    public int getCurrentPacketCount();

    /**
     * @return the current size of the queue in packets.
     */
    public int getCurrentSizePackets();

    /**
     * @return the total number of packets discarded by the queue.
     */
    public int getDiscarded();

    /**
     * @return the number of packets discarded by the queue because it was full.
     */
    public int getDiscardedFull();

    /**
     * @return the number of packets discarded by the queue because they were
     * too late.
     */
    public int getDiscardedLate();

    /**
     * @return the number of packets discarded by the queue due to resetting.
     */
    public int getDiscardedReset();

    /**
     * @return the number of packets discarded by the queue while shrinking.
     */
    public int getDiscardedShrink();

    /**
     * @return the maximum size that the queue reached (in number of packets).
     */
    public int getMaxSizeReached();

    /**
     * @return whether the adaptive jitter buffer mode is enabled.
     */
    public boolean isAdaptiveBufferEnabled();

    /**
     * @return the minimum capacity that the queue reached (in number of packets).
     */
    public int getMinCapacity();

    /**
     * @return the maximum capacity that the queue reached (in number of packets).
     */
    public int getMaxCapacity();

    /**
     * @return the average capacity of the queue reached (in number of packets).
     */
    public double getAverageCapacity();

    /**
     * @return the standard deviation of the queue capacity (in number of packets).
     */
    public double getStandardDeviationCapacity();

    /**
     * @return the minimum size that the queue reached (in number of packets).
     */
    public int getMinSize();

    /**
     * @return the maximum size that the queue reached (in number of packets).
     */
    public int getMaxSize();

    /**
     * @return the average size of the queue reached (in number of packets).
     */
    public double getAverageSize();

    /**
     * @return the standard deviation of the queue size (in number of packets).
     */
    public double getStandardDeviationSize();
}
