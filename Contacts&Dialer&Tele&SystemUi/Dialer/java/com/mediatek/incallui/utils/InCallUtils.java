package com.mediatek.incallui.utils;

import android.os.IBinder;
import android.os.ServiceManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.incallui.call.CallList;
import com.android.incallui.call.DialerCall;
import com.android.incallui.Log;
import com.mediatek.incallui.compat.InCallUiCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import mediatek.telecom.MtkCall.MtkDetails;

import dalvik.system.PathClassLoader;

public class InCallUtils {
    private static final String TAG = InCallUtils.class.getSimpleName();
    private static final String DMAGENT_SERVICE_NAME = "DmAgent";
    private static final String TELECOM_PACKAGE_NAME = "com.android.server.telecom";
    private static final String OUTGOING_FAILED_MSG_RES_ID = "outgoing_call_failed";
    public static final boolean MTK_IMS_SUPPORT = SystemProperties.get(
        "persist.mtk_ims_support").equals("1");
    public static final boolean MTK_VOLTE_SUPPORT = SystemProperties.get(
        "persist.mtk_volte_support").equals("1");

    public static boolean isDMLocked() {
        boolean locked = false;
        /* currently this feature is no need by operator
        String className = "com.mediatek.common.dm.DmAgent";
        String classStubName = "com.mediatek.common.dm.DmAgent.Stub";
        String asIntfMethodName = "asInterface";
        String isLockFlagSetMethodName = "isLockFlagSet";
        Class <?> dmAgentClass = null;
        Class <?> dmAgentStubClass = null;
        Method asInterfaceMethod = null;
        Method isLockFlagSetMethod = null;
        try {
            dmAgentClass = Class.forName(className);
            dmAgentStubClass = Class.forName(classStubName);
            isLockFlagSetMethod = dmAgentClass.getMethod(isLockFlagSetMethodName,
                    (Class<?> [])(null));
            asInterfaceMethod = dmAgentStubClass.getMethod(asIntfMethodName, IBinder.class);
            IBinder binder = ServiceManager.getService(DMAGENT_SERVICE_NAME);
            Object dmAgent = asInterfaceMethod.invoke(null, binder);
            locked = (Boolean) isLockFlagSetMethod.invoke(dmAgent, (Object [])(null));
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        } catch (NoSuchMethodException el) {
            el.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (locked) {
            Log.d(TAG, "isDMLocked(): locked = " + locked);
        }
        */
        return locked;
    }

    /**
     * M: show the same error message as Telecom when can't MO.
     * typically, when one call is in upgrading to video progress, someone
     * is responsible to prevent new outgoing call. Currently, we have nowhere
     * to do this except InCallUI itself.
     * TODO: the Telecom or Lower layer should be responsible to stop new outgoing call while
     * upgrading instead of InCallUI.
     *
     * @param context the ApplicationContext
     * @param call
     */
    public static void showOutgoingFailMsg(Context context, android.telecom.Call call) {
        if (context == null || call == null ||
                android.telecom.Call.STATE_RINGING == call.getState()) {
            return;
        }

        final PackageManager pm = context.getPackageManager();
        Resources telecomResources = null;
        try {
            telecomResources = pm.getResourcesForApplication(TELECOM_PACKAGE_NAME);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "telecomResources not found");
        }

        if (telecomResources != null) {
            int resId = telecomResources.getIdentifier(
                    OUTGOING_FAILED_MSG_RES_ID, "string", TELECOM_PACKAGE_NAME);
            String msg = telecomResources.getString(resId);
            Log.d(TAG, "showOutgoingFailMsg msg-->" + msg);

            if (!TextUtils.isEmpty(msg)) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * When there have more than one active call or background call and has no
     * incoming, it will be true, otherwise false.
     */
    public static boolean canHangupAllCalls() {
        CallList callList = CallList.getInstance();
        DialerCall call = callList.getFirstCall();
        if (call != null && !DialerCall.State.isIncomingOrWaiting(call.getState())
                && callList.getActiveAndHoldCallsCount() > 1
                && InCallUiCompat.isMtkTelecomCompat()) {
            return true;
        }
        return false;
    }

    /**
     * When there have more than one active call or background call and has no
     * incoming, it will be true, otherwise false.
     */
    public static boolean canHangupAllHoldCalls() {
        CallList callList = CallList.getInstance();
        DialerCall call = callList.getFirstCall();
        if (call != null && !DialerCall.State.isIncomingOrWaiting(call.getState())
                && callList.getActiveAndHoldCallsCount() > 1
                && InCallUiCompat.isMtkTelecomCompat()) {
            return true;
        }
        return false;
    }

    /**
     * When there has one active call and a incoming call which can be answered,
     * it will be true, otherwise false.
     */
    public static boolean canHangupActiveAndAnswerWaiting() {
        CallList callList = CallList.getInstance();
        DialerCall call = callList.getFirstCall();
        if (call != null && DialerCall.State.isIncomingOrWaiting(call.getState())
                && callList.getActiveCall() != null
                && !isCdmaCall(call)
                && InCallUiCompat.isMtkTelecomCompat()) {
            return true;
        }
        return false;
    }

    /**
     * Check if the call's account has CAPABILITY_CDMA_CALL_PROVIDER.
     */
    public static boolean isCdmaCall(DialerCall call) {
        if (null == call) {
            return false;
        }

        return call.hasProperty(MtkDetails.MTK_PROPERTY_CDMA);
    }

    /**
     * when hold call have the ECT capable call,it will be true,otherwise false.
     */
    public static boolean canEct() {
        final DialerCall call = CallList.getInstance().getBackgroundCall();
        if (call != null && call.can(
                mediatek.telecom.MtkCall.MtkDetails.MTK_CAPABILITY_CONSULTATIVE_ECT)) {
            return true;
        }
        return false;
    }

    public static boolean canBlindEct(DialerCall call) {
        if (call != null) {
            return call.can(
                    mediatek.telecom.MtkCall.MtkDetails.MTK_CAPABILITY_BLIND_OR_ASSURED_ECT);
        }
        return false;
    }

    /**
     * M: [1A1H2W]indicate is under two incoming call state or not.
     */
    public static boolean isTwoIncomingCalls() {
        return CallList.getInstance().getIncomingCall() != null
                && CallList.getInstance().getSecondaryIncomingCall() != null;
    }
}
