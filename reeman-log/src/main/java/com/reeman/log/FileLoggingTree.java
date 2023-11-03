package com.reeman.log;

import com.elvishew.xlog.XLog;
import com.elvishew.xlog.flattener.ClassicFlattener;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.Printer;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy;
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class FileLoggingTree extends Timber.Tree {

    private final String defaultTAG;

    private final int logLevel;

    private final boolean withAndroidPrinter;
    private final Map<String, Printer> printerMap = new HashMap<>();

    private final Printer androidPrinter = new AndroidPrinter(true);


    public FileLoggingTree(int priority,boolean withAndroidPrinter, String rootPath, String tag, List<String> pathList) {
        this.logLevel = priority;
        this.defaultTAG = tag;
        this.withAndroidPrinter = withAndroidPrinter;
        for (String path : pathList) {
            printerMap.put(path, new FilePrinter
                    .Builder(rootPath + File.separator + path)
                    .fileNameGenerator(new LogFileName(false))
                    .backupStrategy(new NeverBackupStrategy())
                    .flattener(new ClassicFlattener())
                    .cleanStrategy(new FileLastModifiedCleanStrategy(7 * 24 * 60 * 60 * 1000))
                    .build());
        }
    }

    public FileLoggingTree(int priority,boolean withAndroidPrinter,  String tag, Map<String, Printer> printerMap) {
        this.logLevel = priority;
        this.defaultTAG = tag;
        this.withAndroidPrinter = withAndroidPrinter;
        this.printerMap.putAll(printerMap);

    }

    @Override
    protected void log(int priority, String tag, @NotNull String message, Throwable t) {
        if (tag == null) {
            tag = defaultTAG;
        }
        if (priority < logLevel) {
            return;
        }
        if (printerMap.containsKey(tag)) {
            if (withAndroidPrinter) {
                XLog.tag(tag).printers(androidPrinter, printerMap.get(tag)).log(priority, message, t);
            }else {
                XLog.tag(tag).printers(printerMap.get(tag)).log(priority, message, t);
            }
        } else {
            if (withAndroidPrinter) {
                XLog.tag(tag).printers(androidPrinter,printerMap.get(defaultTAG)).log(priority, message, t);
            }else {
                XLog.tag(tag).printers(printerMap.get(defaultTAG)).log(priority, message, t);
            }
        }
    }
}
