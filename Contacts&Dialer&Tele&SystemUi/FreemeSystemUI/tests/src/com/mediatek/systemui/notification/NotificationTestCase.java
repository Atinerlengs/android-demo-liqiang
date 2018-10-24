package com.mediatek.systemui.notification;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.testing.TestableContext;

import org.junit.Rule;


public class NotificationTestCase {
    @Rule
    public final TestableContext mContext =
            new TestableContext(InstrumentationRegistry.getContext(), null);

    protected Context getContext() {
        return mContext;
    }
}
