/*Author: Abdukl Ghani
 * Website: https://abdulghani.tech
 * Github Repo: https://github.com/abdulghanitech/rpad-ml
 * */
package com.techglows.rpadml;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "RPADML_AccessServ";
    private String currentURL = "";
    private String GoogleApiURL = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=AIzaSyDVhCTR3IWUfteUGVugMEepE235_50TlLY";
    private String ML_URL = "https://rpadml.herokuapp.com/api";

    public RequestQueue queue;
    public boolean networkInit;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if(networkInit){
            //already network request queue initialized
        }else{
            //init network request queue
            queue = Volley.newRequestQueue(this);
        }

        AccessibilityNodeInfo source = event.getSource();

        if (source == null)
            return;

        final String packageName = String.valueOf(source.getPackageName());

        // Add browser package list here (comma seperated values)
        String BROWSER_LIST = "com.android.chrome, com.UCMobile.intl, org.mozilla.firefox, com.instagram.android, com.facebook.katana";

        List<String> browserList
                = Arrays.asList(BROWSER_LIST.split(",\\s*"));
        if (event.getEventType()
                == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (!browserList.contains(packageName)) {
                return;
            }
        }

        if (browserList.contains(packageName)) {
            try {
                // App opened is a browser.
                // Parse urls in browser.
                if (AccessibilityEvent
                        .eventTypeToString(event.getEventType())
                        .contains("TYPE_WINDOW_CONTENT_CHANGED")) {
                    AccessibilityNodeInfo nodeInfo = event.getSource();
                    getUrlsFromViews(nodeInfo);
                }
            } catch(StackOverflowError ex){
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Method to loop through all the views and try to find a URL.
     * @param info
     */
    public void getUrlsFromViews(AccessibilityNodeInfo info) {

        try {
            if (info == null)
                return;

            if (info.getText() != null && info.getText().length() > 0) {

                String capturedText = info.getText().toString();

                if (capturedText.contains("https://")
                        || capturedText.contains("http://") || capturedText.contains("www.")) {
                    if (!currentURL.equals(capturedText)) {
                        // Do something with the url.
                        currentURL = capturedText;
                        Log.d(TAG, "Found URL: "+capturedText);
                        checkURL(currentURL);
                    }

                }
            }

            for (int i = 0; i < info.getChildCount(); i++) {
                AccessibilityNodeInfo child = info.getChild(i);
                getUrlsFromViews(child);
                if(child != null){
                    child.recycle();
                }
            }
        } catch(StackOverflowError ex){
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void checkURL(String url){
        if(url.contains("instagram")){
            url = instagramURLDecoder(url);
        }
        //first check with Google
        checkGoogleSafeBrowsing(url);
        //then go for machine learning
        //showFloatingWindow("https://www.google.com/sjhsfh/fsagfiushf");
    }

    private void showFloatingWindow(String urlToShowinView){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Intent i = new Intent(this, FloatingViewService.class);
            i.putExtra("url",urlToShowinView);
            startService(i);
            //finish();
        } else if (Settings.canDrawOverlays(this)) {
            Intent i = new Intent(this, FloatingViewService.class);
            i.putExtra("url",urlToShowinView);
            startService(i);
            //finish();
        } else {
            //askPermission();
            Log.d(TAG, "You need System Alert Window Permission to do this");
            //Toast.makeText(this, "You need System Alert Window Permission to do this", Toast.LENGTH_SHORT).show();
        }
    }

    private void machineLearningCheck(String urlToCheck){
        try{
            //url
            final String urlToCheck2 = urlToCheck;
            JSONObject urlObject = new JSONObject();
            urlObject.put("url",urlToCheck);
            String url = ML_URL;
            JsonObjectRequest jsonObjectRequest  = new JsonObjectRequest(Request.Method.POST, url, urlObject,
                    new Response.Listener<JSONObject>()
                    {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            Log.d("Response", response.toString());
                            if(response.has("prediction")){
                                Log.d(TAG, "Got Response!!");
                                try{
                                    int Result = response.getInt("prediction");
                                    if(Result == 1){
                                        //phishing site
                                        //show floatUI
                                        showFloatingWindow(urlToCheck2);
                                        Log.d(TAG, "Phishing Site!!");
                                    }
                                }catch(JSONException jsx){
                                    Log.d(TAG, jsx.toString());
                                }

                                //showFloatingWindow();
                            }
                        }
                    },
                    new Response.ErrorListener()
                    {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            Log.d("Error.Response", error.toString());
                        }
                    }
            )
            {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String>  params = new HashMap<String, String>();
                    params.put("Content-Type", "application/json");
                    return params;
                }
            };



            //queue.add(postRequest);
            //jsonObjectRequest.setTag(REQ_TAG);
            queue.add(jsonObjectRequest);

        }catch(Exception ex){

        }
    }

    private void checkGoogleSafeBrowsing(String urlToCheck){
        try{
            final String urlToCheck2 = urlToCheck;
            JSONObject json = getJsonObject(urlToCheck);
            Log.d(TAG, json.toString());
            String url = GoogleApiURL;
            JsonObjectRequest jsonObjectRequest  = new JsonObjectRequest(Request.Method.POST, url, json,
                    new Response.Listener<JSONObject>()
                    {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            Log.d("Response", response.toString());
                            if(response.has("matches")){
                                Log.d(TAG, "Phishing Site!!");
                                showFloatingWindow(urlToCheck2);
                            }else{
                                //check with machine learning API
                                machineLearningCheck(urlToCheck2);
                            }
                        }
                    },
                    new Response.ErrorListener()
                    {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            Log.d("Error.Response", error.toString());
                        }
                    }
            )
            {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String>  params = new HashMap<String, String>();
                    params.put("Content-Type", "application/json");
                    return params;
                }
            };



            //queue.add(postRequest);
            //jsonObjectRequest.setTag(REQ_TAG);
            queue.add(jsonObjectRequest);
        }catch(JSONException jsx){
            Log.d(TAG, jsx.toString());
        }

    }

    private String instagramURLDecoder(String url){
        String mainLink = url;
        try{
            String afterDecode = URLDecoder.decode(url, "UTF-8");
            Uri uri = Uri.parse(afterDecode);
            mainLink = uri.getQueryParameter("u");
            Log.d(TAG, "got intagram main link: "+mainLink);
        }catch(UnsupportedEncodingException unsupp){
            Log.d(TAG, unsupp.toString());
        }
        return mainLink;
    }

    private JSONObject getJsonObject(String url) throws JSONException {
        //main object
        JSONObject jsonObject = new JSONObject();

        //url
        JSONObject urlObject = new JSONObject();
        urlObject.put("url",url);

        List <JSONObject> urlList = new ArrayList<JSONObject>();
        urlList.add(urlObject);

        JSONArray urlArray = new JSONArray(urlList);

        //client
        JSONObject clientObject = new JSONObject();
        JSONObject clientJson = new JSONObject();
        clientJson.put("clientId", "RPADML");
        clientJson.put("clientVersion", "1.5.2");
        //clientObject.put("client", clientJson);

        //threat
        JSONObject threatObject = new JSONObject();
        JSONObject threatJson = new JSONObject();
        threatJson.put("threatTypes","SOCIAL_ENGINEERING");
        threatJson.put("platformTypes","WINDOWS");
        threatJson.put("threatEntryTypes","URL");
        threatJson.put("threatEntries",urlArray);
        //threatObject.put("threatInfo",threatJson);

        //final Object
        jsonObject.put("client",clientJson);
        jsonObject.put("threatInfo",threatJson);


        return jsonObject;
    }

    @Override
    public void onInterrupt() {

    }
}
