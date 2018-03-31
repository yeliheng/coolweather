package com.coolweather.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.coolweather.android.util.APKVersionCodeUtils;
import com.coolweather.android.util.HttpUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by yeliheng on 2018/3/31.
 * 功能:检查版本更新
 */

public class Update{
    final public String url = "https://raw.githubusercontent.com/yeliheng/coolweather/master/version.json";//版本请求地址
    /*
    * 检查版本更新
    * */
    public void checkUpdate(final Context context){
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("正在检查更新，稍等哦...");
        progressDialog.show();
        HttpUtil.sendOkHttpRequest(url, new Callback() {//发送http请求检查版本
           @Override
           public void onFailure(Call call, IOException e) {

           }
           @Override
           public void onResponse(Call call, Response response) throws IOException {

               String responseText = response.body().string();//获取报文主体内容
               try {
                   JSONObject jsonObject = new JSONObject(responseText);
                   final String version = jsonObject.getString("version");
                   if(version.equals(APKVersionCodeUtils.getVerName(context))){
                       /*
                       * 注:这里会出现一个问题:
                       * W/System.err: java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()
                       * 原因:Toast只能在UI线程弹出，如果一定要在子线程弹，那么就通过 new Handler(Looper.getMainLooper()) 来弹
                       * */
                       new Thread(new Runnable() {
                           @Override
                           public void run() {
                               Handler handler = new Handler(Looper.getMainLooper());
                               handler.post(new Runnable() {
                                   @Override
                                   public void run() {
                                       Toast.makeText(context,"恭喜,软件已经是最新版本!",Toast.LENGTH_SHORT).show();
                                       progressDialog.dismiss();//关闭进度条
                                   }
                               });
                           }
                       }).start();
                   }else{
                      new Thread(new Runnable() {
                           @Override
                           public void run() {
                               Handler handler = new Handler(Looper.getMainLooper());
                               handler.post(new Runnable() {
                                   @Override
                                   public void run() {
                                       progressDialog.dismiss();//关闭进度条
                                       AlertDialog versionDialog = new AlertDialog.Builder(context)
                                               .setTitle("φ(>ω<*) 软件有新版本啦~")
                                               .setMessage("发现新版本!\n" +
                                                       "是否现在更新?\n" +
                                                       "移动数据请注意流量哦~")
                                               .setNegativeButton("好", new DialogInterface.OnClickListener() {
                                                   @Override
                                                   public void onClick(DialogInterface dialog, int which) {
                                                       downloadNewVersion(context);//下载新版本
                                                   }
                                               })
                                               .setPositiveButton("下次再来", new DialogInterface.OnClickListener() {
                                                   @Override
                                                   public void onClick(DialogInterface dialog, int which) {
                                                       dialog.dismiss();
                                                   }
                                               }).create();
                                       versionDialog.show();
                                   }
                               });
                           }
                       }).start();
                   }
               }catch (Exception e){
                   e.printStackTrace();
               }

           }
       });
    }
    public void downloadNewVersion(Context context){//下载新版本逻辑
        Toast.makeText(context,"正在下载新版本...",Toast.LENGTH_SHORT).show();
        //创建下载任务,downloadUrl就是下载链接
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse("https://github.com/yeliheng/coolweather/blob/master/app/release/app-release.apk"));
        //指定下载路径和下载文件名
        request.setDestinationInExternalPublicDir("/download/", "weather.apk");
        //获取下载管理器
        DownloadManager downloadManager= (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载任务加入下载队列，否则不会进行下载
        Toast.makeText(context,"下载完成，请手动到download目录下安装\n后期将修复此问题",Toast.LENGTH_LONG).show();
    }
}

