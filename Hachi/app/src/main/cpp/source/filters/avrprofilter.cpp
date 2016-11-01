//
// Created by lshw on 16/10/27.
//
#include <cstdio>
#include <cstdlib>
#include <string.h>
#include <android/log.h>
#include "../../include/avrpro_std_data.h"
#include "../../include/com_waylens_hachi_snipe_remix_AvrproFilter.h"
#include "../include/avrpro_common.h"


static avrpro_smart_filter smart_filter = NULL;

JNIEXPORT jint JNICALL Java_com_waylens_hachi_snipe_remix_AvrproFilter_native_1avrpro_1smart_1filter_1init
        (JNIEnv *env, jobject obj, jint type, jstring localDirectory, jint targetLength) {
    SMART_FILTER_TYPE filter_type = SMART_RANDOMCUTTING;
    if (type >= 0 && type < SMART_MAX_INDEX) {
        filter_type = SMART_FILTER_TYPE(type);
    }
    const char * local_directory = NULL;
    jboolean isCopy;
    local_directory = (*env).GetStringUTFChars(localDirectory, &isCopy);
    avrpro_smart_filter ret = avrpro_smart_filter_init(filter_type, local_directory, targetLength);
    if (ret != NULL) {
        smart_filter = ret;
        return 0;
    } else {
        return -1;
    }
}

/*
 * Class:     com_waylens_hachi_snipe_remix_AvrproFilter
 * Method:    native_avrpro_smart_filter_is_data_parsed
 * Signature: (ILcom/waylens/hachi/snipe/remix/AvrproClipInfo;II)Z
 */
JNIEXPORT jboolean JNICALL Java_com_waylens_hachi_snipe_remix_AvrproFilter_native_1avrpro_1smart_1filter_1is_1data_1parsed
        (JNIEnv *env, jobject obj, jint initRet, jobject clipInfo, jint offset, jint duration) {
    avrpro_clip_info_t clip_info;
    jclass clazz;
    jboolean isCopy;
    clazz = (*env).GetObjectClass(clipInfo);
    jfieldID ClipInfo_guid_str_fieldId = (*env).GetFieldID(clazz, "guid_str", "Ljava/lang/String;");
    jstring ClipInfo_guid_str_field = (jstring)(*env).GetObjectField(clipInfo, ClipInfo_guid_str_fieldId);
    const char* guid_str = (*env).GetStringUTFChars(ClipInfo_guid_str_field, &isCopy);
    memcpy(clip_info.guid_str, guid_str, strlen(guid_str) + 1);
    AVRPRO_LOGE("guid_str = %s ; length = %d", clip_info.guid_str, strlen(clip_info.guid_str));

    jfieldID ClipInfo_id_fieldId = (*env).GetFieldID(clazz, "id", "I");
    jint ClipInfo_id_field = (*env).GetIntField(clipInfo, ClipInfo_id_fieldId);
    clip_info.id = ClipInfo_id_field;

    jfieldID ClipInfo_type_fieldId = (*env).GetFieldID(clazz, "type", "I");
    jint ClipInfo_type_field = (*env).GetIntField(clipInfo, ClipInfo_type_fieldId);
    clip_info.type = ClipInfo_type_field;

    jfieldID ClipInfo_start_time_lo_fieldId = (*env).GetFieldID(clazz, "start_time_lo", "I");
    jint ClipInfo_start_time_lo_field = (*env).GetIntField(clipInfo, ClipInfo_start_time_lo_fieldId);
    clip_info.start_time_lo = ClipInfo_start_time_lo_field;

    jfieldID ClipInfo_start_time_hi_fieldId = (*env).GetFieldID(clazz, "start_time_hi", "I");
    jint ClipInfo_start_time_hi_field = (*env).GetIntField(clipInfo, ClipInfo_start_time_hi_fieldId);
    clip_info.start_time_hi = ClipInfo_start_time_hi_field;

    jfieldID ClipInfo_duration_ms_fieldId = (*env).GetFieldID(clazz, "duration_ms", "I");
    jint ClipInfo_duration_ms_field = (*env).GetIntField(clipInfo, ClipInfo_duration_ms_fieldId);
    clip_info.duration_ms = ClipInfo_duration_ms_field;

    jboolean ret = avrpro_smart_filter_is_data_parsed(smart_filter, &clip_info, offset, duration);

    return ret;

}

