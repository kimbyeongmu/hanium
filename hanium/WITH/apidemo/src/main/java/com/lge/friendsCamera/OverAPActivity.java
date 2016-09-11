/*
 * Copyright 2016 LG Electronics Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lge.friendsCamera;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.lge.octopus.ConnectionManager;
import com.lge.octopus.Listeners;
import com.lge.octopus.OctopusManager;
import com.lge.osclibrary.FriendsCameraSettings;
import com.lge.osclibrary.HTTP_SERVER_INFO;
import com.lge.osclibrary.HttpAsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

public class OverAPActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = OverAPActivity.class.getSimpleName();
    private Context mContext;

    private Button connectOverAP;
    private EditText ssid;
    private EditText passwd;
    private ProgressDialog mProgressDialog;

    /**
     * Over AP listener
     */
    Listeners.OverAP mConnectionCallback = new Listeners.OverAP() {

        /**
         * Return caller Id
         * @return Unique value for this listener
         */
        @Override
        public String asCallerId() {
            Log.e(TAG, ">>>>> asCallerId called");
            return OverAPActivity.class.getName();
        }

        /**
         * Connected by over ap
         * @param host Bundle which includes 'serverIp' for over ap
         */
        @Override
        public void onConnected(Bundle host) {
            String serverIp = host.getString("serverIp");
            Log.e(TAG, ">>>>> onConnected :: hostAddress : " + serverIp);
            HTTP_SERVER_INFO.IP = serverIp;

            //Turn on UI buttons
            FriendsCameraApplication.controlUIInCurrentActivity(true);
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.cancel();
            finish();
        }

        /**
         * Over ap disconnected
         */
        @Override
        public void onDisconnected() {
            Log.e(TAG, ">>>>> onDisConnected :: hostAddress : ");
            //Turn off UI buttons
            FriendsCameraApplication.controlUIInCurrentActivity(false);

            //Unregister over ap listener and try to connect with the camera by soft ap
            OctopusManager.get(mContext).unregisterListener(mConnectionCallback);
            HTTP_SERVER_INFO.IP = HTTP_SERVER_INFO.SOFTAP_IP;

            FriendsCameraSharedPreference sharedPreference = new FriendsCameraSharedPreference(mContext);
            String prevBtAddr = sharedPreference.getPreference(FriendsCameraSharedPreference.BT_ADDR);

            Log.e(TAG,"###### Enable Friends Wifi (BT ADDRESS :: " + prevBtAddr + ")");
            OctopusManager.getInstance(mContext).getConnectionManager().enableFriendWifiAP(prevBtAddr);
        }

        /**
         * Notify the connection process status
         * @param type state: the connection status changed with peer device /
         *             error: fail to connect to peer device
         * @param code state code or error code
         */
        @Override
        public void onResponse(Listeners.Type type, int code) {
            Log.e(TAG,">>>>> OnResponse called");
            if (type == Listeners.Type.state) {
                if (code == ConnectionManager.Result.NSD_DISCOVERY_STARTED) {
                    Log.e(TAG, "onResponse : Start NSD Discovery: " + ConnectionManager.Result.getString(code));
                } else if (code == ConnectionManager.Result.CONNECTED) {
                    Log.e(TAG, "onResponse : Connected to AP: " + ConnectionManager.Result.getString(code));
                }
            } else {
                Log.e(TAG, "onResponse : OverAP Failed: " + ConnectionManager.Result.getString(code));
                if (mProgressDialog != null && mProgressDialog.isShowing())
                    mProgressDialog.cancel();
                //Turn off the UI button
                FriendsCameraApplication.controlUIInCurrentActivity(false);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupViews();
        initialize();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "### onDestroy");

    }

    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupViews() {
        setContentView(R.layout.overap_layout);

        getSupportActionBar().setTitle(R.string.camera_overap);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        connectOverAP = (Button) findViewById(R.id.button_overap_connect);
        connectOverAP.setOnClickListener(this);

        ssid = (EditText) findViewById(R.id.overap_ssid);
        passwd = (EditText) findViewById(R.id.overap_passwd);
    }

    private void initialize() {
        mContext = this;
        FriendsCameraApplication.setContext(mContext);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_overap_connect:
                connectOverAP();
                break;
        }
    }

    //Start to connect to device by over ap
    private void connectOverAP() {
        String ssid_string = ssid.getText().toString();
        String ssid_passwd_string = passwd.getText().toString();
        OctopusManager.get(mContext).connect(ssid_string, ssid_passwd_string, ConnectionManager.OVER_AP);
        OctopusManager.get(mContext).registerListener(mConnectionCallback);
        mProgressDialog = ProgressDialog.show(mContext, "", "Processing...", true, false);
    }
}
