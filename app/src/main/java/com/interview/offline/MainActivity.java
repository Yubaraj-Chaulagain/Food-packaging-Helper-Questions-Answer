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

        // HTML बाट Android microphone call गर्न
        webView.addJavascriptInterface(
                new AndroidVoiceBridge(),
                "AndroidVoice"
        );

        webView.loadUrl("file:///android_asset/index.html");
    }

    /**
     * JavaScript → Android microphone bridge
     *
     * HTML बाट:
     * AndroidVoice.startVoiceSearch("search")
     * वा
     * AndroidVoice.startVoiceSearch("answer")
     * call हुन्छ।
     */
    private class AndroidVoiceBridge {

        @JavascriptInterface
        public void startVoiceSearch(String mode) {

            runOnUiThread(() -> {

                if (mode == null || mode.trim().isEmpty()) {
                    currentMode = "search";
                } else {
                    currentMode = mode;
                }

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

    /**
     * Android speech recognition खोल्ने
     */
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
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    Locale.US.toLanguageTag()
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    currentMode.equals("answer")
                            ? "Speak your answer"
                            : "Speak your search"
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    1
            );

            if (intent.resolveActivity(getPackageManager()) == null) {

                sendVoiceResult(
                        "",
                        "Speech recognition service is not available on this device."
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

    /**
     * Microphone permission result
     */
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

    /**
     * Speech recognition result
     */
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

                    String transcript = results.get(0);

                    sendVoiceResult(
                            transcript,
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

    /**
     * Android बाट HTML मा voice result पठाउने
     */
    private void sendVoiceResult(
            String transcript,
            String error
    ) {

        if (webView == null) {
            return;
        }

        String safeTranscript =
                JSONObject.quote(
                        transcript == null ? "" : transcript
                );

        String safeError =
                JSONObject.quote(
                        error == null ? "" : error
                );

        String safeMode =
                JSONObject.quote(
                        currentMode == null ? "search" : currentMode
                );

        String script =
                "if(typeof window.onNativeVoiceResult === 'function'){" +
                        "window.onNativeVoiceResult(" +
                        safeMode + "," +
                        safeTranscript + "," +
                        safeError +
                        ");" +
                        "}";

        runOnUiThread(() ->
                webView.evaluateJavascript(
                        script,
                        null
                )
        );
    }

    /**
     * Android Back button
     */
    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    /**
     * WebView cleanup
     */
    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.removeJavascriptInterface(
                    "AndroidVoice"
            );

            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
