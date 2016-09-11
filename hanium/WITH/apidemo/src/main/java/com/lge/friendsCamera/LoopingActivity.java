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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.lge.osclibrary.HttpAsyncTask;
import com.lge.osclibrary.OSCCommandsExecute;
import com.lge.osclibrary.OSCCommandsStatus;
import com.lge.osclibrary.OSCParameterNameMapper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Record looping video based on looping video option
 * Before start recording looping video, 'captureMode' should be set as '_looping'
 */
public class LoopingActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = LoopingActivity.class.getName();
    private Context mContext;

    private EditText editTextLoopingVideo;
    private Button buttonStartCapture;
    private Button buttonStopCapture;

    private Boolean isRecording;

    private Handler mHandler;
    private Runnable mRunnable;
    private long delay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpViews();
        initialize();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, TAG + " onStop");
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        if(isRecording){
            stopCaptureLooping();
        }
    }

    private void setUpViews() {
        setContentView(R.layout.looping_layout);

        editTextLoopingVideo = (EditText) findViewById(R.id.edittext_loopingVideo);

        buttonStartCapture = (Button) findViewById(R.id.button_startLooping);
        buttonStartCapture.setOnClickListener(this);

        buttonStopCapture = (Button) findViewById(R.id.button_stopLooping);
        buttonStopCapture.setOnClickListener(this);

        getSupportActionBar().setTitle(R.string.capture_looping);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initialize() {
        mContext = this;
        isRecording = false;
        FriendsCameraApplication.setContext(mContext);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_startLooping:
                isRecording = true;
                setOptionLooping();
                break;
            case R.id.button_stopLooping:
                isRecording = false;
                stopCaptureLooping();
                break;
        }
    }

    /**
     * Set options for looping
     * Get looping minute value from edit text and make option value of '_loopingVideo'
     * API: /osc/commands/execute (camera.setOptions)
     */
    private void setOptionLooping() {
        JSONObject setParam = new JSONObject();
        JSONObject optionParam = new JSONObject();

        try {
            String loopingMinutes = editTextLoopingVideo.getText().toString();
            String loopingParam = "looping_video_";
            if (loopingMinutes.equalsIgnoreCase("MAX")){
                loopingParam += "max";
            } else if(loopingMinutes.equals("120")) {
                loopingParam += "120";
            } else if(loopingMinutes.equals("60")) {
                loopingParam += "60";
            } else if(loopingMinutes.equals("20")){
                loopingParam += "20";
            } else if(loopingMinutes.equals("5")) {
                loopingParam += "5";
                delay = 5*60*1000;
            } else if(loopingMinutes.equalsIgnoreCase("off")){
                loopingParam += "off";
            }

            setParam.put("_loopingVideo", loopingParam);
            optionParam.put(OSCParameterNameMapper.Options.OPTIONS, setParam);

            OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.setOptions", optionParam);
            commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                @Override
                public void onResponse(OSCReturnType type, Object response) {
                    //check option setting success
                    //success -> start capture
                    //fail -> show error message
                    if (type == OSCReturnType.SUCCESS) {
                        Toast.makeText(mContext, response.toString(), Toast.LENGTH_SHORT).show();
                        startCaptureLooping();
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

    /**
     * Start video looping
     * API: osc/commands/execute (camera.startCapture)
     */
    private void startCaptureLooping() {
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.startCapture", null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, final Object response) {
                mRunnable = new Runnable() {
                    @Override
                    public void run() {
                        String commandId = Utils.getCommandId(response);
                        checkCommandsStatus(commandId);
                    }
                };
                mHandler = new Handler();
                mHandler.postDelayed(mRunnable, delay);

                if (type == OSCReturnType.SUCCESS) {
                    String state = Utils.getCommandState(response);
                    if (state != null && state.equals(OSCParameterNameMapper.STATE_INPROGRESS)) {
                        String commandId = Utils.getCommandId(response);
                        checkCommandsStatus(commandId);
                        return;
                    }


                } else {
                    Utils.showTextDialog(mContext, getString(R.string.response),
                            Utils.parseString(response));
                }
            }
        });
        commandsExecute.execute();
    }

    /**
     * Stop video looping
     * API: osc/commands/execute (camera.startCapture)
     */
    private void stopCaptureLooping(){
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.stopCapture",null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, Object response) {
                String state = Utils.getCommandState(response);
                if(state != null && state.equals(OSCParameterNameMapper.STATE_INPROGRESS)){
                    String commandId = Utils.getCommandId(response);
                    checkCommandsStatus(commandId);
                } else{
                    Utils.showTextDialog(mContext, "Response  ", Utils.parseString(response));
                }
                if (mHandler != null)
                    mHandler.removeCallbacks(mRunnable);
            }
        });
        commandsExecute.execute();
    }

    /**
     * Check the status for previous inProgress commands.
     * Determine whether the command has completed.
     * @param commandId command Id of previous command
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

            }
        });
        commandsStatus.execute();
    }


}
