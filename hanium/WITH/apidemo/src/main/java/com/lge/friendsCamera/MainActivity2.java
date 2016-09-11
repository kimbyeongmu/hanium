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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Main Activity
 * Info, State, CheckForUpdates APIs are executed in this activity
 * Other APIs are executed in other activity
 */
public class MainActivity2 extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main2_layout);
    }
    public void back1(View v) {
        Intent intent = new Intent(getApplicationContext(),SuccessActivity.class);
        startActivity(intent);
    }

    public void video_(View v) {
        Intent intent = new Intent(MainActivity2.this,DownloadFileListViewActivity2.class);
        intent.putExtra("type", "video");
        startActivity(intent);
    }

    public void picture_(View v) {
        Intent intent = new Intent(MainActivity2.this,DownloadFileListViewActivity2.class);
        intent.putExtra("type", "image");
        startActivity(intent);
    }
}