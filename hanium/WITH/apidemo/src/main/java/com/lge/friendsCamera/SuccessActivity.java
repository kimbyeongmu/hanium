package com.lge.friendsCamera;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class SuccessActivity extends AppCompatActivity  {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);
    }
    public void home_back(View v) {
        Intent intent = new Intent(getApplicationContext(), login_main.class);
        startActivity(intent);
    }
    public void live(View v) {
        Intent intent = new Intent (Intent.ACTION_VIEW, Uri.parse("http://192.168.10.67:1935/livetest/camera.stream/playlist.m3u8"));
        startActivity(intent);
    }

    public void gotoRssi(View v) {
        Intent intent = new Intent(getApplicationContext(), ble.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP );
        intent.addFlags(Intent. FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    public void down(View v) {
        Intent intent = new Intent(getApplicationContext(), download.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP );
        intent.addFlags(Intent. FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
    public void upload(View v) {
        Intent intent = new Intent(getApplicationContext(), upload_.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP );
        intent.addFlags(Intent. FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
    public void one(View v) {//360 카메라
        Intent intent = new Intent(getApplicationContext(),com.lge.friendsCamera.MainActivity.class);
        startActivity(intent);
    }

}