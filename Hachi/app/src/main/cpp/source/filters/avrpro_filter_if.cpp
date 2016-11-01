#include <stdint.h>
#include <stdio.h>
#include "avf_std_media.h"
#include "avf_error_code.h"
#include "vdb_local.h"
#include "vdb_cmd.h"
#include "../include/avrpro_array.h"
#include "../include/avrpro_malloc.h"
#include "../include/avrpro_common.h"
#include "avrpro_std_data.h"
#include "../include/avrpro_filter_manager.h"

uint32_t g_avrpro_log_flag = ALL_LOGS;

void parseRawDataBlock(FilterManager * manager, uint8_t * data_buf, uint32_t size);
void parseRawDataFromIOSBlock(FilterManager * manager, uint8_t * data_buf, uint32_t size);
void parseRawDataFromAndroidBlock(FilterManager * manager, uint8_t * data_buf, uint32_t size);

#ifndef TOTAL_PID
#define TOTAL_PID	128
static const uint8_t g_pid_data_size_table[TOTAL_PID] =
{
    4,4,2,2,1,1,2,2,		// 00 - 07
    2,2,1,1,2,1,1,1,		// 08 - 0F

    2,1,1,1,2,2,2,2,		// 10 - 17
    2,2,2,2,1,1,1,2,		// 18 - 1F

    4,2,2,2,4,4,4,4,		// 20 - 27
    4,4,4,4,1,1,1,1,		// 28 - 2F

    1,2,2,1,4,4,4,4,		// 30 - 37
    4,4,4,4,2,2,2,2,		// 38 - 3F

    4,4,2,2,2,1,1,1,		// 40 - 47
    1,1,1,1,1,2,2,4,		// 48 - 4F

    4,1,1,2,2,2,2,2,		// 50 - 57
    2,2,1,1,1,2,2,1,		// 58 - 5F

    4,1,1,2,5,2,5,3,		// 60 - 67
    7,7,5,5,5,6,5,3,		// 68 - 6F

    9,5,5,5,5,7,7,5,		// 70 - 77
    9,9,7,7,9,1,1,13,		// 78 - 7F
};
#endif

static void default_print_proc(const char *msg)
{
    printf("%s\n", msg);
}

void (*avrpro_print_proc)(const char *) = default_print_proc;

void avrpro_set_print_proc(void (*p)(const char*))
{
    avrpro_print_proc = p;
}

void avrpro_set_logs(const char *logs)
{
    int result = 0;
    while (true) {
        int c = *logs++;
        if (c == 0)
            break;
        switch (c) {
        case 'e': result |= AVRPRO_LOG_E; break;
        case 'w': result |= AVRPRO_LOG_W; break;
        case 'i': result |= AVRPRO_LOG_I; break;
        case 'v': result |= AVRPRO_LOG_V; break;
        case 'd': result |= AVRPRO_LOG_D; break;
        case 'p': result |= AVRPRO_LOG_P; break;
        }
    }
    g_avrpro_log_flag = result;
}

avrpro_smart_filter avrpro_smart_filter_init(enum SMART_FILTER_TYPE type, const char * local_directory, uint32_t target_length)
{
    FilterManager * mgr = new FilterManager(type, local_directory, target_length);
    return (avrpro_smart_filter)mgr;
}

bool avrpro_smart_filter_is_data_parsed(avrpro_smart_filter filter, avrpro_clip_info_t * ci, uint32_t offset,
                                        uint32_t duration)
{
    FilterManager * mgr = (FilterManager *)filter;
    if (mgr->isClipInfoChanged(ci)) {
        mgr->resetManager();
    }
    return mgr->isTypeFilteredAndCached();
}

