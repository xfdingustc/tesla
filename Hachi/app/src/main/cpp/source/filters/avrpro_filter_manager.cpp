#include <sys/stat.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "../include/avrpro_array.h"
#include "../include/avrpro_malloc.h"
#include "../include/avrpro_common.h"
#include "avrpro_std_data.h"
#include "../include/avrpro_filter_manager.h"


FilterManager::FilterManager(SMART_FILTER_TYPE type, const char * path, uint32_t length) :
        target_length_ms_(length),
        segIndex_(0),
        top_speed_kph(0),
        filteredPickedIdx_(NULL),
        logged_seg_index_(0),
        filter_type_(type),
        current_clip_info_(NULL),
        current_clip_reviewed_(false)
{
    gpsDataList_._Init();
    obdDataList_._Init();
    iioDataList_._Init();
    segCandidatesList_._Init();
    filteredSegList_._Init();
    clipInfoList_._Init();
    memset(sfdb_path_, 0, 1024);
    memset(sql_cmds_, 0, 1024);
    memset(current_sfdb_version_, 0, 8);
    memset(seg_in_proc_, 0, sizeof(seg_in_proc_));
    params_.inbound_speed_kph = INBOUND_SPEED_THRESHOLD_KPH;
    params_.outbound_speed_kph = OUTBOUND_SPEED_THRESHOLD_KPH;
    params_.inbound_bf_gforce_mg = INBOUND_BF_GFORCE_THRESHOLD;
    params_.outbound_bf_gforce_mg = OUTBOUND_BF_GFORCE_THRESHOLD;
    params_.inbound_lr_gforce_mg = INBOUND_LR_GFORCE_THRESHOLD;
    params_.outbound_lr_gforce_mg = OUTBOUND_LR_GFORCE_THRESHOLD;
    params_.inbound_ud_gforce_mg = INBOUND_UD_GFORCE_THRESHOLD;
    params_.outbound_ud_gforce_mg = OUTBOUND_UD_GFORCE_THRESHOLD;
    params_.seg_candidate_duration_ms = SEGMENT_CANDIDATE_DURATION;
    params_.seg_connect_duration_ms = SEGMENT_CONNECT_TOLERANCE;
    params_.min_seg_duration = MINIMUM_SEGMENT_DURATION;
    params_.max_seg_duration = MAXIMUM_SEGMENT_DURATION;
    if (path) {
        //AVRPRO_LOGD("the path prefix is %s", path);
        uint32_t prefix_len = (uint32_t)strlen(path);
        strcpy(sfdb_path_, path);
        if (sfdb_path_[prefix_len - 1] != '/' || sfdb_path_[prefix_len - 1] != '\\') {
            sfdb_path_[prefix_len] = '/';
            strcpy(sfdb_path_ + prefix_len + 1, SMART_FILTER_DB_NAME);
        } else {
            strcpy(sfdb_path_ + prefix_len, SMART_FILTER_DB_NAME);
        }
        //AVRPRO_LOGD("the full path is %s", sfdb_path_);
    } else {
        AVRPRO_LOGW("the path prefix is null, using the current dir");
        strcpy(sfdb_path_, SMART_FILTER_DB_NAME);
    }
    openSFDB();
}

FilterManager::~FilterManager()
{
    gpsDataList_._Release();
    obdDataList_._Release();
    iioDataList_._Release();
    segCandidatesList_._Release();
    filteredSegList_._Release();

    for (int i = 0; i < clipInfoList_._Size(); i++) {
        if (clipInfoList_._At(i)) {
            avrpro_free(*clipInfoList_._At(i));
        }
    }
    clipInfoList_._Release();
    if (filteredPickedIdx_) {
        avrpro_free(filteredPickedIdx_);
        filteredPickedIdx_ = NULL;
    }
    if (pSQLSFDB_) {
        sqlite3_close(pSQLSFDB_);
    }
}

