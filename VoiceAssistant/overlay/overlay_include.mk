AFTER_MARSHMALLOW := $(shell if [ "$(PLATFORM_SDK_VERSION)" -ge "23" ]; then echo true; else echo false; fi)
AFTER_LOLLIPOP := $(shell if [ "$(PLATFORM_SDK_VERSION)" -ge "21" ]; then echo true; else echo false; fi)
AFTER_OREO := $(shell if [ "$(PLATFORM_SDK_VERSION)" -ge "26" ]; then echo true; else echo false; fi)
AFTER_NOUGAT := $(shell if [ "$(PLATFORM_SDK_VERSION)" -ge "25" ]; then echo true; else echo false; fi)


EXCLUDE_FILES := src/com/smartisanos/sara/util/MultiSdkAdapter.java \
		src/com/smartisanos/sara/util/ActivityUtil.java
ifeq ($(AFTER_OREO), true)
    OVERLAY_FILES := $(call all-java-files-under, overlay/8.0)
else ifeq ($(AFTER_MARSHMALLOW), true)
    OVERLAY_FILES := $(call all-java-files-under, overlay/6.0)
else ifeq ($(AFTER_LOLLIPOP), true)
    OVERLAY_FILES := $(call all-java-files-under, overlay/5.0)
else
    OVERLAY_FILES := $(call all-java-files-under, overlay/4.4)
endif

LOCAL_SRC_FILES := $(filter-out $(EXCLUDE_FILES), $(LOCAL_SRC_FILES))
LOCAL_SRC_FILES += $(OVERLAY_FILES)

NOUGAT_NEED_EXCLUDE := \
                    src/com/smartisanos/sara/lock/widget/LockPasswordLayout.java \
                    src/com/smartisanos/sara/lock/widget/SecureLockManager.java \
                    src/com/smartisanos/sara/lock/widget/FingerprintController.java \
                    src/com/smartisanos/sara/lock/util/LockPasswordUtil.java

NOUGAT_NEED_INCLUDE := $(call all-java-files-under, overlay/nougat/)

ifeq ($(AFTER_NOUGAT), true)
    LOCAL_SRC_FILES := $(filter-out $(NOUGAT_NEED_EXCLUDE), $(LOCAL_SRC_FILES))
    LOCAL_SRC_FILES += $(NOUGAT_NEED_INCLUDE)
else ifeq ($(AFTER_NOUGAT), false)
endif


OREO_NEED_EXCLUDE := \
                    src/com/smartisanos/sara/bubble/revone/manager/StartedAppManager.java

OREO_NEED_INCLUDE := $(call all-java-files-under, overlay/oreo/)

COMMON_ORIGINAL_FILES := $(common_dir)/src/com/smartisanos/ideapills/common/util/MultiSdkUtils.java

ifeq ($(AFTER_OREO), true)
    LOCAL_SRC_FILES := $(filter-out $(OREO_NEED_EXCLUDE), $(LOCAL_SRC_FILES))
    LOCAL_SRC_FILES += $(OREO_NEED_INCLUDE)
    LOCAL_SRC_FILES := $(subst $(COMMON_ORIGINAL_FILES), $(common_dir)/overlay/oreo/MultiSdkUtils.java, ${LOCAL_SRC_FILES})
else ifeq ($(AFTER_OREO), false)
endif
