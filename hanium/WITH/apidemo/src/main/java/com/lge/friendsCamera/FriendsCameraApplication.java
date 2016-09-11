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
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.lge.octopus.module.OctopusApplication;

/**
 * Application class to initialize for connection
 */
public class FriendsCameraApplication extends OctopusApplication {

    private static final String TAG = FriendsCameraApplication.class.getSimpleName();
    private static Context mContext;
    private static boolean mIsConnected;


    @Override
    public void onCreate(){
        super.onCreate();

    }

    public static void setContext(Context context){
        mContext = context;
    }
    public static Context  getContext(){
        return mContext;
    }

    public static void settIsConnected(boolean isConnect){ mIsConnected = isConnect;}
    public static boolean getIsConnected(){ return mIsConnected; }

    public static void controlUIInCurrentActivity(boolean isConnect){
        Log.e(TAG,"++++++++++++++++++control UI CALLED + " + isConnect);
        mIsConnected = isConnect;
        Context currentActivityContext = FriendsCameraApplication.getContext();
        String currentActivityName = currentActivityContext.getClass().getSimpleName();
        Log.e(TAG," Activity name = " + currentActivityName);
        if (currentActivityName.equals(MainActivity.class.getSimpleName())) {
            if (isConnect) {
                setMainUI(currentActivityContext, true);
            } else {
                setMainUI(currentActivityContext, false);
            }
        } else {
            if(!isConnect) {
                //Disconnected and not Main activity
                boolean isException = false;
                String[] exceptionLists = new String[]{
                        DownloadFileListViewActivity.class.getSimpleName(),
                        ConnectionActivity.class.getSimpleName(),
                        OverAPActivity.class.getSimpleName()
                };
                for(String exception: exceptionLists) {
                    if (currentActivityName.equals(exception)) {
                        isException = true;
                        break;
                    }
                }
                if(!isException){
                    startMain(currentActivityContext);
                }
            }
        }
    }

    /**
     * set Main UI
     * @param context
     * @param enable
     */
    public static void setMainUI(Context context, boolean enable){
        Log.d(TAG, "setMainUI = " + enable);
        ((MainActivity) context).updateStateBasedOnWifiConnection(enable);
    }

    /**
     * start Main Activity when friends camera is disconnected.
     * @param context
     */
    private static void startMain(Context context) {
        //Log.d(TAG, "startMain");
        Intent tempIntent = new Intent(context, MainActivity.class);
        tempIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(tempIntent);
        Toast.makeText(context, "Loose connection with camera. Go back to Main", Toast.LENGTH_SHORT).show();
    }
}