void FilterManager::resetManager(bool clearFiltered)
{
    segIndex_ = 0;
    top_speed_kph = 0;
    current_clip_reviewed_ = false;
    memset(seg_in_proc_, 0, sizeof(seg_in_proc_));
    params_.inbound_speed_kph = INBOUND_SPEED_THRESHOLD_KPH;
    params_.outbound_speed_kph = OUTBOUND_SPEED_THRESHOLD_KPH;
    params_.inbound_bf_gforce_mg = INBOUND_BF_GFORCE_THRESHOLD;
    params_.outbound_bf_gforce_mg = OUTBOUND_BF_GFORCE_THRESHOLD;
    params_.inbound_lr_gforce_mg = INBOUND_LR_GFORCE_THRESHOLD;
    params_.outbound_lr_gforce_mg = OUTBOUND_LR_GFORCE_THRESHOLD;
    params_.inbound_ud_gforce_mg = INBOUND_UD_GFORCE_THRESHOLD;
    params_.outbound_ud_gforce_mg = OUTBOUND_UD_GFORCE_THRESHOLD;
    params_.seg_candidate_duration_ms = SEGMENT_CANDIDATE_DURATION;
    params_.seg_connect_duration_ms = SEGMENT_CONNECT_TOLERANCE;
    params_.min_seg_duration = MINIMUM_SEGMENT_DURATION;
    params_.max_seg_duration = MAXIMUM_SEGMENT_DURATION;
    gpsDataList_._Release();
    obdDataList_._Release();
    iioDataList_._Release();
    segCandidatesList_._Release();
    if (clearFiltered) {
        filteredSegList_._Release();
        logged_seg_index_ = 0;
    }
    if (filteredPickedIdx_) {
        avrpro_free(filteredPickedIdx_);
        filteredPickedIdx_ = NULL;
    }
}

int FilterManager::loadDBVersion(void * para, int n_column,
                                 char ** column_value, char ** column_name)
{
    FilterManager * obj = reinterpret_cast<FilterManager *>(para);
    strncpy(obj->current_sfdb_version_, column_value[1], 8);
    return 0;
}

int FilterManager::initSFDB()
{
    int ret = sqlite3_open(sfdb_path_, &pSQLSFDB_);
    if (ret != SQLITE_OK) {
        AVRPRO_LOGE("Init: Failed to open the sf database!");
        return -1;
    }
    sqlite3_exec(pSQLSFDB_, "drop table VERSION", NULL, NULL, NULL);
    sqlite3_exec(pSQLSFDB_, "drop table CLIPINFO", NULL, NULL, NULL);
    sqlite3_exec(pSQLSFDB_, "drop table SEGMENTINFO", NULL, NULL, NULL);

    sprintf(sql_cmds_, CREATEVERSION);
    ret = sqlite3_exec(pSQLSFDB_, sql_cmds_, NULL, NULL, NULL);
    if (ret != SQLITE_OK) {
        AVRPRO_LOGE("error happens when creating vesrion table");
    }

    memset(sql_cmds_, 0, 1024);
    sprintf(sql_cmds_, "insert into VERSION Values(1, %s)", SMART_FILTER_VERSION);
    ret = sqlite3_exec(pSQLSFDB_, sql_cmds_, NULL, NULL, NULL);
    if (ret != SQLITE_OK) {
        AVRPRO_LOGE("error happens when insert vesrion info");
    }

    memset(sql_cmds_, 0, 1024);
    sprintf(sql_cmds_, CREATECLIPINFO);
    ret = sqlite3_exec(pSQLSFDB_, sql_cmds_, NULL, NULL, NULL);
    if (ret != SQLITE_OK) {
        AVRPRO_LOGE("error happens when create clipinfo table");
    }

    memset(sql_cmds_, 0, 1024);
    sprintf(sql_cmds_, CREATESEGMENTINFO);
    ret = sqlite3_exec(pSQLSFDB_, sql_cmds_, NULL, NULL, NULL);
    if (ret != SQLITE_OK) {
        AVRPRO_LOGE("error happens when create segmentinfo table");
    }

    return 0;
}

int FilterManager::openSFDB()
{
    FILE *file = NULL;
    if ((file = fopen(sfdb_path_, "r"))) {
        fclose(file);
        int ret = sqlite3_open(sfdb_path_, &pSQLSFDB_);
        if (ret != SQLITE_OK) {
            AVRPRO_LOGE("Error: Failed to open the sf database!");
            return -1;
        }
        char *errmsg = NULL;
        ret = sqlite3_exec(pSQLSFDB_, "select * from VERSION",
                           loadDBVersion, this, &errmsg);
        if (ret != SQLITE_OK) {
            AVRPRO_LOGE("get sfdb version error");
            sqlite3_free(errmsg);
            return -1;
        }
        if (strcmp(current_sfdb_version_, SMART_FILTER_VERSION)) {
            AVRPRO_LOGD("current sfdb version %s too old, rebuild it", current_sfdb_version_);
            return initSFDB();
        }
    } else {
        file = fopen(sfdb_path_, "w");
        fclose(file);
        return initSFDB();
    }

    return 0;
}

