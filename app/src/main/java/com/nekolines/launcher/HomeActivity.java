package com.nekolines.launcher;

import android.annotation.SuppressLint;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static android.content.ContentValues.TAG;

public class HomeActivity extends Activity {

    private LocationManager locationManager;
    private String locationProvider;
    private String Lati=null;//wei
    private String Longt=null;//jing
    private AppACache mCache;
    private String WeatherKey;

    @SuppressLint("MissingPermission")
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        initView();

        new TimeThread().start();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //获取所有可用的位置提供器
        List<String> providers = locationManager.getProviders(true);
        if(providers.contains(LocationManager.GPS_PROVIDER)){
            //如果是GPS
            locationProvider = LocationManager.GPS_PROVIDER;
        }else if(providers.contains(LocationManager.NETWORK_PROVIDER)){
            //如果是Network
            locationProvider = LocationManager.NETWORK_PROVIDER;
        }else{
            return ;
        }
        //获取Location
        @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(locationProvider);
        if(location!=null){
            Lati= String.valueOf(location.getLatitude());
            Longt= String.valueOf(location.getLongitude());
        }
        //监视地理位置变化
        locationManager.requestLocationUpdates(locationProvider, 3000, 1, locationListener);


        new WeatherThread().start();
        refreshView(getWindow().getDecorView());
    }

    public void showApps(View v){

        Intent i = new Intent(this, AppsListActivity.class);

        startActivity(i);

    }

    public void showSetting(View v){
        Intent intent =  new Intent(android.provider.Settings.ACTION_SETTINGS);
        startActivity(intent);
    }

    private TextView time;
    private TextView WEA;
    private ImageView WEAIC;
    private TextView Hito;
    private TextView From;
    private TextView Power;
    private WebImageView BGIM;
    public static final int MSG_ONE = 1;
    public static final int WEATH = 2;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //通过消息的内容msg.what  分别更新ui
            switch (msg.what) {
                case MSG_ONE:
                    //获取网络时间
                    //请求网络资源是耗时操作。放到子线程中进行
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getNetTime();
                        }
                    }).start();
                    break;
                case WEATH:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getWeather();
                            changeBG();
                            getHitoko();
                        }
                    }).start();
                    break;
                default:
                    break;
            }
        }
    };


    private void getWeather() {
        String result=null;
        if (Lati==null){
            runOnUiThread(new Runnable() {
                @Override

                public void run() {
                    WEA.setText(getResources().getString(R.string.app_nolocation));
                    WEAIC.setImageDrawable(getResources().getDrawable(R.drawable.icon999));
                    Toast.makeText(HomeActivity.this, getResources().getString(R.string.locationNull), Toast.LENGTH_SHORT).show();
                }
            });

        }else {
        URL url =null;
        if(mCache.getAsString("weather")==null){
        try {
           url=new URL("https://free-api.heweather.net/s6/weather/now?location="+Longt+","+Lati+"&key="+WeatherKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");//使用GET方法获取
            conn.setConnectTimeout(5000);
            int code = conn.getResponseCode();
            if (code == 200) {
                /**
             * 如果获取的code为200，则证明数据获取是正确的。
             */
                InputStream is = conn.getInputStream();
                byte[] data = readStream(is); // 把输入流转换成字符串组
                result = new String(data); // 把字符串组转换成字符串
            result = result.replace("[", "");
            result = result.replace("]", "");
            mCache.put("weather", result, 1800);
            Log.d(TAG,"天气数据缓存过期，正在联网更新中。");
            }
        }catch (Exception e){
            e.printStackTrace();
        }}else{
            result=mCache.getAsString("weather");
        }

        try{
        JSONObject WEATHER = new JSONObject(result);
        ApplicationInfo appInfo = getApplicationInfo();
        final String tmpe=WEATHER.getJSONObject("HeWeather6").getJSONObject("now").getString("tmp")+"°";
        final int ids= getResources().getIdentifier("icon"+WEATHER.getJSONObject("HeWeather6").getJSONObject("now").getString("cond_code") ,"drawable", appInfo.packageName);
        runOnUiThread(new Runnable() {
            @Override

            public void run() {
                WEA.setText(tmpe);
                WEAIC.setImageDrawable(getResources().getDrawable(ids));
            }
        });}catch (Exception e){
            e.printStackTrace();
        }
    }}

    private void changeBG() {
        BGIM.setImageURL("https://unsplash.it/1280/800?random");
    }

    private JSONObject getHitokoJson(){
        //超过三十个字就重来！
        URL url =null;
        String Hito="";
        JSONObject Hitoko=null;
        try {
            url=new URL("https://v1.hitokoto.cn/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");//使用GET方法获取
            conn.setConnectTimeout(5000);
            int code = conn.getResponseCode();
            if (code == 200) {
                /**
                 * 如果获取的code为200，则证明数据获取是正确的。
                 */
                InputStream is = conn.getInputStream();
                byte[] data = readStream(is); // 把输入流转换成字符串组
                String result = new String(data); // 把字符串组转换成字符串
                Hitoko = new JSONObject(result);
                ApplicationInfo appInfo = getApplicationInfo();
                Hito=Hitoko.getString("hitokoto");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if(Hito.length()>30){
            Log.d(TAG,Hito);
            return getHitokoJson();
        }else {
            return Hitoko;
        }
    }

    private void getHitoko() {
        JSONObject Hitoko=getHitokoJson();
        try {
            final String hitokoto=ToSBC(Hitoko.getString("hitokoto"));
            final String form= Hitoko.getString("from");;

            runOnUiThread(new Runnable() {
                @Override

                public void run() {
                    Hito.setText(hitokoto);
                    From.setText(form);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public class TimeThread extends Thread {
        //重写run方法
        @Override
        public void run() {
            super.run();
            // do-while  一 什么什么 就
            do {
                try {
                    //每隔一秒 发送一次消息
                    Thread.sleep(1000);
                    Message msg = new Message();
                    //消息内容 为MSG_ONE
                    msg.what = MSG_ONE;
                    //发送
                    handler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }

    public class WeatherThread extends Thread {
        //重写run方法
        @Override
        public void run() {
            super.run();
            // do-while  一 什么什么 就
            do {
                try {
                    //每隔一秒 发送一次消息
                    Thread.sleep(3600000);
                    Message msg = new Message();
                    //消息内容 为MSG_ONE
                    msg.what = WEATH;
                    //发送
                    handler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }

    private void initView() {
        time = findViewById(R.id.time);
        WEA = findViewById(R.id.degrees);
        WEAIC = findViewById(R.id.weather);
        Hito = findViewById(R.id.hitoko);
        From = findViewById(R.id.author);
        BGIM = findViewById(R.id.home_bg);
        Power = findViewById(R.id.power);
        WeatherKey="和风天气key";
    }

    private void getNetTime() {
        URL url = null;//取得资源对象
        try {
            DateFormat formatter = new SimpleDateFormat("HH:mm");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            final String format = formatter.format(calendar.getTime());
            final String pow= String.valueOf(checkBattery());
            runOnUiThread(new Runnable() {
                @Override

                public void run() {
                    if(Integer.valueOf(pow)>=70){
                        Power.setTextColor(getResources().getColor(R.color.batteryHigh));
                    }else if(Integer.valueOf(pow)<70&&Integer.valueOf(pow)>40){
                        Power.setTextColor(getResources().getColor(R.color.batteryMid));
                    }else{
                        Power.setTextColor(getResources().getColor(R.color.batteryLow));
                    }
                    time.setText(format);
                    Power.setText(pow);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int checkBattery()
    {
        //通过粘性广播读取电量
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intentBattery = registerReceiver(null, intentFilter);//注意，粘性广播不需要广播接收器
        int batteryLevel=0;
        if(intentBattery!=null)
        {
            //获取当前电量
            batteryLevel = intentBattery.getIntExtra("level", 0);
            //电量的总刻度
            int batterySum = intentBattery.getIntExtra("scale", 100);
        }
        return batteryLevel;
    }

    LocationListener locationListener =  new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle arg2) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onLocationChanged(Location location) {
            //如果位置发生变化,重新显示
            Lati= String.valueOf(location.getLatitude());
            Longt= String.valueOf(location.getLongitude());

        }
    };

    private static byte[] readStream(InputStream inputStream) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            bout.write(buffer, 0, len);
        }
        bout.close();
        inputStream.close();
        return bout.toByteArray();
    }

    public void onBackPressed() {
// 这里处理逻辑代码，大家注意：该方法仅适用于2.0或更新版的sdk
        Toast.makeText(HomeActivity.this, getResources().getString(R.string.onbcpr), Toast.LENGTH_SHORT).show();
        return;
    }

    public static String ToSBC(String input) {
        char c[] = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == ' ') {
                c[i] = '\u3000';
            } else if (c[i] < '\177') {
                c[i] = (char) (c[i] + 65248);
            }
        }
        return new String(c);
    }

    private void refreshView(View v){
        mCache = AppACache.get(this);
        new Thread(){
            public void run(){
                changeBG();
                getWeather();
                getHitoko();
                getNetTime();
            }
        }.start();
    }

    public void researchALL(View v){
        refreshView(v);
        Toast.makeText(HomeActivity.this, getResources().getString(R.string.resc), Toast.LENGTH_SHORT).show();
    }
}