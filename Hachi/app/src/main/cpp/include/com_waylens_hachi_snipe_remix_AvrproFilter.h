/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_waylens_hachi_snipe_remix_AvrproFilter */

#ifndef _Included_com_waylens_hachi_snipe_remix_AvrproFilter
#define _Included_com_waylens_hachi_snipe_remix_AvrproFilter
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_waylens_hachi_snipe_remix_AvrproFilter
 * Method:    native_avrpro_smart_filter_init
 * Signature: (ILjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_waylens_hachi_snipe_remix_AvrproFilter_native_1avrpro_1smart_1filter_1init
  (JNIEnv *, jobject, jint, jstring, jint);

/*
 * Class:     com_waylens_hachi_snipe_remix_AvrproFilter
 * Method:    native_avrpro_smart_filter_is_data_parsed
 * Signature: (ILcom/waylens/hachi/snipe/remix/AvrproClipInfo;II)Z
 */
JNIEXPORT jboolean JNICALL Java_com_waylens_hachi_snipe_remix_AvrproFilter_native_1avrpro_1smart_1filter_1is_1data_1parsed
  (JNIEnv *, jobject, jint, jobject, jint, jint);

/*
 * Class:     com_waylens_hachi_snipe_remix_AvrproFilter
 * Method:    native_avrpro_smart_filter_feed_data
 * Signature: (I[BI)I
 */
JNIEXPORT jint JNICALL Java_com_waylens_hachi_snipe_remix_AvrproFilter_native_1avrpro_1smart_1filter_1feed_1data
  (JNIEnv *, jobject, jint, jbyteArray, jint, jint);

/*
 * Class:     com_waylens_hachi_snipe_remix_AvrproFilter
 * Method:    native_avrpro_smart_filter_read_results
 * Signature: (IZ)Lcom/waylens/hachi/snipe/remix/AvrproSegmentInfo;
 */
JNIEXPORT jobject JNICALL Java_com_waylens_hachi_snipe_remix_AvrproFilter_native_1avrpro_1smart_1filter_1read_1results
  (JNIEnv *, jobject, jint, jboolean);

/*
 * Class:     com_waylens_hachi_snipe_remix_AvrproFilter
 * Method:    native_avrpro_smart_filter_deint
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_waylens_hachi_snipe_remix_AvrproFilter_native_1avrpro_1smart_1filter_1deint
  (JNIEnv *, jobject, jint);

#ifdef __cplusplus
}
#endif
#endif