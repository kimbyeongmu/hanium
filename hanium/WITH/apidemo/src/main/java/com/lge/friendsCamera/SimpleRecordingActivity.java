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

/**
 * Record video, check recording status and live snapshot during recording
 * Before start recording video, 'captureMode' should be set as 'video'
 */
public class SimpleRecordingActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = RecordVideoActivity.class.getSimpleName();

    private Context mContext;

    enum recordState {STOP_RECORDING, IS_RECORDING, PAUSE_RECORDING}

    private recordState currentRecordState;

    private static final String START = "camera.startCapture";
    private static final String RESUME = "camera._resumeRecording";
    private static final String PAUSE = "camera._pauseRecording";
    private static final String STOP = "camera.stopCapture";

    private Button buttonRecording;
    private Button buttonStop;
    private Button buttonLiveSnapShot;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupViews();
        initialize();
    }

    private void initialize() {
        mContext = this;
        FriendsCameraApplication.setContext(mContext);

        currentRecordState = recordState.STOP_RECORDING;
    }

    private void setupViews() {
        setContentView(R.layout.simplerecording_layout);

        getSupportActionBar().setTitle(R.string.capture_video);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button buttonRecordingStatus = (Button) findViewById(R.id.button_recordingStatus);
        buttonRecordingStatus.setOnClickListener(this);

        buttonRecording = (Button) findViewById(R.id.button_startVideo);
        buttonRecording.setOnClickListener(this);

        buttonStop = (Button) findViewById(R.id.button_stopVideo);
        buttonStop.setOnClickListener(this);

        buttonLiveSnapShot = (Button) findViewById(R.id.button_liveSnapShot);
        buttonLiveSnapShot.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_recordingStatus:
                getRecordingStatus();
                break;
            case R.id.button_startVideo:
                startVideo();
                break;
            case R.id.button_stopVideo:
                stopVideo();
                break;
            case R.id.button_liveSnapShot:
                liveSnapShot();
                break;
        }
    }

    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Captures an image during recordings, saving lat/long coordinates to EXIF
     * This api is enable only during recording and half mode (180 degree picture)
     * API : /osc/commands/execute (camera._liveSnapshot)
     */
    private void liveSnapShot() {
        mProgressDialog = ProgressDialog.show(mContext, "", "Processing...", true, false);
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera._liveSnapshot", null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, Object response) {
                String state = Utils.getCommandState(response);
                if (state != null && state.equals(OSCParameterNameMapper.STATE_INPROGRESS)) {
                    String commandId = Utils.getCommandId(response);
                    checkCommandsStatus(commandId);
                } else {
                    if (mProgressDialog.isShowing())
                        mProgressDialog.cancel();
                    Utils.showTextDialog(mContext, "Response  ", Utils.parseString(response));
                }
            }
        });
        commandsExecute.execute();
    }

    /**
     * start/resume/pause recording video
     */
    private void startVideo() {
        mProgressDialog = ProgressDialog.show(mContext, "", "Waiting..", true, false);
        if (currentRecordState == recordState.STOP_RECORDING) {
            changeRecordingStatus(START);
        } else if (currentRecordState == recordState.PAUSE_RECORDING) {
            changeRecordingStatus(RESUME);
        } else {  //is recording
            changeRecordingStatus(PAUSE);
        }
    }

    /**
     * stop recording video
     */
    private void stopVideo() {
        //Stop Recording
        mProgressDialog = ProgressDialog.show(mContext, "", "Waiting..", true, false);
        changeRecordingStatus(STOP);
    }

    /**
     * get recording status
     * API : /osc/commands/execute (camera._getRecordingStatus)
     */
    private void getRecordingStatus() {
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera._getRecordingStatus", null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, Object response) {
                Utils.showTextDialog(mContext, getString(R.string.response), Utils.parseString(response));
            }
        });
        commandsExecute.execute();
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
        if (currentRecordState != recordState.STOP_RECORDING) {
            changeRecordingStatus(STOP);
        }
        Log.v(TAG, "RecordVideo onDestroy");
    }

    /**
     * Change recording status
     * API : /osc/commands/execute
     * (camera.startCapture, camera._resumeRecording, camera._pauseRecording )
     *
     * @param command START / RESUME / PAUSE / STOP
     */
    private void changeRecordingStatus(final String command) {
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute(command, null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, Object response) {
                Log.v(TAG, "command :: " + command);
                if (type == OSCReturnType.SUCCESS) {
                    String state = Utils.getCommandState(response);
                    Log.v(TAG, "state = " + state);
                    if (state != null) {
                        if (state.equals(OSCParameterNameMapper.STATE_INPROGRESS)) {
                            String commandId = Utils.getCommandId(response);
                            checkCommandsStatus(commandId);
                            return;
                        } else {
                            // state == done
                            setRecordingState(command);
                            /*if (mProgressDialog.isShowing())
                                mProgressDialog.cancel();*/
                        }
                    }
                }
                Utils.showTextDialog(mContext, getString(R.string.response),
                        Utils.parseString(response));
                if (mProgressDialog.isShowing())
                    mProgressDialog.cancel();

            }
        });
        commandsExecute.execute();
    }

    /**
     * Check the status for previous inProgress commands.
     * Determine whether start/resume/pause recording and liveSnapshot have completed.
     *
     * @param commandId command Id of previous request
     *                  API : /osc/commands/status
     */
    private void checkCommandsStatus(final String commandId) {
        final OSCCommandsStatus commandsStatus = new OSCCommandsStatus(commandId);
        commandsStatus.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, final Object response) {
                if (type == OSCReturnType.SUCCESS) {
                    String state = Utils.getCommandState((String) response);
                    Log.v(TAG, "state = " + state);
                    if (state != null && state.equals(OSCParameterNameMapper.STATE_INPROGRESS)) {
                        checkCommandsStatus(commandId);
                    } else {
                        if (mProgressDialog.isShowing())
                            mProgressDialog.cancel();
                        Utils.showTextDialog(mContext, getString(R.string.response),
                                Utils.parseString(response));
                        updateUIBasedOnResponse(response);
                    }
                } else {
                    Utils.showTextDialog(mContext, getString(R.string.response),
                            Utils.parseString(response));
                }
            }
        });
        commandsStatus.execute();
    }

    private void updateUIBasedOnResponse(Object response) {
        String commandName = Utils.getCommandName(response);
        if (!commandName.equals("camera._liveSnapshot")) {
            setRecordingState(commandName);
        }
    }

    /**
     * set recording state
     * @param command START / RESUME / PAUSE / STOP
     */
    private void setRecordingState(String command) {
        //change recording status and UI button
        Log.v(TAG, "Change recording state from " + command);
        if (command.equals(START)) {
            currentRecordState = recordState.IS_RECORDING;
        } else if (command.equals(RESUME)) {
            currentRecordState = recordState.IS_RECORDING;
        } else if (command.equals(PAUSE)) {
            currentRecordState = recordState.PAUSE_RECORDING;
        } else { //camera._stopRecording
            currentRecordState = recordState.STOP_RECORDING;
        }
        setRecordingButton();
    }

    /**
     * set recording button
     */
    private void setRecordingButton() {
        if (currentRecordState == recordState.STOP_RECORDING) {
            buttonRecording.setText(R.string.start_recording);
        } else if (currentRecordState == recordState.PAUSE_RECORDING) {
            buttonRecording.setText(R.string.resume_recording);
        } else {   //IS_RECORDING
            buttonRecording.setText(R.string.pause_recording);
        }
    }
}
