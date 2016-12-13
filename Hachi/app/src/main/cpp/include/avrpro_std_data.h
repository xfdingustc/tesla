#ifndef __AVRPRO_STD_DATA_H__
#define __AVRPRO_STD_DATA_H__

#include <stdint.h>

#ifdef IOS_OS
#include "vdb_ios_types.h"
#endif
//-----------------------------------------------------------------------
//  TODO: Do we need to use SQLCipher??
//-----------------------------------------------------------------------

//-----------------------------------------------------------------------
//  filter type for smart remix
//-----------------------------------------------------------------------
enum SMART_FILTER_TYPE {
    SMART_RANDOMCUTTING = 0,    // a mixture of all types
    SMART_FAST_FURIOUS,     // speed in a high range
    SMART_ACCELERATION,     // dramatic gforce change 
    SMART_SHARPTURN,        // a sharp turn 
    SMART_BUMPINGHARD,      // driving on a bumping road
    SMART_SMOOTH_RUNNING,   // driving at a constant high speed
    SMART_RANDOMPICK,       // random pick segments from highlights
    SMART_HIGH_RPM,       // high rpm
    SMART_MAX_INDEX         // not a real type
};

//-----------------------------------------------------------------------
//  filter type for smart remix
//-----------------------------------------------------------------------
enum RAW_DATA_SOURCE_TYPE {
    DEVICE_AVF = 0,
    DEVICE_IOS,
    DEVICE_ANDROID
};
//-----------------------------------------------------------------------
//  clip info
//-----------------------------------------------------------------------
typedef struct avrpro_clip_info_s {
    char guid_str[40];
    uint32_t id;
    uint32_t type;
    uint32_t start_time_lo;
    uint32_t start_time_hi;
    uint32_t duration_ms;
} avrpro_clip_info_t;

//-----------------------------------------------------------------------
//  segment info
//-----------------------------------------------------------------------
typedef struct avrpro_segment_info_s {
    avrpro_clip_info_t * parent_clip;
    int32_t inclip_offset_ms;
    int32_t duration_ms;
    int32_t filter_type;
    int32_t max_speed_kph;
} avrpro_segment_info_t;

//-----------------------------------------------------------------------
//  gps info
//-----------------------------------------------------------------------
typedef struct gps_parsed_data_s {
    uint64_t clip_time_ms;
    float speed;
    double latitude;
    double longitude;
    double altitude;
    float track;   
    uint32_t utc_time;
    uint32_t utc_time_usec;
} avrpro_gps_parsed_data_t;

//-----------------------------------------------------------------------
//  obd info
//-----------------------------------------------------------------------
typedef struct obd_parsed_data_s {
    uint64_t clip_time_ms;
    uint32_t speed;
    uint32_t rpm;
    uint32_t throttle_position;
} avrpro_obd_parsed_data_t;

//-----------------------------------------------------------------------
//  iio info
//-----------------------------------------------------------------------
typedef struct iio_parsed_data_s {
    uint64_t clip_time_ms;
    // accel : g x 1000 = mg
    int32_t accel_x;
    int32_t accel_y;
    int32_t accel_z;

    // Orientation
    // Euler : Degrees x 1000 = mDegrees
    int32_t euler_heading;
    int32_t euler_roll;
    int32_t euler_pitch;    
} avrpro_iio_parsed_data_t;

//-----------------------------------------------------------------------
//  ios data header
//-----------------------------------------------------------------------
typedef struct avrpro_ios_data_header_s {
    int32_t data_type;//kRawData_OBD, gps, iio ...
    int32_t start_time_sec;
    int32_t length_sec;
    int32_t data_count;
    int32_t reserved[4];
} avrpro_ios_data_header_t;

//-----------------------------------------------------------------------
//  lap timer info
//-----------------------------------------------------------------------
#define TOTAL_CHECK_POINTS  1000
typedef struct avrpro_laps_data_s {
    uint32_t total_laps;
    uint32_t best_lap_time_ms;  // in ms
    uint32_t top_speed_kph;     // in kph
    // more in future
} avrpro_laps_data_t;

typedef struct avrpro_lap_data_s {
    uint32_t lap_time_ms;       // in ms
    uint32_t inclip_start_offset_ms;   // start ms offset in video
    uint32_t check_interval_ms; // compare to the best lap every interval ms
    int32_t delta_ms_to_best[TOTAL_CHECK_POINTS];  // compared results in ms
    // more in future
} avrpro_lap_data_t;

void avrpro_set_print_proc(void (*p)(const char*));

// ---------------------------------------------------------------
//  smart filter APIs
// ---------------------------------------------------------------
typedef void* avrpro_smart_filter;
// return NULL on error
avrpro_smart_filter avrpro_smart_filter_init(enum SMART_FILTER_TYPE type, const char * local_directory, uint32_t target_length);
// set offset to 0 and duration to clip length to filter the whole clip
bool avrpro_smart_filter_is_data_parsed(avrpro_smart_filter filter, avrpro_clip_info_t * ci, uint32_t offset, uint32_t duration);
int avrpro_smart_filter_feed_data(avrpro_smart_filter filter, uint8_t * data_buf, uint32_t size, enum RAW_DATA_SOURCE_TYPE source);
// read the results sequentially, one record a time
// return 0 on success
// return -1 on reach the end
int avrpro_smart_filter_read_results(avrpro_smart_filter filter, avrpro_segment_info_t * si, bool fromStart);
int avrpro_smart_filter_deinit(avrpro_smart_filter filter);

// parser of lap timer
avrpro_smart_filter avrpro_lap_timer_filter_init(avrpro_clip_info_t * ci);
// must set start position before feed gps data
int avrpro_lap_timer_set_start(avrpro_smart_filter filter, avrpro_gps_parsed_data_t startNode);
// do not feed non-gps data
int avrpro_lap_timer_feed_gps_data(avrpro_smart_filter filter, uint8_t * data_buf,
                                   uint32_t size, enum RAW_DATA_SOURCE_TYPE source);
// return an array of avrpro_lap_data_t, size = laps->total_laps, an array of avrpro_gps_parsed_data_t, size = *gps_num
void avrpro_lap_timer_read_results(avrpro_smart_filter filter, avrpro_laps_data_t * laps, avrpro_lap_data_t ** laplist,
                                                  avrpro_gps_parsed_data_t ** gpslist, uint32_t * gps_num);
avrpro_gps_parsed_data_t * avrpro_lap_timer_get_gps_by_time(avrpro_smart_filter filter, uint64_t clip_time_ms);
int avrpro_lap_timer_deinit(avrpro_smart_filter filter);
#endif
