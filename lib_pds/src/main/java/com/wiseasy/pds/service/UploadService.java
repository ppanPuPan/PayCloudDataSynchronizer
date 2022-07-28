package com.wiseasy.pds.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wiseasy.pds.db.DbHelper;
import com.wiseasy.pds.db.TableRecord;
import com.wiseasy.pds.network.RetrofitApi;
import com.wiseasy.pds.network.RetrofitClient;
import com.wiseasy.pds.response.BaseResponse;

import java.io.IOException;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Response;

/**
 * @author hugo
 * @date 2022/6/15
 */
public class UploadService extends IntentService {
    private static AlarmManager mAlarmManager;
    private static SQLiteDatabase db;
    private RetrofitApi serviceApi;

    public static void start(Context context) {
        mAlarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        DbHelper dbHelper = new DbHelper(context, "pds.db", null, 1);
        db = dbHelper.getWritableDatabase();
        Intent intent = new Intent(context, UploadService.class);
        context.startService(intent);
    }


    public UploadService() {
        super("UploadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //如果是非30分钟闹钟触发，则清除闹钟
        cancelOneTimeAlarm();
        upload();
    }

    /**
     * 查询record表中的数据，然后启动上送，上送成功后删除
     */
    private void upload() {
        serviceApi = RetrofitClient.getApi();
        JSONArray transactionComplete = TableRecord.query(db, TableRecord.RECORD_TYPE_COMPLETE_TRANSACTION);
        transactionComplete = doUpLoad(transactionComplete);
        TableRecord.update(db, TableRecord.RECORD_TYPE_COMPLETE_TRANSACTION, transactionComplete.toJSONString());
        JSONArray transactionClose = TableRecord.query(db, TableRecord.RECORD_TYPE_CLOSE_TRANSACTION);
        transactionClose = doUpLoad(transactionClose);
        TableRecord.update(db, TableRecord.RECORD_TYPE_COMPLETE_TRANSACTION, transactionClose.toJSONString());
        JSONArray transactionLog = TableRecord.query(db, TableRecord.RECORD_TYPE_LOG);
        transactionLog = doUpLoad(transactionLog);
        TableRecord.update(db, TableRecord.RECORD_TYPE_COMPLETE_TRANSACTION, transactionLog.toJSONString());
        if (!transactionClose.isEmpty() || !transactionComplete.isEmpty() || !transactionLog.isEmpty()) {
            setOneTimeAlarm();
        }
    }

    private JSONArray doUpLoad(JSONArray array) {
        if (null == array || array.isEmpty()) {
            return array;
        }
        JSONArray endJSONArray = array;
        for (int i = 0; i > array.size(); i++) {
            JSONObject jsonObject = array.getJSONObject(i);
            Call<JSONObject> result = serviceApi.sendRequest(RetrofitClient.createJsonRequestBody(jsonObject));
            try {
                Response<JSONObject> data = result.execute();
                if (data.isSuccessful()) {
                    endJSONArray.remove(i);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return endJSONArray;
    }

    /**
     * 设置一次性闹钟，30分钟后触发
     */
    private void setOneTimeAlarm() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 1800);
        Intent intent = new Intent(AlarmReceiver.ALARM_EVENT);
        PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pIntent);
    }

    /**
     * 清除闹钟
     */
    private void cancelOneTimeAlarm() {
        Intent intent = new Intent(AlarmReceiver.ALARM_EVENT);
        PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmManager.cancel(pIntent);
    }
}
