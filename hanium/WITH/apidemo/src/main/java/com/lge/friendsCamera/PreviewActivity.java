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

import com.google.android.exoplayer.demo.player.DemoPlayer;
import com.google.android.exoplayer.demo.player.ExtractorRendererBuilder;
import com.lge.osclibrary.HTTP_SERVER_INFO;
import com.lge.osclibrary.HttpAsyncTask;
import com.lge.osclibrary.OSCCommandsExecute;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Get preview of friends camera
 */
public class PreviewActivity extends AppCompatActivity {
    private final static String TAG = PreviewActivity.class.getSimpleName();
    private Context mContext;
    private PreviewReceiveThread mPreviewReceiveThread = null;
    private float mRatioWidth = 2f;
    private float mRatioHeight = 2f;

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;

    private static int sPreviewScreenWidth = 0;
    private static int sPreviewScreenHeight = 0;

    private static String previewUri=null;
    private static String imageServerIP=null;
    private static String imagePort=null;

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_startpreview:
                    startPreview();
                    break;
                case R.id.button_stoppreview:
                    stopPreview();
                    break;
                case R.id.button_startstillpreview:
                    startstillPreview();
                    break;
                case R.id.button_stopstillpreview:
                    stopstillPreview();
                    break;

            }
        }
    };
    private Button buttonStartPreview;
    private Button buttonStopPreview;
    private Button buttonStartStillPreview;
    private Button buttonStopStillPreview;

    SurfaceView surfaceView;
    Surface surface;
    SurfaceHolder surfaceHolder;
    DemoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpView();
        initialize();
        if(previewUri != null){
            startPlayer();
        }
        if(imagePort != null && imageServerIP != null){
            startStillPlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
        if (mPreviewReceiveThread != null) {
            mPreviewReceiveThread.cancel();
            mPreviewReceiveThread.interrupt();
            mPreviewReceiveThread = null;
        }
    }

    private void setUpView() {
        setContentView(R.layout.preview_layout);

        getSupportActionBar().setTitle(R.string.preview);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        buttonStartPreview = (Button) findViewById(R.id.button_startpreview);
        buttonStopPreview = (Button) findViewById(R.id.button_stoppreview);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        buttonStartStillPreview = (Button) findViewById(R.id.button_startstillpreview);
        buttonStopStillPreview = (Button) findViewById(R.id.button_stopstillpreview);
    }

    private void initialize() {
        mContext = this;
        FriendsCameraApplication.setContext(mContext);

        buttonStartPreview.setOnClickListener(clickListener);
        buttonStopPreview.setOnClickListener(clickListener);
        buttonStartStillPreview.setOnClickListener(clickListener);
        buttonStopStillPreview.setOnClickListener(clickListener);
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
     * start preview (for idle state)
     * API : /osc/commands/execute (camera._startPreview)
     */
    private void startPreview() {
        imageServerIP = null;
        imagePort = null;

        JSONObject parameter = new JSONObject();
        try {
            parameter.put("_streamType", "UDP");
            OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera._startPreview", parameter);
            commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                @Override
                public void onResponse(OSCReturnType type, Object response) {
                    Utils.showTextDialog(mContext, getString(R.string.response), Utils.parseString(response));
                    if (type == OSCReturnType.SUCCESS) {

                        String tempUri = (String) response;
                        try {
                            JSONObject obj = new JSONObject(tempUri);
                            if (obj.has("results")) {
                                obj = obj.getJSONObject("results");
                            }
                            previewUri = obj.getString("_previewUri");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        startPlayer();
                    }
                }
            });
            commandsExecute.execute();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void startPlayer(){
        surfaceHolder = surfaceView.getHolder();
        surface = surfaceHolder.getSurface();

        Uri u = Uri.parse(previewUri);
        player = new DemoPlayer(new ExtractorRendererBuilder(PreviewActivity.this, "TEST", u));
        player.prepare();
        player.setSurface(surface);
        player.setPlayWhenReady(true);
    }

    /**
     * stop preview
     * API : /osc/commands/execute (camera._stopPreview)
     */
    private void stopPreview() {
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera._stopPreview", null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, Object response) {
                Utils.showTextDialog(mContext, getString(R.string.response),
                        Utils.parseString(response));
            }
        });
        commandsExecute.execute();


        if (player != null){
            player.release();
            previewUri = null;
        }
        //@// TODO: 2016-04-15 Socket is not closed
        //player=null;
    }

    /**
     * start still preview (for recording)
     * API : /osc/commands/execute (camera._startStillPreview)
     */
    private void startstillPreview() {
        previewUri = null;
        JSONObject parameter = new JSONObject();
        try {
            parameter.put("_streamType", "UDP");
            OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera._startStillPreview", null);
            commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                @Override
                public void onResponse(OSCReturnType type, Object response) {
                    Utils.showTextDialog(mContext, getString(R.string.response), Utils.parseString(response));
                    if (type == OSCReturnType.SUCCESS) {

                        String tempUri = (String) response;
                        try {
                            JSONObject obj = new JSONObject(tempUri);
                            while (obj.has("results")) {
                                obj = obj.getJSONObject("results");
                            }
                            imageServerIP = HTTP_SERVER_INFO.IP;
                            imagePort = obj.getString("_port");

                            startStillPlayer();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                }
            });
            commandsExecute.execute();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void startStillPlayer(){
        surfaceHolder = surfaceView.getHolder();
        surface = surfaceHolder.getSurface();
        mPreviewReceiveThread = new PreviewReceiveThread(imageServerIP, Integer.parseInt(imagePort));
        mPreviewReceiveThread.start();
    }


    /**
     * stop still preview
     * API : /osc/commands/execute (camera._stopStillPreview)
     */
    private void stopstillPreview() {

        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera._stopStillPreview", null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, Object response) {
                Utils.showTextDialog(mContext, getString(R.string.response),
                        Utils.parseString(response));
            }
        });
        commandsExecute.execute();

        if (mPreviewReceiveThread != null) {
            mPreviewReceiveThread.cancel();
            mPreviewReceiveThread.interrupt();
            mPreviewReceiveThread = null;
            imagePort = null;
            imageServerIP = null;
        }
    }

    private void updatePreview(byte[] data) {
        if (surface == null || data == null) {
            return;
        }

        Bitmap bitmap = createPreviewBitmap(data);
        try {
            Canvas canvas = surface.lockCanvas(null);
            canvas.drawBitmap(bitmap, 0, 0, null);
            surface.unlockCanvasAndPost(canvas);
        } catch (Exception e) {
        }
        bitmap.recycle();
    }

    private Bitmap createPreviewBitmap(byte[] data) {
        BitmapFactory.Options options;
        options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        options.inMutable = true;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (mPreviewWidth == 0 || sPreviewScreenHeight == 0) {
            mPreviewWidth = bitmap.getWidth();
            mPreviewHeight = bitmap.getHeight();
            calcRatio();
        }
        Matrix matrix = new Matrix();
        matrix.setScale(mRatioWidth, mRatioHeight);
        Bitmap rotate = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();

        return rotate;
    }

    private void calcRatio() {
        if (mPreviewWidth == 0 || mPreviewHeight == 0 || sPreviewScreenWidth == 0
                || sPreviewScreenHeight == 0) {
            return;
        }

        mRatioWidth = ((float) sPreviewScreenHeight) / ((float) mPreviewWidth);
        mRatioHeight = ((float) sPreviewScreenWidth) / ((float) mPreviewHeight);
    }

    private class PreviewReceiveThread extends Thread {
        private static final int SOCKET_TIME_OUT = 5000;
        private static final byte FAIL_MESSAGE = 2;

        private volatile boolean mCancelled;
        private Socket mSocket = null;
        private DataInputStream mDataInputStream = null;
        private OutputStream mOutputStream = null;
        private String IP = null;
        private int Port = -1;
        private final int MAX_PREVIEW_DATA_SIZE = 1024 * 1024;

        public PreviewReceiveThread(String ip, int port) {
            IP = ip;
            Port = port;
        }

        public void cancel() {
            mCancelled = true;
        }

        private void connectSocket() {
            try {
                Log.d("PreviewStill", IP + "   " + Port);
                mSocket = new Socket(IP, Port);
                mSocket.setSoTimeout(SOCKET_TIME_OUT);
                InputStream inputStream = mSocket.getInputStream();
                mOutputStream = mSocket.getOutputStream();
                if (inputStream == null || mOutputStream == null) {
                    return;
                }
                mDataInputStream = new DataInputStream(new BufferedInputStream(inputStream));
                if (mDataInputStream == null) {
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void closeSocket() {
            try {
                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }
                if (mDataInputStream != null) {
                    mDataInputStream.close();
                    mDataInputStream = null;
                }
                if (mOutputStream != null) {
                    mOutputStream.close();
                    mOutputStream = null;
                }
            } catch (IOException ie) {
                ie.printStackTrace();
            }
        }

        @Override
        public void run() {
            connectSocket();
            while (!mCancelled && mSocket != null && mSocket.isConnected()) {
                try {
                    int length = mDataInputStream.readInt();
                    if (length > MAX_PREVIEW_DATA_SIZE) {
                        mOutputStream.write(FAIL_MESSAGE);
                        mOutputStream.flush();
                        continue;
                    }
                    byte[] imageByte = new byte[length];
                    if (length > 0) {
                        mDataInputStream.readFully(imageByte);
                    }
                    updatePreview(imageByte);
                    imageByte = null;
                } catch (SocketTimeoutException ste) {
                    try {
                        mOutputStream.write(FAIL_MESSAGE);
                        mOutputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    try {
                        if (mSocket != null) {
                            mSocket.close();
                        }
                    } catch (IOException ie) {
                        ie.printStackTrace();
                    } finally {
                        if (mPreviewReceiveThread != null) {
                            mPreviewReceiveThread.cancel();
                            mPreviewReceiveThread.interrupted();
                            mPreviewReceiveThread = null;
                        }
                        //mOnSocketListener.retrySocket();
                    }
                }
            }
            closeSocket();
        }
    }
}
