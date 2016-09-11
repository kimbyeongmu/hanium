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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import com.lge.osclibrary.HttpAsyncTask;
import com.lge.osclibrary.OSCCommandsExecute;
import com.lge.osclibrary.OSCCommandsStatus;
import com.lge.osclibrary.OSCParameterNameMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Show 3 modes (image, interval, _burstshot) to take picture
 * and change capture mode for interval shot and burst shot
 */
public class TakePictureActivity extends AppCompatActivity implements View.OnClickListener {
    ///////////////////
    private TimerTask mTask;
    private Timer mTimer;

    private final static String TAG = TakePictureActivity.class.getSimpleName();
    private Context mContext;

    private ProgressDialog mProgressDialog;

    private Button button_manualMetaData;
    private Button button_takePicture;
    private Button button_takeInterval;
    private Button button_takeBurstShot;
    private Switch autoPicture;


    private enum CaptureMode {IMAGE, INTERVAL, BURSTSHOT, OTHERS}

    final String optionCaptureMode = "captureMode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupViews();
        initialize();
       // adapter = new CustomListAdapter(this, itemInfo, itemBitmap);
      //  adapter.setType(CustomListAdapter.selectedGalleryType.CAMERA_IMAGE);

    }



    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

  //  protected void stop_picture() {
   //
   // }

    private void setupViews() {
        setContentView(R.layout.takepicture_layout);

        getSupportActionBar().setTitle(R.string.take_picture);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        /*
        button_manualMetaData = (Button) findViewById(R.id.button_manualMetaData);
        button_manualMetaData.setOnClickListener(this);
*/
        button_takePicture = (Button) findViewById(R.id.button_takePicture);
        button_takePicture.setOnClickListener(this);
/*
        button_takeInterval = (Button) findViewById(R.id.button_takeInterval);
        button_takeInterval.setOnClickListener(this);

        button_takeBurstShot = (Button) findViewById(R.id.button_takeBurstshot);
        button_takeBurstShot.setOnClickListener(this);
*/
        mTask = new TimerTask() {
            @Override
            public void run() {

                OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.takePicture", null);
                commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                    @Override
                    public void onResponse(OSCReturnType type, final Object response) {
                        if (type == OSCReturnType.SUCCESS) {
                            String state = Utils.getCommandState(response);
                            if (state != null && state.equals(OSCParameterNameMapper.STATE_INPROGRESS)) {
                                return;
                            }
                        }
                    }
                });
                commandsExecute.execute();
            }
        };


        mTimer = new Timer();

        autoPicture = (Switch) findViewById(R.id.autoPic);
        autoPicture.setChecked(false);
        autoPicture.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton cb,
                                         boolean isChecking) {
                if(isChecking){
                    mTimer.schedule(mTask,10000,20000); // 시간 설정
                    //  switchStatus.setText("Switch is currently ON");
                }
                else{
                    mTimer.cancel();
                    //         switchStatus.setText("Switch is currently OFF");
                }

            }

        });

    }

    private void initialize() {
        mContext = this;
        FriendsCameraApplication.setContext(mContext);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            /*
            case R.id.button_manualMetaData:
                getManualMetaData();
                break;
                */
            case R.id.button_takePicture:
                takePicture();
                break;
            /*
            case R.id.button_takeInterval:
                getOptionCaptureMode(CaptureMode.INTERVAL);
                break;
            case R.id.button_takeBurstshot:
                getOptionCaptureMode(CaptureMode.BURSTSHOT);
                break;
                */
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "TakePicture onResume");
        if (!FriendsCameraApplication.getIsConnected()) {
            ((TakePictureActivity) mContext).finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "TakePicture onDestroy");
    }

    /**
     *  Get manual meta data values(white balance, exposure value, iso , shutter speed)
     *  API: /osc/commands/execute (camera._manualMetaData)
     */
    private void getManualMetaData() {
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera._manualMetaData", null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, Object response) {
                Utils.showTextDialog(mContext, getString(R.string.response),
                        Utils.parseString(response));
            }
        });
        commandsExecute.execute();
    }

    /**
     * Captures an image, saving lat/long coordinates to EXIF
     * API : /osc/commands/execute (camera.takePicture)
     */
    private void takePicture() {
        mProgressDialog = ProgressDialog.show(mContext, "", "Processing...", true, false);
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.takePicture", null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, final Object response) {
                if (type == OSCReturnType.SUCCESS) {
                    String state = Utils.getCommandState(response);
                    if (state != null && state.equals(OSCParameterNameMapper.STATE_INPROGRESS)) {
                        String commandId = Utils.getCommandId(response);
                        checkCommandsStatus(commandId);
                        return;
                    }
                }
                if (mProgressDialog.isShowing())
                    mProgressDialog.cancel();
                Utils.showTextDialog(mContext, getString(R.string.response), Utils.parseString(response));

            }
        });
        commandsExecute.execute();
    }

    private void takeInterval(){
        Intent i = new Intent(mContext, CaptureIntervalActivity.class);
        startActivity(i);
    }

    private void takeBurstShot(){
        Intent i = new Intent(mContext, BurstShotActivity.class);
        startActivity(i);
    }


    /**
     * get captureMode option and compare with parameter 'requestedMode'
     * API: /osc/commands/execute (camera.getOptions)
     * @param requestedMode change captureMode as this parameter value
     */
    private void getOptionCaptureMode(final CaptureMode requestedMode) {
        JSONObject parameters = new JSONObject();


        try {
            JSONArray optionParameter = new JSONArray();
            optionParameter.put(optionCaptureMode);

            parameters.put(OSCParameterNameMapper.Options.OPTIONNAMES, optionParameter);
            OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.getOptions", parameters);

            commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                @Override
                public void onResponse(OSCReturnType type, Object response) {
                    CaptureMode currentMode = CaptureMode.OTHERS;
                    try {

                        if (type == OSCReturnType.SUCCESS) {
                            //If the getOption request get response successfully,
                            //save the capture mode
                            JSONObject jObject = new JSONObject((String) response);

                            JSONObject results = jObject.getJSONObject(OSCParameterNameMapper.RESULTS);
                            JSONObject options = results.getJSONObject(OSCParameterNameMapper.Options.OPTIONS);
                            String captureMode = options.getString(optionCaptureMode);

                            if (captureMode.equals("image")) {
                                currentMode = CaptureMode.IMAGE;
                            } else if (captureMode.equals("interval")){
                                currentMode = CaptureMode.INTERVAL;
                            } else if (captureMode.equals("_burstshot")){
                                currentMode = CaptureMode.BURSTSHOT;
                            }
                            askToChangeCaptureMode(currentMode, requestedMode);
                        } else {
                            Utils.showTextDialog(mContext, getString(R.string.response),
                                    Utils.parseString(response));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            commandsExecute.execute();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void askToChangeCaptureMode(CaptureMode currentMode, final CaptureMode requestedMode){
        if(currentMode != requestedMode){
            DialogInterface.OnClickListener okListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mProgressDialog = ProgressDialog.show
                                    (mContext, "", "Setting..", true, false);
                            setCaptureMode(requestedMode);
                        }
                    };
            DialogInterface.OnClickListener cancelListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    };

            String dialog = "Do you want to change captureMode to";
            if(requestedMode == CaptureMode.BURSTSHOT){
                dialog += " 'burstshot'?";
            } else if(requestedMode == CaptureMode.INTERVAL){
                dialog += " 'interval'?";
            } else if(requestedMode == CaptureMode.IMAGE){
                dialog += " 'image'?";
            }
            Utils.showSelectDialog(
                    mContext, "Note: ",
                    dialog,
                    okListener, cancelListener);
        } else{
            if(requestedMode == CaptureMode.BURSTSHOT){
                takeBurstShot();
            } else if (requestedMode == CaptureMode.INTERVAL){
                takeInterval();
            } else if (requestedMode == CaptureMode.IMAGE){
                takePicture();
            }
        }
    }

    /**
     * change captureMode as parameter
     * API: /osc/commands/execute (camera.setOptions)
     * @param mode captureMode value to be set
     */
    private void setCaptureMode(CaptureMode mode) {
        JSONObject setParam = new JSONObject();
        JSONObject optionParam = new JSONObject();

        final CaptureMode curMode = mode;

        try {
            if(curMode == CaptureMode.BURSTSHOT){
                setParam.put("captureMode", "_burstshot");
            } else if(curMode == CaptureMode.INTERVAL){
                setParam.put("captureMode", "interval");
            } else if(curMode == CaptureMode.IMAGE){
                setParam.put("captureMode", "image");
            }

            optionParam.put("options", setParam);

            OSCCommandsExecute commandsExecute =
                    new OSCCommandsExecute("camera.setOptions", optionParam);
            commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                @Override
                public void onResponse(OSCReturnType type, Object response) {
                    if (mProgressDialog != null)
                        mProgressDialog.cancel();

                    if (type == OSCReturnType.SUCCESS) {
                        Toast.makeText(mContext, "Set captureMode to successfully",
                                Toast.LENGTH_SHORT).show();
                        if(curMode == CaptureMode.BURSTSHOT){
                            takeBurstShot();
                        } else if(curMode == CaptureMode.INTERVAL){
                            takeInterval();
                        } else if(curMode == CaptureMode.IMAGE){
                            takePicture();
                        }
                    } else {
                        Utils.showTextDialog(mContext, getString(R.string.response), Utils.parseString(response));
                    }
                }
            });
            commandsExecute.execute();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check the status for previous inProgress commands.
     * Determine whether camera.takePicture has completed.
     * @param commandId command Id of previous camera.takePicture
     * API : /osc/commands/status
     */
    private void checkCommandsStatus(final String commandId) {
        OSCCommandsStatus commandsStatus = new OSCCommandsStatus(commandId);
        commandsStatus.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, final Object response) {
                if (type == OSCReturnType.SUCCESS) {
                    String state = Utils.getCommandState((String) response);
                    if (state != null && state.equals(OSCParameterNameMapper.STATE_INPROGRESS)) {
                        checkCommandsStatus(commandId);
                        return;
                    }
                }
                Utils.showTextDialog(mContext, getString(R.string.response), Utils.parseString(response));
                if (mProgressDialog.isShowing())
                    mProgressDialog.cancel();

            }
        });
        commandsStatus.execute();
    }




}
