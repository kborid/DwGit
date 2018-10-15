LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
#include frameworks/av/media/libstagefright/codecs/common/Config.mk

LOCAL_MODULE := libmp3lame_va

LOCAL_C_INCLUDES := \
$(LOCAL_PATH)/include/ \
$(LOCAL_PATH)/frontend/ \
$(LOCAL_PATH)/libmp3lame/ \
$(LOCAL_PATH)/mpglib


LOCAL_SRC_FILES := \
libmp3lame/bitstream.c \
libmp3lame/fft.c \
libmp3lame/id3tag.c \
libmp3lame/mpglib_interface.c \
libmp3lame/presets.c \
libmp3lame/quantize.c \
libmp3lame/reservoir.c \
libmp3lame/tables.c \
libmp3lame/util.c \
libmp3lame/VbrTag.c \
libmp3lame/encoder.c \
libmp3lame/gain_analysis.c \
libmp3lame/lame.c \
libmp3lame/newmdct.c \
libmp3lame/psymodel.c \
libmp3lame/quantize_pvt.c \
libmp3lame/set_get.c \
libmp3lame/takehiro.c \
libmp3lame/vbrquantize.c \
libmp3lame/version.c \
mpglib/common.c \
mpglib/dct64_i386.c \
mpglib/decode_i386.c \
mpglib/layer1.c \
mpglib/layer2.c \
mpglib/layer3.c \
mpglib/tabinit.c \
mpglib/interface.c \
frontend/main.c \
frontend/lame_main.c \
frontend/amiga_mpega.c \
frontend/brhist.c \
frontend/get_audio.c \
frontend/lametime.c \
frontend/parse.c \
frontend/timestatus.c \
frontend/console.c 

LOCAL_CFLAGS += -DSTDC_HEADERS -DHAVE_MPGLIB -DHAVE_STDINT_H -DHAVE_LIMITS_H

#LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_TAGS := debug

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
