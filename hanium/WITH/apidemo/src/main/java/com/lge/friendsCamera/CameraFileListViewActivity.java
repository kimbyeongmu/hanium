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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.lge.osclibrary.HttpAsyncTask;
import com.lge.osclibrary.HttpAsyncTask.OnHttpListener;
import com.lge.osclibrary.OSCCommandsExecute;
import com.lge.osclibrary.OSCParameterNameMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.io.Serializable;

/**
 * Show file lists of Friends Camera
 */
public class CameraFileListViewActivity extends AppCompatActivity {

    private TimerTask mTask;
    private Timer mTimer;
    Handler handler;

    private String commandId;
    private Switch autoPicture;

    private final static String TAG = CameraFileListViewActivity.class.getSimpleName();
    private Context mContext;
    CustomListAdapter adapter;
    private ListView mListView;

    private final int entryCount = 2; //Parameter value for entryCount (camera.getListFiles)

    private static final String IMAGE = "image";
    private static final String VIDEO = "video";

    private ProgressDialog mProgressDialog;

    private boolean startDownloading; //Check whether it is downloading or not
    private String currentDownloadFile; //Recent download file name

    private String mediaType; // Media type for this activity (image or video)

    //Array list for file information HashMap
    ArrayList<HashMap<String, String>> itemInfo = new ArrayList<HashMap<String, String>>();

    HashMap<String, String> te = new HashMap<String, String>();

    ArrayList<HashMap<String, String>> temporalList = new ArrayList<HashMap<String, String>>();
    private Button bt_parse;
    private Button bt_parse_;
    //Array list for thumbnail id
    ArrayList<Integer> itemBitmap = new ArrayList<>();


/////////////////////////////
final String uploadFilePath = "storage/emulated/0/DCIM/friendsCameraSample/";//경로를 모르겠으면, 갤러리 어플리케이션 가서 메뉴->상세 정보
    String uploadFileName = "20160823_201313.jpg";
     Button uploadButton;
     int serverResponseCode = 0;

    ProgressDialog dialog = null;
    String upLoadServerUri = null;
    //////////////////////////
    Intent intent;
    FileName fname;

    public int length = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupViews();
        initialize();

        uploadButton = (Button)findViewById(R.id.uploadButton);


        upLoadServerUri = "http://203.255.60.146/UploadToServer.php";//서버컴퓨터의 ip주소

