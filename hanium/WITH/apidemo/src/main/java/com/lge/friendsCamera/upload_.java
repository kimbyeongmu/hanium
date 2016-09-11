package com.lge.friendsCamera;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class upload_ extends Activity implements View.OnClickListener {


    public static final int CONNECTION_TIMEOUT=10000;
    public static final int READ_TIMEOUT=15000;
    // LOG

    private String TAGLOG = "MainActivityLoG";



    // 이미지넣는 뷰와 업로드하기위환 버튼

    private ImageView ivUploadImage;

    private Button btnUploadImage;



    // 서버로 업로드할 파일관련 변수

    public String uploadFilePath;

    public String uploadFileName;

    private int REQ_CODE_PICK_PICTURE = 1;



    // 파일을 업로드 하기 위한 변수 선언

    private int serverResponseCode = 0;



    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.upload_);



        // 변수 초기화

        InitVariable();

    }



    // 초기화

    private void InitVariable() {

        // 이미지를 넣을 뷰

        ivUploadImage = (ImageView) findViewById(R.id.iv_upload_image);

        ivUploadImage.setOnClickListener(this);

        btnUploadImage = (Button) findViewById(R.id.btn_upload_image);

        btnUploadImage.setOnClickListener(this);

    }



    // ====================================
    // ======================================================

    // ==================================== 사진을 불러오는 소스코드 ============================

    @Override

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQ_CODE_PICK_PICTURE) {

            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                String path = getPath(uri);
                String name = getName(uri);
                uploadFilePath = path;
                uploadFileName = name;
                Log.i(TAGLOG, "[onActivityResult] uploadFilePath:" + uploadFilePath + ", uploadFileName:" + uploadFileName);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 3;
                Bitmap bit = BitmapFactory.decodeFile(path,options);
              //  bit.createScaledBitmap(bit,4000,4000,false);
                ivUploadImage.setImageBitmap(bit);
            }
        }
    }
    // 실제 경로 찾기
    private String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);

        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();

        return cursor.getString(column_index);

    }



    // 파일명 찾기

    private String getName(Uri uri) {

        String[] projection = {MediaStore.Images.ImageColumns.DISPLAY_NAME};

        Cursor cursor = managedQuery(uri, projection, null, null, null);

        int column_index = cursor

                .getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME);

        cursor.moveToFirst();

        return cursor.getString(column_index);

    }



    // uri 아이디 찾기

    private String getUriId(Uri uri) {

        String[] projection = {MediaStore.Images.ImageColumns._ID};

        Cursor cursor = managedQuery(uri, projection, null, null, null);

        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID);

        cursor.moveToFirst();

        return cursor.getString(column_index);

    }

    // ==========================================================================================





    // ==========================================================================================

    // ============================== 사진을 서버에 전송하기 위한 스레드 ========================

    public void button16(View v) {
        Intent intent = new Intent(getApplicationContext(),SuccessActivity.class);
        startActivity(intent);
    }


    private class UploadImageToServer extends AsyncTask<String, String, String> {

        ProgressDialog mProgressDialog;

        String fileName = uploadFilePath;

        HttpURLConnection conn = null;

        DataOutputStream dos = null;

        String lineEnd = "\r\n";

        String twoHyphens = "--";

        String boundary = "*****";

        int bytesRead, bytesAvailable, bufferSize;

        byte[] buffer;

        int maxBufferSize = 1 * 10240 * 10240;

        File sourceFile = new File(uploadFilePath);



        @Override

        protected void onPreExecute() {

            // Create a progressdialog

            mProgressDialog = new ProgressDialog(upload_.this);

            mProgressDialog.setTitle("Loading...");

            mProgressDialog.setMessage("Image uploading...");

            mProgressDialog.setCanceledOnTouchOutside(false);

            mProgressDialog.setIndeterminate(false);

            mProgressDialog.show();

        }



        @Override

        protected String doInBackground(String... serverUrl) {

            if (!sourceFile.isFile()) {

                runOnUiThread(new Runnable() {

                    public void run() {

                        Log.i(TAGLOG, "[UploadImageToServer] Source File not exist :" + uploadFilePath);

                    }

                });

                return null;

            } else {

                try {

                    // open a URL connection to the Servlet

                    FileInputStream fileInputStream = new FileInputStream(sourceFile);

                    URL url = new URL(serverUrl[0]);



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

                    Log.i(TAGLOG, "fileName: " + fileName);



                    dos = new DataOutputStream(conn.getOutputStream());



                    // 사용자 이름으로 폴더를 생성하기 위해 사용자 이름을 서버로 전송한다.

                    dos.writeBytes(twoHyphens + boundary + lineEnd);

                    dos.writeBytes("Content-Disposition: form-data; name=\"data1\"" + lineEnd);

                    dos.writeBytes(lineEnd);

                    dos.writeBytes("newImage");

                    dos.writeBytes(lineEnd);



                    // 이미지 전송

                    dos.writeBytes(twoHyphens + boundary + lineEnd);

                    dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\"; filename=\"" + fileName + "\"" + lineEnd);

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



                    Log.i(TAGLOG, "[UploadImageToServer] HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);



                    if (serverResponseCode == 200) {

                        runOnUiThread(new Runnable() {

                            public void run() {

                                Toast.makeText(upload_.this, "File Upload Completed", Toast.LENGTH_SHORT).show();

                            }

                        });

                    }

                    //close the streams //

                    fileInputStream.close();

                    dos.flush();

                    dos.close();



                } catch (MalformedURLException ex) {

                    ex.printStackTrace();

                    runOnUiThread(new Runnable() {

                        public void run() {

                            Toast.makeText(upload_.this, "MalformedURLException", Toast.LENGTH_SHORT).show();

                        }

                    });

                    Log.i(TAGLOG, "[UploadImageToServer] error: " + ex.getMessage(), ex);

                } catch (Exception e) {

                    e.printStackTrace();

                    runOnUiThread(new Runnable() {

                        public void run() {

                            Toast.makeText(upload_.this, "Got Exception : see logcat ", Toast.LENGTH_SHORT).show();

                        }

                    });

                    Log.i(TAGLOG, "[UploadImageToServer] Upload file to server Exception Exception : " + e.getMessage(), e);

                }

                Log.i(TAGLOG, "[UploadImageToServer] Finish");

                return null;

            } // End else block

        }



        @Override

        protected void onPostExecute(String s) {

            mProgressDialog.dismiss();

        }

    }

    // ==========================================================================================



    @Override

    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.iv_upload_image:

                Intent i = new Intent(Intent.ACTION_PICK);

                i.setType(MediaStore.Images.Media.CONTENT_TYPE);

                i.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI); // images on the SD card.



                // 결과를 리턴하는 Activity 호출

                startActivityForResult(i, REQ_CODE_PICK_PICTURE);

                break;

            case R.id.btn_upload_image:

                if (uploadFilePath != null) {
                    UploadImageToServer uploadimagetoserver = new UploadImageToServer();

                    uploadimagetoserver.execute("http://203.255.60.146/UploadToServer.php/uploads");
                    new AsyncLogin().execute(uploadFileName,uploadFileName,uploadFileName);
                } else {

                    Toast.makeText(upload_.this, "You didn't insert any image", Toast.LENGTH_SHORT).show();

                }
                break;

        }

    }
    private class AsyncLogin extends AsyncTask<String,Void,String>
    {
        ProgressDialog pdLoading = new ProgressDialog(upload_.this);
        HttpURLConnection conn;
        URL url = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("\tLoading...");
            pdLoading.setCancelable(false);
            pdLoading.show();

        }
        @Override
        protected String doInBackground(String... params) {
            try {

                // Enter URL address where your php file resides
                url = new URL("http://203.255.60.146/name_.php");

            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return "exception";
            }
            try {
                // Setup HttpURLConnection class to send and receive data from php and mysql
                conn = (HttpURLConnection)url.openConnection();
                conn.setReadTimeout(50000);
                conn.setConnectTimeout(50000);
                conn.setRequestMethod("POST");

                // setDoInput and setDoOutput method depict handling of both send and receive
                conn.setDoInput(true);
                conn.setDoOutput(true);

                // Append parameters to URL
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("email", params[0])
                        .appendQueryParameter("password", params[1])
                        .appendQueryParameter("phoneNumber", params[2]);
                String query = builder.build().getEncodedQuery();

                // Open connection for sending data
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();
                conn.connect();

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                return "exception";
            }

            try {

                int response_code = conn.getResponseCode();

                // Check if successful connection made
                if (response_code == HttpURLConnection.HTTP_OK) {

                    // Read data sent from server
                    InputStream input = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    // Pass data to onPostExecute method
                    return(result.toString());

                }else{

                    return("unsuccessful");
                }

            } catch (IOException e) {
                e.printStackTrace();
                return "exception";
            } finally {
                conn.disconnect();
            }


        }

        @Override
        protected void onPostExecute(String result) {

            //this method will be running on UI thread

            pdLoading.dismiss();

            if(result.equalsIgnoreCase("true"))
            {
                /* Here launching another activity when login successful. If you persist login state
                use sharedPreferences of Android. and logout button to clear sharedPreferences.
                 */

            }else if (result.equalsIgnoreCase("false")){

                // If username and password does not match display a error message
                Toast.makeText(upload_.this, "Invalid email or password", Toast.LENGTH_LONG).show();

            } else if (result.equalsIgnoreCase("exception") || result.equalsIgnoreCase("unsuccessful")) {

                Toast.makeText(upload_.this, "OOPs! Something went wrong. Connection Problem.", Toast.LENGTH_LONG).show();

            }
        }

    }

}
