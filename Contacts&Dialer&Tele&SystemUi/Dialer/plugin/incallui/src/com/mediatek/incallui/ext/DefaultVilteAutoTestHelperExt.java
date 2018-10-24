package com.mediatek.incallui.ext;
import android.content.Context;
import android.util.Log;


public class DefaultVilteAutoTestHelperExt implements IVilteAutoTestHelperExt{

  /**
    * called when incallserviceimpl execute onbind.
    * register BroadcastReceiver to receiver vilte auto test  broadcast
    * @param context host Context
    * @param obj1 the instance of InCallPresenter
    * @param obj2 the instance of TelecomAdapter
    */
    @Override
    public void registerReceiver(Context context, Object obj1,Object obj2) {
         Log.d("DefaultVilteAutoTestHelperExt", "this is in default register" );
    }
  /**
    * called when incallserviceimpl teardown
    * unregister BroadcastReceiver
    */

     @Override
     public void unregisterReceiver( ) {
       Log.d("DefaultVilteAutoTestHelperExt", "this is in default unregister" );
     }


  /**
     * called to judge whether allow video is cropped nor not.
     * @return whether allow video is cropped nor not.
     */
    @Override
    public boolean isAllowVideoCropped() {
      return true;
    }
}

