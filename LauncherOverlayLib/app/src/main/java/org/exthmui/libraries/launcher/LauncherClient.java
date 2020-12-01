package org.exthmui.libraries.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PatternMatcher;
import android.os.RemoteException;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.lang.ref.WeakReference;

public class LauncherClient {
    private static final String TAG = "LauncherClient";

    private final Activity mActivity;
    private OverlayCallback mCurrentCallbacks;
    private boolean mDestroyed;
    private boolean mIsResumed;
    private LauncherClientCallbacks mLauncherClientCallbacks;
    private ILauncherOverlay mOverlay;
    private int mLauncherServiceOptions;
    private int mServiceStatus;
    public final BaseClientService mBaseService;
    public final LauncherClientService mLauncherService;
    private final BroadcastReceiver mUpdateReceiver;
    private WindowManager.LayoutParams mWindowAttrs;

    public LauncherClient(Activity activity, LauncherClientCallbacks callbacks, String targetPackage, boolean overlayEnabled) {
        mUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mBaseService.disconnect();
                mLauncherService.disconnect();
                reconnectIfNeed();
            }
        };
        mIsResumed = false;
        mDestroyed = false;
        mServiceStatus = -1;
        mActivity = activity;
        mLauncherClientCallbacks = callbacks;

        mBaseService = new BaseClientService(activity, targetPackage, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
        mLauncherService = new LauncherClientService(activity, targetPackage);
        mLauncherService.mClient = new WeakReference<>(this);

        mLauncherServiceOptions = overlayEnabled ? 3 : 2;
        mActivity.registerReceiver(mUpdateReceiver, getPackageFilter(
            targetPackage, Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_REMOVED));
        reconnectIfNeed();
    }

    /**
     * Creates an intent filter to listen for actions with a specific package in the data field.
     */
    public static IntentFilter getPackageFilter(String pkg, String... actions) {
        IntentFilter packageFilter = new IntentFilter();
        for (String action : actions) {
            packageFilter.addAction(action);
        }
        packageFilter.addDataScheme("package");
        packageFilter.addDataSchemeSpecificPart(pkg, PatternMatcher.PATTERN_LITERAL);
        return packageFilter;
    }

    public void startMove() {
        if (mOverlay == null) return;

        try {
            mOverlay.startScroll();
        } catch (RemoteException ignored) {
        }
    }

    public void onScrolled(float progressX) {
        if (mOverlay == null) return;

        try {
            mOverlay.onScroll(progressX);
        } catch (RemoteException ignored) {
        }
    }

    public void endMove() {
        if (mOverlay == null) return;

        try {
            mOverlay.endScroll();
        } catch (RemoteException ignored) {
        }
    }

    public void setOverlayEnabled(boolean enabled) {
        mLauncherServiceOptions = enabled ? 3 : 2;
        applyWindowToken();
    }

    final void setOverlay(ILauncherOverlay overlay) {
        mOverlay = overlay;
        if (mOverlay == null) {
            notifyStatusChanged(Constants.STATE_IDLE);
        } else if (mWindowAttrs != null) {
            applyWindowToken();
        }
    }

    public void openOverlay(boolean anim) {
        if (mOverlay == null) return;

        try {
            mOverlay.openOverlay(anim ? 1 : 0);
        } catch (RemoteException ignored) {
        }
    }

    public void hideOverlay(boolean animate) {
        if (mOverlay == null) return;

        try {
            mOverlay.closeOverlay(animate ? 1 : 0);
        } catch (RemoteException ignored) {
        }
    }

    public final void onAttachedToWindow() {
        if (mDestroyed) return;
        setWindowAttrs(mActivity.getWindow().getAttributes());
    }

    public final void onDetachedFromWindow() {
        if (mDestroyed) return;
        setWindowAttrs(null);
    }

    public void onStart() {
        if (mDestroyed) return;
        reconnectIfNeed();
    }

    public void onPause() {
        if (mDestroyed) return;
        mIsResumed = false;
        if (mOverlay != null && mWindowAttrs != null) {
            try {
                mOverlay.onPause();
            } catch (RemoteException ignored) {
            }
        }
    }

    public void onResume() {
        if (mDestroyed) return;
        reconnectIfNeed();
        mIsResumed = true;
        if (mOverlay != null && mWindowAttrs != null) {
            try {
                mOverlay.onResume();
            } catch (RemoteException ignored) {
            }
        }
    }

    public void onStop() {
        if (mDestroyed) return;
        mLauncherService.setStopped(true);
        mBaseService.disconnect();
    }

    public void onDestroy() {
        mDestroyed = true;
        mLauncherService.setStopped(true);
        mBaseService.disconnect();
        mActivity.unregisterReceiver(mUpdateReceiver);
        if (mCurrentCallbacks != null) {
            mCurrentCallbacks.clear();
            mCurrentCallbacks = null;
        }
    }

    private void setWindowAttrs(WindowManager.LayoutParams windowAttrs) { //detach 时 为null
        mWindowAttrs = windowAttrs;
        if (mWindowAttrs != null) {
            applyWindowToken();
        } else if (mOverlay != null) {
            try {
                mOverlay.windowDetached(mActivity.isChangingConfigurations());
            } catch (RemoteException ignored) {
            }
            mOverlay = null;
        }
    }

    private void reconnectIfNeed() {
        if (!mDestroyed && (!mLauncherService.connect() || !mBaseService.connect())) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyStatusChanged(Constants.STATE_IDLE);
                }
            });
        }
    }

    private void notifyStatusChanged(int status) {
        if (mServiceStatus != status) {
            mServiceStatus = status;
            mLauncherClientCallbacks.onServiceStateChanged((status & 1) != 0, true);
        }
    }

    private void applyWindowToken() {
        if (mOverlay == null) {
            Log.e(TAG, "applyWindowToken() . mOverlay == null");
            return;
        }
        if (mCurrentCallbacks == null) {
            Log.i(TAG, "new OverlayCallback()");
            mCurrentCallbacks = new OverlayCallback();
        }
        mCurrentCallbacks.setClient(this);
        try {
            mOverlay.windowAttached(mWindowAttrs, mCurrentCallbacks, mLauncherServiceOptions);
            if (mIsResumed) {
                mOverlay.onResume();
            } else {
                mOverlay.onPause();
            }
        } catch (RemoteException ignored) {
        }
    }

    private static class OverlayCallback extends ILauncherOverlayCallback.Stub implements Handler.Callback {
        private LauncherClient mClient;
        private final Handler mUIHandler;
        private Window mWindow;
        private boolean mWindowHidden;
        private WindowManager mWindowManager;
        private int mWindowShift;

        OverlayCallback() {
            mWindowHidden = false;
            mUIHandler = new Handler(Looper.getMainLooper(), this);
        }

        void setClient(LauncherClient client) {
            mClient = client;
            mWindowManager = client.mActivity.getWindowManager();

            Point p = new Point();
            mWindowManager.getDefaultDisplay().getRealSize(p);
            mWindowShift = Math.max(p.x, p.y);

            mWindow = client.mActivity.getWindow();
        }

        private void hideActivityNonUI(boolean isHidden) {
            if (mWindowHidden != isHidden) {
                mWindowHidden = isHidden;
            }
        }

        void clear() {
            mClient = null;
            mWindowManager = null;
            mWindow = null;
        }

        @Override
        public boolean handleMessage(Message msg) {
            if (mClient == null) {
                Log.e(TAG, "OverlayCallback. handleMessage. mClient == null");
                return true;
            }
            switch (msg.what) {
                case Constants.MSG_SCROLL_CHANGED:
                    if ((mClient.mServiceStatus & 1) != 0) {
                        mClient.mLauncherClientCallbacks.onOverlayScrollChanged((float) msg.obj);
                    }
                    return true;
                case 3: //:TODO 未知.
                    WindowManager.LayoutParams attrs = mWindow.getAttributes();
                    if ((boolean) msg.obj) {
                        attrs.x = mWindowShift;
                        attrs.flags = attrs.flags | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
                    }
                    mWindowManager.updateViewLayout(mWindow.getDecorView(), attrs);
                    return true;
                case Constants.MSG_STATUS_CHANGED:
                    mClient.notifyStatusChanged(msg.arg1);
                    return true;
                default:
                    return false;
            }
        }

        // ---- implement ILauncherOverlayCallback.Stub , server will callback. ---
        @Override
        public void overlayScrollChanged(float progress) {
            mUIHandler.removeMessages(Constants.MSG_SCROLL_CHANGED);
            Message.obtain(mUIHandler, Constants.MSG_SCROLL_CHANGED, progress).sendToTarget();

            if (progress > 0) {
                hideActivityNonUI(false);
            }
        }

        @Override
        public void overlayStatusChanged(int status) {
            Message.obtain(mUIHandler, Constants.MSG_STATUS_CHANGED, status, 0).sendToTarget();
        }
        // ---- implement ILauncherOverlayCallback.. --- end ------
    }

}
