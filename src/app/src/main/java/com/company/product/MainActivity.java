package com.company.product;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // if your website starts with www, exclude it
    private static final String myWebSite = "example.com";

    WebView webView;
    ProgressDialog progressDialog;

    // for handling file upload, set a static value, any number you like
    // this value will be used by WebChromeClient during file upload
    private static final int file_chooser_activity_code = 1;
    private static ValueCallback<Uri[]> mUploadMessageArr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize the progressDialog
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setCancelable(true);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        // get the webview from the layout
        webView = findViewById(R.id.webView);

        // for handling Android Device [Back] key press
        webView.canGoBackOrForward(99);

        // handling web page browsing mechanism
        webView.setWebViewClient(new myWebViewClient());

        // handling file upload mechanism
        webView.setWebChromeClient(new myWebChromeClient());

        // some other settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setUserAgentString(new WebView(this).getSettings().getUserAgentString());

        // set the downlaod listner
        webView.setDownloadListener(downloadListener);

        // load the website
        webView.loadUrl("https://" + myWebSite);
    }

    // after the file chosen handled, variables are returned back to MainActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // check if the chrome activity is a file choosing session
        if (requestCode == file_chooser_activity_code) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri[] results = null;

                // Check if response is a multiple choice selection containing the results
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    // Response is a single choice selection
                    results = new Uri[]{data.getData()};
                }

                mUploadMessageArr.onReceiveValue(results);
                mUploadMessageArr = null;
            } else {
                mUploadMessageArr.onReceiveValue(null);
                mUploadMessageArr = null;
                Toast.makeText(MainActivity.this, "Error getting file", Toast.LENGTH_LONG).show();
            }
        }
    }

    class myWebViewClient extends android.webkit.WebViewClient {

//        // ==============================
//        // Restricting app to only visit links of my website
//        // https://stackoverflow.com/a/38484061/520848
//
//        // for handling api lower than 24
//        @SuppressWarnings("deprecation")
//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view, String url) {
//            final Uri uri = Uri.parse(url);
//            return handleUri(uri);
//        }
//
//        // for handling api higher than 24
//        @TargetApi(Build.VERSION_CODES.N)
//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//            final Uri uri = request.getUrl();
//            return handleUri(uri);
//        }
//
//        private boolean handleUri(final Uri uri) {
//            //Log.i(TAG, "Uri =" + uri);
//            final String host = uri.getHost();
//            final String scheme = uri.getScheme();
//            // Based on some condition you need to determine if you are going to load the url
//            // in your web view itself or in a browser.
//            // You can use `host` or `scheme` or any part of the `uri` to decide.
//            if (host == myWebSite) {
//                // Returning false means that you are going to load this url in the webView itself
//                return false;
//            } else {
//                // Returning true means that you need to handle what to do with the url
//                // e.g. open web page in a Browser
//                final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                startActivity(intent);
//                return true;
//            }
//        }
//
//        // End - Restricting app to only visit links of my website
//        // ==============================

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            //showing the progress bar once the page has started loading
            progressDialog.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // hide the progress bar once the page has loaded
            progressDialog.dismiss();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            // show the error message = no internet access
            webView.loadUrl("file:///android_asset/no_internet.html");
            // hide the progress bar on error in loading
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), "Internet issue", Toast.LENGTH_SHORT).show();
        }
    }

    // Calling WebChromeClient to select files from the device
    public class myWebChromeClient extends WebChromeClient {
        @SuppressLint("NewApi")
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> valueCallback, FileChooserParams fileChooserParams) {

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // set single file type, e.g. "image/*" for images
            intent.setType("*/*");

            // set multiple file types
            String[] mimeTypes = {"image/*", "application/pdf"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

            Intent chooserIntent = Intent.createChooser(intent, "Choose file");
            ((Activity) webView.getContext()).startActivityForResult(chooserIntent, file_chooser_activity_code);

            // Save the callback for handling the selected file
            mUploadMessageArr = valueCallback;

            return true;
        }
    }

    DownloadListener downloadListener = new DownloadListener() {
        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

            progressDialog.dismiss();
            Intent i = new Intent(Intent.ACTION_VIEW);

            // example of URL = https://www.example.com/invoice.pdf
            i.setData(Uri.parse(url));
            startActivity(i);
        }
    };

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack();
        } else {
            finish();
        }
    }
}