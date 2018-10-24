package com.freeme.dialer.contacts.list.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.dialer.R;
import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.freeme.dialer.contacts.FreemeContactsNotificationChannelsUtil;
import com.freeme.dialer.utils.FreemeEntranceRequst;

public class FreemeMultiChoiceHandlerListener {
    private static final String TAG = "FreemeMultiChoiceHandlerListener";

    private static final int ERROR_USIM_EMAIL_LOST = 6;

    /**
     * the key used for {@link #onFinished(int, int, int)} called time. This is
     * help the test case to get the really time finished.
     */

    public static final String KEY_FINISH_TIME = "key_finish_time";

    static final String DEFAULT_NOTIFICATION_TAG = "MultiChoiceServiceProgress";

    private final NotificationManager mNotificationManager;

    // context should be the object of FreemeMultiChoiceService
    private final Service mContext;

    /*add for support dialer to use mtk contactImportExport @{*/
    private String mCallingActivityName = null;

    private long mLastReportTime;

    public FreemeMultiChoiceHandlerListener(Service service, String callingActivityName) {
        this(service);
        mCallingActivityName = callingActivityName;
    }
    //@}

    public FreemeMultiChoiceHandlerListener(Service service) {
        mContext = service;
        mNotificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public synchronized void onProcessed(final int requestType, final int jobId, final int currentCount,
                                  final int totalCount, final String contactName) {
        FreemeLogUtils.i(TAG, "[onProcessed]requestType = " + requestType + ",jobId = " + jobId
                + ",currentCount = " + currentCount + ",totalCount = " + totalCount
                + ",contactName = " + contactName);
        long currentTime = System.currentTimeMillis();
        if (((currentTime - mLastReportTime) < 400) &&
                currentCount != 0 && currentCount != 1 && currentCount != totalCount) {
            FreemeLogUtils.d(TAG, "[onProcessed]return. currentTime=" + currentTime +
                    ", mLastReportTime=" + mLastReportTime);
            return;
        }

        final String totalCountString = String.valueOf(totalCount);
        final String tickerText;
        final String description;
        tickerText = mContext.getString(R.string.notifier_progress_delete_message,
                String.valueOf(currentCount), totalCountString, contactName);
        if (totalCount == -1) {
            description = mContext
                    .getString(R.string.notifier_progress_delete_will_start_message);
        } else {
            description = mContext.getString(R.string.notifier_progress_delete_description,
                    contactName);
        }
        int statIconId = android.R.drawable.ic_menu_delete;

        FreemeLogUtils.d(TAG, "[onProcessed] notify DEFAULT_NOTIFICATION_TAG,description: " + description);
        if (currentCount == 0) {
            FreemeContactsNotificationChannelsUtil.createDefaultChannel(mContext.getApplicationContext());
        }
        final Notification notification = constructProgressNotification(
                mContext.getApplicationContext(), requestType, description, tickerText, jobId,
                totalCount, currentCount, statIconId);
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
        mLastReportTime = currentTime;
    }

    private final String CONTACT_PACKAGE_NAME = "com.android.dialer";
    public synchronized void onFinished(final int requestType, final int jobId, final int total) {
        long currentTimeMillis = System.currentTimeMillis();
        FreemeLogUtils.i(TAG, "[onFinished] jobId = " + jobId + " total = " + total + " requestType = "
                + requestType);
        long endTime = System.currentTimeMillis();
        FreemeLogUtils.d(TAG, "[CMCC Performance test][Contacts] delete 1500 contacts end [" + endTime
                + "]");
        // Dismiss FreemeMultiChoiceConfirmActivity
        Intent i = new Intent().setAction(
                FreemeMultiChoiceConfirmActivity.ACTION_MULTICHOICE_PROCESS_FINISH);
        i.putExtra(KEY_FINISH_TIME, currentTimeMillis);
        mContext.sendBroadcast(i);
        i = null;

        // A good experience is to cache the resource.
        final String title = mContext.getString(R.string.notifier_finish_delete_title);
        final String description = mContext.getString(R.string.notifier_finish_delete_content, total);
        // statIconId = R.drawable.ic_stat_delete;
        final int statIconId = android.R.drawable.ic_menu_delete;

        /*
         * support Dialer to use mtk contacts import/export when finished exported between phone
         * contacts and sim contacts.click Notification will jump to callingActivity.@{
         */
        final Intent intent = new Intent();
        intent.setComponent(new ComponentName(CONTACT_PACKAGE_NAME,
                FreemeEntranceRequst.CLASS_NAME_CONTACTS));
        FreemeLogUtils.i(TAG, "[onFinished] mCallingActivityName = " + mCallingActivityName +
                ",intent = " + intent.toString());
        //@}

        final Notification notification = constructFinishNotification(mContext, title, description,
                intent, statIconId);
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
        FreemeLogUtils.d(TAG, "[onFinished] notify DEFAULT_NOTIFICATION_TAG");
    }

    public synchronized void onFailed(final int requestType, final int jobId, final int total,
                               final int succeeded, final int failed) {
        FreemeLogUtils.i(TAG, "[onFailed] requestType =" + requestType + " jobId = " + jobId
                + " total = " + total + " succeeded = " + succeeded + " failed = " + failed);
        final int titleId = R.string.notifier_fail_delete_title;
        final int contentId = R.string.notifier_multichoice_process_report;
        /*
         * Bug Fix by Mediatek Begin. Original Android's code: xxx CR ID:
         * ALPS00251890 Descriptions:
         */
        /**
         * M: fixed CR ALPS00783536 @{
         */
        FreemeMultiChoiceHandlerListener.ReportDialogInfo reportDialogInfo = new FreemeMultiChoiceHandlerListener.ReportDialogInfo();
        reportDialogInfo.setmTitleId(titleId);
        reportDialogInfo.setmContentId(contentId);
        reportDialogInfo.setmJobId(jobId);
        reportDialogInfo.setmTotalNumber(total);
        reportDialogInfo.setmSucceededNumber(succeeded);
        reportDialogInfo.setmFailedNumber(failed);
        /** @} */
        final Notification notification = constructReportNotification(mContext, reportDialogInfo);
        /*
         * Bug Fix by Mediatek End.
         */
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
        FreemeLogUtils.d(TAG, "[onFailed] onProcessed notify DEFAULT_NOTIFICATION_TAG");

    }

    synchronized void onFailed(final int requestType, final int jobId, final int total,
                               final int succeeded, final int failed, final int errorCause) {
        FreemeLogUtils.d(TAG, "[onFailed] requestType =" + requestType + " jobId = " + jobId
                + " total = " + total + " succeeded = " + succeeded + " failed = " + failed
                + " errorCause = " + errorCause + " ");
        int titleId = R.string.notifier_fail_delete_title;
        final int contentId = R.string.notifier_multichoice_process_report;
        /*
         * Bug Fix by Mediatek Begin. Original Android's code: xxx CR ID:
         * ALPS00251890 Descriptions:
         */
        /**
         * M: fixed CR ALPS00783536 @{
         */
        FreemeMultiChoiceHandlerListener.ReportDialogInfo reportDialogInfo = new FreemeMultiChoiceHandlerListener.ReportDialogInfo();
        reportDialogInfo.setmTitleId(titleId);
        reportDialogInfo.setmContentId(contentId);
        reportDialogInfo.setmJobId(jobId);
        reportDialogInfo.setmTotalNumber(total);
        reportDialogInfo.setmSucceededNumber(succeeded);
        reportDialogInfo.setmFailedNumber(failed);
        reportDialogInfo.setmErrorCauseId(errorCause);
        /** @} */
        final Notification notification = constructReportNotification(mContext, reportDialogInfo);
        /*
         * Bug Fix by Mediatek End.
         */
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
        FreemeLogUtils.d(TAG, "[onFailed]onProcessed notify DEFAULT_NOTIFICATION_TAG");

    }

    public synchronized void onCanceled(final int requestType, final int jobId, final int total,
                                 final int succeeded, final int failed) {
        FreemeLogUtils.i(TAG, "[onCanceled] requestType =" + requestType + " jobId = " + jobId
                + " total = " + total + " succeeded = " + succeeded + " failed = " + failed);
        final int titleId = R.string.notifier_cancel_delete_title;
        final int contentId;
        if (total != -1) {
            contentId = R.string.notifier_multichoice_process_report;
        } else {
            contentId = -1;
        }
        /*
         * Bug Fix by Mediatek Begin. Original Android's code: xxx CR ID:
         * ALPS00251890 Descriptions:
         */

        /**
         * M: fixed CR ALPS00783536 @{
         */
        FreemeMultiChoiceHandlerListener.ReportDialogInfo reportDialogInfo = new FreemeMultiChoiceHandlerListener.ReportDialogInfo();
        reportDialogInfo.setmTitleId(titleId);
        reportDialogInfo.setmContentId(contentId);
        reportDialogInfo.setmJobId(jobId);
        reportDialogInfo.setmTotalNumber(total);
        reportDialogInfo.setmSucceededNumber(succeeded);
        reportDialogInfo.setmFailedNumber(failed);
        /** @} */
        final Notification notification = constructReportNotification(mContext, reportDialogInfo);
        /*
         * Bug Fix by Mediatek End.
         */
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
        FreemeLogUtils.d(
                TAG,
                "[onCanceled]onProcessed notify DEFAULT_NOTIFICATION_TAG: "
                        + mContext.getString(titleId));
    }

    /*
     * Bug Fix by Mediatek Begin. Original Android's code: xxx CR ID:
     * ALPS00249590 Descriptions:
     */
    public synchronized void onCanceling(final int requestType, final int jobId) {
        FreemeLogUtils.i(TAG, "[onCanceling] requestType : " + requestType + " | jobId : " + jobId);
        final String description = mContext.getString(R.string.multichoice_confirmation_title_delete);
        int statIconId = android.R.drawable.ic_menu_delete;

        final Notification notification = constructCancelingNotification(mContext, description,
                jobId, statIconId);
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
        FreemeLogUtils.d(TAG, "[onCanceling] description: " + description);
    }

    /*
     * Bug Fix by Mediatek End.
     */

    /**
     * Constructs a Notification telling users the process is finished.
     *
     * @param context
     * @param title
     * @param description Content of the Notification
     * @param intent      Intent to be launched when the Notification is clicked. Can be
     *                    null.
     * @param statIconId
     */
    public static Notification constructFinishNotification(Context context, String title,
                                                           String description, Intent intent, final int statIconId) {
        FreemeLogUtils.i(TAG, "[constructFinishNotification] title : " + title + " | description : "
                + description + ",statIconId = " + statIconId);
        FreemeContactsNotificationChannelsUtil.createDefaultChannel(context);
        return new Notification.Builder(context)
                .setChannelId(FreemeContactsNotificationChannelsUtil.DEFAULT_CHANNEL)
                .setAutoCancel(true)
                .setSmallIcon(statIconId)
                .setContentTitle(title)
                .setContentText(description)
                .setTicker(title + "\n" + description)
                .setContentIntent(
                        PendingIntent.getActivity(context, 0, (intent != null ? intent
                                : new Intent()), 0)).getNotification();
    }

    /**
     * Constructs a {@link Notification} showing the current status of
     * import/export. Users can cancel the process with the Notification.
     *
     * @param context      The service of MultichoiceService
     * @param requestType  delete
     * @param description  Content of the Notification.
     * @param tickerText
     * @param jobId
     * @param totalCount   The number of vCard entries to be imported. Used to show
     *                     progress bar. -1 lets the system show the progress bar with
     *                     "indeterminate" state.
     * @param currentCount The index of current vCard. Used to show progress bar.
     * @param statIconId
     */
    public static Notification constructProgressNotification(Context context, int requestType,
                                                             String description, String tickerText, int jobId, int totalCount, int currentCount,
                                                             int statIconId) {
        FreemeLogUtils.i(TAG, "[constructProgressNotification]requestType = " + requestType
                + ",description = " + description + ",tickerText = " + tickerText + ",jobId = "
                + jobId + ",totalCount = " + totalCount + ",currentCount = " + currentCount
                + ",statIconId = " + statIconId);
        Intent cancelIntent = new Intent(context, FreemeMultiChoiceConfirmActivity.class);
        cancelIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        cancelIntent.putExtra(FreemeMultiChoiceConfirmActivity.JOB_ID, jobId);
        cancelIntent.putExtra(FreemeMultiChoiceConfirmActivity.ACCOUNT_INFO, "TODO finish");
        cancelIntent.putExtra(FreemeMultiChoiceConfirmActivity.TYPE, requestType);

        final Notification.Builder builder = new Notification.Builder(context);
        // builder.setOngoing(true).setProgress(totalCount, currentCount,
        // totalCount == -1).setTicker(
        // tickerText).setContentTitle(description).setSmallIcon(statIconId).setContentIntent(
        // PendingIntent.getActivity(context, 0, cancelIntent,
        // PendingIntent.FLAG_UPDATE_CURRENT));
        builder.setOngoing(true)
                .setChannelId(FreemeContactsNotificationChannelsUtil.DEFAULT_CHANNEL)
                .setProgress(totalCount, currentCount, totalCount == -1)
                .setContentTitle(description)
                .setSmallIcon(statIconId)
                .setContentIntent(
                        PendingIntent.getActivity(context, jobId, cancelIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT));
        if (totalCount > 0) {
            builder.setContentText(context.getString(R.string.percentage,
                    String.valueOf(currentCount * 100 / totalCount)));
        }
        return builder.getNotification();
    }

    /**
     * Constructs a Notification telling users the process is canceled.
     *
     * @param context
     * @param reportDialogInfo Content of the Notification
     */
    /*
     * Bug Fix by Mediatek Begin. Original Android's code: xxx CR ID:
     * ALPS00251890 Descriptions: add int jobId
     */
    public static Notification constructReportNotification(Context context,
                                                           ReportDialogInfo reportDialogInfo) {
        FreemeLogUtils.i(TAG, "[constructReportNotification]");
        Intent reportIntent = new Intent(context, FreemeMultiChoiceConfirmActivity.class);
        reportIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        reportIntent.putExtra(FreemeMultiChoiceConfirmActivity.REPORTDIALOG, true);
        reportIntent.putExtra(FreemeMultiChoiceConfirmActivity.REPORT_DIALOG_INFO,
                reportDialogInfo);

        /**
         * M: fixed CR ALPS00783536 @{
         */
        String title;
        String content;
        int titleId = reportDialogInfo.mTitleId;
        int contentId = reportDialogInfo.mContentId;
        int totalNumber = reportDialogInfo.mTotalNumber;
        int succeededNumber = reportDialogInfo.mSucceededNumber;
        int failedNumber = reportDialogInfo.mFailedNumber;
        int jobIdNumber = reportDialogInfo.mJobId;
        int errorCauseId = reportDialogInfo.mErrorCauseId;

        if ((errorCauseId == ERROR_USIM_EMAIL_LOST) && (failedNumber == 0)) {
            title = context.getString(titleId);
        } else {
            title = context.getString(titleId, totalNumber);
        }

        if (contentId == -1) {
            content = "";
        } else {
            content = context.getString(contentId, succeededNumber, failedNumber);
        }
        /** @} */

        FreemeContactsNotificationChannelsUtil.createDefaultChannel(context);
        if (content == null || content.isEmpty()) {
            return new Notification.Builder(context)
                    .setChannelId(FreemeContactsNotificationChannelsUtil.DEFAULT_CHANNEL)
                    .setAutoCancel(true)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle(title)
                    .setTicker(title)
                    .setContentIntent(
                            PendingIntent.getActivity(context, jobIdNumber, new Intent(),
                                    PendingIntent.FLAG_UPDATE_CURRENT)).getNotification();
        } else {
            return new Notification.Builder(context)
                    .setChannelId(FreemeContactsNotificationChannelsUtil.DEFAULT_CHANNEL)
                    .setAutoCancel(true)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setTicker(title + "\n" + content)
                    .setContentIntent(
                            PendingIntent.getActivity(context, jobIdNumber, reportIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT)).getNotification();
        }
    }

    /*
     * Bug Fix by Mediatek End.
     */
    /*
     * Bug Fix by Mediatek Begin. Original Android's code: xxx CR ID:
     * ALPS00249590 Descriptions:
     */
    public static Notification constructCancelingNotification(Context context, String description,
                                                              int jobId, int statIconId) {
        FreemeLogUtils.i(TAG, "[constructCancelingNotification]description = " + description
                + ",jobId = " + jobId + ",statIconId = " + statIconId);
        FreemeContactsNotificationChannelsUtil.createDefaultChannel(context);
        final Notification.Builder builder = new Notification.Builder(context);
        builder.setOngoing(true)
                .setChannelId(FreemeContactsNotificationChannelsUtil.DEFAULT_CHANNEL)
                .setProgress(-1, -1, true)
                .setContentTitle(description)
                .setSmallIcon(statIconId)
                .setContentIntent(
                        PendingIntent.getActivity(context, jobId, new Intent(),
                                PendingIntent.FLAG_UPDATE_CURRENT));

        return builder.getNotification();
    }

    /*
     * Bug Fix by Mediatek End.
     */

    // visible for test
    public void cancelAllNotifition() {
        mNotificationManager.cancelAll();
        FreemeLogUtils.i(TAG, "[cancelAllNotifition]");
    }

    /**
     * M: fixed CR ALPS00783536 @{
     */
    public static class ReportDialogInfo implements Parcelable {

        private int mTitleId;
        private int mContentId;
        private int mJobId;
        private int mErrorCauseId = -1;
        private int mTotalNumber;
        private int mSucceededNumber;
        private int mFailedNumber;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mTitleId);
            dest.writeInt(mContentId);
            dest.writeInt(mJobId);
            dest.writeInt(mErrorCauseId);
            dest.writeInt(mTotalNumber);
            dest.writeInt(mSucceededNumber);
            dest.writeInt(mFailedNumber);
        }

