package net.sf.fmj.ejmf.toolkit.util;

import javax.swing.border.*;

/**
 * From the book: Essential JMF, Gordon, Talley (ISBN 0130801046). Used with
 * permission.
 *
 * @author Steve Talley & Rob Gordon
 *
 */
public class BorderConstants
{
    public static final int GAP = 10;

    public static final EmptyBorder emptyBorder = new EmptyBorder(GAP, GAP,
            GAP, GAP);

    public static final CompoundBorder etchedBorder = new CompoundBorder(
            new EtchedBorder(), emptyBorder);
}
