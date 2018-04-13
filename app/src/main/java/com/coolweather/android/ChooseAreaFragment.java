package com.coolweather.android;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.APKVersionCodeUtils;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static android.content.Context.CLIPBOARD_SERVICE;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_ITEM = 3;//ListView中的选择地区
    public static final int LEVEL_PROVINCE = 0;//省级
    public static final int LEVEL_CITY = 1;//市级
    public static final int LEVEL_COUNTY = 2;//县级
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    /*
    * 省链表
    * */
    private List<Province> provinceList;
    /*
    * 市链表
    * */
    private List<City> cityList;
    /*
    * 县链表
    * */
    private List<County> countyList;
    private Province selectedProvince;//XC选中的省份
    private City selectedCity;//选中的市
    private int currentLevel;//当前选中级别
   // private APKVersionCodeUtils versionCodeUtils;//版本信息获取
    Update update = new Update();
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                if(currentLevel == LEVEL_ITEM){
                    switch (pos){
                        case 0://选择地区
                            queryProvinces();
                            break;
                        case 1://设置
                            openSettingsActivity();
                            break;
                        case 2://关于软件
                            showSoftInfo();
                            break;
                        case 3://检查更新
                            //申请权限
                            if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                                ActivityCompat.requestPermissions(getActivity(),new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                            }else{
                                update.checkUpdate(getContext());//检查更新
                            }
                            break;
                        case 4://软件源码
                            goToGithub();
                            break;
                        case 5://更新日志

                            break;
                        case 6://官网
                            goToWebsite();
                            break;
                        case 7://捐助
                            donateMe();
                            break;
                    }
                }else if(currentLevel ==LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(pos);
                    queryCities();//查询城市
                }else if(currentLevel ==LEVEL_CITY){
                    selectedCity = cityList.get(pos);
                    queryCounties();//查询县
                }else if(currentLevel == LEVEL_COUNTY){//点击县级数据时，自动获取天气id,并跳转到新的活动
                    String weatherId = countyList.get(pos).getWeatherId();
                    if(getActivity() instanceof MainActivity) {//判断是不是MainActivity的实例
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        Log.d("MainActivity",intent.getStringExtra("new_weather_id") + "test");
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){//判断是不是WeatherActivity的实例
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("new_weather_id",weatherId);//写入新的地区id缓存(刷新的时候用)
                        editor.commit();
                      //  Log.d("WeatherActivity",intent.getStringExtra("new_weather_id") + "test");
                        activity.drawerLayout.closeDrawers();//关闭滑动菜单
                        activity.swipeRefresh.setRefreshing(true);//显示刷新进度
                        activity.requestWeather(weatherId);//请求天气信息
                    }
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {//返回键按下监听
            @Override
            public void onClick(View view) {
                if(currentLevel == LEVEL_COUNTY){//如果级别为县，则查询市
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }else if(currentLevel ==LEVEL_PROVINCE){
                    queryItems();
                }
            }
        });
        queryItems();
       // queryProvinces();//查询省份
    }
    /*
    * 滑动菜单内容(待添加)
    * */
        private void queryItems(){
            titleText.setText("叫兽天气");
            backButton.setVisibility(View.GONE);//关闭返回按钮
            if(dataList.size() > 0){
                dataList.clear();
            }
            dataList.add("选择地区");//0
            dataList.add("设置");//1
            dataList.add("关于软件");//2
            dataList.add("检查更新");//3
            dataList.add("软件源码");//4
            dataList.add("更新日志");//5
            dataList.add("官网");//6
            dataList.add("捐助");//7
            adapter.notifyDataSetChanged();//通知适配器更新数据
            listView.setSelection(0);//光标移动到0
            currentLevel = LEVEL_ITEM;
        }
    /*
    * 查询中国所有的省，查询方式:数据库优先，失败时自动向服务器查询
    * */
    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.VISIBLE);
        provinceList = DataSupport.findAll(Province.class);
        if(provinceList.size() > 0){
            dataList.clear();
            for(Province province : provinceList){//遍历所有省份
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();//通知适配器更新数据
            listView.setSelection(0);//光标移动到0
            currentLevel = LEVEL_PROVINCE;
        }else{
            String address = "http://guolin.tech/api/china";//服务器地址
            queryFromServer(address,"province");//向服务器查询
        }
    }
    /*
    * 查询城市，方法同上
    * */
    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);//数据库查询
        if(cityList.size() > 0){//逻辑同上
            dataList.clear();
            for (City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;//服务器地址
            queryFromServer(address,"city");
        }
    }
    /*
    * 查询县，方法同上
    * */
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size() > 0){
            dataList.clear();
            for (County county :countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address,"county");
        }
    }
    /*
    * 向服务器查询
    * */
    private void queryFromServer(String address,final String type){
        showProgressDialog();//显示查询进度条函数
        HttpUtil.sendOkHttpRequest(address, new Callback() {//向服务器发送Http请求(OkHttp)
            @Override
            public void onFailure(Call call, IOException e) {//失败回调
                e.printStackTrace();
                getActivity().runOnUiThread(new Runnable() {//回到主线程来更新UI
                    @Override
                    public void run() {
                        closeProgressDialog();//关闭进度条
                        Toast.makeText(getContext(),"哎呀,网络出问题了,获取不到数据",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {//请求回调
                String responseText = response.body().string();//获取报文主体内容
                boolean result = false;//返回结果，默认false
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);//调用Utility类中的方法来解析服务器返回的json数据
                }else if("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());//同上
                }else if("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if(result){//对返回的结果进行处理
                    getActivity().runOnUiThread(new Runnable() {//回到主线程来更新UI
                        @Override
                        public void run() {
                            closeProgressDialog();//关闭进度条
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }
    /*
    * 显示进度条
    * */
    private void showProgressDialog(){
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在获取数据,请稍等...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    /*
    * 关闭进度条
    * */
    private void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }
    public void showSoftInfo() {//显示软件信息
        String versionName = APKVersionCodeUtils.getVerName(getContext());
        final AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("软件信息")
                .setMessage("本软件提供各地区天气查询服务\n" +
                        "版本:" + versionName + "\n" +
                        "☞开发人员:叫兽°\n" +
                        "☞Email:yeliheng00@163.com\n" +
                        "☞天气数据API来源:和风天气\n" +
                        "☞背景图片API:微软Bing每日一图\n" +
                        "☞软件源码已在GitHub开放\n" +
                        "☞本软件受Apache2.0开源协议保护"
                )
                .setIcon(R.drawable.tip)
                .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        alertDialog.show();

    }
    public void goToGithub(){//跳转到GitHub
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("源码地址")
                .setMessage("https://github.com/yeliheng/coolweather\n" +
                        "你的关注就是对我最大的鼓励！")
                .setNegativeButton("打开", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent= new Intent();
                        intent.setAction("android.intent.action.VIEW");
                        Uri content_url = Uri.parse("https://github.com/yeliheng/coolweather");
                        intent.setData(content_url);
                        startActivity(intent);
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
                alertDialog.show();
    }
    public void donateMe(){//捐助逻辑
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("捐助我")
                .setMessage("捐助的数额请随意，此资金将被用于维护项目以及服务器")
                .setNegativeButton("支付宝", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                                .setTitle("支付宝")
                                .setMessage("yeliheng00@163.com")
                                .setNegativeButton("复制", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ClipData myClip;
                                        ClipboardManager myClipboard;
                                        myClipboard = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
                                        String text = "yeliheng00@163.com";//如果有内容直接添加就好
                                        myClip = ClipData.newPlainText("text", text);//text是内容
                                        myClipboard.setPrimaryClip(myClip);
                                        Toast.makeText(getContext(),"已复制",Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).create();
                        alertDialog.show();
                    }
                })
                .setPositiveButton("微信", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                                .setTitle("微信")
                                .setMessage("微信号:977782528")
                                .setNegativeButton("复制", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ClipData myClip;
                                        ClipboardManager myClipboard;
                                        myClipboard = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
                                        String text = "977782528";//如果有内容直接添加就好
                                        myClip = ClipData.newPlainText("text", text);//text是内容
                                        myClipboard.setPrimaryClip(myClip);
                                        Toast.makeText(getContext(),"已复制",Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).create();
                        alertDialog.show();
                    }
                })
                .setNeutralButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.show();
    }
    public void goToWebsite(){//跳转到官网
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("官方网站")
                .setMessage("http://www.ccyun.club\n" +
                        "枫叶人工智能实验室")
                .setNegativeButton("打开", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent= new Intent();
                        intent.setAction("android.intent.action.VIEW");
                        Uri content_url = Uri.parse("http://www.ccyun.club");
                        intent.setData(content_url);
                        startActivity(intent);
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        alertDialog.show();
    }
    private void openSettingsActivity(){//打开设置界面
        Intent intent = new Intent(getActivity(),SettingsActivity.class);
        startActivity(intent);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {//提权回调
        switch (requestCode){
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    update.checkUpdate(getContext());
                }else{
                    Toast.makeText(getContext(),"获取必要权限失败!请重新授权!",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}