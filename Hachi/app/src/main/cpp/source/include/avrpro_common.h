#ifndef __AVRPRO_LOG_H__
#define __AVRPRO_LOG_H__
#include <stdint.h>

//-----------------------------------------------------------------------
//
//  log & assertion
//
//-----------------------------------------------------------------------

#define AVRPRO_LOG_E       (1 << 0)    // error
#define AVRPRO_LOG_W       (1 << 1)    // warning
#define AVRPRO_LOG_I       (1 << 2)    // info
#define AVRPRO_LOG_V       (1 << 3)    // verbose
#define AVRPRO_LOG_D       (1 << 4)    // debug
#define AVRPRO_LOG_P       (1 << 5)    // perror

#define ALL_LOGS        (AVRPRO_LOG_E|AVRPRO_LOG_W|AVRPRO_LOG_I|AVRPRO_LOG_D|AVRPRO_LOG_P)
#define BASE_LOGS       (AVRPRO_LOG_E|AVRPRO_LOG_W|AVRPRO_LOG_P|AVRPRO_LOG_I)
#define ERROR_LOGS      (AVRPRO_LOG_E)
#define NO_LOGS         (0)

#if defined(ANDROID_OS) || defined(WIN32_OS)

#define C_NONE

#define C_BLACK 
#define C_WHITE 

#define C_GRAY
#define C_LIGHT_GRAY

#define C_RED
#define C_LIGHT_RED

#define C_GREEN
#define C_LIGHT_GREEN

#define C_BLUE
#define C_LIGHT_BLUE

#define C_CYAN
#define C_LIGHT_CYAN

#define C_PURPLE
#define C_LIGHT_PURPLE

#define C_BROWN
#define C_YELLOW

#else

#define C_NONE          "\033[0m"

#define C_BLACK         "\033[0;30m"
#define C_WHITE         "\033[1;37m"

#define C_GRAY          "\033[1;30m"
#define C_LIGHT_GRAY    "\033[0;37m"

#define C_RED           "\033[0;31m"
#define C_LIGHT_RED     "\033[1;31m"

#define C_GREEN         "\033[0;32m"
#define C_LIGHT_GREEN   "\033[1;32m"

#define C_BLUE          "\033[0;34m"
#define C_LIGHT_BLUE    "\033[1;34m"

#define C_CYAN          "\033[0;36m"
#define C_LIGHT_CYAN    "\033[1;36m"

#define C_PURPLE        "\033[0;35m"
#define C_LIGHT_PURPLE  "\033[1;35m"

#define C_BROWN         "\033[0;33m"
#define C_YELLOW        "\033[1;33m"

#endif

extern uint32_t g_avrpro_log_flag;

#define AVRPRO_LOG_ALL "ewidp"

void avrpro_set_logs(const char *logs);

#ifndef LOG_TAG
#define LOG_TAG ""
#endif

#ifdef MINGW
#define LLD "%I64d"
#else
#define LLD "%" PRIi64
#endif

#ifdef WIN32_OS
#define FMT_ZD  "%d"
#else
#define FMT_ZD  "%zd"
#endif

#ifdef __ANDROID__

#include <android/log.h>

// =================================================
//  Android
// =================================================
#define LOG_TAG "JNITag"