/*
 * Class:     com_waylens_hachi_snipe_remix_AvrproFilter
 * Method:    native_avrpro_smart_filter_feed_data
 * Signature: (I[BI)I
 */
JNIEXPORT jint JNICALL Java_com_waylens_hachi_snipe_remix_AvrproFilter_native_1avrpro_1smart_1filter_1feed_1data
        (JNIEnv *env, jobject obj, jint filterRet, jbyteArray dataBuf, jint size, jint sourceType) {
    jbyte* data_buf = (jbyte*)malloc(size);
    memset(data_buf, 0, size);
    jboolean isCopy;
    (*env).GetByteArrayRegion(dataBuf, 0, size, data_buf);

    RAW_DATA_SOURCE_TYPE source_type = DEVICE_ANDROID;
    jint ret = avrpro_smart_filter_feed_data(smart_filter, (uint8_t*)data_buf, size, source_type);
    free(data_buf);
    return ret;
}

/*
 * Class:     com_waylens_hachi_snipe_remix_AvrproFilter
 * Method:    native_avrpro_smart_filter_read_results
 * Signature: (IZ)Lcom/waylens/hachi/snipe/remix/AvrproSegmentInfo;
 */
JNIEXPORT jobject JNICALL Java_com_waylens_hachi_snipe_remix_AvrproFilter_native_1avrpro_1smart_1filter_1read_1results
        (JNIEnv *env, jobject obj, jint filterRet, jboolean fromResult) {
    avrpro_segment_info_t segment_info;
    avrpro_clip_info_t *clip_info;
    jobject result;
    int ret = avrpro_smart_filter_read_results(smart_filter, &segment_info, fromResult);
    __android_log_print(ANDROID_LOG_DEBUG, "JNITag", "%d", ret);
    if (ret == 0) {
        if (segment_info.parent_clip != NULL) {
            clip_info = segment_info.parent_clip;
            jclass ClipInfo_class = (*env).FindClass("com/waylens/hachi/snipe/remix/AvrproClipInfo");
            jmethodID ClipInfo_class_methodId = (*env).GetMethodID(ClipInfo_class, "<init>", "(Ljava/lang/String;IIIII)V");
            __android_log_print(ANDROID_LOG_DEBUG, "JNITag", "guid_str length = %d", strlen(clip_info->guid_str));
            jstring ClipInfo_guid_str = (*env).NewStringUTF((const char*)(clip_info->guid_str));
            jobject instanceClipInfoResult = (*env).NewObject(ClipInfo_class, ClipInfo_class_methodId, ClipInfo_guid_str,
                                            clip_info->id, clip_info->type, clip_info->start_time_lo, clip_info->start_time_hi,
                                            clip_info->duration_ms);
            AVRPRO_LOGD("time_ms_low: %d, time_ms_high: %d", clip_info->start_time_lo, clip_info->start_time_hi);
            jclass SegmentInfo_class = (*env).FindClass("com/waylens/hachi/snipe/remix/AvrproSegmentInfo");
            jmethodID SegmentInfo_class_methodId = (*env).GetMethodID(SegmentInfo_class, "<init>", "(Lcom/waylens/hachi/snipe/remix/AvrproClipInfo;IIII)V");
            jobject instanceSegmentInfoResult = (*env).NewObject(SegmentInfo_class, SegmentInfo_class_methodId, instanceClipInfoResult,
                                                                 segment_info.inclip_offset_ms, segment_info.duration_ms, segment_info.filter_type,
                                                                segment_info.max_speed_kph);
            AVRPRO_LOGD("in clip offset: %d, duration: %d", segment_info.inclip_offset_ms, segment_info.duration_ms);
            return instanceSegmentInfoResult;
        }
    }
    return NULL;
}
/*
 * Class:     com_waylens_hachi_snipe_remix_AvrproFilter
 * Method:    native_avrpro_smart_filter_deint
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_waylens_hachi_snipe_remix_AvrproFilter_native_1avrpro_1smart_1filter_1deint
        (JNIEnv *env, jobject obj, jint filterRet) {
    return avrpro_smart_filter_deinit(smart_filter);
}