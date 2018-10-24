package com.mediatek.systemui.notification;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;

import static junit.framework.Assert.assertEquals;

import android.app.NotificationChannel;
import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class NotificationChannelTest extends NotificationTestCase {

    @Test
    public void testWriteToParcel() {
        NotificationChannel channel =
                new NotificationChannel("1", "one", IMPORTANCE_DEFAULT);
        Parcel parcel = Parcel.obtain();
        channel.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        NotificationChannel channel1 = NotificationChannel.CREATOR.createFromParcel(parcel);
        assertEquals(channel, channel1);
    }

    @Test
    public void testSystemBlockable() {
        NotificationChannel channel = new NotificationChannel("a", "ab", IMPORTANCE_DEFAULT);
        assertEquals(false, channel.isBlockableSystem());
        channel.setBlockableSystem(true);
        assertEquals(true, channel.isBlockableSystem());
    }
}