int FilterManager::updateSFDB(uint32_t start_idx)
{
    if (!pSQLSFDB_) {
        AVRPRO_LOGE("sfdb not initialized!");
        return -1;
    }

    sprintf(sql_cmds_, "insert into CLIPINFO(GUID, CLIPTYPE, CLIPID, DURATION, STTL, STTH) Values(\'%s\', %d, %d, %d, %d, %d)",
            current_clip_info_->guid_str, current_clip_info_->type, current_clip_info_->id,
            current_clip_info_->duration_ms, current_clip_info_->start_time_lo, current_clip_info_->start_time_hi);
    AVRPRO_LOGD("sql add clip %s", sql_cmds_);
    int result = sqlite3_exec(pSQLSFDB_, sql_cmds_, NULL, NULL, NULL);
    char * errmsg = NULL;
    char ** dbResult;
    int nRow, nColumn, index, clipIndex = 0;
    sprintf(sql_cmds_, "select * from CLIPINFO where GUID=\'%s' and CLIPID=%d and DURATION=%d and STTL=%d and STTH=%d",
            current_clip_info_->guid_str, current_clip_info_->id, current_clip_info_->duration_ms,
            current_clip_info_->start_time_lo, current_clip_info_->start_time_hi);
    sqlite3_get_table(pSQLSFDB_, sql_cmds_, &dbResult, &nRow, &nColumn, &errmsg);
    if (nRow > 0) {
        index = nColumn;
        clipIndex = atoi(dbResult[index]);
    }
    sqlite3_free_table(dbResult);

    uint32_t new_speed_threshold = (top_speed_kph * 9 / 10);

    if (new_speed_threshold < params_.inbound_speed_kph) {
        new_speed_threshold = params_.inbound_speed_kph;
    }
    for (int i = start_idx; i < filteredSegList_._Size(); i++) {
        avrpro_segment_info_t * curr_seg = filteredSegList_._At(i);
        if (curr_seg->filter_type == SMART_FAST_FURIOUS) {
            if (curr_seg->max_speed_kph < new_speed_threshold) {
                AVRPRO_LOGD("current seg speed %d < new threshold %d", curr_seg->max_speed_kph, new_speed_threshold);
                filteredSegList_._Remove(i);
                i--;
                continue;
            }
        }
        sprintf(sql_cmds_, "insert into SEGMENTINFO(CLIPINDEX, FILTERTYPE, STARTOFFSET, DURATION, MAXSPEED) Values(%d, %d, %d, %d, %d)", clipIndex, curr_seg->filter_type, curr_seg->inclip_offset_ms, curr_seg->duration_ms, curr_seg->max_speed_kph);
        result = sqlite3_exec(pSQLSFDB_, sql_cmds_, NULL, NULL, NULL);
    }
    return 0;
}

bool FilterManager::isClipInfoChanged(avrpro_clip_info_t * ci)
{
    if (!current_clip_info_) {
        current_clip_info_ = (avrpro_clip_info_t *)avrpro_malloc(sizeof(avrpro_clip_info_t));
        *current_clip_info_ = *ci;
        clipInfoList_._Append(&current_clip_info_);
        return true;
    }
    if (current_clip_info_->guid_str == ci->guid_str &&
        current_clip_info_->id == ci->id &&
        current_clip_info_->start_time_lo == ci->start_time_lo &&
        current_clip_info_->start_time_hi == ci->start_time_hi &&
        current_clip_info_->duration_ms == ci->duration_ms) {
        return false;
    } else {
        if (!current_clip_reviewed_ && segCandidatesList_._Size() > 0) {
            reviewSegCandidates();
            if (filter_type_ != SMART_RANDOMPICK) {
                updateSFDB(logged_seg_index_);
            }
            logged_seg_index_ = filteredSegList_._Size();
        }
        current_clip_info_ = (avrpro_clip_info_t *)avrpro_malloc(sizeof(avrpro_clip_info_t));
        *current_clip_info_ = *ci;
        clipInfoList_._Append(&current_clip_info_);
        resetManager();
        return true;
    }
}

