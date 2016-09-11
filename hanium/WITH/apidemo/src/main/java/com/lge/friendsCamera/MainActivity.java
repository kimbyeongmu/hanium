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

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.lge.octopus.ConnectionManager;
import com.lge.octopus.Listeners;
import com.lge.octopus.OctopusManager;
import com.lge.osclibrary.HTTP_SERVER_INFO;
import com.lge.osclibrary.HttpAsyncTask;
import com.lge.osclibrary.OSCCheckForUpdates;
import com.lge.osclibrary.OSCInfo;
import com.lge.osclibrary.OSCParameterNameMapper;
import com.lge.osclibrary.OSCState;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main Activity
 * Info, State, CheckForUpdates APIs are executed in this activity
 * Other APIs are executed in other activity
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = MainActivity.class.getSimpleName();

    public Context mContext = this;

    private Button buttonSoftAP;
    private Button buttonOverAP;
    private Button buttonCameraImage;
    private Button buttonCameraVideo;
    private Button buttonDownloadImage;
    private Button buttonDownloadVideo;
    private Button buttonInfo;
    private Button buttonState;
    private Button buttonCheckForUpdate;
    private Button buttonOptions;
    private Button buttonTakePicture;
    private Button buttonRecordVideo;
    private Button buttonSettings;
    private Button buttonPreview;


    private TextView connectStatus;
    private TextView URL;

    private String fingerPrint;

    private ProgressDialog mProgressDialog;

    /**
     * Soft AP listener
     */
    Listeners.SoftAP mSoftAPConnectionListener = new Listeners.SoftAP() {

        /**
         * Connected by soft ap
         */
        @Override
        public void onConnected() {
            Log.e(TAG, ">>>>>>>>>>>>>>>>>>> connected ------ soft ap");
            FriendsCameraApplication.controlUIInCurrentActivity(true);
            HTTP_SERVER_INFO.IP = HTTP_SERVER_INFO.SOFTAP_IP;
        }

        /**
         * Soft ap disconnected
         */
        @Override
        public void onDisconnected() {
            Log.e(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>> disconnected --- soft ap");
            FriendsCameraApplication.controlUIInCurrentActivity(false);
        }

        /**
         * Notify the connection state change
         * @param type state: the connection status changed with peer device /
         *             error: fail to connect to peer device
         * @param code state code or error code
         */
        @Override
        public void onResponse(Listeners.Type type, int code) {
            Log.e(TAG, ">>>>>>>>>>>>>>>>>>> onResponse ------ soft ap");
            Log.e(TAG, type + "           " + code);
            if (type == Listeners.Type.state) {
                if (code == ConnectionManager.Result.WIFI_SCANNED) {
                    Log.e(TAG, " WIFI_SCANNED() ------ soft ap");

                } else if (code == ConnectionManager.Result.FRIEND_AP_ENABLED) {
                    Log.e(TAG, " FRIEND_AP_ENABLED() ------ soft ap");
                    if (!(FriendsCameraApplication.getContext().getClass().getSimpleName()
                            .equals("ConnectionActivity"))) {

                        FriendsCameraSharedPreference sharedPreference = new FriendsCameraSharedPreference(mContext);
                        String prevSsid = sharedPreference.getPreference(FriendsCameraSharedPreference.SSID);
                        String prevPw = sharedPreference.getPreference(FriendsCameraSharedPreference.PASSWORD);
                        Log.i(TAG, " Start to connect ------ soft ap");
                        Log.i(TAG, " ssid = " + prevSsid + " : pw = " + prevPw);

                        OctopusManager.getInstance(mContext).getConnectionManager().connect(prevSsid, prevPw, ConnectionManager.SOFT_AP);
                    }
                }
                return;
            }

        }

        /**
         * Return caller Id
         * @return Unique value for this listener
         */
        @Override
        public String asCallerId() {
            Log.e(TAG, ">>>>>>>>>>>>>>>>>>> asCallerId ------ soft ap");
            return MainActivity.class.getName();
        }


    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Main onCreate");
        super.onCreate(savedInstanceState);
        setupViews();
        initialize();

    }

    private void setupViews() {
        setContentView(R.layout.main_layout);

        connectStatus = (TextView) findViewById(R.id.text_state);
        URL = (TextView) findViewById(R.id.textIPAddr);

        //1. Soft AP
        buttonSoftAP = (Button) findViewById(R.id.button_softap);
        buttonSoftAP.setOnClickListener(this);
/*
        //2. Over AP
        buttonOverAP = (Button) findViewById(R.id.button_overap);
        buttonOverAP.setOnClickListener(this);
*/
        //3. Get image list
        buttonCameraImage = (Button) findViewById(R.id.button_cameraimage);
        buttonCameraImage.setOnClickListener(this);

        //4. Get video list
        buttonCameraVideo = (Button) findViewById(R.id.button_cameravideo);
        buttonCameraVideo.setOnClickListener(this);

        //5. Image list for downloaded images(images in DCIM/LGC1Sample)
        //   Connect with viewer
        buttonDownloadImage = (Button) findViewById(R.id.button_downloadimage);
        buttonDownloadImage.setOnClickListener(this);

        //6. Video list for downloaded videos(videos in DCIM/LGC1Sample)
        //   Connect with viewer
        buttonDownloadVideo = (Button) findViewById(R.id.button_downloadvideo);
        buttonDownloadVideo.setOnClickListener(this);
/*
        //7. Get camera info (info)
        buttonInfo = (Button) findViewById(R.id.button_info);
        buttonInfo.setOnClickListener(this);

        //8. Get camera state (state)
        buttonState = (Button) findViewById(R.id.button_state);
        buttonState.setOnClickListener(this);

        //9. Check for update
        buttonCheckForUpdate = (Button) findViewById(R.id.button_checkforupdate);
        buttonCheckForUpdate.setOnClickListener(this);

        //10. Set camera options (camera.setOption, camera.getOption)
        buttonOptions = (Button) findViewById(R.id.button_option);
        buttonOptions.setOnClickListener(this);
*/
        //11. Take picture
        buttonTakePicture = (Button) findViewById(R.id.button_takePicture);
        buttonTakePicture.setOnClickListener(this);
/*
        //12. Record video
        buttonRecordVideo = (Button) findViewById(R.id.button_recordVideo);
        buttonRecordVideo.setOnClickListener(this);

        //13. Camera Settings
        buttonSettings = (Button) findViewById(R.id.button_settings);
        buttonSettings.setOnClickListener(this);

        //14. Preview
        buttonPreview = (Button) findViewById(R.id.button_preview);
        buttonPreview.setOnClickListener(this);
*/
    }

    public void back(View v) {
        Intent intent = new Intent(getApplicationContext(),SuccessActivity.class);
        startActivity(intent);
    }

    private void initialize() {
        FriendsCameraApplication.setContext(this);
        FriendsCameraApplication.settIsConnected(checkIsConnectedToDevice());

        boolean isConnected = FriendsCameraApplication.getIsConnected();
        updateStateBasedOnWifiConnection(isConnected);

        checkFileWritePermission();
        fingerPrint = "";

        OctopusManager.get(this).registerListener(mSoftAPConnectionListener);
    }

    @Override
    public void onClick(View view) {
        Intent i;
        switch (view.getId()) {
            case R.id.button_softap:
                //connect with camera
                i = new Intent(mContext, ConnectionActivity.class);
                startActivity(i);
                break;
            /*
            case R.id.button_overap:
                i = new Intent(mContext, OverAPActivity.class);
                startActivity(i);
                break;
                */
            case R.id.button_cameraimage:
                //   show an image info, get full image, delete image on camera for selected image
                i = new Intent(mContext, CameraFileListViewActivity.class);
                i.putExtra("type", "image");
                startActivity(i);
                break;
            case R.id.button_cameravideo:
                //   show an video info, get full video, delete video on camera for selected video
                i = new Intent(mContext, CameraFileListViewActivity.class);
                i.putExtra("type", "video");
                startActivity(i);
                break;
            case R.id.button_downloadimage:
                //   show an image info, get full image, delete image on phone for selected image
                i = new Intent(mContext, DownloadFileListViewActivity.class);
                i.putExtra("type", "image");
                startActivity(i);
                break;
            case R.id.button_downloadvideo:
                //   show an video info, get full video, delete video on phone for selected video
                i = new Intent(mContext, DownloadFileListViewActivity.class);
                i.putExtra("type", "video");
                startActivity(i);
                break;
            /*
            case R.id.button_info:
                getCameraInfo();
                break;
            case R.id.button_state:
                getCameraState();
                break;
            case R.id.button_checkforupdate:
                if (fingerPrint.equals("")) {
                    Utils.showAlertDialog(mContext, null,
                            "/osc/state has not been executed. Please click 'Camera State' button", null);
                } else {
                    getCameraCheckForUpdate();
                }
                break;
            case R.id.button_option:
                i = new Intent(mContext, OptionsActivity.class);
                startActivity(i);
                break;
            */
            case R.id.button_takePicture:
                i = new Intent(mContext, TakePictureActivity.class);
                startActivity(i);
                break;
            /*
            case R.id.button_recordVideo:
                i = new Intent(mContext, RecordVideoActivity.class);
                startActivity(i);
                break;
            case R.id.button_settings:
                i = new Intent(mContext, SettingsActivity.class);
                startActivity(i);
                break;
            case R.id.button_preview:
                i = new Intent(mContext, PreviewActivity.class);
                startActivity(i);
                break;
            */
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "Main onResume");
        FriendsCameraApplication.setContext(this);

        updateStateBasedOnWifiConnection(FriendsCameraApplication.getIsConnected());
        URL.setText(HTTP_SERVER_INFO.IP + ":" + HTTP_SERVER_INFO.PORT);
    }

    public boolean checkIsConnectedToDevice() {
        ConnectivityManager connmanager;

        connmanager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        NetworkInfo activeNetwork = connmanager.getActiveNetworkInfo();
        if (!(activeNetwork != null && wifiManager.isWifiEnabled())) {
            return false;
        }

        WifiInfo info = wifiManager.getConnectionInfo();

        String ssid = info.getSSID();
        Log.d(TAG, " ssid = " + ssid + "isWifiConnect ---      " + info.getSupplicantState());
        Log.d(TAG, " info = " + info);

        if (ssid.contains(".OSC") && info.getSupplicantState().toString().equals("COMPLETED")) {
            return true;
        } else {
            return false;
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "Main onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "Main onPause");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Main onDestroy");
        OctopusManager.get(this).unregisterListener(mSoftAPConnectionListener);
    }


    /**
     * Get the basic information of the LG 360 CAM device
     * API : /osc/info
     */
    private void getCameraInfo() {
        final OSCInfo oscInfo = new OSCInfo();
        oscInfo.setListener(new OSCInfo.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, Object response) {
                String title = getString(R.string.camera_info);
                Utils.showTextDialog(mContext, title, Utils.parseString(response));
            }
        });

        oscInfo.execute();
    }

    /**
     * Get the device information that change over time such as battery level, battery state, etc.
     * API : /osc/state
     */
    private void getCameraState() {
        final OSCState oscState = new OSCState();
        oscState.setListener(new OSCInfo.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, Object response) {
                String title = getString(R.string.camera_state);
                if (type == OSCReturnType.SUCCESS) {
                    try {
                        JSONObject jObject = new JSONObject((String) response);
                        fingerPrint = jObject.getString(OSCParameterNameMapper.FINGERPRINT);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Utils.showTextDialog(mContext, title, Utils.parseString(response));
            }
        });
        oscState.execute();
    }


    /**
     * Update the fingerprint to reflect the current camera state by comparing it with the fingerprint held by the client.
     * API : /osc/checkForUpdate
     */
    private void getCameraCheckForUpdate() {
        final String title = getString(R.string.check_update);
        mProgressDialog = ProgressDialog.show(mContext, null, "Checking...", true, false);
        final OSCCheckForUpdates oscCheckForUpdates = new OSCCheckForUpdates(fingerPrint, 1);
        oscCheckForUpdates.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, Object response) {
                if (type == OSCReturnType.SUCCESS) {
                    JSONObject jObject = null;
                    try {
                        mProgressDialog.cancel();
                        jObject = new JSONObject(response.toString());

                        String responseFingerprint = jObject.getString(OSCParameterNameMapper.LOCAL_FINGERPRINT);
                        if (fingerPrint.equals(responseFingerprint)) {
                            Utils.showAlertDialog(mContext, title, "State is same\n\n" + Utils.parseString(response), null);
                        } else {
                            Utils.showAlertDialog(mContext, title,
                                    "State is updated, Please check state by /osc/state\n\n" + Utils.parseString(response), null);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Utils.showTextDialog(mContext, title, Utils.parseString(response));
                }
            }
        });
        oscCheckForUpdates.execute();
    }

    /**
     * Update Main UI.
     * Buttons are enable when camera is connected
     *
     * @param state
     */
    public void updateStateBasedOnWifiConnection(boolean state) {

        buttonCameraImage.setEnabled(state);
        buttonCameraVideo.setEnabled(state);
//        buttonInfo.setEnabled(state);
//        buttonState.setEnabled(state);
//        buttonCheckForUpdate.setEnabled(state);
//        buttonOptions.setEnabled(state);
        buttonTakePicture.setEnabled(state);
//        buttonRecordVideo.setEnabled(state);
//        buttonSettings.setEnabled(state);
//        buttonPreview.setEnabled(state);
//        buttonOverAP.setEnabled(state);

        if (state) {
            connectStatus.setText(R.string.wifi_status_connect);
        } else {
            connectStatus.setText(R.string.wifi_status_disconnect);
        }
    }

    private final int MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    /**
     * Ask "write storage" permission to user
     */
    private void checkFileWritePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission for write external storage is granted");
                } else {
                    Log.d(TAG, "Permission for write external storage is denied");
                }
        }
    }
}