#define AVRPRO_LOG(fmt, _logf, _alog, args...) \
    do { \
        if (test_flag(g_avrpro_log_flag, _logf)) { \
            __android_log_print(_alog, LOG_TAG, fmt, ##args); \
        } \
    } while (0)

#define AVRPRO_LOGE(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_E, ANDROID_LOG_ERROR, ##args)
#define AVRPRO_LOGW(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_W, ANDROID_LOG_WARN, ##args)
#define AVRPRO_LOGI(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_I, ANDROID_LOG_INFO, ##args)
#define AVRPRO_LOGV(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_V, ANDROID_LOG_VERBOSE, ##args)
#define AVRPRO_LOGD(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_D, ANDROID_LOG_DEBUG, ##args)
#define AVRPRO_LOGDW(fmt, args...) AVRPRO_LOG(fmt, AVRPRO_LOG_D, ANDROID_LOG_DEBUG, ##args)
#define AVRPRO_LOGP(fmt, args...)  AVRPRO_LOG(fmt ": %s", AVRPRO_LOG_P, ANDROID_LOG_ERROR, ##args, strerror(errno))

#elif defined(WIN32_OS) || defined(MAC_OS)

// =================================================
//  Windows/MAC
// =================================================

extern "C" void (*avrpro_print_proc)(const char *);

#define AVRPRO_LOG(fmt, _logf, _logs, args...) \
    do { \
        if (test_flag(g_avrpro_log_flag, _logf)) { \
            char _b_[512]; \
            sprintf(_b_, _logs LOG_TAG " - " fmt, ##args); \
            (avrpro_print_proc)(_b_); \
        } \
    } while (0)

#define AVRPRO_LOGE(fmt, args...)  AVRPRO_LOG(fmt " (line %d)", AVRPRO_LOG_W, "E/", ##args, __LINE__)
#define AVRPRO_LOGW(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_W, "W/", ##args)
#define AVRPRO_LOGI(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_I, "I/", ##args)
#define AVRPRO_LOGV(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_V, "V/", ##args)
#define AVRPRO_LOGD(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_D, "D/", ##args)
#define AVRPRO_LOGDW(fmt, args...) AVRPRO_LOG(fmt, AVRPRO_LOG_D, "D/", ##args)
#define AVRPRO_LOGP(fmt, args...)  AVRPRO_LOG(fmt ": %s", AVRPRO_LOG_P, "P/", ##args, strerror(errno))

#elif defined(IOS_OS)
// =================================================
//  IOS
// =================================================

extern "C" void (*avrpro_print_proc)(const char *);

#define AVRPRO_LOG(fmt, _logf, _logs, args...) \
do { \
if (test_flag(g_avrpro_log_flag, _logf)) { \
char _b_[512]; \
sprintf(_b_, _logs LOG_TAG " - " fmt, ##args); \
(avrpro_print_proc)(_b_); \
} \
} while (0)

#define AVRPRO_LOGE(fmt, args...)  AVRPRO_LOG(fmt " (line %d)", AVRPRO_LOG_W, "E/", ##args, __LINE__)
#define AVRPRO_LOGW(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_W, "W/", ##args)
#define AVRPRO_LOGI(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_I, "I/", ##args)
#define AVRPRO_LOGV(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_V, "V/", ##args)
#define AVRPRO_LOGD(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_D, "D/", ##args)
#define AVRPRO_LOGDW(fmt, args...) AVRPRO_LOG(fmt, AVRPRO_LOG_D, "D/", ##args)
#define AVRPRO_LOGP(fmt, args...)  AVRPRO_LOG(fmt ": %s", AVRPRO_LOG_P, "P/", ##args, strerror(errno))

#endif

#if 0

// =================================================
//  Others
// =================================================

extern int (*avrpro_printf)(const char *fmt, ...);

#define AVRPRO_LOG(fmt, _logf, _logs, _cbegin, _cend, args...) \
    do { \
        if (test_flag(g_avrpro_log_flag, _logf)) { \
            avrpro_printf(_cbegin _logs LOG_TAG " - " fmt "\n" _cend, ##args); \
        } \
    } while (0)

#define AVRPRO_LOGE(fmt, args...)  AVRPRO_LOG(fmt " (line %d)", AVRPRO_LOG_E, "E/", C_RED, C_NONE, ##args, __LINE__)
#define AVRPRO_LOGW(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_W, "W/", C_PURPLE, C_NONE, ##args)
#define AVRPRO_LOGI(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_I, "I/", , , ##args)
#define AVRPRO_LOGV(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_V, "V/", , , ##args)
#define AVRPRO_LOGD(fmt, args...)  AVRPRO_LOG(fmt, AVRPRO_LOG_D, "D/", , , ##args)
#define AVRPRO_LOGDW(fmt, args...) AVRPRO_LOG(fmt, AVRPRO_LOG_D, "D/", C_PURPLE, C_NONE, ##args)
#define AVRPRO_LOGP(fmt, args...)  AVRPRO_LOG(fmt ": %s", AVRPRO_LOG_P, "P/", C_RED, C_NONE, ##args, strerror(errno))

#endif

#define set_flag(_flags, _f) \
    do { (_flags) |= (_f); } while (0)

#define clear_flag(_flags, _f) \
    do { (_flags) &= ~(_f); } while (0)

#define test_flag(_flags, _f) \
    ((_flags) & (_f))

#define set_or_clear_flag(_cond, _flags, _f) \
    do { \
        if (_cond) set_flag(_flags, _f); \
        else clear_flag(_flags, _f); \
    } while (0)

//-----------------------------------------------------------------------
//
//  byte ordering
//
//-----------------------------------------------------------------------
static inline uint32_t avrpro_read_be_32(uint8_t *ptr) {
    return (uint32_t)(ptr[0] << 24) | 
            (uint32_t)(ptr[1] << 16) | 
            (uint32_t)(ptr[2] << 8) | 
            (uint32_t)ptr[3];
}

static inline uint32_t avrpro_read_be_16(uint8_t *ptr) {
    return (ptr[0] << 8) | ptr[1];
}

static inline void avrpro_write_be_32(uint8_t *ptr, uint32_t value) {
    ptr[0] = value >> 24;
    ptr[1] = value >> 16;
    ptr[2] = value >> 8;
    ptr[3] = value;
}

#endif
