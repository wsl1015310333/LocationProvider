package com.example.wangsl.locationprovider;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {
Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button=(Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {







        Log.e("getLnt",   getLngAndLatWithNetwork());
                Location location = LocationUtils.getInstance( MainActivity.this ).showLocation();
                if (location != null) {
                    String address = "纬度：" + location.getLatitude() + "经度：" + location.getLongitude();
                    Log.d( "FLY.LocationUtils", address );

                }else {
                    Log.d( "FLY.LocationUtils", "Null" );
                }





                TelephonyManager mTelephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

                // 返回值MCC + MNC
                String operator = mTelephonyManager.getNetworkOperator();
//                int mcc = Integer.parseInt(operator.substring(0, 3));
//                int mnc = Integer.parseInt(operator.substring(3));
//                int mcc = operator;
//                int mnc = Integer.parseInt(operator.substring(3));

                // 中国移动和中国联通获取LAC、CID的方式
                GsmCellLocation locationa = (GsmCellLocation) mTelephonyManager.getCellLocation();
                final int lac = locationa.getLac();
                final int cellId = locationa.getCid();

                Log.i("--", " MCC = " + operator + "\t MNC = " + operator + "\t LAC = " + lac + "\t CID = " + cellId);

                new Thread() {
                    @Override
                    public void run() {
                        try {
                            String json = getJsonCellPos(460, 1, lac, cellId);
                            Log.i("--", "request = " + json);

                            String url = "http://www.minigps.net/minigps/map/google/location";
                            String result = httpPost(url, json);
                            Log.i("--", "result = " + result);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }.start();
                // 中国电信获取LAC、CID的方式
                /*CdmaCellLocation location1 = (CdmaCellLocation) mTelephonyManager.getCellLocation();
                lac = location1.getNetworkId();
                cellId = location1.getBaseStationId();
                cellId /= 16;*/

                // 获取邻区基站信息
                List<NeighboringCellInfo> infos = mTelephonyManager.getNeighboringCellInfo();
                StringBuffer sb = new StringBuffer("总数 : " + infos.size() + "\n");
                for (NeighboringCellInfo info1 : infos) { // 根据邻区总数进行循环
                    sb.append(" LAC : " + info1.getLac()); // 取出当前邻区的LAC
                    sb.append(" CID : " + info1.getCid()); // 取出当前邻区的CID
                    sb.append(" BSSS : " + (-113 + 2 * info1.getRssi()) + "\n"); // 获取邻区基站信号强度
                }

                Log.i("--", " 获取邻区基站信息:" + sb.toString());


            }
        });
    }


    /**
     * 调用第三方公开的API根据基站信息查找基站的经纬度值及地址信息
     */
    public String httpPost(String url, String jsonCellPos) throws IOException {
        byte[] data = jsonCellPos.toString().getBytes();
        URL realUrl = new URL(url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) realUrl.openConnection();
        httpURLConnection.setConnectTimeout(6 * 1000);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setDoInput(true);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
        httpURLConnection.setRequestProperty("Accept-Charset", "GBK,utf-8;q=0.7,*;q=0.3");
        httpURLConnection.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
        httpURLConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8");
        httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
        httpURLConnection.setRequestProperty("Content-Length", String.valueOf(data.length));
        httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        httpURLConnection.setRequestProperty("Host", "www.minigps.net");
        httpURLConnection.setRequestProperty("Referer", "http://www.minigps.net/map.html");
        httpURLConnection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.4 (KHTML, like Gecko) Chrome/22.0.1229.94 Safari/537.4X-Requested-With:XMLHttpRequest");

        httpURLConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        httpURLConnection.setRequestProperty("Host", "www.minigps.net");

        DataOutputStream outStream = new DataOutputStream(httpURLConnection.getOutputStream());
        outStream.write(data);
        outStream.flush();
        outStream.close();

        if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = httpURLConnection.getInputStream();
            return new String(read(inputStream));
        }
        return null;
    }

    /**
     * 获取JSON形式的基站信息
     * @param mcc 移动国家代码（中国的为460）
     * @param mnc 移动网络号码（中国移动为0，中国联通为1，中国电信为2）；
     * @param lac 位置区域码
     * @param cid 基站编号
     * @return json
     * @throws JSONException
     */
    private String getJsonCellPos(int mcc, int mnc, int lac, int cid) throws JSONException {
        JSONObject jsonCellPos = new JSONObject();
        jsonCellPos.put("version", "1.1.0");
        jsonCellPos.put("host", "maps.google.com");

        JSONArray array = new JSONArray();
        JSONObject json1 = new JSONObject();
        json1.put("location_area_code", "" + lac + "");
        json1.put("mobile_country_code", "" + mcc + "");
        json1.put("mobile_network_code", "" + mnc + "");
        json1.put("age", 0);
        json1.put("cell_id", "" + cid + "");
        array.put(json1);

        jsonCellPos.put("cell_towers", array);
        return jsonCellPos.toString();
    }

    /**
     * 读取IO流并以byte[]形式存储
     * @param inputSream InputStream
     * @return byte[]
     * @throws IOException
     */
    public byte[] read(InputStream inputSream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        int len = -1;
        byte[] buffer = new byte[1024];
        while ((len = inputSream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        outStream.close();
        inputSream.close();

        return outStream.toByteArray();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocationUtils.getInstance( this ).removeLocationUpdatesListener();
    }
    //从网络获取经纬度
    public String getLngAndLatWithNetwork() {
        double latitude = 0.0;
        double longitude = 0.0;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.e("LanAndLat","null");
            return "";
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }catch (Exception e){

        }
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) {
            latitude = location.getLatitude();
            Log.e("latitude", String.valueOf(latitude));
            longitude = location.getLongitude();
        }else {
            Log.e("null","null");
        }
        return longitude + "," + latitude;
    }
    LocationListener locationListener = new LocationListener() {

        // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        // Provider被enable时触发此函数，比如GPS被打开
        @Override
        public void onProviderEnabled(String provider) {

        }

        // Provider被disable时触发此函数，比如GPS被关闭
        @Override
        public void onProviderDisabled(String provider) {

        }

        //当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
        @Override
        public void onLocationChanged(Location location) {
        }
    };
}
