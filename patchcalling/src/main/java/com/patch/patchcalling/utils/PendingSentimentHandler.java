package com.patch.patchcalling.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class PendingSentimentHandler extends ContextWrapper {

    private static final String FILE_NAME = "pendingSentiments.json";

    private static PendingSentimentHandler instance = null;
    private static final String TAG = "PendingSentimentHandler";

    private PendingSentimentHandler(Context base) {
        super(base);
    }

    public static PendingSentimentHandler getInstance(Context context) {
        if (instance == null) {
            instance = new PendingSentimentHandler(context);
        }
        return instance;
    }

    public void writeToFile(JSONObject jsonObjectToWrite) {
        JSONArray jsonArray = new JSONArray();
        try {
            if (readFromFile() != null) {
                //it means file contains data of failed sentiments
                //so appending new item to jsonArray
                jsonArray = readFromFile();
                jsonArray.put(jsonObjectToWrite);
            } else {
                jsonArray.put(jsonObjectToWrite);
            }

            String text = jsonArray.toString();
            writeDataToFile(text);
        } catch (Exception e) {
            if (getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());            //e.printStackTrace();
        }

    }

    public void writeToFile(JSONArray jsonArrayToWrite) throws Exception {
        String text = jsonArrayToWrite.toString();
        writeDataToFile(text);
    }

    private void writeDataToFile(String data) {
        FileOutputStream fos = null;

        try {
            fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
            fos.write(data.getBytes());   //writing data to bytes format
        } catch (FileNotFoundException e) {
            if (getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
            //e.printStackTrace();
        } catch (IOException e) {
            if (getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
            //e.printStackTrace();
        } catch (Exception e) {
            if (getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
            //e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    if (getApplicationContext() != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                }
            }
        }
    }

    public JSONArray readFromFile() {
        FileInputStream fis = null;
        JSONArray jsonArrayToRead = null;

        try {
            if (isFileExists()) {
                fis = openFileInput(FILE_NAME);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String text;

                while ((text = br.readLine()) != null) {
                    sb.append(text).append("\n");
                }
                jsonArrayToRead = new JSONArray(sb.toString());
            }
        } catch (FileNotFoundException e) {
            if(getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage() , Log.getStackTraceString(e), getApplicationContext());
        } catch (IOException e) {
            if(getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
        } catch (JSONException e) {
            if(getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
        } catch (Exception e) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    if(getApplicationContext() != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                } catch (Exception e) {
                    if(getApplicationContext() != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                }
            }
            return jsonArrayToRead;  //it contains null when calling this method without writting otherwise return written data
        }
    }

    public Boolean isFileExists() throws Exception {
        File dir = getFilesDir();
        File file = new File(dir, "pendingSentiments.json");
        return file.exists();
    }

    public void deleteFile() throws Exception {
        File dir = getFilesDir();
        File file = new File(dir, "pendingSentiments.json");
        file.delete();
    }

}