int FilterManager::fetchNextFilteredSegInfo(avrpro_segment_info_t * si, bool fromStart)
{
    //AVRPRO_LOGE("%s enter", __FUNCTION__);
    if (!current_clip_reviewed_ && segCandidatesList_._Size() > 0) {
        reviewSegCandidates();
        if (filter_type_ != SMART_RANDOMPICK) {
            updateSFDB(logged_seg_index_);
        }
        logged_seg_index_ = filteredSegList_._Size();
    }
    if (filteredSegList_._Size() <= 0) {
        return -1;
    }

    int total_count = filteredSegList_._Size();

    if (fromStart) {
        uint32_t total_ms = 0;
        uint32_t picked_ms = 0;
        uint32_t rand_idx = 0;
        if (filteredPickedIdx_) {
            avrpro_free(filteredPickedIdx_);
        }
        filteredPickedIdx_ = (int32_t *)avrpro_malloc(total_count * sizeof(int32_t));
        memset(filteredPickedIdx_, -1, total_count * sizeof(int32_t));
        if (filter_type_ == SMART_RANDOMCUTTING) {
            for (int i = 0; i < filteredSegList_._Size(); i++) {
                avrpro_segment_info_t * prev = filteredSegList_._At(i);
                for (int j = i + 1; j < filteredSegList_._Size(); j++) {
                    avrpro_segment_info_t * next = filteredSegList_._At(j);
                    if (prev->parent_clip != next->parent_clip) {
                        break;
                    }
                    if ((next->inclip_offset_ms <= (prev->inclip_offset_ms + prev->duration_ms) &&
                         next->inclip_offset_ms >= prev->inclip_offset_ms) ||
                        ((next->inclip_offset_ms + next->duration_ms) <= (prev->inclip_offset_ms + prev->duration_ms) &&
                         (next->inclip_offset_ms + next->duration_ms) >= prev->inclip_offset_ms)) {
                        int32_t new_start = MIN(prev->inclip_offset_ms, next->inclip_offset_ms);
                        int32_t new_end = MAX(prev->inclip_offset_ms + prev->duration_ms, next->inclip_offset_ms + next->duration_ms);
                        prev->inclip_offset_ms = new_start;
                        prev->duration_ms = new_end - new_start;
                        filteredSegList_._Remove(j);
                        j--;
                    }
                }
            }
            total_count = filteredSegList_._Size();
            for (int i = 0; i < total_count; i++) {
                total_ms += filteredSegList_._At(i)->duration_ms;
            }
        } else {
            for (int i = 0; i < total_count; i++) {
                if (filteredSegList_._At(i)->filter_type == filter_type_) {
                    total_ms += filteredSegList_._At(i)->duration_ms;
                }
            }
        }

        while (picked_ms < target_length_ms_ && picked_ms < total_ms) {
            rand_idx = rand() % total_count;
            int i = 0;
            bool valid_rand = false;
            for (i = 0; i < total_count; i++) {
                if (rand_idx == filteredPickedIdx_[i]) {
                    valid_rand = false;
                    break;
                } else if (filteredPickedIdx_[i] < 0) {
                    valid_rand = true;
                    break;
                }
            }
            if (i >= total_count) break;
            if ((filteredSegList_._At(rand_idx)->filter_type != filter_type_ &&
                 filter_type_ != SMART_RANDOMCUTTING)) {
                continue;
            }
            if (!valid_rand) {
                continue;
            }
            filteredPickedIdx_[i] = rand_idx;
            picked_ms += filteredSegList_._At(rand_idx)->duration_ms;
            AVRPRO_LOGD("pick random index is %d", rand_idx);
        }
        AVRPRO_LOGD("now picked ms is %d", picked_ms);
        segIndex_ = 0;
    }

    if (filteredPickedIdx_[segIndex_] < 0 || segIndex_ >= total_count) {
        AVRPRO_LOGD("reach the end: %d", segIndex_);
        return -1;
    }
    *si = * filteredSegList_._At(filteredPickedIdx_[segIndex_]);
    segIndex_++;
    return 0;
    /*if (filter_type_ == SMART_RANDOMCUTTING) {
        for (int i = segIndex_; i < total_count; i++) {
            if (filteredPickedIdx_[i] > 0) {
                *si = *(filteredSegList_._At(i));
                segIndex_ = i + 1;
                return 0;
            }
        }
    } else {
        for (int i = segIndex_; i < total_count; i++) {
            if (filteredPickedIdx_[i] > 0 && filteredSegList_._At(i)->filter_type == filter_type_) {
                *si = *(filteredSegList_._At(i));
                segIndex_ = i + 1;
                return 0;
            }
        }
    }

    return -1; */
}

