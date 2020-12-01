package org.exthmui.libraries.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

public class BaseClientService implements ServiceConnection {
    private boolean mConnected;
    private final Context mContext;
    private final int mFlags;
    private final String mPackage;

    BaseClientService(Context context, String pkg, int flags) {
        mContext = context;
        mFlags = flags;
        mPackage = pkg;
    }

    public final boolean connect() {
        if (!mConnected) {
            try {
                mConnected = mContext.bindService(getServiceIntent(mContext, mPackage), this, mFlags);
            } catch (Throwable e) {
                Log.e("BaseClientService", "Unable to connect to overlay service", e);
            }
        }
        return mConnected;
    }

    public final void disconnect() {
        if (mConnected) {
            mContext.unbindService(this);
            mConnected = false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    private static Intent getServiceIntent(Context context, String targetPackage) {
        Uri uri = Uri.parse("app://" + context.getPackageName() + ":" + Process.myUid()).buildUpon()
                .appendQueryParameter("v", Integer.toString(7))
                .appendQueryParameter("cv", Integer.toString(9))
                .build();
        return new Intent("com.android.launcher3.WINDOW_OVERLAY")
                .setPackage(targetPackage)
                .setData(uri);
    }

}