#LOCAL_PATH:=$(call my-dir)

#include $(CLEAR_VARS)
#OpenCV_INSTALL_MODULES := on
#OpenCV_CAMERA_MODULES := off
#OPENCV_LIB_TYPE :=STATIC

#ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
#include $(LOCAL_PATH)/native/jni/OpenCV.mk
#else
#include $(OPENCV_MK_PATH)
#endif
#LOCAL_MODULE := blur
#LOCAL_SRC_FILES := blur.c
#LOCAL_LDLIBS +=  -lm -llog
#include $(BUILD_SHARED_LIBRARY)






LOCAL_PATH := $(call my-dir)

# Create BitmapUtils library

include $(CLEAR_VARS)

LOCAL_LDLIBS    := -llog -ljnigraphics

LOCAL_MODULE    := blur
LOCAL_SRC_FILES := blur.c

LOCAL_CFLAGS    =  -ffast-math -O3 -funroll-loops

include $(BUILD_SHARED_LIBRARY)

# Renderscript support library is not available for ARM before v7a currently
ifneq ($(TARGET_ARCH_ABI),armeabi)
	# Add prebuilts for Renderscript Support
	#include $(CLEAR_VARS)

	#LOCAL_MODULE := librsjni
	#LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/librsjni.so

	#include $(PREBUILT_SHARED_LIBRARY)

	#include $(CLEAR_VARS)

	#LOCAL_MODULE := libRSSupport
	#LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libRSSupport.so
	#include $(PREBUILT_SHARED_LIBRARY)
endif

