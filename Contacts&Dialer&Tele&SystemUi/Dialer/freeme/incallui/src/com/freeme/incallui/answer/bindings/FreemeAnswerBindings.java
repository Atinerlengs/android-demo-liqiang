package com.freeme.incallui.answer.bindings;

import com.android.incallui.answer.protocol.AnswerScreen;
import com.freeme.incallui.answer.impl.FreemeAnswerFragment;

public class FreemeAnswerBindings {
    public static AnswerScreen createAnswerScreen(
            String callId,
            boolean isVideoCall,
            boolean isVideoUpgradeRequest,
            boolean isSelfManagedCamera,
            boolean allowAnswerAndRelease,
            boolean hasCallOnHold) {
        return FreemeAnswerFragment.newInstance(
                callId,
                isVideoCall,
                isVideoUpgradeRequest,
                isSelfManagedCamera,
                allowAnswerAndRelease,
                hasCallOnHold);
    }
}
