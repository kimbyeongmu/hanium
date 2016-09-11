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
 * Capture equirectangular images at regular intervals using options; captureNumber and captureInterval
 * Before start capture, 'captureMode' should be set as 'interval'
 */
public class CaptureIntervalActivity extends AppCompatActivity {
    private final static String TAG = CaptureIntervalActivity.class.getSimpleName();
    private Context mContext;

    EditText editTextNumber;
    EditText editTextInterval;
    Button buttonStartCapture;
    Button buttonStopCapture;

    String captureNumber; // value of option captureNumber
    String captureInterval; //value of option captureInterval
    long delay; //time to capture interval = captureNumber * captureInterval

    boolean successSetOptions;

    private Handler mHandler;
    private Runnable mRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setUpViews();
        initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, TAG + " onResume");
        if (!FriendsCameraApplication.getIsConnected()) {
            ((CaptureIntervalActivity) mContext).finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, TAG + " onStop");
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    private void setUpViews() {
        setContentView(R.layout.captureinterval_layout);
        editTextNumber = (EditText) findViewById(R.id.edittext_number);
        editTextInterval = (EditText) findViewById(R.id.edittext_interval);
        buttonStartCapture = (Button) findViewById(R.id.button_startcapture);
        buttonStopCapture = (Button) findViewById(R.id.button_stopcapture);

        getSupportActionBar().setTitle(R.string.capture_interval);
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
        FriendsCameraApplication.setContext(mContext);

        successSetOptions = false;
        buttonStartCapture.setOnClickListener(clickListener);
        buttonStopCapture.setOnClickListener(clickListener);
    }

    /**
     * Click listener for buttons
     */
    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_startcapture:
                    //Set captureNumber, captureInterval options and start capture
                    setAndStartCaptureInterval();
                    break;
                case R.id.button_stopcapture:
                    stopCapture();
                    break;
            }
        }
    };

    /**
     * Set captureNumber and captureInterval options first, and then start capture
     */
    private void setAndStartCaptureInterval() {
        Log.v(TAG, "clicked");
        if (!successSetOptions) {
            getInputValue();
            setOptionsForCaptureInterval();
        } else {
            startCapture();
            successSetOptions = false;
        }
    }

    /**
     * Set options for interval capture
     * API: osc/commands/execute (camera.setOptions)
     */
    private void setOptionsForCaptureInterval() {
        JSONObject setParam = new JSONObject();
        JSONObject optionParam = new JSONObject();

        try {
            setParam.put("captureNumber", Integer.parseInt(captureNumber));
            setParam.put("captureInterval", Integer.parseInt(captureInterval));

            optionParam.put(OSCParameterNameMapper.Options.OPTIONS, setParam);

            OSCCommandsExecute commandsExecute = new OSCCommandsExecute
                    ("camera.setOptions", optionParam);
            commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                @Override
                public void onResponse(OSCReturnType type, Object response) {
                    //check option setting success
                    //success -> start capture
                    //fail -> show error message
                    if (type == OSCReturnType.SUCCESS) {
                        successSetOptions = true;
                        Toast.makeText(mContext, response.toString(), Toast.LENGTH_SHORT).show();
                        setAndStartCaptureInterval();
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
     * Get value from editText and set delay
     */
    private void getInputValue() {
        captureNumber = editTextNumber.getText().toString();
        captureInterval = editTextInterval.getText().toString();
        delay = Integer.parseInt(captureInterval) * 1000 * (Integer.parseInt(captureNumber));
        Log.d(TAG, "delay = " + delay);
    }

    /**
     * Start interval capture
     * API: osc/commands/execute (camera.startCapture)
     */
    private void startCapture() {

        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.startCapture", null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, final Object response) {
                if (type == OSCReturnType.SUCCESS) {

                    mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            String commandId = Utils.getCommandId((String) response);
                            checkCommandsStatus(commandId);
                        }
                    };
                    mHandler = new Handler();
                    mHandler.postDelayed(mRunnable, delay);

                } else {
                    Utils.showTextDialog(mContext, getString(R.string.response),
                            Utils.parseString(response));
                }
            }
        });
        commandsExecute.execute();
    }

    /**
     * Check the status for previous inProgress commands.
     * Determine whether camera.startCapture has completed.
     * API: /osc/commands/status
     *
     * @param commandId command Id of previous camera.startCapture
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

    /**
     * Stop capture
     * API: /osc/commands/execute (camera.stopCapture)
     */
    private void stopCapture() {
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.stopCapture", null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, Object response) {
                Utils.showTextDialog(mContext, getString(R.string.response), Utils.parseString(response));
                if (mHandler != null)
                    mHandler.removeCallbacks(mRunnable);
            }
        });
        commandsExecute.execute();
    }
}
