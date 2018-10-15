#ifndef __AUDIO_ENCODER_H_
#define __AUDIO_ENCODER_H_


#include "FileWriter.h"

enum encode_type {
    AUDIO_ENCODER_WAVE = 0,  //default type
    AUDIO_ENCODER_MP3  = 1,
    AUDIO_ENCODER_FLAC = 2,
    AUDIO_ENCODER_APE  = 3,
    AUDIO_ENCODER_PCM  = 4,
};

class AudioEncoder : public FileWriter {
public:
    AudioEncoder() {}
    virtual ~AudioEncoder() {}

    static class AudioEncoder* createAudioEncoder(encode_type audioTpye, const char *outPutFilePath);

    virtual int32_t initEncoder(uint32_t samplerate, uint32_t numchannels, uint32_t bitrates, int32_t pcmFormat) = 0;

    virtual file_status_t encoding(int16_t *buffer, int32_t buffersize) = 0;

    virtual file_status_t finishEncoding() = 0;

private:
};












#endif