int avrpro_smart_filter_feed_data(avrpro_smart_filter filter, uint8_t * data_buf, uint32_t size, RAW_DATA_SOURCE_TYPE source)
{
    FilterManager * mgr = (FilterManager *)filter;
    if (!mgr) {
        AVRPRO_LOGE("cannot feed because null smart filter manager.");
        return -1;
    }

    if (!data_buf || size == 0) {
        AVRPRO_LOGW("wrong raw data block!");
        return 0;
    }

    switch (source) {
        case DEVICE_AVF: {
            parseRawDataBlock(mgr, data_buf, size);
        }
            break;
        case DEVICE_IOS: {
            parseRawDataFromIOSBlock(mgr, data_buf, size);
        }
            break;
        case DEVICE_ANDROID: {
            parseRawDataFromAndroidBlock(mgr, data_buf, size);
        }
            break;
        default:
            break;
    }

    return 0;
}

int avrpro_smart_filter_read_results(avrpro_smart_filter filter, avrpro_segment_info_t * si, bool fromStart)
{
    FilterManager * mgr = (FilterManager *)filter;
    if (!mgr) {
        AVRPRO_LOGE("NULL smart filter manager");
        return -1;
    }
    return mgr->fetchNextFilteredSegInfo(si, fromStart);
}

int avrpro_smart_filter_deinit(avrpro_smart_filter filter)
{
    FilterManager * mgr = (FilterManager *)filter;
    if (filter) {
        AVRPRO_LOGE("delete smart filter");
        delete mgr;
    }
    return 0;
}

void parseOBDv1(uint8_t * data_buf, uint32_t offset, avrpro_obd_parsed_data_t * pOBD)
{
    obd_raw_data_t *obdData = (obd_raw_data_t *)(data_buf + offset);
    obd_index_t *indexArray = (obd_index_t *)((const char *)(data_buf + offset)
                                              + sizeof(obd_raw_data_t));
    int indexNum = obdData->pid_info_size / sizeof(obd_index_t);
    uint8_t *data = (uint8_t *)((const char*)(data_buf + offset) +
                                            sizeof(obd_raw_data_t) + obdData->pid_info_size);

    if(indexNum > 0x0d) {
        /*if (indexArray[0x05].flag & 0x1) {
            pOBD->temperature = data[indexArray[0x05].offset];
            pOBD->temperature -= 40;
            pOBD->valid |= 1 << OBD_TEMP;
        }*/
        if (indexArray[0x0c].flag & 0x1) {
            pOBD->rpm = data[indexArray[0x0c].offset];
            pOBD->rpm <<= 8;
            pOBD->rpm |= data[indexArray[0x0c].offset+1];
            pOBD->rpm >>= 2;
        }
        if (indexArray[0x0d].flag & 0x1) {
            pOBD->speed = data[indexArray[0x0d].offset];
        }
    }
    if (indexNum > 0x11) {
        if (indexArray[0x11].flag & 0x1) {
            /* throttle position */
            pOBD->throttle_position = data[indexArray[0x11].offset] * 100 / 256;

        }
    }
}

void parseOBDv2(uint8_t * data_buf, uint32_t offset, avrpro_obd_parsed_data_t * pOBD)
{
    uint8_t pid = 0;
    uint8_t * data = NULL;
    uint32_t count = offset;
//    int pid_0b = -1;
//    int pid_0c = -1;
//    int pid_33 = -1;
//    int pid_4f = -1;
//    int pid_62 = -1;
//    int pid_63 = -1;

    for (;;) {
        pid = *(uint8_t *)(data_buf + count);
        if (pid == 0) break;
        count += 1;
        data = (uint8_t *)(data_buf + count);
        switch (pid) {
        case 0x0c:
            pOBD->rpm = data[0];
            pOBD->rpm <<= 8;
            pOBD->rpm |= data[1];
            pOBD->rpm >>= 2;
            break;
        case 0x0d:
            pOBD->speed = data[0];
            break;
        case 0x11:
            /* throttle position */
            pOBD->throttle_position = data[0] * 100 / 256;
            break;
        default:
            break;
        }
        count += g_pid_data_size_table[pid];
    }
    return;
}

