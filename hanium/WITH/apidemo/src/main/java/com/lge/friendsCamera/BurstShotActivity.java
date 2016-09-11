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
 * Capture the number of images followed by the option; _burstshotNumber
 * Before start capture, 'captureMode' should be set as '_burstshot'
 */
public class BurstShotActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = BurstShotActivity.class.getName();
    private Context mContext;

    private ProgressDialog mProgressDialog;

    private EditText editTextNumber;
    private Button buttonStartCapture;

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
            ((BurstShotActivity) mContext).finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, TAG + " onStop");
    }

    private void setUpViews() {
        setContentView(R.layout.burstshot_layout);
        editTextNumber = (EditText) findViewById(R.id.burstshot_number);
        buttonStartCapture = (Button) findViewById(R.id.button_startBurstshot);

        getSupportActionBar().setTitle(R.string.capture_burstshot);
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

        buttonStartCapture.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_startBurstshot:
                setOptionCapture();
                break;
        }
    }

    /**
     * Set options for burst shot
     * API: osc/commands/execute (camera.setOptions)
     */
    private void setOptionCapture() {
        JSONObject setParam = new JSONObject();
        JSONObject optionParam = new JSONObject();

        try {
            setParam.put("_burstshotNumber", Integer.parseInt(editTextNumber.getText().toString()));

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
                        Toast.makeText(mContext, response.toString(), Toast.LENGTH_SHORT).show();
                        startCaptureBurstshot();
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
     * Start burst shot. /osc/commands/status and camera.stopCapture is not used for burst shot.
     * This api will not show the list of result files
     * API: osc/commands/execute (camera.startCapture)
     */
    private void startCaptureBurstshot() {

        mProgressDialog = ProgressDialog.show(mContext, "", "Processing...", true, false);
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.startCapture", null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, Object response) {
                if (mProgressDialog.isShowing())
                    mProgressDialog.cancel();
                Utils.showTextDialog(mContext, "Response  ", Utils.parseString(response));
            }
        });
        commandsExecute.execute();
    }

}
