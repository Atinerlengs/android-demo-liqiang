
package com.freeme.contacts.editor;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;

import com.android.contacts.R;

import java.util.ArrayList;

import android.widget.AdapterView.OnItemClickListener;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class FreemeLabelSpinner extends Spinner implements OnItemClickListener {

    private ArrayList<String> list;
    public static String text;

    public FreemeLabelSpinner(Context context) {
        super(context);
    }

    public FreemeLabelSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FreemeLabelSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    SpinnerAdapter mAdapter;

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        super.setAdapter(adapter);
        mAdapter = new DropDownAdapter(adapter);
    }

    PopupWindow mPop;
    TextView mCancelText;
    TextView mTitleText;

    @Override
    public boolean performClick() {
        Context context = getContext();
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final View view = inflater.inflate(R.layout.freeme_drop_spinner, null);
        final ListView listview = (ListView) view
                .findViewById(R.id.freeme_spinner_drop_listview);
        listview.setAdapter((ListAdapter) mAdapter);
        listview.setOnItemClickListener(this);
        mCancelText = view.findViewById(R.id.freeme_drop_cancel);
        mCancelText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPop != null) {
                    mPop.dismiss();
                }
            }
        });
        mTitleText = view.findViewById(R.id.freeme_drop_title);
        mTitleText.setText(context.getResources().getString(R.string.freeme_choose_label));
        mPop = new PopupWindow(view,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                true);
        mPop.setClippingEnabled(false);
        mPop.showAtLocation(view, Gravity.NO_GRAVITY, 0, 0);
        return true;
    }

    public ArrayList<String> getList() {
        return list;
    }

    public void setList(ArrayList<String> list) {
        this.list = list;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mSelectedListener != null) {
            mSelectedListener.onItemSelected(parent, view, position, id);
        }
        if (position != mAdapter.getCount() - 1) {
            dismissPop();
        }
    }

    public void dismissPop() {
        if (mPop != null) {
            mPop.dismiss();
            mPop = null;
        }
    }

    private static class DropDownAdapter implements ListAdapter, SpinnerAdapter {

        private SpinnerAdapter mAdapter;
        private ListAdapter mListAdapter;

        public DropDownAdapter(SpinnerAdapter adapter) {
            mAdapter = adapter;

            if (adapter instanceof ListAdapter) {
                mListAdapter = (ListAdapter) adapter;
            }
        }

        public int getCount() {
            return mAdapter == null ? 0 : mAdapter.getCount();
        }

        public Object getItem(int position) {
            return mAdapter == null ? null : mAdapter.getItem(position);
        }

        public long getItemId(int position) {
            return mAdapter == null ? -1 : mAdapter.getItemId(position);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return getDropDownView(position, convertView, parent);
        }

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return (mAdapter == null) ? null : mAdapter.getDropDownView(position, convertView, parent);
        }

        public boolean hasStableIds() {
            return mAdapter != null && mAdapter.hasStableIds();
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            if (mAdapter != null) {
                mAdapter.registerDataSetObserver(observer);
            }
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (mAdapter != null) {
                mAdapter.unregisterDataSetObserver(observer);
            }
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call.
         * Otherwise, return true.
         */
        public boolean areAllItemsEnabled() {
            if (mListAdapter != null) {
                return mListAdapter.areAllItemsEnabled();
            } else {
                return true;
            }
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call.
         * Otherwise, return true.
         */
        public boolean isEnabled(int position) {
            if (mListAdapter != null) {
                return mListAdapter.isEnabled(position);
            } else {
                return true;
            }
        }

        public int getItemViewType(int position) {
            return 0;
        }

        public int getViewTypeCount() {
            return 1;
        }

        public boolean isEmpty() {
            return getCount() == 0;
        }
    }

    private OnItemSelectedListener mSelectedListener;

    @Override
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mSelectedListener = listener;
    }
}