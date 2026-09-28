package com.fulkumari.farattendance;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.GeolocationPermissions;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;

    private static final int PERMISSION_CODE = 101;
    private static final int FILE_CHOOSER_CODE = 102;

    private boolean startLocationAfterPermission = false;

    private PermissionRequest pendingCameraRequest;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;

    private ValueCallback<Uri[]> filePathCallback;

    private static final String WEBSITE =
            "https://fulkumari.com.np/Liq/Efs23.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {

            // Website camera permission
            @Override
            public void onPermissionRequest(
                    PermissionRequest request) {

                runOnUiThread(() -> {

                    boolean cameraRequested = false;

                    for (String resource : request.getResources()) {
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE
                                .equals(resource)) {
                            cameraRequested = true;
                        }
                    }

                    if (!cameraRequested) {
                        request.deny();
                        return;
                    }

                    if (checkSelfPermission(
                            Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {

                        grantCameraPermission(request);

                    } else {
                        pendingCameraRequest = request;
                        requestPermissions(
                                new String[]{
                                        Manifest.permission.CAMERA
                                },
                                PERMISSION_CODE
                        );
                    }
                });
            }

            // Website GPS permission
            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin,
                    GeolocationPermissions.Callback callback) {

                if (hasLocationPermission()) {
                    callback.invoke(origin, true, false);
                } else {
                    pendingGeoOrigin = origin;
                    pendingGeoCallback = callback;
                    requestAppPermissions(false);
                }
            }

            // HTML file/photo chooser support
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = callback;

                Intent intent =
                        params.createIntent();

                try {
                    startActivityForResult(
                            intent,
                            FILE_CHOOSER_CODE
                    );
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(
                            MainActivity.this,
                            "File picker खोल्न सकिएन",
                            Toast.LENGTH_LONG
                    ).show();
                    return false;
                }

                return true;
            }
        });

        webView.addJavascriptInterface(
                new LocationBridge(),
                "AndroidLocation"
        );

        webView.loadUrl(WEBSITE);

        // App खुल्दा आवश्यक permission माग्ने
        requestAppPermissions(false);
    }

    private boolean hasLocationPermission() {

        return checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        ||
        checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAppPermissions(
            boolean startLocation) {

        if (startLocation) {
            startLocationAfterPermission = true;
        }

        java.util.ArrayList<String> permissions =
                new java.util.ArrayList<>();

        if (checkSelfPermission(
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            permissions.add(
                    Manifest.permission.CAMERA);
        }

        if (!hasLocationPermission()) {
            permissions.add(
                    Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(
                    Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

            permissions.add(
                    Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissions.isEmpty()) {
            requestPermissions(
                    permissions.toArray(new String[0]),
                    PERMISSION_CODE
            );
        } else {
            handlePendingPermissions();
        }
    }

    private void grantCameraPermission(
            PermissionRequest request) {

        java.util.ArrayList<String> allowed =
                new java.util.ArrayList<>();

        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE
                    .equals(resource)) {
                allowed.add(resource);
            }
        }

        if (!allowed.isEmpty()) {
            request.grant(
                    allowed.toArray(new String[0]));
        } else {
            request.deny();
        }
    }

    private void handlePendingPermissions() {

        // Pending camera request
        if (pendingCameraRequest != null) {

            if (checkSelfPermission(
                    Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {

                grantCameraPermission(
                        pendingCameraRequest);
            } else {
                pendingCameraRequest.deny();
            }

            pendingCameraRequest = null;
        }

        // Pending website GPS request
        if (pendingGeoCallback != null) {

            boolean granted = hasLocationPermission();

            pendingGeoCallback.invoke(
                    pendingGeoOrigin,
                    granted,
                    false
            );

            pendingGeoCallback = null;
            pendingGeoOrigin = null;
        }

        // Pending native live location start
        if (startLocationAfterPermission) {

            startLocationAfterPermission = false;

            if (hasLocationPermission()) {
                startNativeLocationService();
            } else {
                Toast.makeText(
                        this,
                        "Location permission आवश्यक छ",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void startNativeLocationService() {

        if (!hasLocationPermission()) {
            requestAppPermissions(true);
            return;
        }

        Intent intent = new Intent(
                MainActivity.this,
                LocationForegroundService.class
        );

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            startForegroundService(intent);

        } else {
            startService(intent);
        }

        Toast.makeText(
                this,
                "Live Location Service Started",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void stopNativeLocationService() {

        Intent intent = new Intent(
                MainActivity.this,
                LocationForegroundService.class
        );

        intent.setAction("STOP");

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            startForegroundService(intent);
        } else {
            startService(intent);
        }

        Toast.makeText(
                this,
                "Live Location Service Stopped",
                Toast.LENGTH_SHORT
        ).show();
    }

    private class LocationBridge {

        @JavascriptInterface
        public void start() {
            runOnUiThread(() -> {
                startNativeLocationService();
            });
        }

        @JavascriptInterface
        public void stop() {
            runOnUiThread(() -> {
                stopNativeLocationService();
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == PERMISSION_CODE) {
            handlePendingPermissions();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == FILE_CHOOSER_CODE) {

            if (filePathCallback != null) {

                Uri[] results = null;

                if (resultCode == RESULT_OK &&
                        data != null) {

                    Uri uri = data.getData();

                    if (uri != null) {
                        results = new Uri[]{uri};
                    }
                }

                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        }
    }

    @Override
    public void onBackPressed() {

        if (webView != null &&
                webView.canGoBack()) {

            webView.goBack();

        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
}
