package net.maxsmr.commonutils.android.gui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.maxsmr.commonutils.data.Observable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TouchWebView extends WebView implements Handler.Callback {

    private static final Logger logger = LoggerFactory.getLogger(TouchWebView.class);

    private static final int CLICK_ON_WEBVIEW = 1;
    private static final int CLICK_ON_URL = 2;

    private final Handler mWebViewHandler = new Handler(Looper.getMainLooper(), this);

    private final PageLoadObservable mPageLoadObservable = new PageLoadObservable();

    private final ScrollChangeObservable mScrollChangeObservable = new ScrollChangeObservable();

    private TouchWebViewClient webViewClient;

    private TouchWebChromeClient webChromeClient;

    public TouchWebView(Context context) {
        this(context, null);
    }

    public TouchWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
//        gestureDetector = new GestureDetector(context, new GestureListener());
        setWebViewClient(new TouchWebViewClient(mWebViewHandler, mPageLoadObservable));
        setWebChromeClient(new TouchWebChromeClient());
    }

    @NonNull
    public Handler getClickHandler() {
        return mWebViewHandler;
    }

    public void addPageLoadListener(@NonNull OnPageLoadListener pageLoadListener) {
        mPageLoadObservable.registerObserver(pageLoadListener);
    }

    public void removePageLoadListener(@NonNull OnPageLoadListener pageLoadListener) {
        mPageLoadObservable.unregisterObserver(pageLoadListener);
    }

    public void addScrollChangeListener(@NonNull OnScrollChangeListener scrollChangeListener) {
        mScrollChangeObservable.registerObserver(scrollChangeListener);
    }

    public void removeScrollChangeListener(@NonNull OnScrollChangeListener scrollChangeListener) {
        mScrollChangeObservable.unregisterObserver(scrollChangeListener);
    }

    @NonNull
    public TouchWebViewClient getWebViewClient() {
        if (webViewClient == null) {
            throw new IllegalStateException(TouchWebViewClient.class.getSimpleName() + " was not initialized");
        }
        return webViewClient;
    }

    @Override
    public final void setWebViewClient(WebViewClient client) {
        if (!(client instanceof TouchWebViewClient)) {
            throw new IllegalArgumentException("client " + client + " is not instance of " + TouchWebViewClient.class);
        }
        super.setWebViewClient(webViewClient = (TouchWebViewClient) client);
    }

    @Nullable
    public TouchWebChromeClient getWebChromeClient() {
        if (webChromeClient == null) {
            throw new IllegalStateException(TouchWebChromeClient.class.getSimpleName() + " was not initialized");
        }
        return webChromeClient;
    }

    @Override
    public final void setWebChromeClient(WebChromeClient client) {
        if (!(client instanceof TouchWebChromeClient)) {
            throw new IllegalArgumentException("client " + client + " is not instance of " + WebChromeClient.class);
        }
        super.setWebChromeClient(webChromeClient = (TouchWebChromeClient) client);
    }

    @NonNull
    public ScrollState getScrollState() {
        float scale = webViewClient.getLastScale();
        if (scale <= 0) {
            scale = getScale();
        }
        int height = (int) Math.floor(getContentHeight() * scale);
        int webViewHeight = getMeasuredHeight();
        int scrollY = getScrollY();
//        logger.debug("height=" + height + ", webViewHeight=" + webViewHeight + ", scrollY=" + scrollY);
        if (scrollY + webViewHeight >= height - 5) {
            return ScrollState.BOTTOM;
        } else if (height - scrollY - 5 <= webViewHeight) {
            return ScrollState.TOP;
        } else {
            return ScrollState.BETWEEN;
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        switch (getScrollState()) {
            case TOP:
                mScrollChangeObservable.dispatchScrolledToTop();
                break;
            case BOTTOM:
                mScrollChangeObservable.dispatchScrolledToBottom();
                break;
        }
        mScrollChangeObservable.dispatchScrollChanged(l, t, oldl, oldt);
        super.onScrollChanged(l, t, oldl, oldt);
    }

    @Override
    @CallSuper
    public boolean handleMessage(Message msg) {
        if (msg.what == CLICK_ON_URL) {
            mWebViewHandler.removeMessages(CLICK_ON_WEBVIEW);
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
            mWebViewHandler.sendEmptyMessageDelayed(CLICK_ON_WEBVIEW, 500);
        }
        return super.onTouchEvent(event);
//        return /* event.getAction() == MotionEvent.ACTION_UP && */  gestureDetector.onTouchEvent(event);
    }

    private View.OnClickListener clickListener;

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        clickListener = l;
    }

    public static class TouchWebViewClient extends WebViewClient {

        private final Handler mWebViewHandler;

        private final PageLoadObservable mPageLoadObservable;

        private float lastScale = 0;

        public TouchWebViewClient(Handler handler, PageLoadObservable pageLoadObservable) {
            mWebViewHandler = handler;
            mPageLoadObservable = pageLoadObservable;
        }

        public float getLastScale() {
            return lastScale;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            logger.debug("shouldOverrideUrlLoading(), request=" + request);
            mWebViewHandler.sendEmptyMessage(CLICK_ON_URL);
            return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        public void onPageStarted(WebView view, final String url, final Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mWebViewHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPageLoadObservable.dispatchLoadPageStarted(url, favicon);
                }
            });
        }

        @Override
        public void onPageFinished(WebView view, final String url) {
            super.onPageFinished(view, url);
            mWebViewHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPageLoadObservable.dispatchLoadPageFinished(url);
                }
            });
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            logger.error("onReceivedError(), request=" + WebResourceRequestToString(request) + ", error=" + WebResourceErrorToString(error));
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            logger.error("onReceivedHttpError(), request=" + WebResourceRequestToString(request) + ", errorResponse=" + WebResourceResponseToString(errorResponse));
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            logger.error("onReceivedSslError(), handler=" + handler + ", error=" + error);
        }

        @Override
        public void onScaleChanged(WebView view, float oldScale, float newScale) {
            super.onScaleChanged(view, oldScale, newScale);
            lastScale = newScale;
        }
    }

    public static class TouchWebChromeClient extends WebChromeClient {

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            logger.warn("onJsAlert(), url=" + ", message=" + message);
            return super.onJsAlert(view, url, message, result);
        }

        @Override
        public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
            logger.warn("onJsAlert(), url=" + ", message=" + message);
            return super.onJsBeforeUnload(view, url, message, result);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            logger.debug("onConsoleMessage(), consoleMessage=" + ConsoleMessageToString(consoleMessage));
            return super.onConsoleMessage(consoleMessage);
        }

    }

    public interface OnPageLoadListener {

        void onPageStarted(String url, Bitmap favicon);

        void onPageFinished(String url);
    }

    private static class PageLoadObservable extends Observable<OnPageLoadListener> {

        void dispatchLoadPageStarted(String url, Bitmap favicon) {
            synchronized (mObservers) {
                for (OnPageLoadListener l : mObservers)
                    l.onPageStarted(url, favicon);
            }
        }

        void dispatchLoadPageFinished(String url) {
            synchronized (mObservers) {
                for (OnPageLoadListener l : mObservers)
                    l.onPageFinished(url);
            }
        }
    }

    public interface OnScrollChangeListener {

        void onScrolledToTop();

        void onScrollChanged(int l, int t, int oldl, int oldt);

        void onScrolledToBottom();
    }

    private static class ScrollChangeObservable extends Observable<OnScrollChangeListener> {

        void dispatchScrolledToTop() {
            synchronized (mObservers) {
                for (OnScrollChangeListener l : mObservers)
                    l.onScrolledToTop();
            }
        }

        void dispatchScrollChanged(int l, int t, int oldl, int oldt) {
            synchronized (mObservers) {
                for (OnScrollChangeListener listener : mObservers) {
                    listener.onScrollChanged(l, t, oldl, oldt);
                }
            }
        }

        void dispatchScrolledToBottom() {
            synchronized (mObservers) {
                for (OnScrollChangeListener l : mObservers)
                    l.onScrolledToBottom();
            }
        }
    }

    public static String WebResourceRequestToString(WebResourceRequest request) {
        if (request != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                StringBuilder sb = new StringBuilder();
                sb.append("uri=");
                sb.append(request.getUrl());
                sb.append(", isForMainFrame=");
                sb.append(request.isForMainFrame());
                sb.append(", hasGesture=");
                sb.append(request.hasGesture());
                sb.append(", method=");
                sb.append(request.getMethod());
                sb.append(", requestHeaders=");
                sb.append(request.getRequestHeaders());
                return sb.toString();
            } else {
                return request.toString();
            }
        }
        return null;
    }

    public static String WebResourceErrorToString(WebResourceError error) {
        if (error != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                StringBuilder sb = new StringBuilder();
                sb.append("errorCode=");
                sb.append(error.getErrorCode());
                sb.append(", description=");
                sb.append(error.getDescription());
                return sb.toString();
            } else {
                return error.toString();
            }
        }
        return null;
    }

    public static String WebResourceResponseToString(WebResourceResponse response) {
        if (response != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                StringBuilder sb = new StringBuilder();
                sb.append("statusCode=");
                sb.append(response.getStatusCode());
                sb.append(", reasonPhrase=");
                sb.append(response.getReasonPhrase());
                sb.append(", responseHeaders=");
                sb.append(response.getResponseHeaders());
                return sb.toString();
            } else {
                return response.toString();
            }
        }
        return null;
    }

    public static String ConsoleMessageToString(ConsoleMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append("level=");
        sb.append(message.messageLevel());
        sb.append(", sourceId=");
        sb.append(message.sourceId());
        sb.append(", lineNumber=");
        sb.append(message.lineNumber());
        sb.append(", message=");
        sb.append(message.message());
        return sb.toString();
    }

    public enum ScrollState {
        TOP, BOTTOM, BETWEEN
    }
}