bool FilterManager::isTypeFilteredAndCached()
{
    //AVRPRO_LOGE("%s enter", __FUNCTION__);

    if (!current_clip_info_) {
        return false;
    }
    if (filter_type_ == SMART_RANDOMPICK) {
        return false;
    }
    char * errmsg = NULL;
    char ** dbResult;
    int nRow, nColumn, index, clipIndex;
    sprintf(sql_cmds_, "select * from CLIPINFO where GUID=\'%s' and CLIPID=%d and DURATION=%d and STTL=%d and STTH=%d",
            current_clip_info_->guid_str, current_clip_info_->id, current_clip_info_->duration_ms,
            current_clip_info_->start_time_lo, current_clip_info_->start_time_hi);
    sqlite3_get_table(pSQLSFDB_, sql_cmds_, &dbResult, &nRow, &nColumn, &errmsg);
    if (nRow > 0) {
        index = nColumn;
        clipIndex = atoi(dbResult[index]);
    } else {
        AVRPRO_LOGD("cannot find the clip info");
        return false;
    }
    //filteredSegList_._Clear();
    if (filter_type_ == SMART_RANDOMCUTTING) {
        sprintf(sql_cmds_, "select * from SEGMENTINFO where CLIPINDEX=%d", clipIndex);
        sqlite3_exec(pSQLSFDB_, sql_cmds_, &loadSegmentInfo, this, NULL);
    } else {
        sprintf(sql_cmds_, "select * from SEGMENTINFO where CLIPINDEX=%d and FILTERTYPE=%d", clipIndex, filter_type_);
        sqlite3_exec(pSQLSFDB_, sql_cmds_, &loadSegmentInfo, this, NULL);
    }
    current_clip_reviewed_ = true;
    //AVRPRO_LOGE("%s exit", __FUNCTION__);
    return true;
}

int FilterManager::loadSegmentInfo(void * para, int n_column,
                                   char ** column_value, char ** column_name)
{
    FilterManager * fm = reinterpret_cast<FilterManager *>(para);
    avrpro_segment_info_t si;
    si.parent_clip = fm->current_clip_info_;
    si.filter_type = atoi(column_value[2]);
    si.inclip_offset_ms = atoi(column_value[3]);
    si.duration_ms = atoi(column_value[4]);
    si.max_speed_kph = atoi(column_value[5]);
    fm->filteredSegList_._Append(&si);
    return 0;
}

