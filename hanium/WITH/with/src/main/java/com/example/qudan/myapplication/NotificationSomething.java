package com.example.qudan.myapplication;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;


public class NotificationSomething extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_);
        CharSequence s = "도망간 아이는 ";
        int id=0;

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
        s = "error";
        }
        else {
        id = extras.getInt("notificationId");
        }
        TextView t = (TextView) findViewById(R.id.MU);
        s = s+" "+id;
        t.setText(s);
        NotificationManager nm =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //노티피케이션 제거
        nm.cancel(id);
        }
}
