package com.lge.friendsCamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


public class download extends Activity {
    String myJSON;

    private static final String TAG_RESULTS="result";
    private static final String TAG_NAME = "email";
    private static final String TAG_PWD = "password";
    private static final String TAG_PNUM ="phoneNumber";

    JSONArray peoples = null;

    ArrayList<HashMap<String, String>> personList;
    int k ;
    int current = 0;

    ///////////////////////////
    TextView messageText;
    String imageUrl = "http://203.255.60.146/uploads/";
    Bitmap mSaveBm;
    ImageView bmImage;
    String indexName[] = new String[100];

    private TimerTask mTask;
    private Timer mTimer;
    Handler handler;
    BitmapFactory.Options bmOptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imagedownload);
        messageText  = (TextView)findViewById(R.id.textView2);
        bmImage = (ImageView) findViewById(R.id.image);
        //////////////////////////
        personList = new ArrayList<HashMap<String,String>>();


        Button btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(btnSaveOnClickListener);

        Button pic = (Button) findViewById(R.id.pic_);
        pic.setOnClickListener(picOnClickListener);

        Button next = (Button) findViewById(R.id.next);
        next.setOnClickListener(nextOnClickListener);

        Button pre = (Button) findViewById(R.id.pre);
        pre.setOnClickListener(preOnClickListener);

        bmImage.setScaleType(ImageView.ScaleType.FIT_START);

        bmOptions = new BitmapFactory.Options();
        bmOptions.inSampleSize = 2;
        mTimer = new Timer();

        getData("http://203.255.60.146/getname.php");
 /*
        mTask = new TimerTask() {
            // @Override
            public void run() {

                MatrixTime(1000);
                plus_();
                MatrixTime(1500);
                save_();
            }
        };
      //      mTimer.schedule(mTask,0,10000);
*/

    }
    public void back__(View v) {
        Intent intent = new Intent(getApplicationContext(),SuccessActivity.class);
        startActivity(intent);
    }
    public void MatrixTime(int delayTime){


        long saveTime = System.currentTimeMillis();
        long currTime = 0;


        while( currTime - saveTime < delayTime){
            currTime = System.currentTimeMillis();
        }
    }
    public void plus_(){
        handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(indexName[k] == null) {
                            messageText.setText("complete");
                            mTask.cancel();
                            return;
                        }
                        else {
                            String copyUri = imageUrl + indexName[k];
                            messageText.setText(copyUri);
                            OpenHttpConnection opHttpCon = new OpenHttpConnection();
                            opHttpCon.execute(bmImage, copyUri);
                            k++;
                        }
                    }
                });
            }
        }).start();
    }
    public void save_(){
        handler = new Handler(Looper.getMainLooper());


        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        OutputStream outStream = null;
                        String extStorageDirectory =
                                Environment.getExternalStorageDirectory().toString();

                        File file = new File(extStorageDirectory, indexName[current]+".jpg");
                        try {
                            outStream = new FileOutputStream(file);
                            mSaveBm.compress(
                                    Bitmap.CompressFormat.JPEG, 100, outStream);
                            outStream.flush();
                            outStream.close();

                            Toast.makeText(download.this,
                                    "Saved", Toast.LENGTH_LONG).show();

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            Toast.makeText(download.this,
                                    e.toString(), Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(download.this,
                                    e.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                });

            }
        }).start();
    }
    Button.OnClickListener btnSaveOnClickListener =
            new Button.OnClickListener() {

                public void onClick(View arg0) {
                    OutputStream outStream = null;
                    String extStorageDirectory =
                            Environment.getExternalStorageDirectory().toString();

                    File file = new File("/storage/emulated/0/DCIM/friendsCameraSample/", current+".jpg");
                    try {
                        outStream = new FileOutputStream(file);
                        mSaveBm.compress(
                                Bitmap.CompressFormat.JPEG, 100, outStream);
                        outStream.flush();
                        outStream.close();

                        Toast.makeText(download.this,
                                "Saved", Toast.LENGTH_LONG).show();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(download.this,
                                e.toString(), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(download.this,
                                e.toString(), Toast.LENGTH_LONG).show();
                    }
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    //          File f = new File(mCurrentPhotoPath);
                    Uri contentUri = Uri.fromFile(file);
                    mediaScanIntent.setData(contentUri);
                    sendBroadcast(mediaScanIntent);
                }
            };
    Button.OnClickListener picOnClickListener =
            new Button.OnClickListener() {

                public void onClick(View arg0) {
                    Intent intent = new Intent(download.this,DownloadFileListViewActivity.class);
                    intent.putExtra("type", "image");
                    startActivity(intent);
                }
            };

    public void update(){
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
    }
    Button.OnClickListener nextOnClickListener =
            new Button.OnClickListener() {

                public void onClick(View arg0) {
                    if(k < current ) {
                        current = 0;
                    }
                    String copyUri = imageUrl + indexName[current];
                    messageText.setText(copyUri);
                    OpenHttpConnection opHttpCon = new OpenHttpConnection();
                    opHttpCon.execute(bmImage, copyUri);
                    current++;

                }
            };

    Button.OnClickListener preOnClickListener =
            new Button.OnClickListener() {

                public void onClick(View arg0) {
                    current--;
                    if( current == -1) {
                        current = k;
                    }
                    String copyUri = imageUrl + indexName[current];
                    messageText.setText(copyUri);
                    OpenHttpConnection opHttpCon = new OpenHttpConnection();
                    opHttpCon.execute(bmImage, copyUri);
                }
            };

    private class OpenHttpConnection extends AsyncTask<Object, Void, Bitmap> {

        private ImageView bmImage;

        @Override
        protected Bitmap doInBackground(Object... params) {
            Bitmap mBitmap = null;
            Bitmap resized = null;
            bmImage = (ImageView) params[0];
            String url = (String) params[1];
            InputStream in = null;
            try {
                in = new java.net.URL(url).openStream();
                in.mark(in.available());
                mBitmap = BitmapFactory.decodeStream(in,null,bmOptions);
                resized = Bitmap.createScaledBitmap(mBitmap, 2000,1500, true);
                in.close();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return resized;
        }

        @Override
        protected void onPostExecute(Bitmap bm) {
            super.onPostExecute(bm);
            mSaveBm = bm;
            bmImage.setImageBitmap(bm);
        }
    }
    /////////////////////////////////
    protected void showList(){
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            peoples = jsonObj.getJSONArray(TAG_RESULTS);

            k=peoples.length() -1 ;
            for(int i=0;i<peoples.length();i++){
                JSONObject c = peoples.getJSONObject(i);
                String email = c.getString(TAG_NAME);
                String password = c.getString(TAG_PWD);
                String phoneNumber = c.getString(TAG_PNUM);

                HashMap<String,String> persons = new HashMap<String,String>();

                persons.put(TAG_NAME,email);
                persons.put(TAG_PWD,password);
                persons.put(TAG_PNUM,phoneNumber);

                personList.add(persons);
                indexName[i] = email; //사진이름 받아오기
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void getData(String url){
        class GetDataJSON extends AsyncTask<String, Void, String>{

            @Override
            protected String doInBackground(String... params) {

                String uri = params[0];

                BufferedReader bufferedReader = null;
                try {
                    URL url = new URL(uri);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    StringBuilder sb = new StringBuilder();

                    bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String json;
                    while((json = bufferedReader.readLine())!= null){
                        sb.append(json+"\n");
                    }

                    return sb.toString().trim();

                }catch(Exception e){
                    return null;
                }



            }

            @Override
            protected void onPostExecute(String result){
                myJSON=result;
                showList();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
    }
}