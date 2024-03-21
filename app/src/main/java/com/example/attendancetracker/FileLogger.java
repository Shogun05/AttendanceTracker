package com.example.attendancetracker;

import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileLogger {
    private static final String LOG_TAG = "AppCrashLogger";

    public static void logErrorToFile(String errorMsg, String path) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String logFileName = "error_log_" + timeStamp + ".txt";
        File logFile = new File(path, logFileName);

        try {
            FileWriter fileWriter = new FileWriter(logFile);
            fileWriter.append(errorMsg);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error occurred while logging to file", e);
        }
    }
}

