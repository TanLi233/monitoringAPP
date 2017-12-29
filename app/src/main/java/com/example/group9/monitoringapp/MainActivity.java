package com.example.group9.monitoringapp;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback{
    //    private  LocationManager mLocationManager;
//    private LocationListener mLocationListener;
    private MapFragment mapFragment;
    private LatLng pos = new LatLng(0,0);
    private ArrayList<LatLng> pos_record = new ArrayList<>();
    private GoogleMap map;
    private String data;
    private Thread thread;
    private ArrayList<LatLng> dangerZone = new ArrayList<>();
    private PolygonOptions polygonOptions;
//    private Criteria criteria;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            if (what == 1){
//                Toast.makeText(MainActivity.this, data, Toast.LENGTH_SHORT).show();
                map.clear();
                map.addPolygon(polygonOptions);
                map.addCircle(new CircleOptions()
                        .center(pos)
                        .radius(10)
                        .strokeColor(Color.RED)
                        .fillColor(Color.RED));
            }else if (what == 2){
                Toast.makeText(MainActivity.this, "get failed", Toast.LENGTH_SHORT).show();
            }else if (what == 0){
//                Toast.makeText(MainActivity.this, "insert start!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void onMapReady(final GoogleMap map) {
        this.map = map;

        polygonOptions = new PolygonOptions();
        polygonOptions.strokeColor(Color.RED)
                .strokeWidth(3)
                .fillColor(Color.argb(100, 255, 0, 0));

        getDangerZone();
        try {
            Thread.sleep(2000);
        }catch (Exception e){
            e.printStackTrace();
        }

        for (LatLng latlng: dangerZone) {
            polygonOptions.add(latlng);
        };
        polygonOptions.add(dangerZone.get(0));
        map.addPolygon(polygonOptions);

        final Handler h = new Handler();
        h.postDelayed(new Runnable()
        {
            private long time = 0;

            @Override
            public void run()
            {
                getLastLocation();
                // do stuff then
                // can call h again after work!

                time += 5000;
                h.postDelayed(this, 5000);
            }
        }, 5000); // 5 seconds delay
        try {
            map.setMyLocationEnabled(true);
        }catch (SecurityException e){
            e.printStackTrace();
        }

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                pos_record.add(latLng);
                map.addCircle(new CircleOptions()
                        .center(latLng)
                        .radius(5)
                        .strokeWidth(1)
                        .strokeColor(Color.RED)
                        .fillColor(Color.argb(255, 255, 0, 0))
                        .clickable(false));
                if(pos_record.size() > 1){
                    map.clear();
                    PolygonOptions polygonOptions = new PolygonOptions();
                    polygonOptions.strokeColor(Color.RED)
                                .strokeWidth(3)
                                .fillColor(Color.argb(100, 255, 0, 0));

                    for (LatLng latlng:pos_record) {
                        polygonOptions.add(latlng);
                    };
                    polygonOptions.add(pos_record.get(0));
                    map.addPolygon(polygonOptions);

                }
            }
        });
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    private void getLastLocation(){
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    mHandler.sendEmptyMessage(0);
                    URL url = new URL("http://10.4.3.11/get_location.php");
                    //获取连接对象,此时未建立连接
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    //设置请求方式为Get请求
                    conn.setRequestMethod("GET");

                    //设置连接超时
                    conn.setConnectTimeout(10000);
                    int code = conn.getResponseCode();
                    if (HttpURLConnection.HTTP_OK == code) {
                        InputStream in = conn.getInputStream();
                        data = readStream(in);
                        String[] l = data.split(",");
                        float lat = Float.parseFloat(l[0]);
                        float lng = Float.parseFloat(l[1]);
                        pos = new LatLng(lat,lng);
                        in.close();
                        mHandler.sendEmptyMessage(1);
                    }else{
                        mHandler.sendEmptyMessage(2);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    @Override
    public void onDestroy() {
        thread.interrupt();
        super.onDestroy();
    }
    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }

    private void getDangerZone(){
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    mHandler.sendEmptyMessage(0);
                    URL url = new URL("http://10.4.3.11/get_dangerzone.php");
                    //获取连接对象,此时未建立连接
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    //设置请求方式为Get请求
                    conn.setRequestMethod("GET");

                    //设置连接超时
                    conn.setConnectTimeout(10000);
                    int code = conn.getResponseCode();
                    if (HttpURLConnection.HTTP_OK == code) {
                        InputStream in = conn.getInputStream();
                        String polygon = readStream(in);
                        String[] l = polygon.split(",");
                        for(int i = 0; i < l.length; i=i+2){
                            dangerZone.add(new LatLng(Float.parseFloat(l[i]),Float.parseFloat(l[i+1])));
                        }
                        in.close();
                        mHandler.sendEmptyMessage(1);
                    }else{
                        mHandler.sendEmptyMessage(2);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        thread2.start();
    }
}
