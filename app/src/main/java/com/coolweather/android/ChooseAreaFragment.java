package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
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
    private Province selectedProvince;//选中的省份
    private City selectedCity;//选中的市
    private int currentLevel;//当前选中级别

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
                if(currentLevel ==LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(pos);
                    queryCities();//查询城市
                }else if(currentLevel ==LEVEL_CITY){
                    selectedCity = cityList.get(pos);
                    queryCounties();//查询县
                }else if(currentLevel == LEVEL_COUNTY){//点击县级数据时，自动获取天气id,并跳转到新的活动
                    String weatherId = countyList.get(pos).getWeatherId();
                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    intent.putExtra("weather_id",weatherId);
                    startActivity(intent);
                    getActivity().finish();
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
                }
            }
        });
        queryProvinces();
    }
    /*
    * 查询中国所有的省，查询方式:数据库优先，失败时自动向服务器查询
    * */
    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
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
}