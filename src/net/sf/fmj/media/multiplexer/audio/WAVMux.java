package net.sf.fmj.media.multiplexer.audio;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.sf.fmj.media.codec.*;
import net.sf.fmj.media.multiplexer.*;
import net.sf.fmj.media.renderer.audio.*;

public class WAVMux extends BasicMux
{
    /**
     * The byte index in the header where the word containing the size of the
     * riff chunk resides.
     */
    private static final int RIFF_CHUNK_SIZE_IDX = 4;

    /**
     * The byte index in the header where the word containing the size of the
     * wav 'data' chunk resides.
     */
    private static final int WAV_DATA_CHUNK_SIZE_IDX = 40;

    /**
     * The size in bytes of the header of a PCM wav file, including both the
     * RIFF header, the FMT chunk, and the header of the data chunk.
     */
    private static final int WAV_FILE_HEADER_SIZE = 44;

    private int mBytesWritten = 0;

    public WAVMux()
    {
        supportedInputs = new Format[1];
        supportedInputs[0] = new AudioFormat(
                AudioFormat.LINEAR,
                44000,
                16,
                /* channels */ Format.NOT_SPECIFIED,
                AudioFormat.LITTLE_ENDIAN,
                AudioFormat.SIGNED,
                /* frameSizeInBits */ Format.NOT_SPECIFIED,
                /* frameRate */ Format.NOT_SPECIFIED,
                Format.byteArray);
        supportedOutputs = new ContentDescriptor[1];
        supportedOutputs[0] = new FileTypeDescriptor(
                FileTypeDescriptor.WAVE);
    }

    public String getName()
    {
        return "WAV Audio Multiplexer";
    }

    @Override
    public Format setInputFormat(Format format, int trackID)
    {
        final AudioFormat af = (AudioFormat) format;
        if (af.getSampleSizeInBits() == 8
                && af.getSigned() == AudioFormat.SIGNED)
            return null; // 8-bit is always unsigned for Wav.

        if (af.getSampleSizeInBits() == 16
                && af.getSigned() == AudioFormat.UNSIGNED)
            return null; // 16-bit is always signed for Wav.

        inputs[0] = format;
        return format;
    }

    @Override
    protected boolean needsSeekable()
    {
        // In order to set the Wav header's length field correctly, we need to
        // be able to go back and amend the value once the entire file has been
        // written.
        return true;
    }

    @Override
    protected int write(byte[] data, int offset, int length)
    {
        if (source == null || !source.isConnected())
            return length;

        if (length > 0)
        {
            filePointer += length;
            if (filePointer > fileSize)
                fileSize = filePointer;
            if (fileSizeLimit > 0 && fileSize >= fileSizeLimit)
                fileSizeLimitReached = true;
        }

        int bytesWritten = stream.write(data, offset, length);
        mBytesWritten += bytesWritten;

        return bytesWritten;
    }

    @Override
    protected void writeHeader()
    {
        // Since we request all our inputs in 44kHz signed 16-bit format,
        // it suffices to use the first channel's format for the header, as
        javax.sound.sampled.AudioFormat javaAudioFormat =
                JavaSoundUtils.convertFormat((AudioFormat) inputs[0]);
        byte[] wavHeader = JavaSoundCodec.createWavHeader(javaAudioFormat);

        stream.write(wavHeader, 0, wavHeader.length);
    }

    @Override
    protected void writeFooter()
    {
        // Riff file length excludes the RIFF ID word and the WAV ID word.
        int riffFileLength = mBytesWritten + WAV_FILE_HEADER_SIZE - 8;
        seek(RIFF_CHUNK_SIZE_IDX);
        bufWriteIntLittleEndian(riffFileLength);
        bufFlush();

        seek(WAV_DATA_CHUNK_SIZE_IDX);
        bufWriteIntLittleEndian(mBytesWritten);
        bufFlush();
    }
}
