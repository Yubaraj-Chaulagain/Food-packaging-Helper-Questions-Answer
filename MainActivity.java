package com.interview.offline;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.PermissionRequest;
import android.webkit.WebResourceRequest;

public class MainActivity extends Activity {
  WebView web;
  @Override public void onCreate(Bundle b){ super.onCreate(b); web=new WebView(this); setContentView(web);
    WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true); s.setAllowContentAccess(true); s.setMediaPlaybackRequiresUserGesture(false); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false);
    web.setWebViewClient(new WebViewClient()); web.setWebChromeClient(new WebChromeClient(){ @Override public void onPermissionRequest(PermissionRequest r){ r.grant(r.getResources()); }});
    web.loadUrl("file:///android_asset/index.html"); }
  @Override public void onBackPressed(){ if(web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
