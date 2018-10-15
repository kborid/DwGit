AFTER_OREO := $(shell if [ "$(PLATFORM_SDK_VERSION)" -ge "26" ]; then echo true; else echo false; fi)

ORIGINAL_FILES := src/com/smartisanos/ideapills/common/util/MultiSdkUtils.java
ifeq ($(AFTER_OREO), true)
    LOCAL_SRC_FILES := $(subst $(ORIGINAL_FILES), overlay/oreo/MultiSdkUtils.java, ${LOCAL_SRC_FILES})
endif
