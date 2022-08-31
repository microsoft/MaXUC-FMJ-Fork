# FMJ
FMJ supports movie and audio file playback, as well as viewing of
video from a capture device, on Windows, Mac OS X, and
Linux.

# Requirements
- Java 11

# Directories.
Not all directories are in the release.

Directory                 Description
src                       Main source folder for FMJ
src.ejmf                  Source borrowed and adapted from the EJMF book (with permission)
src.rtp                   RTP manager implementation
src.stubs                 Stubbed implementations of sun/ibm internal JMF classes that can be useful to get JMF-dependent projects to compile
src.ds                    DirectShow Player
src.utils                 LTI utils classes
src.sunibm.replace        Replacements for sun/ibm internal JMF classes that are implemented by extending the FMJ equivalents.  
Not generally needed unless you want to play classpath tricks.  Needed to compile/run unit tests.
src.sunibm.base           Implementations of internal sun/ibm base classes that are often used (unfortunately) by many JMF-based
projects.

# How to build
Run Ant command which will call the default target - `jar-fmj`:  
```
ant
```

# Contributors
Note: not everyone who has contributed is included here.  If you
have contributed and have been overlooked here, don't take it
personally, just let the team know and you'll be added.

Thanks also to others for submitting patches, bug reports, and
feedback.

Thanks also to other open-source projects, books, and examples from
which source has been borrowed/adapted, like SIP-Communicator,
EJMF, and others.

Name:     Ken Larson (kenlars99)
Role:     Developer
Areas:    Project leader
Location: USA and Germany

Name:     Warren Bloomer (stormboy)
Role:     Developer
Areas:    FMJStudio, FMJRegistry, audio/video renderers.
Location: Australia

Name:     Andrew Rowley (zzalsar4)
Role:     Developer
Areas:    RTP
Location: England

Name:     Christian Vincenot (sgt_sagara)
Role:     Developer
Areas:    RTP
Location: France

Name:     Andrey Kuprianov (andreyvk)
Role:     Web designer and administrator
Location: Russia and Thailand

Name:     Stephan Goetter (turms)
Role:     Developer
Areas:    ffmpeg-java
Location: Germany

Name:     Jeremy Wood
Role:     Developer
Areas:    JPEG encoding/decoding, buffer/image conversion, optimization

Name:     Damian Minkov
Role:     Developer (SIP-Communicator)
Areas:    SIP-Communicator RTP Codecs


# Supported Formats
Pure Java processing and playback:
```
Container                  Decode, Encode
Format

RTP
JPEG/RTP                  D,E
ULAW/RTP                  D,E
ALAW/RTP                  D,E
SPEEX/RTP                 D,E
ILBC/RTP                  D,E
WAV
LINEAR (PCM)              D,E
AU
LINEAR (PCM)              D,E
ULAW                      D,?
AIFF
LINEAR (PCM)              D,E

multpart/x-mixed-replace
JPEG                      D,E
GIF                       D,E (encoding only with Java 6+)
PNG                       D,E

Assorted pure Java codecs:
Audio resampling
Video scaling
```

Note: FMJ and JMF can use each others' plugins, assuming the classpath and
registry is set appropriately.

For JMF supported formats, see:
https://java.sun.com/products/java-media/jmf/2.1.1/formats.html

# License
The license can be found in [LICENSE](LICENSE)
See the [NOTICE](NOTICE) file for required notices and attributions.
