package net.tunts.webintent;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.apache.cordova.CordovaActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;

/**
 * WebIntent is a PhoneGap plugin that bridges Android intents and web applications:
 * 
 * 1. web apps can spawn intents that call native Android applications. 2. (after setting up correct intent filters for
 * PhoneGap applications), Android intents can be handled by PhoneGap web applications.
 * 
 * @author boris@borismus.com
 * 
 */
public class WebIntent extends CordovaPlugin {

    private CallbackContext onNewIntentCallback = null;
    private Intent newestIntent;

    /**
     * Executes the request and returns PluginResult.
     * 
     * @param action The action to execute.
     * @param args JSONArray of arguments for the plugin.
     * @param callbackContext The callbackContext used when calling back into JavaScript.
     * @return boolean
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        try {
            if (action.equals("startActivity")) {
                if (args.length() != 1) {
                    PluginResult res = new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(res);
                    return false;
                }

                // Parse the arguments
                JSONObject obj = args.getJSONObject(0);
                String type = obj.has("type") ? obj.getString("type") : null;
                Uri uri = obj.has("url") ? Uri.parse(obj.getString("url")) : null;
                JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
                JSONObject handler = obj.has("handler") ? obj.getJSONObject("handler") : null;
                Map<String, String> extrasMap = new HashMap<String, String>();
                Map<String, String> handlerMap = null;

                // Populate the extras if any exist
                if (extras != null) {
                    JSONArray extraNames = extras.names();
                    for (int i = 0; i < extraNames.length(); i++) {
                        String key = extraNames.getString(i);
                        String value = extras.getString(key);
                        extrasMap.put(key, value);
                    }
                }

                if (handler != null) {
                    handlerMap = new HashMap<String, String>();

                    handlerMap.put("packageName", handler.getString("packageName"));
                    handlerMap.put("className", handler.getString("className"));
                }

                try{
                    startActivity(obj.getString("action"), uri, type, extrasMap, handlerMap);
                } catch (ActivityNotFoundException e){
                    callbackContext.error(e.getMessage());
                    return false;
                }

                callbackContext.success();
                return true;

            } else if (action.equals("hasExtra")) {
                if (args.length() != 1) {
                    PluginResult res = new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(res);
                    return false;
                }
                Intent i = getIntent();
                String extraName = args.getString(0);
                PluginResult res = new PluginResult(PluginResult.Status.OK, i.hasExtra(extraName));
                callbackContext.sendPluginResult(res);
                return true;

            } else if (action.equals("getExtra")) {
                if (args.length() != 1) {
                    PluginResult res = new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(res);
                    return false;
                }
                Intent i = getIntent();
                String extraName = args.getString(0);

                if (i.hasExtra(extraName)) {
                    String r = i.getStringExtra(extraName);
                    if (null == r) {
                        Uri uri = ((Uri) i.getParcelableExtra(extraName));

                        if(uri != null) {
                            r = uri.toString();
                        }

                        Log.d("CORDOVA", "GetExtra");

                        if(null == r) {
                            ArrayList<Uri> list = i.getParcelableArrayListExtra(extraName);

                            JSONArray arr = new JSONArray();
                            for(Uri u : list) {
                                arr.put(u.toString());
                            }

                            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, arr));
                            return true;
                        }
                    }

                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, r));
                    return true;
                } else {

                    Log.d("CORDOVA", "Does not have extra.");

                    PluginResult res = new PluginResult(PluginResult.Status.ERROR);
                    callbackContext.sendPluginResult(res);
                    return false;
                }

            } else if (action.equals("getUri")) {
                if (args.length() != 0) {
                    PluginResult res = new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(res);
                    return false;
                }

                Intent i = getIntent();
                String uri = i.getDataString();

                callbackContext.success(uri);
                return true;

            } else if (action.equals("onNewIntent")) {
                if (args.length() != 0) {
                    PluginResult res = new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(res);
                    return false;
                }

                this.onNewIntentCallback = callbackContext;
                PluginResult res = new PluginResult(PluginResult.Status.NO_RESULT);
                res.setKeepCallback(true);
                callbackContext.sendPluginResult(res);
                return true;

            } else if (action.equals("sendBroadcast")) {
                if (args.length() != 1) {
                    PluginResult res = new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(res);
                    return false;
                }

                // Parse the arguments
                JSONObject obj = args.getJSONObject(0);

                JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
                Map<String, String> extrasMap = new HashMap<String, String>();

                // Populate the extras if any exist
                if (extras != null) {
                    JSONArray extraNames = extras.names();
                    for (int i = 0; i < extraNames.length(); i++) {
                        String key = extraNames.getString(i);
                        String value = extras.getString(key);
                        extrasMap.put(key, value);
                    }
                }

                sendBroadcast(obj.getString("action"), extrasMap);
                callbackContext.success();
                return true;

            }

            PluginResult res = new PluginResult(PluginResult.Status.INVALID_ACTION);
            callbackContext.sendPluginResult(res);
            return false;

        }
        catch (JSONException e) {
            callbackContext.error(e.getMessage());
            return false;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        this.newestIntent = intent;
        if (this.onNewIntentCallback != null) {
            this.onNewIntentCallback.success(intent.getDataString());
        }
    }

    Intent getIntent() {
        if(this.newestIntent != null) {
            return this.newestIntent;
        }

        return ((CordovaActivity) this.cordova.getActivity()).getIntent();
    }

    void startActivity(String action, Uri uri, String type, Map<String, String> extras, Map<String, String> handlerMap) {
        Intent i = (uri != null ? new Intent(action, uri) : new Intent(action));

        if (handlerMap != null) {
            i.setClassName(handlerMap.get("packageName"), handlerMap.get("className"));
        }

        if (type != null && uri != null) {
            i.setDataAndType(uri, type); // Fix the crash problem with android 2.3.6
        } else {
            if (type != null) {
                i.setType(type);
            }
        }

        for (String key : extras.keySet()) {
            String value = extras.get(key);
            // If type is text html, the extra text must sent as HTML
            if (key.equals(Intent.EXTRA_TEXT) && type.equals("text/html")) {
                i.putExtra(key, Html.fromHtml(value));
            } else if (key.equals(Intent.EXTRA_STREAM)) {
                // allowes sharing of images as attachments.
                // value in this case should be a URI of a file
                i.putExtra(key, Uri.parse(value));
            } else if (key.equals(Intent.EXTRA_EMAIL)) {
                // allows to add the email address of the receiver
                i.putExtra(Intent.EXTRA_EMAIL, new String[] { value });
            } else {
                i.putExtra(key, value);
            }
        }
        ((CordovaActivity) this.cordova.getActivity()).startActivity(i);
    }

    void sendBroadcast(String action, Map<String, String> extras) {
        Intent intent = new Intent();
        intent.setAction(action);
        for (String key : extras.keySet()) {
            String value = extras.get(key);
            intent.putExtra(key, value);
        }

        ((CordovaActivity) this.cordova.getActivity()).sendBroadcast(intent);
    }
}
