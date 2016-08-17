package com.waylens.hachi.bgjob;

import com.xfdingustc.snipe.vdb.ClipPos;

/**
 * Created by Xiaofei on 2016/8/17.
 */
public interface Exportable {

    String getJobId();

    int getExportProgress();

    String getOutputFile();

    ClipPos getClipStartPos();
}
