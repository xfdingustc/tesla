#include <cstdio>
#include <cstdlib>
#include "avrpro_malloc.h"

void *__avrpro_malloc(size_t size, const char *file, size_t line)
{
    void *ptr = malloc(size);

#ifdef AVRPRO_DEBUG
    if (ptr) {
        //add_record(size, ptr, MEM_MALLOC, file, line);
        ::memset(ptr, 0xFF, size);
    }
#endif

    return ptr;
}

void avrpro_free(void *ptr)
{
#ifdef avrpro_DEBUG
    if (ptr) {
        free(ptr);
    }
#endif

    free(ptr);
}

