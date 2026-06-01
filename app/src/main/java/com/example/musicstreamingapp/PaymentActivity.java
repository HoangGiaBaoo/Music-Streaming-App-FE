package com.example.musicstreamingapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicstreamingapp.databinding.ActivityPaymentBinding;
import com.example.musicstreamingapp.network.RetrofitClient;

/**
 * WebView mở trang thanh toán VNPay sandbox.
 * Backend redirect 302 → musicapp://payment/result?status=success|failed khi xong.
 * Activity bắt deep link đó, trả status về cho PremiumPlansActivity rồi finish.
 *
 * Lưu ý: backend cấu hình vnp_ReturnUrl = http://localhost:8080/... nhưng "localhost" trỏ về
 * chính thiết bị (máy ảo hoặc điện thoại) → ERR_CONNECTION_REFUSED. Ta rewrite host
 * localhost/127.0.0.1 → host của RetrofitClient.BASE_URL (10.0.2.2 cho máy ảo, IP LAN cho máy
 * thật) để WebView gọi đúng backend mà cả app đang dùng.
 */
public class PaymentActivity extends AppCompatActivity {

    private static final String EXTRA_PAY_URL = "extra_pay_url";
    public static final String EXTRA_STATUS = "status";

    private static final String RESULT_SCHEME = "musicapp";
    private static final String RESULT_HOST = "payment";

    private static final String LOCAL_HOST = "localhost";
    private static final String LOCAL_IP = "127.0.0.1";

    /** Authority (host[:port]) của backend mà app đang dùng, lấy từ RetrofitClient.BASE_URL. */
    private static final String BACKEND_AUTHORITY = Uri.parse(RetrofitClient.BASE_URL).getAuthority();

    private ActivityPaymentBinding b;

    public static Intent newIntent(Context ctx, String payUrl) {
        Intent i = new Intent(ctx, PaymentActivity.class);
        i.putExtra(EXTRA_PAY_URL, payUrl);
        return i;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        String payUrl = getIntent().getStringExtra(EXTRA_PAY_URL);
        if (payUrl == null || payUrl.isEmpty()) {
            finish();
            return;
        }

        b.webView.getSettings().setJavaScriptEnabled(true);
        b.webView.getSettings().setDomStorageEnabled(true);
        b.webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(view, request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(view, Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                // Fallback: vài API level không gọi shouldOverrideUrlLoading cho redirect/scheme lạ.
                if (handleUrl(view, Uri.parse(url))) return;
                b.progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                b.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Bắt ERR_CONNECTION_REFUSED khi redirect tới localhost → thử lại với 10.0.2.2.
                if (request != null && request.isForMainFrame()) {
                    Uri rewritten = rewriteLocalhost(request.getUrl());
                    if (rewritten != null) {
                        view.loadUrl(rewritten.toString());
                        return;
                    }
                }
                super.onReceivedError(view, request, error);
            }
        });

        b.webView.loadUrl(payUrl);
    }

    /**
     * Xử lý mọi URL điều hướng của WebView.
     * @return true nếu đã xử lý (deep link kết quả, hoặc đã rewrite localhost) → không load tiếp URL gốc.
     */
    private boolean handleUrl(WebView view, @Nullable Uri uri) {
        if (uri == null) return false;

        // 1. Deep link kết quả thanh toán.
        if (RESULT_SCHEME.equals(uri.getScheme()) && RESULT_HOST.equals(uri.getHost())) {
            String status = uri.getQueryParameter("status");
            Intent data = new Intent();
            data.putExtra(EXTRA_STATUS, status);
            setResult(RESULT_OK, data);
            finish();
            return true;
        }

        // 2. Rewrite localhost → 10.0.2.2 để WebView trong emulator gọi được backend.
        Uri rewritten = rewriteLocalhost(uri);
        if (rewritten != null) {
            view.loadUrl(rewritten.toString());
            return true;
        }

        return false;
    }

    /**
     * @return Uri đã đổi host localhost/127.0.0.1 → host:port của backend app đang dùng
     *         (giữ nguyên path + query), hoặc null nếu uri không phải http(s) localhost.
     */
    @Nullable
    private Uri rewriteLocalhost(@Nullable Uri uri) {
        if (uri == null || BACKEND_AUTHORITY == null) return null;
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || !scheme.startsWith("http")) return null;
        if (!LOCAL_HOST.equals(host) && !LOCAL_IP.equals(host)) return null;

        return uri.buildUpon().encodedAuthority(BACKEND_AUTHORITY).build();
    }

    @Override
    protected void onDestroy() {
        if (b != null && b.webView != null) {
            b.webView.destroy();
        }
        super.onDestroy();
    }
}
