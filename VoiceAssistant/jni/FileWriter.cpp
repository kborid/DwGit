
#define LOG_TAG "AudioEditorPriv"
//#include "AudioEditor.h"
#include "FileWriter.h"

FileWriter::FileWriter(const char *pcmfile)
: mTempBuf(NULL), mTempBufSize(0), mPcmFd(-1)
{
    if (pcmfile) {
        openFile(pcmfile);
    }
}

FileWriter::~FileWriter() {
    if (mPcmFd > 0) {
        ::close(mPcmFd);
        //ALOGW("delete unfinished file %s", mPcmFilePath.string());
        remove(mPcmFilePath.string());
    }
    if (mTempBuf) {
        free(mTempBuf);
    }
}

int32_t FileWriter::openFile(const char *pcmfile) {
    if (mPcmFd > 0) {
        ::close(mPcmFd);
    }
    mPcmFilePath = pcmfile;
    mPcmFd = ::open(mPcmFilePath.string(), O_CREAT | O_TRUNC | O_WRONLY, 0777);
    if (mPcmFd > 0) {
        //ALOGV("open file %s successful", mPcmFilePath.string());
        return RET_SUCCESS;
    }
    else {
        //ALOGE("file %s open failed error: %s", mPcmFilePath.string(), strerror(errno));
        return RET_FILE_CREATE_ERROR;
    }
}

file_status_t FileWriter::checkStatus() {
    if (mPcmFd <= 0) {
        return FILE_ERROR;
    }

    // write unfinished data last time if any
    if (mTempBuf) {
        file_status_t ret = write(mTempBuf, mTempBufSize);
        return ret;
    }

    return FILE_OK;
}


file_status_t FileWriter::write(void *in, uint32_t bufferSize) {
    int32_t ret;

    ret = ::write(mPcmFd, in, bufferSize);

    if (ret >= 0 && (uint32_t)ret == bufferSize) {
        return FILE_OK;
    }

    //ALOGW("pcm should write %d but write return %d", bufferSize, ret);

    // check if no space
    if (ret >= 0 || (ret == -1 && errno == ENOSPC)) {
        mTempBufSize = ret > 0 ? bufferSize - ret : bufferSize;
        uint32_t size_write = bufferSize - mTempBufSize;
        if (mTempBuf) {
            free(mTempBuf);
        }
        mTempBuf = malloc(mTempBufSize);
        if (!mTempBuf) {
            //ALOGE("No memory!");
            return FILE_ERROR;
        }
        memcpy(mTempBuf, ((char*)in) + size_write, mTempBufSize);
        return FILE_NO_SPACE;
    }

    //ALOGE("write file %s failed with error %s", mPcmFilePath.string(), strerror(errno));
    return FILE_ERROR;
}

void FileWriter::seek(off_t offset) {
    ::lseek(mPcmFd, offset, SEEK_SET);
}

file_status_t FileWriter::finishWrite() {
    ::close(mPcmFd);
    mPcmFd = 0;
    return FILE_OK;
}

