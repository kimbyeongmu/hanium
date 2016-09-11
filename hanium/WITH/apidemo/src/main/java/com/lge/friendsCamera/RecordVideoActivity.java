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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.lge.osclibrary.HttpAsyncTask;
import com.lge.osclibrary.OSCCommandsExecute;
import com.lge.osclibrary.OSCCommandsStatus;
import com.lge.osclibrary.OSCParameterNameMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Show 3 modes (video, _looping, _timelapse) to take video
 * and change capture mode for each mode
 */
public class RecordVideoActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = RecordVideoActivity.class.getSimpleName();

    private Context mContext;

    private Button buttonVideo;
    private Button buttonLooping;
    private Button buttonTimelapse;

    private ProgressDialog mProgressDialog;

    private enum CaptureMode {VIDEO, LOOPING, TIMELAPSE, OTHERS}
    private final ArrayList<String> _captureModeToString =
            new ArrayList<>(Arrays.asList("video","_looping","_timelapse","unknown"));
    private final String optionCaptureMode = "captureMode";
    private CaptureMode currentCaptureMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupViews();
        initialize();
    }

    private void initialize() {
        mContext = this;
        FriendsCameraApplication.setContext(mContext);

        //getOption
        getOptionCaptureMode();
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
        setContentView(R.layout.recordvideo_layout);

        getSupportActionBar().setTitle(R.string.recording);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        buttonVideo = (Button) findViewById(R.id.button_takeSimpleVideo);
        buttonVideo.setOnClickListener(this);

        buttonLooping = (Button) findViewById(R.id.button_takeLooping);
        buttonLooping.setOnClickListener(this);

        buttonTimelapse = (Button) findViewById(R.id.button_takeTimelapse);
        buttonTimelapse.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_takeSimpleVideo:
                checkCaptureMode(CaptureMode.VIDEO);
                break;
            case R.id.button_takeLooping:
                checkCaptureMode(CaptureMode.LOOPING);
                break;
            case R.id.button_takeTimelapse:
                checkCaptureMode(CaptureMode.TIMELAPSE);
                break;
        }
    }

    private void checkCaptureMode(final CaptureMode requestedMode){
        if(currentCaptureMode != requestedMode) {
            //Ask user whether change captureMode as requestedMode or not
            //  yes = Send request to captureMode as requestedMode(camera.setOption)
            //  no = nothing to do
            DialogInterface.OnClickListener okListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mProgressDialog = ProgressDialog.show
                                    (mContext, "", "Setting..", true, false);
                            setCaptureMode(requestedMode);
                        }
                    };

            Utils.showSelectDialog(
                    mContext, "Note: ","Do you want to change captureMode to "
                            + _captureModeToString.get(requestedMode.ordinal()), okListener, null);

        } else{
            callActivitiesBasedOnCaptureMode();
        }
    }

    private void callActivitiesBasedOnCaptureMode(){
        Intent i;
        switch(currentCaptureMode){
            case VIDEO:
                i = new Intent(mContext, SimpleRecordingActivity.class);
                startActivity(i);
                break;
            case LOOPING:
                i = new Intent(mContext, LoopingActivity.class);
                startActivity(i);
                break;
            case TIMELAPSE:
                i = new Intent(mContext, TimelapseActivity.class);
                startActivity(i);
                break;
            case OTHERS:
                Utils.showTextDialog(mContext, getString(R.string.error),
                        "Unknown capture mode");
                break;
            default:

        }
    }

    /**
     * get captureMode option
     * API: /osc/commands/execute (camera.getOptions)
     */
    private void getOptionCaptureMode() {
        JSONObject parameters = new JSONObject();

        try {
            JSONArray optionParameter = new JSONArray();
            optionParameter.put(optionCaptureMode);

            parameters.put(OSCParameterNameMapper.Options.OPTIONNAMES, optionParameter);
            OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.getOptions", parameters);

            commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                @Override
                public void onResponse(OSCReturnType type, Object response) {
                    try {

                        if (type == OSCReturnType.SUCCESS) {
                            //If the getOption request get response successfully,
                            //save the current captureMode
                            JSONObject jObject = new JSONObject((String) response);

                            JSONObject results = jObject.getJSONObject(OSCParameterNameMapper.RESULTS);
                            JSONObject options = results.getJSONObject(OSCParameterNameMapper.Options.OPTIONS);
                            String captureMode = options.getString(optionCaptureMode);

                            int modeIndex = _captureModeToString.indexOf(captureMode);
                            if(modeIndex != -1) {
                                currentCaptureMode = CaptureMode.values()[modeIndex];
                            } else {
                                currentCaptureMode = CaptureMode.OTHERS;
                            }

                            Log.d(TAG, " Current capture mode = " + currentCaptureMode);
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

    /**
     * change captureMode to requestedMode
     * API: /osc/commands/execute (camera.setOptions)
     * @param requestedMode captureMode to be set
     */
    private void setCaptureMode(final CaptureMode requestedMode) {
        JSONObject setParam = new JSONObject();
        JSONObject optionParam = new JSONObject();

        try {
            setParam.put("captureMode", _captureModeToString.get(requestedMode.ordinal()));
            optionParam.put("options", setParam);

            OSCCommandsExecute commandsExecute =
                    new OSCCommandsExecute("camera.setOptions", optionParam);
            commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                @Override
                public void onResponse(OSCReturnType type, Object response) {
                    if (mProgressDialog != null)
                        mProgressDialog.cancel();

                    if (type == OSCReturnType.SUCCESS) {
                        Toast.makeText(mContext,
                                "Set captureMode to '" +
                                        _captureModeToString.get(requestedMode.ordinal()) +
                                        "' successfully",
                                Toast.LENGTH_SHORT).show();
                        currentCaptureMode = requestedMode;
                        callActivitiesBasedOnCaptureMode();
                    } else {
                        Utils.showTextDialog(mContext, getString(R.string.response),
                                Utils.parseString(response));
                    }
                }
            });
            commandsExecute.execute();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "RecordVideo onResume");
        if (!FriendsCameraApplication.getIsConnected()) {
            ((RecordVideoActivity) mContext).finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "RecordVideo onDestroy");
    }
}
