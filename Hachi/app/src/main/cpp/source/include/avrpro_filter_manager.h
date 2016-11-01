#ifndef __AVRPRO_FILTER_MANAGER_H__
#define __AVRPRO_FILTER_MANAGER_H__

#include "avrpro_std_data.h"
#include "avrpro_malloc.h"
#include "avrpro_array.h"
extern "C" {
#include "sqlite3.h"
}

#define SMART_FILTER_DB_NAME   "waylensfilter.db"
#define SMART_FILTER_VERSION   "0.1"
#define HIGH_SPEED_THRESHOLD_KPH    100
#define HIGH_RPM_THRESHOLD          5000
#define HIGH_BF_GFORCE_THRESHOLD    400     // mg
#define HIGH_LR_GFORCE_THRESHOLD    300     // mg
#define HIGH_UD_GFORCE_THRESHOLD    700     // mg
#define SEGMENT_CANDIDATE_DURATION  2000    // ms
#define SEGMENT_CONNECT_TOLERANCE   1000    // ms
#define MINIMUM_SEGMENT_DURATION    3000    // ms
#define MAXIMUM_SEGMENT_DURATION    5000    // ms

#define CREATEVERSION "create table VERSION(ID integer PRIMARY KEY, VER text)"
#define CREATECLIPINFO "create table CLIPINFO(ID integer PRIMARY KEY, GUID text, CLIPTYPE integer, \
        CLIPID integer, DURATION integer, STTL integer, STTH integer)"
#define CREATESEGMENTINFO "create table SEGMENTINFO(ID integer PRIMARY KEY, CLIPINDEX integer, \
        FILTERTYPE integer, STARTOFFSET integer, DURATION integer, MAXSPEED integer)"        

//-----------------------------------------------------------------------
//
// FilterManager
//
//-----------------------------------------------------------------------
class FilterManager {
public:   
    explicit FilterManager(SMART_FILTER_TYPE type, const char * path, uint32_t length);
    ~FilterManager();
    int addGPSNode(avrpro_gps_parsed_data_t * data);
    int addOBDNode(avrpro_obd_parsed_data_t * data);
    int addIIONode(avrpro_iio_parsed_data_t * data);
    int fetchNextFilteredSegInfo(avrpro_segment_info_t * si, bool fromStart = false);
    void resetManager(bool clearFiltered = false);
    bool isClipInfoChanged(avrpro_clip_info_t * ci);
    bool isTypeFilteredAndCached();

private:
    static int loadSegmentInfo(void * para, int n_column,
                       char ** column_value, char ** column_name);
    sqlite3 * pSQLSFDB_;
    char sfdb_path_[1024];
    char sql_cmds_[1024];
    uint32_t target_length_ms_;
    uint32_t segIndex_;
    uint32_t top_speed_kph;
    uint8_t * filteredSelectedIdx_; 
    uint32_t logged_seg_index_;
    SMART_FILTER_TYPE filter_type_;
    avrpro_segment_info_t seg_in_proc_[5];
    avrpro_clip_info_t * current_clip_info_;
    bool current_clip_reviewed_;
    DynamicArray<avrpro_clip_info_t *> clipInfoList_;
    DynamicArray<avrpro_gps_parsed_data_t> gpsDataList_;
    DynamicArray<avrpro_obd_parsed_data_t> obdDataList_;
    DynamicArray<avrpro_iio_parsed_data_t> iioDataList_;
    DynamicArray<avrpro_segment_info_t> segCandidatesList_; 
    DynamicArray<avrpro_segment_info_t> filteredSegList_;
    int openSFDB();
    int initSFDB();
    int updateSFDB(uint32_t start_idx);
    int reviewSegCandidates();
    int generateCandidates(int type, uint64_t clip_time_ms, int32_t param);
};
#endif
