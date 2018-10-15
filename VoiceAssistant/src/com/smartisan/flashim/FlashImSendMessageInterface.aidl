// IMyAidlInterface.aidl
package com.smartisan.flashim;

// Declare any non-default types here with import statements

interface FlashImSendMessageInterface {
    String getAccount();
    int sendVoiceMessage(int messageType,String contactId, String voiceUri,String voiceText);
    int sendFileMessage(int messageType,String contactId, String fileUri);
    int sendImageMessage(int messageType,String contactId, String imageUri);
    int sendVideoMessage(int messageType,String contactId, String videoUri);

}
