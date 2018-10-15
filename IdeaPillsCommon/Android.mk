LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_STATIC_JAVA_LIBRARIES := \
           android-support-v4

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res
LOCAL_AAPT_FLAGS := --auto-add-overlay
include $(LOCAL_PATH)/overlay/overlay_include.mk

LOCAL_PACKAGE_NAME := IdeaPillsCommon

# T2 compile error, link shared library error
LOCAL_32_BIT_ONLY := true

LOCAL_CERTIFICATE := platform

LOCAL_USE_FRAMEWORK_SMARTISANOS := true

include $(BUILD_PACKAGE)