        public static final Parcelable.Creator<FreemeMultiChoiceHandlerListener.ReportDialogInfo> CREATOR =
                new Parcelable.Creator<FreemeMultiChoiceHandlerListener.ReportDialogInfo>() {
                    public FreemeMultiChoiceHandlerListener.ReportDialogInfo createFromParcel(Parcel in) {
                        final FreemeMultiChoiceHandlerListener.ReportDialogInfo values = new FreemeMultiChoiceHandlerListener.ReportDialogInfo();
                        values.mTitleId = in.readInt();
                        values.mContentId = in.readInt();
                        values.mJobId = in.readInt();
                        values.mErrorCauseId = in.readInt();
                        values.mTotalNumber = in.readInt();
                        values.mSucceededNumber = in.readInt();
                        values.mFailedNumber = in.readInt();
                        return values;
                    }

                    @Override
                    public FreemeMultiChoiceHandlerListener.ReportDialogInfo[] newArray(int size) {
                        return new FreemeMultiChoiceHandlerListener.ReportDialogInfo[size];
                    }
                };

        public int getmTitleId() {
            return mTitleId;
        }

        public void setmTitleId(int titleId) {
            this.mTitleId = titleId;
        }

        public int getmContentId() {
            return mContentId;
        }

        public void setmContentId(int contentId) {
            this.mContentId = contentId;
        }

        public int getmJobId() {
            return mJobId;
        }

        public void setmJobId(int jobId) {
            this.mJobId = jobId;
        }

        public void setmErrorCauseId(int errorCauseId) {
            this.mErrorCauseId = errorCauseId;
        }

        public void setmTotalNumber(int totalNumber) {
            this.mTotalNumber = totalNumber;
        }

        public int getmTotalNumber() {
            return mTotalNumber;
        }

        public void setmSucceededNumber(int succeededNumber) {
            this.mSucceededNumber = succeededNumber;
        }

        public int getmSucceededNumber() {
            return mSucceededNumber;
        }

        public void setmFailedNumber(int failedNumber) {
            this.mFailedNumber = failedNumber;
        }

        public int getmFailedNumber() {
            return mFailedNumber;
        }
    }
    /** @ */
}
