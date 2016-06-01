package com.waylens.hachi.hardware.vdtcamera;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Xiaofei on 2016/5/31.
 */
public class CommandThread extends Thread {
    private final Socket mSocket;
    private final BlockingQueue<VdtCameraCommand> mCommandQueue;

    public CommandThread(Socket socket, BlockingQueue<VdtCameraCommand> commandQueue) {
        this.mSocket = socket;
        this.mCommandQueue = commandQueue;
    }


    @Override
    public void run() {
        while (true) {
            VdtCameraCommand command = null;
            try {
                command = mCommandQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }


            try {
                SocketUtils.writeCommand(mSocket, command);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