        uploadButton.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View v) {



                dialog = ProgressDialog.show(CameraFileListViewActivity.this, "", "Uploading file...", true);



                new Thread(new Runnable() {

                    public void run() {

                        runOnUiThread(new Runnable() {

                            public void run() {

                            //    messageText.setText("uploading started.....");

                            }

                        });
                        uploadFile(uploadFilePath  + uploadFileName);
                    }

                }).start();

            }

        });

        bt_parse = (Button)findViewById(R.id.new_button);
        bt_parse_ = (Button)findViewById(R.id.autotake);
        mTask = new TimerTask() {
           // @Override
            public void run() {
                auto();
                MatrixTime(2000);
                _new();
                MatrixTime(1500);
                save();
            }
        };

        mTimer = new Timer();

        autoPicture = (Switch) findViewById(R.id.switch1);
            autoPicture.setChecked(false);
        autoPicture.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton cb,
                                         boolean isChecking) {
                if(isChecking){
                    mTimer.schedule(mTask,0,10000);

                }
                else{
                    mTimer.cancel();
                }
            }
        });
    }

    public void MatrixTime(int delayTime){


        long saveTime = System.currentTimeMillis();
        long currTime = 0;


        while( currTime - saveTime < delayTime){
            currTime = System.currentTimeMillis();
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
     * Set up view based on media type (image or video)
     */
    protected void setupViews() {
        setContentView(R.layout.filelistview_layout);
        mListView = (ListView) findViewById(R.id.list_view);

        //Get media type from intent
        Intent intent = getIntent();
        mediaType = intent.getExtras().getString("type");

        if (mediaType.equals(IMAGE)) {
            getSupportActionBar().setTitle(R.string.camera_image_gallery);
        } else {
            getSupportActionBar().setTitle(R.string.camera_video_gallery);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new CustomListAdapter(this, itemInfo, itemBitmap);
    }

    public void new_(View v) {
       // adapter.clear();
       // setupViews();
       // initialize();
    }

    public void save_(View v) {
        getFullFile_(0);
    }


    public void auto_(View v) {
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.takePicture", null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, final Object response) {
                if (type == OSCReturnType.SUCCESS) {
                    String state = Utils.getCommandState(response);
                    if (state != null && state.equals(OSCParameterNameMapper.STATE_INPROGRESS)) {
                        String commandId = Utils.getCommandId(response);
                        fname = new FileName(commandId,length);
                        length++;
                        return;
                    }
                    te.put(Utils.parseString(response),Utils.parseString(response));
                    temporalList.addAll(itemInfo);
                    temporalList.add(te);
                    itemInfo.clear();
                    itemInfo.addAll(temporalList);
                    adapter.notifyDataSetChanged();
                }
            }
        });
        commandsExecute.execute();
        intent = new Intent(CameraFileListViewActivity.this, SuccessActivity.class);
        intent.putExtra("OBJECT",fname);
        startActivity(intent);
    }



    public void _new() {

        handler = new Handler(Looper.getMainLooper());


        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.clear();
                        setupViews();
                        initialize();
                    }
                });

            }
        }).start();



    }

    public void save() {
        getFullFile_(0);
    }


    public void auto() {
        OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.takePicture", null);
        commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OSCReturnType type, final Object response) {
                if (type == OSCReturnType.SUCCESS) {
                    String state = Utils.getCommandState(response);
                    if (state != null && state.equals(OSCParameterNameMapper.STATE_INPROGRESS)) {
                        String commandId = Utils.getCommandId(response);
                        FileName Fname = new FileName(commandId,length);
                        length++;
                        return;
                    }
                    te.put(Utils.parseString(response),Utils.parseString(response));
                    temporalList.addAll(itemInfo);
                    temporalList.add(te);
                    itemInfo.clear();
                    itemInfo.addAll(temporalList);
                    adapter.notifyDataSetChanged();
                }
            }
        });
        commandsExecute.execute();
    }
    /**
     * Initialize view based on media type (image or video)
     */
    protected void initialize() {
        mContext = this;
        FriendsCameraApplication.setContext(mContext);

        if (mediaType == null) {
            Log.v(TAG, "ERROR: Need to set media type");
            return;
        }

        //Set adapter type based on media type
        if (mediaType.equals(IMAGE)) {
            adapter.setType(CustomListAdapter.selectedGalleryType.CAMERA_IMAGE);
        } else if (mediaType.equals(VIDEO)) {
            adapter.setType(CustomListAdapter.selectedGalleryType.CAMERA_VIDEO);
        }

      //  adapter.notifyDataSetChanged();
        //Set adapter for list view
        //  Multiple choice mode for deleting multiple items
        //  Single choice mode for selecting an item to show info, download, and delete
        mListView.setAdapter(adapter);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setItemsCanFocus(false);
        mListView.setMultiChoiceModeListener(multiChoiceModeListener);
        mListView.setOnItemClickListener(itemClickListener);

        mProgressDialog = ProgressDialog.show(mContext, "", "Loading...", true, false);

        //Get file list from camera
        getListFiles(null);


    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "CameraFileList onResume");
        if (!FriendsCameraApplication.getIsConnected()) {
            ((CameraFileListViewActivity) mContext).finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "CameraFileList onStop");
        if (startDownloading) {
            deleteUnfinishedDownloadFile();
            Toast.makeText(mContext, "Fail to download the file " + currentDownloadFile, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Item click listener for an item selected
     * Show dialog for the selected row item
     */
    AdapterView.OnItemClickListener itemClickListener =
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    CharSequence[] list = new CharSequence[]{"Show Information", "Get File", "Delete"};
                    Utils.showListDialog(mContext, list, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0: {
                                    getFileMetadata(position);
                                    break;
                                }
                                case 1: {
                                    getFullFile(position);
                                    break;
                                }

                                case 2: {
                                    int[] selectedPosition = new int[1];
                                    selectedPosition[0] = position;
                                    deleteFilesFromCamera(selectedPosition, false);
                                }

                                default:
                                    break;
                            }
                        }
                    });
                }
            };


    /**
     * Listener for multiple choice mode
     */
    private AbsListView.MultiChoiceModeListener multiChoiceModeListener =
            new AbsListView.MultiChoiceModeListener()
            {
                private int nr = 0; //The number of selected items

                /**
                 * get View for selected row
                 * @param pos position of the selected row view
                 * @param listView list View which includes the selected row
                 * @return View row view at the position
                 */
                //Get row view at selected position
                private View getViewByPosition(int pos, ListView listView) {
                    final int firstListItemPosition = listView.getFirstVisiblePosition();
                    final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

                    if (pos < firstListItemPosition || pos > lastListItemPosition) {
                        return listView.getAdapter().getView(pos, null, listView);
                    } else {
                        final int childIndex = pos - firstListItemPosition;
                        return listView.getChildAt(childIndex);
                    }
                }

                /**
                 * Set background color for selected row                 *
                 */
                private void setRowViewBackgrounds() {
                    SparseBooleanArray checked = mListView.getCheckedItemPositions();
                    int colorHighlight = ContextCompat.getColor(mContext, R.color.colorHighlight);
                    int colorTransparent = ContextCompat.getColor(mContext, R.color.colorTransparent);
                    for (int i = 0; i < checked.size(); i++) {
                        int position = checked.keyAt(i);
                        Log.v(TAG, "position = " + checked.keyAt(i) + " i = " + i
                                + " value = " + checked.valueAt(i));

                        //Get row view
                        View rowView = getViewByPosition(position, mListView);
                        if (checked.valueAt(i)) {
                            rowView.setBackgroundColor(colorHighlight);
                        } else {
                            rowView.setBackgroundColor(colorTransparent);
                        }
                    }
                }

                @Override
                public void onItemCheckedStateChanged
                        (ActionMode mode, int position, long id, boolean checked) {
                    int mCheckedCount = mListView.getCheckedItemCount();

                    //Count the number of checked items
                    if (checked) {
                        nr++;
                    } else {
                        nr--;
                    }
                    String title = (mCheckedCount > 0) ? (nr + " selected") : (0 + " selected");

                    mode.setTitle(title);
                    setRowViewBackgrounds();
                }


                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    ((Activity) mContext).getMenuInflater().inflate(
                            R.menu.contextual_actions, menu);
                    //Removing the default item click listener
                    mListView.setOnItemClickListener(null);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                    adapter.setDuringDeletion(true);
                    int id = item.getItemId();
                    switch (id) {
                        //Delete all files from camera
                        case R.id.action_delete_all:
                            String message = "Do you want to delete all image files?";
                            DialogInterface.OnClickListener deleteAllListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteFilesFromCamera(null, true);
                                    nr = 0;
                                    mode.setTitle(0 + " selected");
                                }
                            };

                            Utils.showSelectDialog(mContext, null, message, deleteAllListener, null);
                            return true;

                        //Delete selected files from camera
                        case R.id.action_delete:
                            SparseBooleanArray checked = mListView.getCheckedItemPositions();
                            int[] selectedPosition = new int[checked.size()];

                            for (int i = 0; i < checked.size(); i++) {
                                if (checked.valueAt(i)) {
                                    selectedPosition[i] = checked.keyAt(i);
                                    mListView.setItemChecked(selectedPosition[i], false);
                                }
                            }
                            deleteFilesFromCamera(selectedPosition, false);
                            nr = 0;
                            return true;

                        default:
                            Log.v(TAG, "ERROR");
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    nr = 0;
                    mListView.setOnItemClickListener(itemClickListener);
                }

            };

    /**
     * Delete the last downloaded file
     * Call this method when the downloading ends unsuccessfully
     * (ex) loose connection during downloading
     */
    private void deleteUnfinishedDownloadFile() {
        String mTargetDirectory = OSCCommandsExecute.getFileLocation();
        String fileUri = mTargetDirectory + "/" + currentDownloadFile;
        File file = new File(fileUri);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Get file lists from camera
     * API: /osc/commands/execute (camera.listFiles)
     * @param token continuation token from camera response.
     */
    private void getListFiles(String token) {
        JSONObject parameters = new JSONObject();
        try {
            //Set parameter values
            parameters.put(OSCParameterNameMapper.ENTRYCOUNT, entryCount);
            parameters.put(OSCParameterNameMapper.MAXTHUMBSIZE, null);

            //Set fileType parameter (image or video)
            if (mediaType.equals(IMAGE)) {
                parameters.put(OSCParameterNameMapper.FILETYPE, IMAGE);
            } else if (mediaType.equals(VIDEO)) {
                parameters.put(OSCParameterNameMapper.FILETYPE, VIDEO);
            }

            Log.v(TAG, "get list token = " + token);
            if (token != null) {
                //Set continuation token if it exists
                parameters.put(OSCParameterNameMapper.CONTINUATION_TOKEN, token);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (parameters != null) {
            OSCCommandsExecute commandsExecute = null;
            commandsExecute = new OSCCommandsExecute("camera.listFiles", parameters);

            if (commandsExecute == null) {
                Log.v(TAG, "ERROR: media type error");
                return;
            }

            commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                @Override
                public void onResponse(OnHttpListener.OSCReturnType type, Object response) {
                    if (type == OSCReturnType.SUCCESS) {
                        updateList((String) response);
                    } else {
                        Utils.showTextDialog(mContext, getString(R.string.response), Utils.parseString(response));
                    }
                }
            });
            commandsExecute.execute();
        }
    }

    /**
     * Parse response of camera.listFiles request
     * Save file info in hash map
     *
     * if continuation token exists in response, then call getListFiles method
     * if not, update list adapter to show results
     * @param data response of camera.listFiles request
     */
    private void updateList(String data) {
        try {
            JSONObject jObject = new JSONObject(data);

            JSONObject resultData = jObject.getJSONObject(OSCParameterNameMapper.RESULTS);
            JSONArray entries = resultData.getJSONArray(OSCParameterNameMapper.ENTRIES);
            for (int i = 0; i < entries.length(); i++) {
                //Parse file info and save info in hash map
                JSONObject fileInfo = entries.getJSONObject(i);

                HashMap<String, String> info = makeFileInfoMap(fileInfo);
                //Set dummy value for bitmapId
                adapter.addItem(info, -1);
            }

            if (resultData.has(OSCParameterNameMapper.CONTINUATION_TOKEN)) {
                //if continuation token exists call the get list files
                //to get remaining list
                String token = resultData.getString(OSCParameterNameMapper.CONTINUATION_TOKEN);
                Log.v(TAG, "Current token = " + token);
                getListFiles(token);
            } else {
                Log.d(TAG, "Token END");
                // Update lists
                adapter.notifyDataSetChanged();
                if (mProgressDialog.isShowing())
                    mProgressDialog.cancel();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convert file info in JSONObject into HashMap
     * @param fileInfo information of a file (JSONObject)
     * @return information of a file (HashMap)
     */
    private HashMap<String, String> makeFileInfoMap(JSONObject fileInfo) {
        HashMap<String, String> info = new HashMap<>();

        Iterator it = fileInfo.keys();
        while (it.hasNext()) {
            try {
                String key = (String) it.next();
                Object tempValue = fileInfo.get(key);
                String value = tempValue.toString();
                info.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return info;
    }

    /**
     * Get metadata of a selected file
     * API: /osc/commands/execute (camera.getMetadata)
     * @param position a position of the selected row
     */
    private void getFileMetadata(int position) {
        String url = adapter.getInfo(position, OSCParameterNameMapper.FileInfo.URL);
        JSONObject parameters = new JSONObject();
        try {
            parameters.put(OSCParameterNameMapper.FILEURL, url);

            final OSCCommandsExecute commandsExecute = new OSCCommandsExecute("camera.getMetadata", parameters);

            commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                @Override
                public void onResponse(OnHttpListener.OSCReturnType type, Object response) {
                    //Show response in Dialog
                    Utils.showTextDialog(mContext, getString(R.string.response), Utils.parseString(response));
                }
            });
            commandsExecute.execute();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get full-size video or full-size image from camera
     * API: /osc/commands/execute (camera.getFile)
     * @param position  a position of the selected row
     */
    protected void getFullFile(final int position) {
        currentDownloadFile = adapter.getInfo(position, OSCParameterNameMapper.FileInfo.NAME);
        JSONObject parameters = new JSONObject();
        try {
            String url = adapter.getInfo(position, OSCParameterNameMapper.FileInfo.URL);
            parameters.put(OSCParameterNameMapper.FILEURL, url);

            OSCCommandsExecute commandsExecute;

            //Set the data type for request (image or video)
            //It will set different http request header property
            if (mediaType.equals(IMAGE)) {
                commandsExecute = new OSCCommandsExecute("camera.getFile", parameters,
                        OSCCommandsExecute.CommandType.IMAGE);
            } else if (mediaType.equals(VIDEO)) {
                commandsExecute = new OSCCommandsExecute("camera.getFile", parameters,
                        OSCCommandsExecute.CommandType.VIDEO);
            } else {
                Log.d(TAG, "Media type should be image or video");
                return;
            }
            commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                @Override
                public void onResponse(OnHttpListener.OSCReturnType type, Object response) {
                    if (type == OSCReturnType.SUCCESS) {
                        //Get binary data from camera and save successfully
                        //Response of getFile is the fileUri of the saved file
                        String name = adapter.getInfo(position, OSCParameterNameMapper.FileInfo.NAME);
                        handleResponse(name, (String) response);
                    } else {
                        Utils.showTextDialog(mContext, getString(R.string.response), Utils.parseString(response));
                    }
                }
            });
            commandsExecute.execute();
            //Set downloading flag as true
            startDownloading = true;
            mProgressDialog = ProgressDialog.show(mContext, "", "Downloading...", true, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void getFullFile_(int position) {
        currentDownloadFile = adapter.getInfo(position, OSCParameterNameMapper.FileInfo.NAME);
        JSONObject parameters = new JSONObject();
        try {
            String url = adapter.getInfo(position, OSCParameterNameMapper.FileInfo.URL);
            parameters.put(OSCParameterNameMapper.FILEURL, url);

            OSCCommandsExecute commandsExecute;

            //Set the data type for request (image or video)
            //It will set different http request header property
            if (mediaType.equals(IMAGE)) {
                commandsExecute = new OSCCommandsExecute("camera.getFile", parameters,
                        OSCCommandsExecute.CommandType.IMAGE);
            } else if (mediaType.equals(VIDEO)) {
                commandsExecute = new OSCCommandsExecute("camera.getFile", parameters,
                        OSCCommandsExecute.CommandType.VIDEO);
            } else {
                Log.d(TAG, "Media type should be image or video");
                return;
            }
            commandsExecute.setListener(new HttpAsyncTask.OnHttpListener() {
                @Override
                public void onResponse(OnHttpListener.OSCReturnType type, Object response) {
                    if (type == OSCReturnType.SUCCESS) {
                        //Get binary data from camera and save successfully
                        //Response of getFile is the fileUri of the saved file
                        String name = adapter.getInfo(0, OSCParameterNameMapper.FileInfo.NAME);
                        handleResponse(name, (String) response);
                    } else {
                        Utils.showTextDialog(mContext, getString(R.string.response), Utils.parseString(response));
                    }
                }
            });
            commandsExecute.execute();
            //Set downloading flag as true
            startDownloading = true;
       //     mProgressDialog = ProgressDialog.show(mContext, "", "Downloading...", true, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle response of getFile
     * Update android gallery to show downloaded image and show toast message
     * @param fileName downloaded file name
     * @param localUri downloaded file uri on local storage
     */
    private void handleResponse(String fileName, String localUri) {
        //Set downloading flag as false
        //Update android gallery
        startDownloading = false;
        currentDownloadFile = "";
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + (String) localUri)));
        Toast.makeText(mContext, fileName + " is saved", Toast.LENGTH_LONG).show();
        mProgressDialog.cancel();
    }

    /**
     * Delete selected files
     * API: /osc/commands/execute (camera.delete)
     * @param positions positions of the selected row
     * @param removeAll removeAll flag. True = remove All / False = remove only selected items
     */
    private void deleteFilesFromCamera(final int[] positions, final boolean removeAll) {
        String[] fileUrls;

        if (removeAll) {
            //Set parameter for remove all
            fileUrls = new String[1];
            if (mediaType.equals(IMAGE))
                fileUrls[0] = "image";
            else if (mediaType.equals(VIDEO))
                fileUrls[0] = "video";
        } else {
            //Set fileUrls parameter with file urls of selected items
            int len = positions.length;
            fileUrls = new String[len];
            for (int i = 0; i < len; i++) {
                fileUrls[i] = adapter.getInfo(positions[i], OSCParameterNameMapper.FileInfo.URL);
            }
        }

        JSONObject parameters = new JSONObject();
        try {
            JSONArray urlsParameter = new JSONArray();
            for(int i=0; i<fileUrls.length; i++) urlsParameter.put(fileUrls[i]);

            parameters.put(OSCParameterNameMapper.FILEURLS, urlsParameter);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final OSCCommandsExecute cmdExecute = new OSCCommandsExecute("camera.delete", parameters);
        cmdExecute.setListener(new HttpAsyncTask.OnHttpListener() {
            @Override
            public void onResponse(OnHttpListener.OSCReturnType type, Object response) {
                adapter.setDuringDeletion(false);
                if (type == OSCReturnType.SUCCESS) {
                    //Update adapter
                    if (removeAll) {
                        adapter.removeAllItems();
                    } else {
                        adapter.removeItems(positions);
                    }
                    adapter.notifyDataSetChanged();
                }

                if (mProgressDialog.isShowing()) {
                    mProgressDialog.cancel();
                }
                Utils.showTextDialog(mContext, getString(R.string.response), Utils.parseString(response));
            }
        });
        cmdExecute.execute();
        mProgressDialog = ProgressDialog.show(mContext, "", "Processing...", true, false);
    }

    public int uploadFile(String sourceFileUri) {



        String fileName = sourceFileUri;



        HttpURLConnection conn = null;

        DataOutputStream dos = null;

        String lineEnd = "\r\n";

        String twoHyphens = "--";

        String boundary = "*****";

        int bytesRead, bytesAvailable, bufferSize;

        byte[] buffer;

        int maxBufferSize = 1 * 1024 * 1024;

        File sourceFile = new File(sourceFileUri);



        if (!sourceFile.isFile()) {



            dialog.dismiss();



            Log.e("uploadFile", "Source File not exist :"

                    +uploadFilePath + "" + uploadFileName);



            runOnUiThread(new Runnable() {

                public void run() {

                //    messageText.setText("Source File not exist :"

                 //           +uploadFilePath + "" + uploadFileName);

                }

            });



            return 0;



        }

        else

        {

            try {



                // open a URL connection to the Servlet

                FileInputStream fileInputStream = new FileInputStream(sourceFile);

                URL url = new URL(upLoadServerUri);



                // Open a HTTP  connection to  the URL

                conn = (HttpURLConnection) url.openConnection();

                conn.setDoInput(true); // Allow Inputs

                conn.setDoOutput(true); // Allow Outputs

                conn.setUseCaches(false); // Don't use a Cached Copy

                conn.setRequestMethod("POST");

                conn.setRequestProperty("Connection", "Keep-Alive");

                conn.setRequestProperty("ENCTYPE", "multipart/form-data");

                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                conn.setRequestProperty("uploaded_file", fileName);



                dos = new DataOutputStream(conn.getOutputStream());



                dos.writeBytes(twoHyphens + boundary + lineEnd);

                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""

                        + fileName + "\"" + lineEnd);



                dos.writeBytes(lineEnd);



                // create a buffer of  maximum size

                bytesAvailable = fileInputStream.available();



                bufferSize = Math.min(bytesAvailable, maxBufferSize);

                buffer = new byte[bufferSize];



                // read file and write it into form...

                bytesRead = fileInputStream.read(buffer, 0, bufferSize);



                while (bytesRead > 0) {



                    dos.write(buffer, 0, bufferSize);

                    bytesAvailable = fileInputStream.available();

                    bufferSize = Math.min(bytesAvailable, maxBufferSize);

                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);



                }



                // send multipart form data necesssary after file data...

                dos.writeBytes(lineEnd);

                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);



                // Responses from the server (code and message)

                serverResponseCode = conn.getResponseCode();

                String serverResponseMessage = conn.getResponseMessage();



                Log.i("uploadFile", "HTTP Response is : "

                        + serverResponseMessage + ": " + serverResponseCode);



                if(serverResponseCode == 200){



                    runOnUiThread(new Runnable() {

                        public void run() {



                            String msg = "File Upload Completed.\n\n See uploaded file here : \n\n"

                                    +uploadFileName;



                      //      messageText.setText(msg);

                            Toast.makeText(CameraFileListViewActivity.this, "File Upload Complete.",

                                    Toast.LENGTH_SHORT).show();

                        }

                    });

                }



                //close the streams //

                fileInputStream.close();

                dos.flush();

                dos.close();



            } catch (MalformedURLException ex) {



                dialog.dismiss();

                ex.printStackTrace();



                runOnUiThread(new Runnable() {

                    public void run() {

                   //     messageText.setText("MalformedURLException Exception : check script url.");

                        Toast.makeText(CameraFileListViewActivity.this, "MalformedURLException",

                                Toast.LENGTH_SHORT).show();

                    }

                });



                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);

            } catch (Exception e) {



                dialog.dismiss();

                e.printStackTrace();



                runOnUiThread(new Runnable() {

                    public void run() {

                   //     messageText.setText("Got Exception : see logcat ");

                        Toast.makeText(CameraFileListViewActivity.this, "Got Exception : see logcat ",

                                Toast.LENGTH_SHORT).show();

                    }

                });

                Log.e("Upload file server Ex", "Ex : "

                        + e.getMessage(), e);

            }

            dialog.dismiss();

            return serverResponseCode;



        } // End else block

    }


}