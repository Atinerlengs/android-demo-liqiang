package com.mediatek.incallui.ext;
import android.content.Context;

public interface IVilteAutoTestHelperExt {

  /**
    * called when incallserviceimpl execute onbind.
    * register BroadcastReceiver to receiver vilte auto test  broadcast
    * @param context host Context
    * @param obj1 the instance of InCallPresenter
    * @param obj2 the instance of TelecomAdapter
    */
    void registerReceiver(Context context, Object obj1,Object obj2);
  /**
    * called when incallserviceimpl teardown
    * unregister BroadcastReceiver
    */
    void unregisterReceiver();

  /**
     * called to judge whether allow video is cropped nor not.
     * @return whether allow video is cropped nor not.
     */
    boolean isAllowVideoCropped();
}

