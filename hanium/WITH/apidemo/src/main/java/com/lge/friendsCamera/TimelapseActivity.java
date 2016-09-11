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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
 * Record timelapse video based on timelapse speed option
 * Before start recording timelapse video, 'captureMode' should be set as '_timelapse'
 */
public class TimelapseActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = TimelapseActivity.class.getSimpleName();
    private Context mContext;

    private EditText editTextTimelapseSpeed;
    private Button buttonStartCapture;
    private Button buttonStopCapture;

    private Boolean isRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpViews();
        initialize();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, TAG + " onStop");

        if(isRecording){
            stopCaptureTimelapse();
        }
    }

    private void setUpViews() {
        setContentView(R.layout.timelapse_layout);

        editTextTimelapseSpeed = (EditText) findViewById(R.id.edittext_timelapseSpeed);

        buttonStartCapture = (Button) findViewById(R.id.button_startTimelapse);
        buttonStartCapture.setOnClickListener(this);

        buttonStopCapture = (Button) findViewById(R.id.button_stopTimelapse);
        buttonStopCapture.setOnClickListener(this);

        getSupportActionBar().setTitle(R.string.capture_timelapse);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initialize() {
        mContext = this;
        isRecording = false;
        FriendsCameraApplication.setContext(mContext);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_startTimelapse:
                isRecording = true;
                setOptionTimelapse();
                break;
            case R.id.button_stopTimelapse:
                isRecording = false;
                stopCaptureTimelapse();
                break;
        }

    }

    /**
     * Set options for timelapse
     * Get timelapse speed from edit text and make option value of '_timelapseSpeed'
     * API: /osc/commands/execute (camera.setOptions)
     */
    private void setOptionTimelapse() {
        JSONObject setParam = new JSONObject();
        JSONObject optionParam = new JSONObject();

        try {
            String timelapaseSpeed = editTextTimelapseSpeed.getText().toString();
            setParam.put("_timelapseSpeed", Integer.parseInt(timelapaseSpeed));

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
                        startCaptureTimelapse();
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
     * Start timelapse
     * API: osc/commands/execute (camera.startCapture)
     */
    private void startCaptureTimelapse() {
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.startCapture", null);
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
            }
        });
        commandsExecute.execute();
    }

    /**
     * Stop timelapse
     * API: osc/commands/execute (camera.startCapture)
     */
    private void stopCaptureTimelapse() {
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.stopCapture", null);
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
