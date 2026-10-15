package com.company.sms;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;
    private boolean asked = false;
    private static final String[] PERMS = {Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setUserAgentString(ws.getUserAgentString() + " SMMMoonApp");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) { refresh(); }
        });
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");
        try { SmsReceiver.signIn(null); } catch (Exception ignored) { }
    }

    private boolean granted() {
        for (String p : PERMS)
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }

    private void js(String code) { web.post(() -> web.evaluateJavascript(code, null)); }

    private void refresh() {
        js("setState(" + granted() + ")");
        String last = getSharedPreferences("smm", Context.MODE_PRIVATE).getString("last", "");
        js("setLast('" + last.replace("'", "") + "')");
    }

    @Override protected void onResume() { super.onResume(); if (web != null) refresh(); }

    @Override public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        refresh();
        if (!granted()) js("setMsg('Permission দেওয়া হয়নি। Settings থেকে SMS permission Allow করুন।')");
    }

    class Bridge {
        @JavascriptInterface public boolean isOn() { return granted(); }

        @JavascriptInterface public void turnOn() {
            runOnUiThread(() -> {
                if (granted()) { refresh(); return; }
                boolean blocked = asked && !shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS);
                if (blocked) {
                    js("setMsg('Settings > Permissions > SMS Allow korun. Android 13+: App info > ⋮ > Allow restricted settings.')");
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName())));
                } else {
                    asked = true;
                    requestPermissions(PERMS, 1);
                }
            });
        }
    }
}
