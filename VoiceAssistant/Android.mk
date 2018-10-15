LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# fix AndroidManifest.xml
# update the apikey for amap.
ifeq ($(findstring oscar, $(TARGET_PRODUCT)), oscar)
    AMAP_KEY_DEFAULT := cbf31b0dbfec1bf173c8bfc6ecf2c9fc
    AMAP_KEY_OSCAR   := 659a21c1e32181cf4a944ef709286494
    $(shell sed -i 's/${AMAP_KEY_DEFAULT}/${AMAP_KEY_OSCAR}/' ${LOCAL_PATH}/AndroidManifest.xml)
endif

# remove 'android:turnScreenOn="true"' before oreo.
BEFORE_O := $(strip $(shell if [ $(PLATFORM_SDK_VERSION) -lt "26" ]; then echo true; else echo false; fi))
ifeq ($(BEFORE_O),true)
    $(shell sed -i '/android:turnScreenOn/d' ${LOCAL_PATH}/AndroidManifest.xml)
    $(shell sed -i '/android:showWhenLocked/d' ${LOCAL_PATH}/AndroidManifest.xml)
endif

common_dir := ../IdeaPillsCommon
src_dirs := src $(common_dir)/src
res_dirs := res $(common_dir)/res

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/revone_res
LOCAL_AAPT_FLAGS := --auto-add-overlay --extra-packages com.smartisanos.ideapills.common

TARGET_ARCH_ABI := armeabi
LOCAL_MODULE_TAGS :=  optional
LOCAL_CERTIFICATE := platform
LOCAL_SRC_FILES += src/com/smartisanos/music/ISmartisanosMusicService.aidl

LOCAL_SRC_FILES += $(call all-java-files-under, src) \
        /src/com/smartisanos/sara/voicecommand/IVoiceCommandEnvironment.aidl \
	    /src/com/smartisan/flashim/FlashImSendMessageInterface.aidl

LOCAL_STATIC_JAVA_LIBRARIES := \
           picloader_VoiceAssistant \
           amap_location \
           amap_search \
           android-support-v4 \
           android-support-v7-recyclerview \
           android-support-v7-appcompat \
           pinyin4j-2.5.0 \
           ifly_speech

LOCAL_STATIC_ANDROID_LIBRARIES := \
           android-support-customtabs \
           android-support-compat

LOCAL_PACKAGE_NAME := VoiceAssistant

LOCAL_JNI_SHARED_LIBRARIES := libaudioencoder_va

LOCAL_REQUIRED_MODULES := libaudioencoder_va

LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PRIVILEGED_MODULE := true
LOCAL_USE_FRAMEWORK_SMARTISANOS := true
LOCAL_AAPT_FLAGS += --max-res-version $(PLATFORM_SDK_VERSION)
include $(LOCAL_PATH)/overlay/overlay_include.mk
BEFORE_MARSHMALLOW := $(strip $(shell if [ $(PLATFORM_SDK_VERSION) -lt "23" ]; then echo true; else echo false; fi))
ifeq ($(BEFORE_MARSHMALLOW), false)
    LOCAL_JAVA_LIBRARIES := org.apache.http.legacy \
                            telephony-common
    LOCAL_RESOURCE_DIR += frameworks/support/v7/recyclerview/res
    LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.recyclerview
else
    LOCAL_JAVA_LIBRARIES := telephony-common
endif
include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES :=  picloader_VoiceAssistant:$(common_dir)/libs/picloader.jar \
                            amap_location:libs/AMap_Location_V3.6.1_20171012.jar \
                            amap_search:libs/amap_search_5.5.0.jar \
                            ifly_speech:libs/SpeechServiceLibUtil.jar \
                            pinyin4j-2.5.0:libs/pinyin4j-2.5.0.jar \

include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))
