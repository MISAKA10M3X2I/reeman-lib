package com.reeman.log;

import com.elvishew.xlog.printer.file.naming.FileNameGenerator;

import java.util.Date;

public class LogFileName implements FileNameGenerator {
    boolean splitByHour = false;

    public LogFileName() {
    }
    public LogFileName(boolean hour) {
        this.splitByHour = hour;
    }

    @Override
    public boolean isFileNameChangeable() {
        return true;
    }

    @Override
    public String generateFileName(int logLevel, long timestamp) {
        if (splitByHour) {
            return TimeUtil.formatHour(new Date()) +".log";
        }else {
            return TimeUtil.formatDay(new Date())+".log";
        }
    }
}
