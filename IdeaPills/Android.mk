LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES += telephony-common

common_dir := ../IdeaPillsCommon
src_dirs := src $(common_dir)/src
res_dirs := res $(common_dir)/res

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))
LOCAL_AAPT_FLAGS := --auto-add-overlay --extra-packages com.smartisanos.ideapills.common

LOCAL_MODULE_TAGS := optional
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_USE_FRAMEWORK_SMARTISANOS := true
LOCAL_PACKAGE_NAME := IdeaPills
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 \
           picloader_Ideapills

include $(LOCAL_PATH)/overlay/overlay_include.mk

BEFORE_LOLLIPOP := $(strip $(shell if [ $(PLATFORM_SDK_VERSION) -lt "24" ]; then echo true; else echo false; fi))
ifeq ($(BEFORE_LOLLIPOP), true)
    LOCAL_SRC_FILES := $(subst src/com/smartisanos/ideapills/view/BubbleFrameLayout.java, BEFORE_LOLLIPOP/src/BubbleFrameLayout.java, ${LOCAL_SRC_FILES})
endif
include $(BUILD_PACKAGE)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES :=  picloader_Ideapills:$(common_dir)/libs/picloader.jar
include $(BUILD_MULTI_PREBUILT)
include $(call all-makefiles-under,$(LOCAL_PATH))
