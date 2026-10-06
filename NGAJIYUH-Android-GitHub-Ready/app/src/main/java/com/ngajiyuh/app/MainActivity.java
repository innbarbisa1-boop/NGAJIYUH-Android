package com.ngajiyuh.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final String APP_URL = "https://script.google.com/macros/s/AKfycbwNrzz7blEnRRKX27fTCk3IOyXjYLaj5sJn4cg6e6B6Iz2ayct-fazKnUuhGO52wDN5/exec";
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int CAMERA_PERMISSION_REQUEST = 1002;
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        configureWebView();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, CAMERA_PERMISSION_REQUEST);
        }
        if (savedInstanceState == null) webView.loadUrl(APP_URL); else webView.restoreState(savedInstanceState);
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("https://") || url.startsWith("http://")) { view.loadUrl(url); return true; }
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(final PermissionRequest request) { runOnUiThread(() -> request.grant(request.getResources())); }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                try { startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST); return true; }
                catch (Exception e) { filePathCallback = null; Toast.makeText(MainActivity.this, "Tidak dapat membuka pemilih file", Toast.LENGTH_SHORT).show(); return false; }
            }
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int n = data.getClipData().getItemCount(); results = new Uri[n];
                for (int i=0; i<n; i++) results[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data.getData() != null) results = new Uri[]{data.getData()};
        }
        filePathCallback.onReceiveValue(results); filePathCallback = null;
    }

    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onSaveInstanceState(Bundle outState) { webView.saveState(outState); super.onSaveInstanceState(outState); }
    @Override protected void onDestroy() { if (webView != null) { webView.stopLoading(); webView.destroy(); } super.onDestroy(); }
}