void parseRawDataBlock(FilterManager * manager, uint8_t * data_buf, uint32_t size)
{
    uint64_t time_ms;
    uint32_t parsed = 0;
    uint32_t data_type = 0;
    uint32_t num_items = 0;

    parsed = 12;
    if (parsed < size) {
        data_type = *(uint16_t *)(data_buf + parsed);
        parsed += 4;    // jump another 2 bytes of data options
        uint32_t time_ms_lo = *(uint32_t *)(data_buf + parsed);
        parsed += 4;
        uint32_t time_ms_hi = *(uint32_t *)(data_buf + parsed);
        AVRPRO_LOGD("time_ms_low: %d, time_ms_high: %d", time_ms_lo, time_ms_hi);
        parsed += 4;
        time_ms = (uint64_t)((uint64_t)(time_ms_hi) << 32);
        time_ms += (uint64_t)time_ms_lo;
        num_items = *(uint32_t *)(data_buf + parsed);
        parsed += 4;
        if (num_items == 0) {
            AVRPRO_LOGD("no item in block");
            return;
        }
        parsed += 4; // jump data_size
        vdb_raw_data_index_t * index = (vdb_raw_data_index_t *)(data_buf + parsed);
        parsed += num_items * sizeof(vdb_raw_data_index_t);

        if (data_type == kRawData_OBD) {
            for (uint32_t i = 0; i < num_items; i++) {
                avrpro_obd_parsed_data_t obd_parsed;
                memset(&obd_parsed, 0, sizeof(avrpro_obd_parsed_data_t));
                obd_parsed.clip_time_ms = time_ms + index[i].time_offset_ms;
                uint8_t obd_ver = *(uint8_t *)(data_buf + parsed);
                if (index[i].data_size > 0 && obd_ver == OBD_VERSION_1) {
                    parseOBDv1(data_buf, parsed, &obd_parsed);
                } else if (index[i].data_size > 0 && obd_ver == OBD_VERSION_2) {
                    parseOBDv2(data_buf, parsed + 1, &obd_parsed);
                }
                manager->addOBDNode(&obd_parsed);
                parsed += index[i].data_size;
            }
        } else if (data_type == kRawData_ACC) {
            for (uint32_t i = 0; i < num_items; i++) {
                avrpro_iio_parsed_data_t pose_parsed;
                memset(&pose_parsed, 0, sizeof(avrpro_iio_parsed_data_t));
                pose_parsed.clip_time_ms = time_ms + index[i].time_offset_ms;
                if (index[i].data_size == sizeof(acc_raw_data_t)) {
                    acc_raw_data_t * acc_buf = (acc_raw_data_t *)(data_buf + parsed);
                    pose_parsed.accel_x = acc_buf->accel_x;
                    pose_parsed.accel_y = acc_buf->accel_y;
                    pose_parsed.accel_z = acc_buf->accel_z;
                } else if (index[i].data_size == sizeof(iio_raw_data_t)) {
                    iio_raw_data_t * iio_buf = (iio_raw_data_t *)(data_buf + parsed);
                    pose_parsed.accel_x = iio_buf->accel_x;
                    pose_parsed.accel_y = iio_buf->accel_y;
                    pose_parsed.accel_z = iio_buf->accel_z;
                    pose_parsed.euler_heading = iio_buf->euler_heading;
                    pose_parsed.euler_pitch = iio_buf->euler_pitch;
                    pose_parsed.euler_roll = iio_buf->euler_roll;
                }
                manager->addIIONode(&pose_parsed);
                parsed += index[i].data_size;
            }
        } else if (data_type == kRawData_GPS) {
            for (uint32_t i = 0; i < num_items; i++) {
                avrpro_gps_parsed_data_t gps_parsed;
                memset(&gps_parsed, 0, sizeof(avrpro_gps_parsed_data_t));
                gps_parsed.clip_time_ms = time_ms + index[i].time_offset_ms;
                if (index[i].data_size == sizeof(gps_raw_data_v3_t)) {
                    gps_raw_data_v3_t * gps_buf = (gps_raw_data_v3_t *)(data_buf + parsed);
                    gps_parsed.altitude = gps_buf->altitude;
                    gps_parsed.latitude = gps_buf->latitude;
                    gps_parsed.longitude = gps_buf->longitude;
                    gps_parsed.speed = gps_buf->speed;
                    gps_parsed.track = gps_buf->track;
                    gps_parsed.utc_time = gps_buf->utc_time;
                    gps_parsed.utc_time_usec = gps_buf->utc_time_usec;
                }
                manager->addGPSNode(&gps_parsed);
                parsed += index[i].data_size;
            }
        } else {
            AVRPRO_LOGW("wrong data type: %d", data_type);
        }
    }
}

