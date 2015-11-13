//
// Created by Xiaofei on 2015/11/12.
//
#define LOG_TAG "remuxer_jni"

#include "jni.h"
#include "com_waylens_hachi_ui_services_download_HttpRemuxer.h"


JNIEXPORT void JNICALL Java_com_waylens_hachi_ui_services_download_HttpRemuxer_native_1init
        (JNIEnv *env, jobject thiz) {

}

/*
 * Class:     com_waylens_hachi_ui_services_download_HttpRemuxer
 * Method:    native_release
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_waylens_hachi_ui_services_download_HttpRemuxer_native_1release
        (JNIEnv *env, jobject thiz) {

}

/*
 * Class:     com_waylens_hachi_ui_services_download_HttpRemuxer
 * Method:    native_set_iframe_only
 * Signature: (Z)I
 */
JNIEXPORT jint JNICALL Java_com_waylens_hachi_ui_services_download_HttpRemuxer_native_1set_1iframe_1only
        (JNIEnv *env, jobject thiz, jboolean bIframeOnly) {
    return 0;
}

/*
 * Class:     com_waylens_hachi_ui_services_download_HttpRemuxer
 * Method:    native_setAudio
 * Signature: (ZLjava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_waylens_hachi_ui_services_download_HttpRemuxer_native_1setAudio
        (JNIEnv *env, jobject thiz, jboolean disableAudio, jstring audioFileName, jstring format) {
    return 0;
}

/*
 * Class:     com_waylens_hachi_ui_services_download_HttpRemuxer
 * Method:    native_run
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_waylens_hachi_ui_services_download_HttpRemuxer_native_1run
        (JNIEnv * env, jobject thiz, jstring inputFile, jstring inputFormat, jstring outputFile,
         jstring outputFormat, jint duration_ms) {
    return 0;
}

/*
 * Class:     com_waylens_hachi_ui_services_download_HttpRemuxer
 * Method:    native_finalize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_waylens_hachi_ui_services_download_HttpRemuxer_native_1finalize
        (JNIEnv *env, jobject thiz) {

}
