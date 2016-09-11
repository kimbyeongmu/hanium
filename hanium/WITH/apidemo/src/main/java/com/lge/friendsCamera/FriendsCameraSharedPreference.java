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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;


public class FriendsCameraSharedPreference {

    private static final String PREF_NAME = "friendsCameraPreference";
    private static final String DEFAULT = null;

    private static Context mContext;

    //Keys for shared preference data
    public static final String BT_ADDR = "btAddress";
    public static final String SSID = "ssid";
    public static final String PASSWORD = "password";

    public FriendsCameraSharedPreference(Context context){
        mContext = context;
    }

    public String getPreference(String key){
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        return pref.getString(key,DEFAULT);
    }

    public void savePrefernce(String key, String value){
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putString(key,value);
        editor.commit();
    }

    public void removePreference(String key){
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.remove(key);
        editor.commit();
    }

    public void removeAllPreferences(){
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.clear();
        editor.commit();
    }
}
