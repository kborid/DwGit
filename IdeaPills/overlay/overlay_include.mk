AFTER_OREO := $(shell if [ "$(PLATFORM_SDK_VERSION)" -ge "26" ]; then echo true; else echo false; fi)

OREO_NEED_EXCLUDE := \
                    src/com/smartisanos/ideapills/ExtDisplayProxyActivity.java \
                    src/com/smartisanos/ideapills/PptActivity.java

OREO_NEED_INCLUDE := $(call all-java-files-under, overlay/oreo/)

COMMON_ORIGINAL_FILES := $(common_dir)/src/com/smartisanos/ideapills/common/util/MultiSdkUtils.java

ifeq ($(AFTER_OREO), true)
    LOCAL_SRC_FILES := $(filter-out $(OREO_NEED_EXCLUDE), $(LOCAL_SRC_FILES))
    LOCAL_SRC_FILES += $(OREO_NEED_INCLUDE)
    LOCAL_SRC_FILES := $(subst $(COMMON_ORIGINAL_FILES), $(common_dir)/overlay/oreo/MultiSdkUtils.java, ${LOCAL_SRC_FILES})
else ifeq ($(AFTER_OREO), false)
endif
