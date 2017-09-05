package com.elsonsmith.miningpoolhubmusicoinstats;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MPHMusicoinStats extends AppCompatActivity {

    private static final String TAG = MPHMusicoinStats.class.getSimpleName();
    Double MCcurrentPrice;
    String APIKey;

    //HTTP REST API
    private class LongOperation extends AsyncTask<String, Void, Integer> {
        Double MCUserHashrate, MCUserRecentCreds, MCUserConfBal, MCUserUnConfBal;
        Double MC24HrProfits, MC30DayProfits, MC365DayProfits;
        long MCcurrentDiff;
        JSONObject jsonObjUserObjects;
        int responseCode = 0;
        int isInitDone = 0;
        @Override
        protected Integer doInBackground(String... params) {
            try {
                for (Integer i = 0; i < params.length; i++) {
                    URL url = new URL(params[i]);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    try {
                        con.setRequestMethod("GET");
                        con.setReadTimeout(5000);
                        con.setConnectTimeout(5000);
                        con.connect();
                        StringBuilder sb = new StringBuilder();
                        InputStreamReader in = new InputStreamReader(con.getInputStream(), Charset.defaultCharset());
                        BufferedReader bufferedReader = new BufferedReader(in);
                        if (bufferedReader != null) {
                            int cp;
                            while ((cp = bufferedReader.read()) != -1) {
                                sb.append((char) cp);
                            }
                            bufferedReader.close();
                        }
                        responseCode = con.getResponseCode();
                        //Log.d(TAG, Integer.toString(con.getResponseCode()));
                        //Log.d(TAG, con.getResponseMessage());
                        //Log.d(TAG, sb.toString());
                        if (params[i].contains("getuserworkers")) {
                            isInitDone = 2;
                            jsonObjUserObjects = new JSONObject(sb.toString());
                        }
                        else {
                            switch (params[i]) {
                                case "https://api.coinmarketcap.com/v1/ticker/musicoin/?convert=USD":
                                    try {
                                        JSONArray jsonObj = new JSONArray(sb.toString());
                                        MCcurrentPrice = jsonObj.getJSONObject(0).getDouble("price_usd");
                                        MCcurrentPrice = Math.round(MCcurrentPrice * 10000.0) / 10000.0;
                                    } catch (Exception ex) {
                                    }
                                    break;
                                case "https://whattomine.com/coins/178.json":
                                    try {
                                        JSONObject jsonObj = new JSONObject(sb.toString());
                                        MCcurrentDiff = Math.round(jsonObj.getLong("difficulty") / 1000000.0);
                                    } catch (Exception ex2) {
                                    }
                                    break;
                                default:
                                    isInitDone = 1;
                                    jsonObjUserObjects = new JSONObject(sb.toString());
                                    break;
                            }
                        }
                        //System.out.println(MCcurrentPrice);
                    } catch (Exception e) {
                        System.out.println(e);
                    } finally {
                        con.disconnect();
                    }
                }
            }
            catch (Exception e) { }
            return responseCode;
        }

        protected void onPostExecute(Integer result) {
            if (responseCode != 200) {
                changeUserObjects(null);
                changeUserWorkers(null);
                this.cancel(true);
            }
            else if (isInitDone == 0) {
                changePriceMethod(MCcurrentPrice, MCcurrentDiff);
            }
            else if (isInitDone == 1) {
                changeUserObjects(jsonObjUserObjects);
            }
            else if (isInitDone == 2) {
                changeUserWorkers(jsonObjUserObjects);
                changeUserWorkers(jsonObjUserObjects); //dunno why after changing the API key the first one doesn't grab all workers...so we call it again. Meh.
            }
        }

        @Override
        protected void onPreExecute() {

        }

        protected void onProgressUpdate(Void... values) {}
    }

    private void changePriceMethod(double MCcurrentPrice, long MCcurrentDiff) {
        final TextView textViewToChange = (TextView)findViewById(R.id.textView7);
        String MCCDFinal = NumberFormat.getNumberInstance(Locale.US).format(MCcurrentDiff);
        textViewToChange.setText("$"+MCcurrentPrice+" per MUSIC coin @\n"+MCCDFinal+"M difficulty");
    }
    private void changeUserWorkers(JSONObject userObjects) {
        final TextView multilineText = (TextView)findViewById(R.id.textView_workers);
        multilineText.setText("\n");
        ArrayList<String> hashrateObj = new ArrayList<String>();
        ArrayList<String> titleObj = new ArrayList<String>();
        try {
            JSONArray userWorkerDataArray = userObjects.getJSONObject("getuserworkers").getJSONArray("data");
            for (int i = 0; i < userWorkerDataArray.length(); i++) {
                JSONObject value = userWorkerDataArray.getJSONObject(i);
                String isActiveWorker = value.getString("monitor");
                //System.out.println(isActiveWorker);
                if (isActiveWorker.equals("0")) {
                    continue;
                }
                else {
                    String component = value.getString("username");
                    Double component2 = value.getDouble("hashrate");
                    component2 = Math.round((component2 / 1000) * 100.0) / 100.0;
                    multilineText.append(component + "\n");
                    multilineText.append(component2.toString() + " MH/s\n\n");
                }
            }
        }
        catch (Exception ex) {
            System.out.println("TEST FAIL: " + ex);
        }
        /*
        ArrayList<String> hashrateObj = new ArrayList<String>();
        ArrayList<String> titleObj = new ArrayList<String>();
        try {
            JSONArray userWorkerDataArray = userObjects.getJSONObject("getuserworkers").getJSONArray("data");
            for (int i = 0; i < userWorkerDataArray.length(); i++) {
                JSONObject value = userWorkerDataArray.getJSONObject(i);
                String isActiveWorker = value.getString("monitor");
                System.out.println(isActiveWorker);
                if (isActiveWorker.equals("0")) {
                    continue;
                }
                else {
                    String component = value.getString("username");
                    titleObj.add(component);
                    Double component2 = value.getDouble("hashrate");
                    component2 = Math.round((component2 / 1000) * 100.0) / 100.0;
                    hashrateObj.add(component2.toString() + " MH/s");
                }
            }
        }
        catch (Exception ex) { System.out.println("TEST FAIL: " + ex); }
        ListView lv = (ListView)findViewById(listview_workers);
        String[] titleArray = titleObj.toArray(new String[titleObj.size()]);
        String[] subItemArray = hashrateObj.toArray(new String[hashrateObj.size()]);
        ArrayList<HashMap<String, String>> data;
        data = new ArrayList<HashMap<String, String>>();
        for(int i=0;i<titleArray.length;i++){
            HashMap<String,String> datum = new HashMap<String, String>();
            datum.put("RouterName", titleArray[i]);
            datum.put("RouterIP", subItemArray[i]);
            data.add(datum);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2, new String[] {"RouterName", "RouterIP"}, new int[] {android.R.id.text1, android.R.id.text2});
        lv.setAdapter(adapter);
        */
    }

    public void inputAPIKey ()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView textView = new TextView(this);
        textView.setText("Please input your MiningPoolHub Musicoin API Key");
        textView.setPadding(20,10,20,10); textView.setTextColor(Color.BLACK); textView.setTextSize(18); //textView.setTypeface(null,BOLD);
        builder.setCustomTitle(textView);
        // Set up the input
        final EditText input = new EditText(this);
        input.setText(APIKey);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        builder.setView(input);
        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                APIKey = input.getText().toString();
                new LongOperation().execute("https://musicoin.miningpoolhub.com/index.php?page=api&action=getdashboarddata&api_key="+APIKey);
                new LongOperation().execute("https://musicoin.miningpoolhub.com/index.php?page=api&action=getuserworkers&api_key="+APIKey);
                try {
                    File temp = new File("data/data/" + getPackageName() + "/mphmcoinAPIKey.txt");
                    BufferedWriter bw = new BufferedWriter(new FileWriter(temp)); bw.write(APIKey); bw.close();
                    String tempPathAbsolute = temp.getAbsolutePath();
                }
                catch (Exception ex) {}
                //Toast.makeText(getApplicationContext(), "Receiver IP (" + APIKey + ") successfully assigned.", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
        return;
    }

    private void changeUserObjects(JSONObject userObjects) {
        final TextView textViewToChange = (TextView)findViewById(R.id.user_hashrate);
        final TextView textViewToChange2 = (TextView)findViewById(R.id.user_recentCreds);
        final TextView textViewToChange3 = (TextView)findViewById(R.id.user_confirmedBal);
        final TextView textViewToChange4 = (TextView)findViewById(R.id.user_unconfirmedBal);
        final TextView textViewToChange5 = (TextView)findViewById(R.id.credits24hrs);
        final TextView textViewToChange6 = (TextView)findViewById(R.id.credits30Days);
        final TextView textViewToChange7 = (TextView)findViewById(R.id.creditsYearly);
        //String MCCDFinal = NumberFormat.getNumberInstance(Locale.US).format(MCcurrentDiff);
        double userHashrate, userRecentCreds, userConfirmed, userUnConfirmed, credits24hrs, creditsmonthly, creditsyearly;
        userHashrate = userRecentCreds = userConfirmed = userUnConfirmed = credits24hrs = creditsmonthly = creditsyearly = 0;
        try {
            userHashrate = userObjects.getJSONObject("getdashboarddata").getJSONObject("data").getJSONObject("personal").getDouble("hashrate");
            userRecentCreds = userObjects.getJSONObject("getdashboarddata").getJSONObject("data").getJSONObject("recent_credits_24hours").getDouble("amount");
            userConfirmed = userObjects.getJSONObject("getdashboarddata").getJSONObject("data").getJSONObject("balance").getDouble("confirmed");
            userUnConfirmed = userObjects.getJSONObject("getdashboarddata").getJSONObject("data").getJSONObject("balance").getDouble("unconfirmed");
            userHashrate = Math.round(userHashrate * 100.0) / 100.0;
            userRecentCreds = Math.round(userRecentCreds * 100.0) / 100.0;
            userConfirmed = Math.round(userConfirmed * 100.0) / 100.0;
            userUnConfirmed = Math.round(userUnConfirmed * 100.0) / 100.0;
            credits24hrs = Math.round((userRecentCreds * MCcurrentPrice) * 100.0) / 100.0;
            creditsmonthly = Math.round(((userRecentCreds * MCcurrentPrice)*30) * 100.0) / 100.0;
            creditsyearly = Math.round(((userRecentCreds * MCcurrentPrice)*365) * 100.0) / 100.0;
        }
        catch (Exception ex) { System.out.println("TEST FAIL: " + ex); }
        textViewToChange.setText(userHashrate+ " MH/s");
        textViewToChange2.setText(userRecentCreds+ "");
        textViewToChange3.setText(userConfirmed+ "");
        textViewToChange4.setText(userUnConfirmed+ "");
        textViewToChange5.setText("$"+credits24hrs);
        textViewToChange6.setText("$"+creditsmonthly);
        textViewToChange7.setText("$"+creditsyearly);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //get already exising API key
        try {
            File temp = new File("data/data/" + getPackageName() + "/mphmcoinAPIKey.txt");
            String tempPathAbsolute = temp.getAbsolutePath();
            StringBuilder text = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(temp));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            br.close();
            APIKey = text.toString();
        }
        catch (Exception ex) {}
        //start
        new LongOperation().execute("https://api.coinmarketcap.com/v1/ticker/musicoin/?convert=USD", "https://whattomine.com/coins/178.json");
        new LongOperation().execute("https://musicoin.miningpoolhub.com/index.php?page=api&action=getdashboarddata&api_key="+APIKey);
        new LongOperation().execute("https://musicoin.miningpoolhub.com/index.php?page=api&action=getuserworkers&api_key="+APIKey);
        //superInit
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mphmusicoin_stats);
        //FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                inputAPIKey();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                                break;
                        }
                    }
                };
                builder.setMessage("Settings").setPositiveButton("Set API Key", dialogClickListener).setNegativeButton("Refresh", dialogClickListener);
                builder.show();
            }
        });
    }
}