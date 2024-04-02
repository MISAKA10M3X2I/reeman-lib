package com.reeman.serialport.controller;


import com.reeman.serialport.BuildConfig;
import com.reeman.serialport.util.Parser;

import java.io.File;

import kotlin.jvm.Synchronized;
import timber.log.Timber;

public class PowerBoardReceiver {
    private static PowerBoardReceiver INSTANCE;
    private SerialPortParser parser;

    private final StringBuilder sb = new StringBuilder();

    public static PowerBoardReceiver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PowerBoardReceiver();
        }
        return INSTANCE;
    }

    public void start(String port) throws Exception {
        parser = new SerialPortParser(new File(port), 115200, this::writeToLocal);
        parser.start();
    }

    public void stop() {
        if (parser != null) {
            parser.stop();
            parser = null;
        }
        INSTANCE = null;
    }

    private synchronized void writeToLocal(byte[] data, int len) {
        String message = Parser.bytesToASCII(data, len);
        sb.append(message);
        String msg = sb.toString();
        int index = msg.lastIndexOf("\n");
        if (index > 0) {
            Timber.tag(BuildConfig.LOG_POWER_BOARD).w(msg.substring(0, index - 1));
            sb.delete(0, index + 1);
        }
    }
}
