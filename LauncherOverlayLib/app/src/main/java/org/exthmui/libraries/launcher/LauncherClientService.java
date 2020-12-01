package org.exthmui.libraries.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.lang.ref.WeakReference;

public class LauncherClientService extends BaseClientService {

    public ILauncherOverlay mOverlay;
    public WeakReference<LauncherClient> mClient;
    private boolean mStopped;

    public LauncherClientService(Context context, String pkg) {
        super(context, pkg, Context.BIND_AUTO_CREATE | Context.BIND_WAIVE_PRIORITY);
    }

    public final void setStopped(boolean stopped) {
        mStopped = stopped;
        cleanUp();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        setClient(ILauncherOverlay.Stub.asInterface(service));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        setClient(null);
        cleanUp();
    }

    private void cleanUp() {
        if (mStopped && mOverlay == null) {
            disconnect();
        }
    }

    private void setClient(ILauncherOverlay overlay) {
        mOverlay = overlay;
        LauncherClient client = getClient();
        if (client != null) {
            client.setOverlay(mOverlay);
        }
    }

    public final LauncherClient getClient() {
        return mClient != null ? mClient.get() : null;
    }
}