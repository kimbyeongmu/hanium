package com.lge.friendsCamera;


import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.lge.friendsCamera.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class user_page extends Activity {

    String myJSON;

    private static final String TAG_RESULTS="result";
    private static final String TAG_NAME = "email";
    private static final String TAG_PWD = "password";
    private static final String TAG_PNUM ="phoneNumber";

    JSONArray peoples = null;

    ArrayList<HashMap<String, String>> personList;

    ListView list;
    public void back_user(View v) {
        Intent intent = new Intent(getApplicationContext(),login_main.class);
        startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_page);
        list = (ListView) findViewById(R.id.listView);
        personList = new ArrayList<HashMap<String,String>>();
        getData("http://203.255.60.146/getdata.php");
    }


    protected void showList(){
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            peoples = jsonObj.getJSONArray(TAG_RESULTS);

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
            }

            ListAdapter adapter = new SimpleAdapter(
                    user_page.this, personList, R.layout.list_item,
                    new String[]{TAG_NAME,TAG_PWD,TAG_PNUM},
                    new int[]{R.id.email, R.id.password, R.id.phoneNumber}
            );

            list.setAdapter(adapter);

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

