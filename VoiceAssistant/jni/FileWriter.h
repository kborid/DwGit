#ifndef PCMMANAGER_H_
#define PCMMANAGER_H_

//#include <media/AudioRecord.h>
//#include <media/AudioSystem.h>
//#include <media/stagefright/MediaSource.h>
//#include <media/stagefright/MediaBuffer.h>
#include <utils/List.h>
#include <utils/String8.h>
#include <system/audio.h>
#include <utils/Log.h>

#include <errno.h>
#include <stdarg.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <dlfcn.h>
#include <string.h>
#include <stdlib.h>

using android::String8;

/* Function return value*/
#define RET_SUCCESS              0
#define RET_PCM_NOT_EXIST       -1
#define RET_PCM_WRONG_STATUS    -2
#define RET_PCM_NOT_AVAILABLE   -3
#define RET_UNSUPPORTED_PARAMETER   -4
#define RET_WRONG_CALL_ORDER    -5
#define RET_NO_MEMORY           -6
#define RET_NO_SPACE            -7
#define RET_FILE_PARSE_ERROR    -8
#define RET_FILE_NOT_EXIST      -9
#define RET_FILE_CREATE_ERROR   -10
#define RET_FILE_WRITE_ERROR    -11
#define RET_ENCODER_FAILED      -12
#define RET_RECORDER_FAILED     -13
#define RET_PLAYER_FAILED       -14

#define AE_ASSERT(a) \
    do { \
        if (0 == (a)) { \
            abort(); \
        } \
    } while (0)

typedef enum {
    FILE_OK,
    FILE_NO_SPACE,  //  should  inform  user to free space, then call checkStatus()
    FILE_ERROR,     //  should  give up writing
} file_status_t;

class FileWriter{

public:
    FileWriter(const char *pcmfile = NULL);
    virtual ~FileWriter();

    int32_t openFile(const char *pcmfile);
    file_status_t checkStatus();
    file_status_t write(void *buf, uint32_t length);
    void    seek(off_t offset);

    /* If not calling finishWrite(), then the file is considered imcomplete and
       will be deleted in this->destructor */
    file_status_t finishWrite();


private:
    void* mTempBuf;
    uint32_t mTempBufSize;
    String8 mPcmFilePath;
    int mPcmFd;
};





#endif
