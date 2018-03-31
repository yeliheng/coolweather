package com.coolweather.android.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.coolweather.android.R;
import com.coolweather.android.WeatherActivity;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    private final String key = "33c00ceb7990401eb4ca9932ede7240e";//API密钥
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {//服务逻辑
        int hour = Integer.parseInt(getSettings());
        updateWeather();//更新天气
        updateBingPic();//更新每日一图
       // sendNotification();//发送通知
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);//系统时间管理服务
        int anHour = hour * 60 * 60 * 1000;//八小时的毫秒数 时-分-秒-毫秒
       // Log.d("刷新频率","" + anHour);
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;//触发时间
        Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this,0,i,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent,flags,startId);
    }
    /*
    * 更新天气信息
    * */
    private void updateWeather(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        if(weatherString != null){
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=" + key;//请求地址
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseText);
                    if(weather != null && "ok".equals(weather.status)){//判断信息是否有效
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();//存储数据到SD卡
                        editor.putString("weather",responseText);
                        editor.apply();
                }
                }
            });
        }
    }
    /*
    * 更新必应每日一图
    * */
    private void updateBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";//必应图片接口
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic",bingPic);//将从服务器获取到的图片地址存储到SD卡
                editor.apply();
            }
        });
    }
    private String getSettings(){//获取配置信息
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String update_time = preferences.getString("update_time","8");//获取天气刷新频率，默认8
        return update_time;
    }
    private void sendNotification(){//服务触发时发送通知
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(getBaseContext())
                .setContentTitle("天气更新通知")
                .setContentText("天气信息已更新!")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.logo)
                .build();
        manager.notify(1,notification);
    }
}
