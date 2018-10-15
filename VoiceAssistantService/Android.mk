LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
IFLYTEK_FILES_DIR := ../../../vendor/iflytek
LOCAL_MODULE_TAGS :=  optional
LOCAL_CERTIFICATE := platform

LOCAL_SRC_FILES += $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := \
           android-support-v4 \
           SpeechServiceLibUtil_VoiceAssistant \
           hanlp-portable-1.3.2 \
           commons-lang3-3.4

LOCAL_PACKAGE_NAME := VoiceAssistantService
LOCAL_REQUIRED_MODULES := \
           libBugly.so \
           libaitalkone2p0_yd_v1.so \
           libaudio_resample.so \
           libesrfrontvad.so \
           libmsc5_suit_1081.so

LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_USE_FRAMEWORK_SMARTISANOS := true
include $(BUILD_PACKAGE)


include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := SpeechServiceLibUtil_VoiceAssistant:$(IFLYTEK_FILES_DIR)/SpeechSuite/libs/SpeechServiceLibUtil.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += \
          commons-lang3-3.4:libs/commons-lang3-3.4.jar \
          hanlp-portable-1.3.2:libs/hanlp-portable-1.3.2.jar
include $(BUILD_MULTI_PREBUILT)
