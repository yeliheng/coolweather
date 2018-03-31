package com.coolweather.android;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import static org.litepal.LitePalApplication.getContext;

public class SettingsActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {
    private Button back_button;
    private RadioGroup radioGroup;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        back_button = findViewById(R.id.back_button);
        radioGroup = findViewById(R.id.radioGroup);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        radioGroup.setOnCheckedChangeListener(this);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String update_time = preferences.getString("update_time","");
            switch (update_time){
                case "2":
                    radioGroup.check(R.id.two_hours);
                    break;
                case "4":
                    radioGroup.check(R.id.four_hours);
                    break;
                case "8":
                    radioGroup.check(R.id.eight_hours);
                    break;
                default:
                    radioGroup.check(R.id.eight_hours);
            }

    }

    private void writeSettings(String hours){//写入设置
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("update_time",hours);//写入刷新频率
        editor.commit();//提交修改
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {//单选按钮回调
        switch (checkedId){
            case R.id.two_hours:
                writeSettings("2");
                //Toast.makeText(getContext(),"已写入2小时",Toast.LENGTH_SHORT).show();
                break;
            case R.id.four_hours:
                writeSettings("4");
               // Toast.makeText(getContext(),"已写入4小时",Toast.LENGTH_SHORT).show();
                break;
            case R.id.eight_hours:
                writeSettings("8");
                //Toast.makeText(getContext(),"已写入8小时",Toast.LENGTH_SHORT).show();
                break;
            default:
                writeSettings("8");
        }
    }
}
