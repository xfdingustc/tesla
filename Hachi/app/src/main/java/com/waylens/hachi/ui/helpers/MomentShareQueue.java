package com.waylens.hachi.ui.helpers;

import com.waylens.hachi.ui.entities.LocalMoment;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Richard on 2/19/16.
 */
public class MomentShareQueue {
    BlockingQueue<LocalMoment> mBlockingQueue = new LinkedBlockingQueue<>();

}
