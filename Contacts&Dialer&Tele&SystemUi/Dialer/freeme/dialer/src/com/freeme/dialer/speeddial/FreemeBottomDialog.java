package com.freeme.dialer.speeddial;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.dialer.R;

public class FreemeBottomDialog extends DialogFragment {
    private String[] mItems;
    private View mRootView;
    private OnClickListener mListener;

    private static final String ARGS_ITEMS = "items";

    public static FreemeBottomDialog newInstance(String[] items) {
        Bundle args = new Bundle();
        args.putStringArray(ARGS_ITEMS, items);
        FreemeBottomDialog bottomDialog = new FreemeBottomDialog();
        bottomDialog.setArguments(args);

        return bottomDialog;
    }

    private void initData() {
        Bundle args = getArguments();
        if (args != null) {
            mItems = args.getStringArray(ARGS_ITEMS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        initData();
        mRootView = inflater.inflate(R.layout.freeme_layout_bottom_dialog, container, false);
        return mRootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView listView = (ListView) view.findViewById(R.id.lv_bottom_dialog);
        listView.setAdapter(new FreemeBottomDialogAdapter(
                getContext(),
                R.layout.freeme_layout_item_bottom_dialog,
                R.id.text1,
                mItems));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null) {
                    mListener.onClick(position);
                }

                dismiss();
            }
        });
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.FreemeDialog_Bottom);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.BOTTOM;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(params);
    }

    public void setListener(OnClickListener listener) {
        mListener = listener;
    }

    public interface OnClickListener {
        void onClick(int position);
    }

    private class FreemeBottomDialogAdapter extends ArrayAdapter {

        private static final int TITLE_COLOR = 0xff333333;
        private static final float TITLE_SIZE = 17.0f;

        private Context mContext;

        public FreemeBottomDialogAdapter(Context context, int resource,
                                         int textViewResourceId, Object[] objects) {
            super(context, resource, textViewResourceId, objects);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = (LinearLayout) LinearLayout.inflate(mContext,
                        R.layout.freeme_layout_item_bottom_dialog, null);
            }
            TextView textView = (TextView) convertView.findViewById(R.id.text1);
            if (position == 0 && textView != null) {
                textView.setTextColor(TITLE_COLOR);
                textView.setTextSize(TITLE_SIZE);
            }
            return super.getView(position, convertView, parent);
        }
    }
}
