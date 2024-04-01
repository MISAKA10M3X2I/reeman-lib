package com.reeman.serialport.controller;


import com.reeman.serialport.BuildConfig;
import com.reeman.serialport.util.Parser;

import java.io.File;

import timber.log.Timber;

public class PowerBoardReceiver {
    private static PowerBoardReceiver INSTANCE;
    private SerialPortParser parser;

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

    private void writeToLocal(byte[] data, int len) {
        Timber.tag(BuildConfig.LOG_POWER_BOARD).w(Parser.bytesToASCII(data,len));
    }
}
