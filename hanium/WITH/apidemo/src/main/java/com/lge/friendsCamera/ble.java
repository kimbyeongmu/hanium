package com.lge.friendsCamera;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ble extends Activity implements BeaconConsumer {
    private int id_[] = new int[100];
    private int num=0;
    private NotificationManager nm = null;
    private SoundPool sound_pool;
    private int sound_beep;
    private BeaconManager beaconManager;
    private List<Beacon> beaconList = new ArrayList<>();
    TextView textView;
    TextView state;
    private Notification mNoti;
    Vibrator vibrator;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        id_[0]=533;
        // 실제로 비콘을 탐지하기 위한 비콘매니저 객체를 초기화
        beaconManager = BeaconManager.getInstanceForApplication(this);
        textView = (TextView)findViewById(R.id.Textview);
        state = (TextView)findViewById(R.id.state);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        // 여기가 중요한데, 기기에 따라서 setBeaconLayout 안의 내용을 바꿔줘야 하는듯 싶다.
        // 필자의 경우에는 아래처럼 하니 잘 동작했음.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        // 비콘 탐지를 시작한다. 실제로는 서비스를 시작하는것.
        beaconManager.bind(this);
        state.setText("실행중");

       // handler.sendEmptyMessage(0);
        handler.sendEmptyMessage(0);
    }
    public void initSound(){
        sound_pool = new SoundPool(5, AudioManager.STREAM_MUSIC,0);
        sound_beep = sound_pool.load(getBaseContext(),R.raw.beep,1);
    }
    public void playSound(){
        sound_pool.play(sound_beep,1f,1f,0,0,1f);
    }

    public void success_back(View v) {
        Intent intent = new Intent(getApplicationContext(),SuccessActivity.class);
        startActivity(intent);
    }
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            // 비콘이 감지되면 해당 함수가 호출된다. Collection<Beacon> beacons에는 감지된 비콘의 리스트가,
            // region에는 비콘들에 대응하는 Region 객체가 들어온다.
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    beaconList.clear();
                    for (Beacon beacon : beacons) {
                        beaconList.add(beacon);
                    }
                }
            }

        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }

    // start
    public void OnButtonClicked(View view){
        // 아래에 있는 handleMessage를 부르는 함수. 맨 처음에는 0초간격이지만 한번 호출되고 나면
        // 1초마다 불러온다.
        state.setText("실행중");
        beaconManager.bind(this);
        handler.sendEmptyMessage(0);
    }
    ////중지하기
    public void can(View view){
        state.setText("중지됨");
        beaconList.clear();
        handler.removeMessages(0);
        beaconManager.removeAllMonitorNotifiers();
        nm.cancel(id_[0]);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            // 비콘의 아이디와 거리를 측정하여 textView에 넣는다.
            textView.setText("");
                for (Beacon beacon : beaconList) {
                    textView.append( beacon.getBluetoothName() +"어린이가"+Double.parseDouble(String.format("%.3f", beacon.getDistance())) + "만큼 떨어져 있습니다" + "m\n");
                    if (beacon.getDistance() > 1) {
                        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        mNoti = new NotificationCompat.Builder(getApplicationContext())
                                .setContentTitle(beacon.getBluetoothName() + "가 유치원을 벗어낫습니다!")
                                .setContentText("확인하세요")
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setTicker("벗어남!!")
                                .setAutoCancel(true)
                                .build();
                        nm.notify(beacon.getBeaconTypeCode(), mNoti);
                        vibrator.vibrate(1000);
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }
           //     progressBar.setVisibility(View.INVISIBLE);
                // 자기 자신을 1초마다 호출
                handler.sendEmptyMessageDelayed(0, 10000);
            }
    };
}