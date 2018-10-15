
#define LOG_TAG "AudioEditorPriv"
#include "AudioEncoder.h"
#include "Mp3Encoder.h"
//#include "WaveEncoder.h"


class AudioEncoder* AudioEncoder::createAudioEncoder(encode_type audioType, const char *outPutFilePath) {
    AudioEncoder* ae;

    switch (audioType) {
    //case AUDIO_ENCODER_WAVE:
    //    ae = new WaveEncoder();
    //    break;
    case AUDIO_ENCODER_MP3:
        ae = new Mp3Encoder();
        break;
    default:
        return NULL;
    }
    if (ae->openFile(outPutFilePath) != RET_SUCCESS) {
        delete ae;
        return NULL;
    }
    return ae;
}

