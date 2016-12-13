#ifndef __AVRPRO_ARRAY_H__
#define __AVRPRO_ARRAY_H__

#include <stdint.h>
#include <string.h>
#include "avrpro_common.h"
#include "avrpro_malloc.h"

//-----------------------------------------------------------------------
//
// avrpro array
//
//-----------------------------------------------------------------------
template <typename DT>
struct DynamicArray
{
    DT *elems;
    uint32_t count;
    uint32_t cap;

    void _Init() {
        elems = NULL;
        count = 0;
        cap = 0;
    }

    void _Clear() {
        count = 0;
    }

    void _Release() {
        if (elems) {
            avrpro_free(elems);
            _Init();
        }
    }

    bool _Resize(uint32_t inc, uint32_t extra) {
        if (inc == 0) {
            inc = cap / 2;
            if (inc == 0) {
                inc = 2;
            }
        }
        uint32_t new_cap = cap + inc;
        DT *new_elems = (DT *)avrpro_malloc((new_cap + extra) * sizeof(DT));
        if (new_elems == NULL) {
            return false;
        }
        if (elems) {
            ::memcpy(new_elems, elems, count * sizeof(DT));
            avrpro_free(elems);
        }
        cap = new_cap;
        elems = new_elems;
        return true;
    }

    DT *_At(uint32_t index) {
        if (index >= count) {
            return NULL;
        }
        return &(elems[index]);
    }

    DT * _Tail() {
        if (count == 0) {
            return NULL;
        } else {
            return &(elems[count - 1]);
        }
    }

    int _Size() {
        return count;
    }
    // append at the tail of array, resize the array if needed
    // extra > 0: always reserve extra items at the tail. they are not counted
    DT *_Append(const DT *elem, uint32_t inc = 0, uint32_t extra = 0) {
        if (count == cap && !_Resize(inc, extra)) {
            return NULL;
        }
        DT *this_elem = elems + count;
        count++;
        if (elem) {
            *this_elem = *elem;
        }
        return this_elem;
    }

    DT *_Insert(const DT *elem, int index, uint32_t inc = 0, uint32_t extra = 0) {
        if (count == cap && !_Resize(inc, extra)) {
            return NULL;
        }
        for (int j = count; j > index; j--) {
            elems[j] = elems[j - 1];
        }
        count++;
        DT *this_elem = elems + index;
        if (elem) {
            *this_elem = *elem;
        }
        return this_elem;
    }

    void _Remove(uint32_t i) {
        count--;
        for (uint32_t j = i; j < count; j++) {
            elems[j] = elems[j + 1];
        }
    }

    // remove the elem from array
    bool _Remove(const DT& elem) {
        for (uint32_t i = 0; i < count; i++) {
            if (elem == elems[i]) {
                _Remove(i);
                return true;
            }
        }
        return false;
    }
};

#endif