void parseRawDataFromIOSBlock(FilterManager * manager, uint8_t * data_buf, uint32_t size)
{
#ifdef IOS_OS
    if (size < sizeof(avrpro_ios_data_header_t)) {
        AVRPRO_LOGE("Cannot parse ios data, size is tooo small %d.", size);
        return;
    }
    avrpro_ios_data_header_t *header = (avrpro_ios_data_header_t*)data_buf;
    switch (header->data_type) {
        case kRawData_ACC: {
            iioInfor* ptr = (iioInfor*)(data_buf + sizeof(avrpro_ios_data_header_t));
            for (int i = 0; i < header->data_count; i++) {
                avrpro_iio_parsed_data_t pose_parsed;
                memset(&pose_parsed, 0, sizeof(avrpro_iio_parsed_data_t));
                pose_parsed.clip_time_ms = ptr->time * 1000;
                pose_parsed.accel_x = ptr->iio.accel_x;
                pose_parsed.accel_y = ptr->iio.accel_y;
                pose_parsed.accel_z = ptr->iio.accel_z;
                pose_parsed.euler_heading = ptr->iio.euler_heading;
                pose_parsed.euler_pitch = ptr->iio.euler_pitch;
                pose_parsed.euler_roll = ptr->iio.euler_roll;
                manager->addIIONode(&pose_parsed);
                ptr += 1;
            }
        }
            break;
        case kRawData_GPS: {
            gpsInfor* ptr = (gpsInfor*)(data_buf + sizeof(avrpro_ios_data_header_t));
            for (int i = 0; i < header->data_count; i++) {
                avrpro_gps_parsed_data_t gps_parsed;
                memset(&gps_parsed, 0, sizeof(avrpro_gps_parsed_data_t));
                gps_parsed.clip_time_ms = ptr->time * 1000;
                gps_parsed.altitude = ptr->altitude;
                gps_parsed.latitude = ptr->latitude;
                gps_parsed.longitude = ptr->longtitude;
                gps_parsed.speed = ptr->speed;
                gps_parsed.track = ptr->orientation;
                gps_parsed.utc_time = (uint32_t)(ptr->absoluteTime / 1000);
                gps_parsed.utc_time_usec = (uint32_t)((ptr->absoluteTime % 1000) * 1000);
                manager->addGPSNode(&gps_parsed);
                ptr += 1;
            }
        }
            break;
        case kRawData_OBD: {
            obdInfor* ptr = (obdInfor*)(data_buf + sizeof(avrpro_ios_data_header_t));
            for (int i = 0; i < header->data_count; i++) {
                avrpro_obd_parsed_data_t obd_parsed;
                memset(&obd_parsed, 0, sizeof(avrpro_obd_parsed_data_t));
                obd_parsed.clip_time_ms = ptr->time * 1000;
                obd_parsed.speed = ptr->speed;
                obd_parsed.rpm = ptr->rpm;
                obd_parsed.throttle_position = ptr->throttle;
                manager->addOBDNode(&obd_parsed);
                ptr += 1;
            }
        }
            break;
        default:
            break;
    }
#endif
}
void parseRawDataFromAndroidBlock(FilterManager * manager, uint8_t * data_buf, uint32_t size)
{
#ifdef __ANDROID__
    parseRawDataBlock(manager, data_buf, size);
#endif
}
