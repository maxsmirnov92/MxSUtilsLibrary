package net.maxsmr.commonutils.gui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.CallSuper;

import net.maxsmr.commonutils.Observable;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.maxsmr.commonutils.SdkVersionsKt.isAtLeastLollipop;
import static net.maxsmr.commonutils.SdkVersionsKt.isAtLeastMarshmallow;

public class TouchWebView extends WebView implements Handler.Callback {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(TouchWebView.class);

    private static final int CLICK_ON_WEBVIEW = 1;
    private static final int CLICK_ON_URL = 2;

    @NotNull
    private final Handler webViewHandler = new Handler(Looper.getMainLooper(), this);

    @NotNull
    private final TouchWebView.PageLoadObservable pageLoadObservable = new TouchWebView.PageLoadObservable();

    @NotNull
    private final TouchWebView.ScrollChangeObservable scrollChangeObservable = new TouchWebView.ScrollChangeObservable();

    @Nullable
    private TouchWebView.TouchWebViewClient webViewClient;

    @Nullable
    private TouchWebView.TouchWebChromeClient webChromeClient;

    @Nullable
    private OnClickListener clickListener;

    @Nullable
    private BaseInputConnection inputConnection;

    public TouchWebView(Context context) {
        this(context, null);
    }

    public TouchWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
//        gestureDetector = new GestureDetector(context, new GestureListener());
        setWebViewClient(new TouchWebView.TouchWebViewClient());
        setWebChromeClient(new TouchWebView.TouchWebChromeClient());
    }

    @Nullable
    public BaseInputConnection getInputConnection() {
        return inputConnection;
    }

    public void setInputConnection(@Nullable BaseInputConnection inputConnection) {
        this.inputConnection = inputConnection;
    }

    @NotNull
    public Handler getWebViewHandler() {
        return webViewHandler;
    }

    public void addPageLoadListener(@NotNull TouchWebView.OnPageLoadListener pageLoadListener) {
        pageLoadObservable.registerObserver(pageLoadListener);
    }

    public void removePageLoadListener(@NotNull TouchWebView.OnPageLoadListener pageLoadListener) {
        pageLoadObservable.unregisterObserver(pageLoadListener);
    }

    public void addScrollChangeListener(@NotNull TouchWebView.OnScrollChangeListener scrollChangeListener) {
        scrollChangeObservable.registerObserver(scrollChangeListener);
    }

    public void removeScrollChangeListener(@NotNull TouchWebView.OnScrollChangeListener scrollChangeListener) {
        scrollChangeObservable.unregisterObserver(scrollChangeListener);
    }

    @NotNull
    public TouchWebView.TouchWebViewClient getWebViewClient() {
        if (webViewClient == null) {
            throw new IllegalStateException(TouchWebView.TouchWebViewClient.class.getSimpleName() + " was not initialized");
        }
        return webViewClient;
    }

    @Override
    public final void setWebViewClient(WebViewClient client) {
        if (!(client instanceof TouchWebView.TouchWebViewClient)) {
            throw new IllegalArgumentException("client " + client + " is not instance of " + TouchWebView.TouchWebViewClient.class);
        }
        webViewClient = (TouchWebView.TouchWebViewClient) client;
        webViewClient.webViewHandler = webViewHandler;
        webViewClient.pageLoadObservable = pageLoadObservable;
        super.setWebViewClient(webViewClient);
    }

    @Nullable
    public TouchWebView.TouchWebChromeClient getWebChromeClient() {
        if (webChromeClient == null) {
            throw new IllegalStateException(TouchWebView.TouchWebChromeClient.class.getSimpleName() + " was not initialized");
        }
        return webChromeClient;
    }

    @Override
    public final void setWebChromeClient(WebChromeClient client) {
        if (!(client instanceof TouchWebView.TouchWebChromeClient)) {
            throw new IllegalArgumentException("client " + client + " is not instance of " + WebChromeClient.class);
        }
        super.setWebChromeClient(webChromeClient = (TouchWebView.TouchWebChromeClient) client);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if (inputConnection == null) {
            return super.onCreateInputConnection(outAttrs);
        } else {
            return inputConnection;
        }
    }

    @Nullable
    public TouchWebView.ScrollState getScrollState() {
        if (webViewClient != null) {
            float scale = webViewClient.getLastScale();
            if (scale <= 0) {
                scale = getScale();
            }
            int height = (int) Math.floor(getContentHeight() * scale);
            int webViewHeight = getMeasuredHeight();
            int scrollY = getScrollY();
//        logger.d("height=" + height + ", webViewHeight=" + webViewHeight + ", scrollY=" + scrollY);
            if (scrollY + webViewHeight >= height - 5) {
                return TouchWebView.ScrollState.BOTTOM;
            } else if (height - scrollY - 5 <= webViewHeight) {
                return TouchWebView.ScrollState.TOP;
            } else {
                return TouchWebView.ScrollState.BETWEEN;
            }
        }
        return null;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        final ScrollState state = getScrollState();
        if (state != null) {
            switch (state) {
                case TOP:
                    scrollChangeObservable.dispatchScrolledToTop();
                    break;
                case BOTTOM:
                    scrollChangeObservable.dispatchScrolledToBottom();
                    break;
            }
        }
        scrollChangeObservable.dispatchScrollChanged(l, t, oldl, oldt);
        super.onScrollChanged(l, t, oldl, oldt);
    }

    @Override
    @CallSuper
    public boolean handleMessage(Message msg) {
        if (msg.what == CLICK_ON_URL) {
            webViewHandler.removeMessages(CLICK_ON_WEBVIEW);
            return true;
        } else if (msg.what == CLICK_ON_WEBVIEW) {
            if (clickListener != null) {
                clickListener.onClick(this);
            }
            return true;
        }
        return false;
    }

    @Override
    @CallSuper
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            webViewHandler.sendEmptyMessageDelayed(CLICK_ON_WEBVIEW, 500);
        }
        return super.onTouchEvent(event);
