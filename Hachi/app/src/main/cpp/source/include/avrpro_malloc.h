#ifndef __AVRPRO_MALLOC_H__
#define __AVRPRO_MALLOC_H__

void *__avrpro_malloc(size_t size, const char *file, size_t line);
char *__avrpro_strdup(const char *str, const char *file, size_t line);
void avrpro_free(void *ptr);

#ifdef AVRPRO_DEBUG

#define avrpro_malloc(_size) \
    __avrpro_malloc(_size, __FILE__, __LINE__)

#define avrpro_strdup(_str) \
    __avrpro_strdup(_str, __FILE__, __LINE__)

#else

#define avrpro_malloc(_size) \
    __avrpro_malloc(_size, NULL, 0)

#define avrpro_strdup(_str) \
    __avrpro_strdup(_str, NULL, 0)

#endif

// todo - calloc, realloc

#define __avrpro_safe_free(_ptr) \
    do { \
        if (_ptr) { \
            avrpro_free(_ptr); \
        } \
    } while (0)

#define avrpro_safe_free(_ptr) \
    do { \
        if (_ptr) { \
            avrpro_free(_ptr); \
            _ptr = NULL; \
        } \
    } while (0)

#endif

