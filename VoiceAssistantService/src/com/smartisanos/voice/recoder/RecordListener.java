package com.smartisanos.voice.recoder;


/**
 * 用于录音数据回调的接口
 * @author kuncheng
 *
 */
public interface RecordListener
{
	/**
	 * 录音数据回调
	 * @param dataBuffer	录音数据
	 * @param length   录音数据长度
	 */
    void onRecordData(final byte[] dataBuffer, final int length, long timeMillisecond);
    
    /**
     * 录音错误
     * @param errorCode 错误码
     */
    void onRecordError(final int errorCode);
}