int FilterManager::reviewSegCandidates()
{
    if (segCandidatesList_._Size() == 0) {
        return 0;
    }

    avrpro_segment_info_t segment_reviewed = {0};
    avrpro_segment_info_t * candidateInProc = NULL;

    for (int i = SMART_FAST_FURIOUS; i < SMART_MAX_INDEX; i++) {
        memset(&segment_reviewed, 0, sizeof(avrpro_segment_info_t));
        for (int j = 0; j < segCandidatesList_._Size(); j++) {
            candidateInProc = segCandidatesList_._At(j);
            if (candidateInProc->filter_type == i) {
                if (segment_reviewed.inclip_offset_ms <= 0) {
                    segment_reviewed = *candidateInProc;
                } else {
                    if ((candidateInProc->inclip_offset_ms - segment_reviewed.inclip_offset_ms -
                         segment_reviewed.duration_ms) < SEGMENT_CONNECT_TOLERANCE) {
                        int32_t new_duration_ms = candidateInProc->inclip_offset_ms + candidateInProc->duration_ms - segment_reviewed.inclip_offset_ms;
                        if (new_duration_ms <= MAXIMUM_SEGMENT_DURATION) {
                            segment_reviewed.duration_ms = new_duration_ms;
                            //AVRPRO_LOGD("combine candidates, type: %d", segment_reviewed.filter_type);
                        } else {
                            filteredSegList_._Append(&segment_reviewed);
                            segment_reviewed = *candidateInProc;
                            AVRPRO_LOGD("segment reach max, create a new one");
                        }
                    } else {
                        if (segment_reviewed.duration_ms >= MINIMUM_SEGMENT_DURATION) {
                            filteredSegList_._Append(&segment_reviewed);
                            AVRPRO_LOGD("filtered one segment successfully: type: %d, offset: %d, duration: %d",
                                        segment_reviewed.filter_type, segment_reviewed.inclip_offset_ms, segment_reviewed.duration_ms);
                        } else {
                            AVRPRO_LOGD("dropped one short segment: type: %d, offset: %d, duration: %d",
                                        segment_reviewed.filter_type, segment_reviewed.inclip_offset_ms, segment_reviewed.duration_ms);
                        }
                        memset(&segment_reviewed, 0, sizeof(avrpro_segment_info_t));
                    }
                }
            }
        }
    }
    current_clip_reviewed_ = true;

    return 0;
}

int FilterManager::addGPSNode(avrpro_gps_parsed_data_t * data)
{
    if ((uint32_t)(data->speed) > top_speed_kph) {
        top_speed_kph = (uint32_t)(data->speed);
    }
    // it's a node worth notice
    if (seg_in_proc_[SMART_FAST_FURIOUS].inclip_offset_ms == -1) {
        if (data->speed >= params_.inbound_speed_kph) {
            generateCandidates(SMART_FAST_FURIOUS, data->clip_time_ms, data->speed);
        }
    } else {
        generateCandidates(SMART_FAST_FURIOUS, data->clip_time_ms, data->speed);
    }

    gpsDataList_._Append(data);
    return 0;
}

int FilterManager::addOBDNode(avrpro_obd_parsed_data_t * data)
{
    if (gpsDataList_._Size() == 0) {
        if ((uint32_t)(data->speed) > top_speed_kph) {
            top_speed_kph = (uint32_t)(data->speed);
        }
        // it's a node worth notice
        if (seg_in_proc_[SMART_FAST_FURIOUS].inclip_offset_ms == -1) {
            if (data->speed >= params_.inbound_speed_kph) {
                generateCandidates(SMART_FAST_FURIOUS, data->clip_time_ms, data->speed);
            }
        } else {
            generateCandidates(SMART_FAST_FURIOUS, data->clip_time_ms, data->speed);
        }
    }

    obdDataList_._Append(data);
    return 0;
}

int FilterManager::addIIONode(avrpro_iio_parsed_data_t * data)
{
    if (seg_in_proc_[SMART_ACCELERATION].inclip_offset_ms == -1) {
        if (abs(data->accel_z) >= params_.inbound_bf_gforce_mg) {
            generateCandidates(SMART_ACCELERATION, data->clip_time_ms, data->accel_z);
        }
    } else {
        generateCandidates(SMART_ACCELERATION, data->clip_time_ms, abs(data->accel_z));
    }

    if (seg_in_proc_[SMART_SHARPTURN].inclip_offset_ms == -1) {
        if (abs(data->accel_x) >= params_.inbound_lr_gforce_mg) {
            generateCandidates(SMART_SHARPTURN, data->clip_time_ms, data->accel_x);
        }
    } else {
        generateCandidates(SMART_SHARPTURN, data->clip_time_ms, abs(data->accel_x));
    }

    if (seg_in_proc_[SMART_BUMPINGHARD].inclip_offset_ms == -1) {
        if (abs(data->accel_y) >= params_.inbound_ud_gforce_mg) {
            generateCandidates(SMART_BUMPINGHARD, data->clip_time_ms, data->accel_y);
        }
    } else {
        generateCandidates(SMART_BUMPINGHARD, data->clip_time_ms, abs(data->accel_y));
    }

    if (filter_type_ == SMART_RANDOMPICK) {
        generateCandidates(SMART_RANDOMPICK, data->clip_time_ms, 0);
    }
    iioDataList_._Append(data);

    return 0;
}

