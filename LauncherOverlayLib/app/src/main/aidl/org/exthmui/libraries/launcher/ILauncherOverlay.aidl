package org.exthmui.libraries.launcher;

import android.view.WindowManager.LayoutParams;
import org.exthmui.libraries.launcher.ILauncherOverlayCallback;

interface ILauncherOverlay {
    void startScroll();
    void onScroll(float progress);
    void endScroll();
    void windowAttached(in LayoutParams p, in ILauncherOverlayCallback callback, int option);
    void windowDetached(boolean isConfigChange);
    void onPause();
    void onResume();
    void onLifeState(int state); //pause/resume之外的state
    void openOverlay(int v);
    void closeOverlay(int v);
}
