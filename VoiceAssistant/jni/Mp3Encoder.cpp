
#define LOG_TAG "AudioEditorPriv"
#include "Mp3Encoder.h"

Mp3Encoder::Mp3Encoder()
:mSampleRate(0), mChannelsNum(0), mBitRates(0), mp3EncoderOutSize(0)
{
    mp3buffer = new unsigned char[LAME_MAXMP3BUFFER];
    AE_ASSERT(mp3buffer);
}

Mp3Encoder::~Mp3Encoder() {
    delete [] mp3buffer;

    lame_close(gf);
}


int32_t Mp3Encoder::initEncoder(uint32_t samplerate, uint32_t numchannels, uint32_t bitrates, int32_t pcmFormat) {
    size_t  id3v2_size = 0;

    mSampleRate = samplerate;
    mChannelsNum = numchannels;
    mBitRates = bitrates;

    if(pcmFormat == AUDIO_FORMAT_PCM_8_BIT)
        mPcmFrameSize = mChannelsNum;
    else if(pcmFormat == AUDIO_FORMAT_PCM_16_BIT)
        mPcmFrameSize = mChannelsNum * 2;
    else if(pcmFormat == AUDIO_FORMAT_PCM_32_BIT)
        mPcmFrameSize = mChannelsNum * 4;

    gf = lame_init();
    lame_set_num_channels(gf, mChannelsNum);
    lame_set_in_samplerate(gf, mSampleRate);
    lame_set_brate(gf, mBitRates);
    if(mChannelsNum == 2) {
       lame_set_mode(gf, MPEG_mode(2));
    } else {
       lame_set_mode(gf, MPEG_mode(3));
    }

    lame_set_quality(gf, 5);   /* 2=high  5 = medium  7=low */

    int32_t ret = lame_init_params(gf);
    if(ret < 0){
        return RET_ENCODER_FAILED;
    }

    id3v2_size = lame_get_id3v2_tag(gf, 0, 0);
    if (id3v2_size > 0) {
        unsigned char *id3v2tag = new unsigned char [id3v2_size];
        AE_ASSERT(id3v2tag);

        mp3EncoderOutSize = lame_get_id3v2_tag(gf, id3v2tag, id3v2_size);
        file_status_t ret = write(id3v2tag, mp3EncoderOutSize);
        delete [] id3v2tag;

        if (ret != FILE_OK) {
            return RET_ENCODER_FAILED;
        }
    }
    return RET_SUCCESS;
}


file_status_t Mp3Encoder::encoding(int16_t *buffer, int32_t buffer_size) {
    size_t  id3v2_size;
    int32_t inPutFrameCount = buffer_size/mPcmFrameSize;
    if(mChannelsNum == 2){
        mp3EncoderOutSize = lame_encode_buffer_interleaved(gf, buffer, inPutFrameCount, mp3buffer, 0);
    } else {
        mp3EncoderOutSize = lame_encode_buffer(gf, buffer, NULL, inPutFrameCount, mp3buffer, 0);
    }

    /* was our output buffer big enough? */
    if (mp3EncoderOutSize < 0) {
        if (mp3EncoderOutSize == -1)
            printf("mp3 buffer is not big enough... \n");
        else
            printf("mp3 internal error:  error code=%d \n", mp3EncoderOutSize);
        return FILE_ERROR;
    }

    return write(mp3buffer, mp3EncoderOutSize);
}


file_status_t Mp3Encoder::finishEncoding() {
    mp3EncoderOutSize = lame_encode_flush(gf, mp3buffer, sizeof(mp3buffer)); /* may return one more mp3 frame */
    if (mp3EncoderOutSize < 0) {
        if (mp3EncoderOutSize == -1)
            printf("finishMp3encoding mp3 buffer clean already... \n");
        else
            printf("finishMp3encoding mp3 internal error:  error code=%i\n", mp3EncoderOutSize);
    }

    if(mp3EncoderOutSize > 0){
        file_status_t ret = write(mp3buffer, mp3EncoderOutSize);
        if (ret != FILE_OK) {
        }
    }
    return FileWriter::finishWrite();
}

