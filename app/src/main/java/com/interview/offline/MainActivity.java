
package com.interview.offline;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private WebView webView;

    private static final int REQUEST_MICROPHONE = 101;
    private static final int REQUEST_SPEECH = 102;

    private String currentMode = "search";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient());

        webView.addJavascriptInterface(
            new AndroidVoiceBridge(),
            "AndroidVoice"
        );

        webView.loadUrl("file:///android_asset/index.html");
    }

    private class AndroidVoiceBridge {

        @JavascriptInterface
        public void startVoiceSearch(String mode) {

            runOnUiThread(() -> {

                currentMode = mode;

                if (ContextCompat.checkSelfPermission(
                    MainActivity.this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{
                            Manifest.permission.RECORD_AUDIO
                        },
                        REQUEST_MICROPHONE
                    );

                } else {
                    startSpeechRecognition();
                }
            });
        }
    }

    private void startSpeechRecognition() {

        try {

            Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            );

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.US.toLanguageTag()
            );

            intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Speak now"
            );

            intent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                1
            );

            if (intent.resolveActivity(getPackageManager()) == null) {
                sendVoiceResult(
                    "",
                    "Speech recognition service is not available."
                );
                return;
            }

            startActivityForResult(
                intent,
                REQUEST_SPEECH
            );

        } catch (Exception e) {

            sendVoiceResult(
                "",
                "Unable to start voice recognition."
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        );

        if (requestCode == REQUEST_MICROPHONE) {

            if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startSpeechRecognition();

            } else {

                sendVoiceResult(
                    "",
                    "Microphone permission denied."
                );

                Toast.makeText(
                    this,
                    "Please allow microphone permission.",
                    Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        );

        if (requestCode == REQUEST_SPEECH) {

            if (resultCode == RESULT_OK && data != null) {

                ArrayList<String> results =
                    data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    );

                if (results != null && !results.isEmpty()) {

                    sendVoiceResult(
                        results.get(0),
                        ""
                    );

                } else {

                    sendVoiceResult(
                        "",
                        "No speech detected."
                    );
                }

            } else {

                sendVoiceResult(
                    "",
                    "Voice input cancelled or unavailable."
                );
            }
        }
    }

    private void sendVoiceResult(
        String transcript,
        String error
    ) {

        if (webView == null) return;

        String safeTranscript =
            JSONObject.quote(transcript);

        String safeError =
            JSONObject.quote(error);

        String safeMode = JSONObject.quote(currentMode);

String script =
    "if(window.onNativeVoiceResult){" +
    "window.onNativeVoiceResult(" +
    safeMode + "," +
    safeTranscript + "," +
    safeError +
    ");}";

        runOnUiThread(() ->
            webView.evaluateJavascript(script, null)
        );
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
