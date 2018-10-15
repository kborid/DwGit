#include <jni.h>
#include <stdio.h>
#include <unistd.h>
#include <assert.h>
#include "AudioEncoder.h"
#include "utils/Thread.h"
#include "JNIHelp.h"

using namespace android;

const int BUFFER_SIZE = 4096;
const int BIT_RATES = 320000;
const int PCM_FILE_HEADER = 44;

static Mutex sLock;
static jclass sAudioEncoderCls;
static jfieldID sEncoderField;
static AudioEncoder * getAudioEncoder(JNIEnv* env, jobject thiz)
{
    Mutex::Autolock l(sLock);
    AudioEncoder* const p = (AudioEncoder*)env->GetStaticLongField(sAudioEncoderCls, sEncoderField);
    return p;
}

static AudioEncoder * setAudioEncoder(JNIEnv* env, jobject thiz, const AudioEncoder * encoder)
{
    Mutex::Autolock l(sLock);
    AudioEncoder * old = (AudioEncoder*)env->GetStaticLongField(sAudioEncoderCls, sEncoderField);

    if (old != 0) {
        delete old;
    }
    env->SetStaticLongField(sAudioEncoderCls, sEncoderField, (jlong)encoder);
    return old;
}

/*static jboolean audioencoder_wrapper_setDataSource(JNIEnv *env, jobject thiz, jstring srcPath, jstring dstPath){
    ALOGI("AudioEncoder-JNI audioencoder_wrapper_setDataSource");
    if(srcPath == NULL || dstPath == NULL){
        return false;
    }

    if(mAudioEncoder != NULL){
        mAudioEncoder->finishEncoding();
        mAudioEncoder = NULL;
    }

    if(mAudioEncoder == NULL){
        mAudioEncoder = AudioEncoder::createAudioEncoder(AUDIO_ENCODER_MP3, env->GetStringUTFChars(dstPath, NULL));
    }

    assert(mAudioEncoder != NULL);

    mSrcFilePath = env->GetStringUTFChars(srcPath, NULL);

    return true;
}*/

static void audioencoder_wrapper_setup(JNIEnv *env, jobject thiz, jstring dstPath){
    if(dstPath == NULL){
        jniThrowException(env, "java/lang/IllegalArgumentException", "Dst path is NULL");
    }

    AudioEncoder * encoder = AudioEncoder::createAudioEncoder(AUDIO_ENCODER_MP3, env->GetStringUTFChars(dstPath, NULL));
    if(encoder == NULL){
        jniThrowException(env, "java/lang/RuntimeException", "Can not create audio encoder");
    }

    setAudioEncoder(env, thiz, encoder);
}

static jboolean audioencoder_wrapper_setFormat(JNIEnv *env, jobject thiz, jint samplerate, jint numchannels, jint pcmFormat){
    ALOGI("AudioEncoder-JNI audioencoder_wrapper_setFormat");
    AudioEncoder * encoder = getAudioEncoder(env, thiz);
    if(encoder == NULL){
        ALOGE("AudioEncoder-JNI setFormat failed because of no encoder");
        return false;
    }

    encoder->initEncoder(samplerate, numchannels, BIT_RATES, pcmFormat);

    return true;
}
static jboolean audioencoder_wrapper_doEncode(JNIEnv *env, jobject thiz, jstring srcPath){
    ALOGI("AudioEncoder-JNI audioencoder_wrapper_doEncode");
    AudioEncoder * encoder = getAudioEncoder(env, thiz);
    if(encoder == NULL){
        ALOGE("AudioEncoder-JNI setFormat failed because of no encoder");
        return false;
    }

    if(srcPath == NULL){
        ALOGE("AudioEncoder-JNI doEncode without srcPath");
        return false;
    }
    int srcFileFd = ::open(env->GetStringUTFChars(srcPath, NULL), O_RDONLY, 0777);
    if(srcFileFd <= 0){
        ALOGE("open file(%s) failed", env->GetStringUTFChars(srcPath, NULL));
        return false;
    }
    off64_t result = lseek64(srcFileFd, PCM_FILE_HEADER, SEEK_SET);
    if (result == -1) {
        ALOGE("seek failed");
        return false;
    }

    unsigned char * audioBuffer = new unsigned char [BUFFER_SIZE];
    int ret = 0;
    do{
        memset(audioBuffer, 0, BUFFER_SIZE);
        int readRet = ::read(srcFileFd, audioBuffer, BUFFER_SIZE);
        //ALOGI("AudioEncoder-JNI audioencoder_wrapper_doEncode readRet(%d)", readRet);
        if(readRet > 0){
            int encodeRet = encoder->encoding((int16_t*)audioBuffer, BUFFER_SIZE);
            if(encodeRet == -1){
                ret = -1;
                ALOGE("encodeing failed");
                break;
            }
        }
        ret = readRet;
    }while(ret == BUFFER_SIZE);

    ::close(srcFileFd);
    encoder->finishEncoding();
    delete[] audioBuffer;
    audioBuffer = NULL;

    if(ret < 0){
        ALOGE("may be read file failed");
        return false;
    }

    ALOGI("AudioEncoder-JNI audioencoder_wrapper_doEncode DONE!!!");
    return true;
}

static const JNINativeMethod gMethods[] = {
    {"native_setup",   "(Ljava/lang/String;)V",    (void *)audioencoder_wrapper_setup},
    {"setFormat",      "(III)Z",   (void *)audioencoder_wrapper_setFormat},
    {"doEncode",       "(Ljava/lang/String;)Z",    (void *)audioencoder_wrapper_doEncode},
};

// This function only registers the native methods
static int register_audioencoder_methods(JNIEnv *env)
{
    int registerRt = env->RegisterNatives(env->FindClass("com/smartisanos/sara/util/AudioEncoder"), gMethods, 3);
    return registerRt;
}

static int load_audioEncoder_field(JNIEnv *env){
    sAudioEncoderCls = env->FindClass("com/smartisanos/sara/util/AudioEncoder");
    if(sAudioEncoderCls == NULL){
        ALOGE("AudioEncoder-JNI can not find java class");
        return -1;
    }
    sEncoderField = env->GetStaticFieldID(sAudioEncoderCls, "sNativeEncoder", "J");
    if (sEncoderField == NULL) {
        ALOGE("AudioEncoder-JNI can not getFieldID");
        return -1;
    }

    return 0;
}

jint JNI_OnLoad(JavaVM* vm, void* /* reserved */)
{
    ALOGI("AudioEncoder-JNI JNI_OnLoad");
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("AudioEncoder-JNI ERROR: GetEnv failed");
        goto bail;
    }
    assert(env != NULL);

    if (register_audioencoder_methods(env) < 0) {
        ALOGE("AudioEncoder-JNI ERROR: register methods failed");
        goto bail;
    }

    if(load_audioEncoder_field(env) < 0){
        ALOGE("AudioEncoder-JNI ERROR: load audioEncoder java field failed");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}
