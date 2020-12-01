package org.exthmui.libraries.launcher;

public interface LauncherClientCallbacks {
    void onOverlayScrollChanged(float progress);

    void onServiceStateChanged(boolean overlayAttached, boolean hotwordActive);
}
