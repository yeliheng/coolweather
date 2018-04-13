package com.coolweather.android;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.w3c.dom.Text;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    public DrawerLayout drawerLayout;//滑动菜单
    private Button navButton;//菜单按钮
    public SwipeRefreshLayout swipeRefresh;//下拉刷新控件
    private ScrollView weatherLayout;//天气布局
    private TextView titleCity;//城市标题
    private TextView titleUpdateTime;//更新时间
    private TextView degreeText;//温度
    private TextView weatherInfoText;//天气信息
    private LinearLayout forecastLayout;//预报
    private TextView aqiText;//aqi指数
    private TextView pm25Text;//pm2.5指数
    private TextView comfortText;//适宜信息
    private TextView carWashText;//洗车信息
    private TextView sportText;//运动信息
    private ImageView bingPicImg;//必应每日一图
    private final String key = "33c00ceb7990401eb4ca9932ede7240e";//API密钥
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= 21){//判断API版本>Android5.0
            View decorView = getWindow().getDecorView();//使图片与状态栏融为一体
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        //请求权限

        //初始化控件
        bingPicImg = findViewById(R.id.bing_pic_img);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        drawerLayout = findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);//存储数据到sd卡
        String weatherString = prefs.getString("weather",null);
        final String weatherId;
        String bingPic = prefs.getString("bing_pic",null);
        if(bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImg);//使用Glide开源库加载必应图片
        }else{
            loadBingPic();//加载必应图片
        }
        if(weatherString != null){
            //检查是否存在缓存
            //存在时直接本地解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            weatherId = weather.basic.weatherId;
            showWeatherInfo(weather);//显示天气信息
        }else{
            //无缓存时到服务器上查询
            weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {//刷新逻辑
                String newWeatherId = prefs.getString("new_weather_id",weatherId);//从缓存中取，避免刷新的时候数据被weatherId覆盖
                requestWeather(newWeatherId);
            }
        });
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);//打开滑动菜单
            }
        });
    }
    /*
    * 通过weatherId请求天气信息
    * */
    public void requestWeather(final String weatherId){
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=" + key;//请求地址
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {//发送请求(OkHttp)
            @Override
            public void onFailure(Call call, IOException e) {//失败回调
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"网络不给力,获取天气信息失败",Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
                loadBingPic();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {//数据回调
                final String responseText = response.body().string();//获取数据主体
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {//返回主线程来更新界面
                    @Override
                    public void run() {
                        if(weather != null && "ok".equals(weather.status)){//判断信息是否有效
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();//存储数据到SD卡
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);//显示天气信息
                        }else{
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }
    /*
    * 处理并显示Weather实体类中的天气信息
    * */
    public void showWeatherInfo(Weather weather){
        String cityName = weather.basic.cityName;//城市名称
        String updateTime = weather.basic.update.updateTime.split(" ")[1];//格式化时间
        String degree = weather.now.temperature + "℃";//温度信息
        String weatherInfo = weather.now.more.info;//天气信息
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for(Forecast forecast : weather.forecastsList){//遍历预报信息,设置控件信息
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if(weather.aqi != null){//设置aqi相关控件信息
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        //无需注释
        String comfort = "舒适度: " + weather.suggestion.comfort.info;
        String carWash = "洗车指数: " + weather.suggestion.carWash.info;
        String sport = "运动建议: " + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }
    /*
    * 加载必应每日一图
    * */
    private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";//必应图片接口
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {//到服务器上请求图片
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);//将从服务器获取到的图片地址存储到SD卡
                editor.apply();
                runOnUiThread(new Runnable() {//返回主线程来更新UI
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);//通过Glide解析图片
                    }
                });
            }
        });
    }

}
