package com.freeme.dialer.contactsfragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsInternal;
import android.support.v7.widget.RecyclerView;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.dialer.common.Assert;
import com.android.dialer.contactsfragment.R;
import com.android.dialer.logging.InteractionEvent;
import com.android.dialer.logging.Logger;
import com.freeme.actionbar.app.FreemeActionBarUtil;
import com.freeme.contacts.common.utils.FreemeContactsConfig;
import com.freeme.contacts.common.utils.FreemeToast;
import com.freeme.dialer.contacts.list.service.FreemeMultiChoiceService;
import com.freeme.dialer.contactsfragment.FreemeContactsAdapter.IItemLongClickListener;
import com.freeme.dialer.contactsfragment.FreemeContactsAdapter.IItemSelectListener;
import com.mediatek.contacts.simcontact.SubInfoUtils;

class FreemeContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
        View.OnLongClickListener {
    private final LinearLayout headerContainer;
    private final TextView header;
    private final TextView name;
    private final Context context;
    private final FrameLayout bottomLine;
    private final CheckBox checkBox;
    private final ImageView account;
    private IItemLongClickListener mItemLongClickListener;
    private IItemSelectListener mItemSelectListener;

    private String headerText;
    private Uri contactUri;
    private boolean mIsMultiChoiceMode;
    private long mContactId;
    private boolean mIsSelected;

    public FreemeContactViewHolder(View view, IItemLongClickListener itemLongClickListener,
                                   IItemSelectListener selectListener) {
        super(view);
        context = view.getContext();
        mItemLongClickListener = itemLongClickListener;
        mItemSelectListener = selectListener;

        View item = view.findViewById(R.id.click_target);
        item.setOnClickListener(this);
        item.setOnLongClickListener(this);

        headerContainer = view.findViewById(R.id.header_layout);
        header = view.findViewById(R.id.header);
        name = view.findViewById(R.id.contact_name);
        bottomLine = view.findViewById(R.id.list_bottom_line);
        checkBox = view.findViewById(R.id.contacts_select);
        account = view.findViewById(R.id.contacts_account);
    }

    /**
     * Binds the ViewHolder with relevant data.
     *
     * @param headerText        populates the header view.
     * @param displayName       populates the name view.
     * @param contactUri        to be shown by the contact card on photo click.
     * @param showHeader        if header view should be shown {@code True}, {@code False} otherwise.
     * @param isLastItemInGroup if the view is the last one in group(A-Z) {@code True}, {@code False} otherwise.
     */
    public void bind(String headerText, String displayName, Uri contactUri, boolean showHeader,
                     int subId, String accountType, boolean isLastItemInGroup) {
        Assert.checkArgument(!TextUtils.isEmpty(displayName));
        this.contactUri = contactUri;
        this.headerText = headerText;

        name.setText(displayName);
        header.setText(headerText);
        headerContainer.setVisibility(showHeader ? View.VISIBLE : View.GONE);
        bottomLine.setVisibility(!isLastItemInGroup ? View.VISIBLE : View.GONE);

        if (SubInfoUtils.checkSubscriber(subId)) {
            account.setVisibility(View.VISIBLE);
            int slotId = SubscriptionManager.getSlotIndex(subId);
            if (FreemeContactsConfig.MTK_GEMINI_SUPPORT) {
                if (slotId == 0)
                    account.setImageResource(R.drawable.freeme_contact_account_icon_sim1);
                else if (slotId == 1)
                    account.setImageResource(R.drawable.freeme_contact_account_icon_sim2);
                else
                    account.setImageDrawable(null);
            } else {
                account.setImageResource(R.drawable.freeme_contact_account_icon_sim);
            }
        } else if (FreemeContactsConfig.ACCOUNT_TYPE_GOOGLE.equals(accountType)) {
            account.setVisibility(View.VISIBLE);
            account.setImageResource(R.drawable.freeme_contact_account_icon_google);
        } else {
            account.setVisibility(View.GONE);
        }

        checkBox.setVisibility(mIsMultiChoiceMode ? View.VISIBLE : View.GONE);
        checkBox.setChecked(mIsSelected);

        Logger.get(context).logInteraction(
                InteractionEvent.Type.OPEN_QUICK_CONTACT_FROM_CONTACTS_FRAGMENT_ITEM);
    }

    public String getHeader() {
        return headerText;
    }

    public LinearLayout getHeaderView() {
        return headerContainer;
    }

    @Override
    public void onClick(View v) {
        if (mIsMultiChoiceMode) {
            checkBox.setChecked(!checkBox.isChecked());
            if (mItemSelectListener != null) {
                mItemSelectListener.onItemSelect(mContactId, contactUri);
            }
        } else {
            Logger.get(context).logInteraction(
                    InteractionEvent.Type.OPEN_QUICK_CONTACT_FROM_CONTACTS_FRAGMENT_ITEM);
            InputMethodManager imm = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(itemView.getWindowToken(), 0);

            Intent intent = ContactsContract.QuickContact.composeQuickContactsIntent(context, (Rect) null, contactUri,
                    ContactsContract.QuickContact.MODE_LARGE, null /* excludeMimes */);
            intent.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT,
                    context.getResources().getString(R.string.contacts));
            ContactsInternal.startQuickContactWithErrorToast(context, intent);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        InputMethodManager imm = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(itemView.getWindowToken(), 0);
        if (FreemeMultiChoiceService.isCanDelete()) {
            if (mItemLongClickListener != null) {
                mItemLongClickListener.onLongClick();
            }
        } else {
            FreemeToast.toast(context, R.string.freeme_contact_delete_all_tips);
        }
        return true;
    }

    public void setIsMultiChoiceMode(boolean isMultiChoiceMode) {
        this.mIsMultiChoiceMode = isMultiChoiceMode;
    }

    public void setContactId(long contactId) {
        this.mContactId = contactId;
    }

    public void setIsSelected(boolean isSelected) {
        this.mIsSelected = isSelected;
    }

    public void selected() {
        if (mItemSelectListener != null) {
            boolean isOK = mItemSelectListener.onItemSelect(mContactId, contactUri);
            if (isOK) {
                checkBox.setChecked(true);
                setIsSelected(true);
            }
        }
    }
}