int FilterManager::generateCandidates(int type, uint64_t clip_time_ms, int32_t param)
{
    if (seg_in_proc_[type].inclip_offset_ms > 0) {
        seg_in_proc_[type].duration_ms = (uint32_t)(clip_time_ms -
                                                    (uint64_t)(current_clip_info_->start_time_lo) -
                                                    (uint64_t)((uint64_t)(current_clip_info_->start_time_hi) << 32)) -
                                         seg_in_proc_[type].inclip_offset_ms;
        if (seg_in_proc_[type].duration_ms >= SEGMENT_CANDIDATE_DURATION) {
            if (type != SMART_RANDOMPICK) {
                segCandidatesList_._Append(&(seg_in_proc_[type]));
            } else {
                int pick = rand() % 10;
                if (pick >= 3) {
                    segCandidatesList_._Append(&(seg_in_proc_[type]));
                    AVRPRO_LOGD("random pick candidate %d, %d", seg_in_proc_[type].inclip_offset_ms, seg_in_proc_[type].duration_ms);
                }
            }
            //AVRPRO_LOGD("append a new segment candidate: offset: %d, duration: %d, type: %d, speed: %d",
            //    seg_in_proc_[type].inclip_offset_ms, seg_in_proc_[type].duration_ms, type, seg_in_proc_[type].max_speed_kph);
            seg_in_proc_[type].inclip_offset_ms = -1;
            return 0;
        }
        if (type == SMART_FAST_FURIOUS) {
            if (param > seg_in_proc_[type].max_speed_kph) {
                seg_in_proc_[type].max_speed_kph = param;
            }
            if (param < params_.outbound_speed_kph) {
                segCandidatesList_._Append(&(seg_in_proc_[type]));
                //AVRPRO_LOGD("append a new segment candidate: offset: %d, duration: %d, type: %d, speed: %d",
                //    seg_in_proc_[type].inclip_offset_ms, seg_in_proc_[type].duration_ms, type, seg_in_proc_[type].max_speed_kph);
                seg_in_proc_[type].inclip_offset_ms = -1;
            }
        } else if (type == SMART_ACCELERATION) {
            if (param < params_.outbound_bf_gforce_mg) {
                segCandidatesList_._Append(&(seg_in_proc_[type]));
                //AVRPRO_LOGD("append a new segment candidate: offset: %d, duration: %d, type: %d, speed: %d",
                //    seg_in_proc_[type].inclip_offset_ms, seg_in_proc_[type].duration_ms, type, seg_in_proc_[type].max_speed_kph);
                seg_in_proc_[type].inclip_offset_ms = -1;
            }
        } else if (type == SMART_SHARPTURN) {
            if (param < params_.outbound_lr_gforce_mg) {
                segCandidatesList_._Append(&(seg_in_proc_[type]));
                //AVRPRO_LOGD("append a new segment candidate: offset: %d, duration: %d, type: %d, speed: %d",
                //    seg_in_proc_[type].inclip_offset_ms, seg_in_proc_[type].duration_ms, type, seg_in_proc_[type].max_speed_kph);
                seg_in_proc_[type].inclip_offset_ms = -1;
            }
        } else if (type == SMART_BUMPINGHARD) {
            if (param < params_.outbound_ud_gforce_mg) {
                segCandidatesList_._Append(&(seg_in_proc_[type]));
                //AVRPRO_LOGD("append a new segment candidate: offset: %d, duration: %d, type: %d, speed: %d",
                //    seg_in_proc_[type].inclip_offset_ms, seg_in_proc_[type].duration_ms, type, seg_in_proc_[type].max_speed_kph);
                seg_in_proc_[type].inclip_offset_ms = -1;
            }
        }
    } else {
        seg_in_proc_[type].inclip_offset_ms = (uint32_t)(clip_time_ms -
                                                         (uint64_t)(current_clip_info_->start_time_lo) -
                                                         (uint64_t)((uint64_t)(current_clip_info_->start_time_hi) << 32));
        seg_in_proc_[type].filter_type = type;
        seg_in_proc_[type].parent_clip = current_clip_info_;
        if (type == SMART_FAST_FURIOUS) {
            seg_in_proc_[type].max_speed_kph = param;
        }
    }

    return 0;
}
