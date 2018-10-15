#ifndef MP3ENCODER_H_
#define MP3ENCODER_H_

#include <lame/include/lame.h>
//#include <parse.h>
#include <system/audio.h>
#include <errno.h>
#include <stdarg.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <dlfcn.h>

#include "AudioEncoder.h"


class Mp3Encoder : public AudioEncoder {

public:
    Mp3Encoder();
    virtual ~Mp3Encoder();

    virtual int32_t initEncoder(uint32_t samplerate, uint32_t numchannels, uint32_t bitrates, int32_t pcmFormat);

    virtual file_status_t encoding(int16_t *buffer, int32_t buffersize);

    virtual file_status_t finishEncoding();

private:
    lame_global_flags *gf = NULL;

    uint32_t mSampleRate;
    uint32_t mChannelsNum;
    uint32_t mBitRates;
    uint32_t mPcmFrameSize;
    int32_t mp3EncoderOutSize;
    unsigned char* mp3buffer;
};





#endif