//        return /* event.getAction() == MotionEvent.ACTION_UP && */  gestureDetector.onTouchEvent(event);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        clickListener = l;
    }

    // http://stackoverflow.com/questions/5267639/how-to-safely-turn-webview-zooming-on-and-off-as-needed
    @Override
    public void destroy() {
        webViewHandler.postDelayed(TouchWebView.this::providerDestroy, ViewConfiguration.getZoomControlsTimeout());
    }

    private void providerDestroy() {
        super.destroy();
    }

    public static class TouchWebViewClient extends WebViewClient {

        @Nullable
        protected Handler webViewHandler;

        @Nullable
        protected PageLoadObservable pageLoadObservable;

        private float lastScale = 0;

        public TouchWebViewClient() {
        }

        float getLastScale() {
            return lastScale;
        }

        @Override
        @CallSuper
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            logger.d("shouldOverrideUrlLoading(), request=" + request);
            if (webViewHandler != null) {
                webViewHandler.sendEmptyMessage(CLICK_ON_URL);
            }
            return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        @CallSuper
        public void onPageStarted(WebView view, final String url, final Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (webViewHandler != null) {
                webViewHandler.post(() -> {
                    if (pageLoadObservable != null) {
                        pageLoadObservable.dispatchLoadPageStarted(url, favicon);
                    }
                });
            }
        }

        @Override
        @CallSuper
        public void onPageFinished(WebView view, final String url) {
            super.onPageFinished(view, url);
            if (webViewHandler != null) {
                webViewHandler.post(() -> {
                    if (pageLoadObservable != null) {
                        pageLoadObservable.dispatchLoadPageFinished(url);
                    }
                });
            }
        }

        @Override
        @CallSuper
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            logger.e("onReceivedError(), request=" + WebResourceRequestToString(request) + ", error=" + WebResourceErrorToString(error));
        }

        @Override
        @CallSuper
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            logger.e("onReceivedHttpError(), request=" + WebResourceRequestToString(request) + ", errorResponse=" + WebResourceResponseToString(errorResponse));
        }

        @Override
        @CallSuper
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            logger.e("onReceivedSslError(), handler=" + handler + ", error=" + error);
        }

        @Override
        @CallSuper
        public void onScaleChanged(WebView view, float oldScale, float newScale) {
            super.onScaleChanged(view, oldScale, newScale);
            lastScale = newScale;
        }
    }

    public static class TouchWebChromeClient extends WebChromeClient {

        @Override
        @CallSuper
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            logger.w("onJsAlert(), url=" + ", message=" + message);
            return super.onJsAlert(view, url, message, result);
        }

        @Override
        @CallSuper
        public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
            logger.w("onJsAlert(), url=" + ", message=" + message);
            return super.onJsBeforeUnload(view, url, message, result);
        }

        @Override
        @CallSuper
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            logger.d("onConsoleMessage(), consoleMessage=" + ConsoleMessageToString(consoleMessage));
            return super.onConsoleMessage(consoleMessage);
        }

    }

    public interface OnPageLoadListener {

        void onPageStarted(String url, Bitmap favicon);

        void onPageFinished(String url);
    }

    public interface OnScrollChangeListener {

        void onScrolledToTop();

        void onScrollChanged(int l, int t, int oldl, int oldt);

        void onScrolledToBottom();
    }

    public static class PageLoadObservable extends Observable<OnPageLoadListener> {

        void dispatchLoadPageStarted(String url, Bitmap favicon) {
            synchronized (observers) {
                for (TouchWebView.OnPageLoadListener l : observers)
                    l.onPageStarted(url, favicon);
            }
        }

        void dispatchLoadPageFinished(String url) {
            synchronized (observers) {
                for (TouchWebView.OnPageLoadListener l : observers)
                    l.onPageFinished(url);
            }
        }
    }

    public static class ScrollChangeObservable extends Observable<OnScrollChangeListener> {

        void dispatchScrolledToTop() {
            synchronized (observers) {
                for (TouchWebView.OnScrollChangeListener l : observers)
                    l.onScrolledToTop();
            }
        }

        void dispatchScrollChanged(int l, int t, int oldl, int oldt) {
            synchronized (observers) {
                for (TouchWebView.OnScrollChangeListener listener : observers) {
                    listener.onScrollChanged(l, t, oldl, oldt);
                }
            }
        }

        void dispatchScrolledToBottom() {
            synchronized (observers) {
                for (TouchWebView.OnScrollChangeListener l : observers)
                    l.onScrolledToBottom();
            }
        }
    }


    public static String WebResourceRequestToString(WebResourceRequest request) {
        if (request != null) {
            if (isAtLeastLollipop()) {
                return "uri=" +
                        request.getUrl() +
                        ", isForMainFrame=" +
                        request.isForMainFrame() +
                        ", hasGesture=" +
                        request.hasGesture() +
                        ", method=" +
                        request.getMethod() +
                        ", requestHeaders=" +
                        request.getRequestHeaders();
            } else {
                return request.toString();
            }
        }
        return null;
    }

    public static String WebResourceErrorToString(WebResourceError error) {
        if (error != null) {
            if (isAtLeastMarshmallow()) {
                return "errorCode=" +
                        error.getErrorCode() +
                        ", description=" +
                        error.getDescription();
            } else {
                return error.toString();
            }
        }
        return null;
    }

    public static String WebResourceResponseToString(WebResourceResponse response) {
        if (response != null) {
            if (isAtLeastLollipop()) {
                return "statusCode=" +
                        response.getStatusCode() +
                        ", reasonPhrase=" +
                        response.getReasonPhrase() +
                        ", responseHeaders=" +
                        response.getResponseHeaders();
            } else {
                return response.toString();
            }
        }
        return null;
    }

    public static String ConsoleMessageToString(ConsoleMessage message) {
        return "level=" +
                message.messageLevel() +
                ", sourceId=" +
                message.sourceId() +
                ", lineNumber=" +
                message.lineNumber() +
                ", message=" +
                message.message();
    }

    public enum ScrollState {
        TOP, BOTTOM, BETWEEN
    }
}
