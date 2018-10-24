package com.freeme.recents.presentation.view.fragment;

import android.app.Fragment;
import android.content.Context;

import com.freeme.recents.RecentsUtils;
import com.freeme.recents.presentation.event.FreemeEventBus;

public class BaseFragment extends Fragment {

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FreemeEventBus.getDefault().register(this, RecentsUtils.EVENT_BUS_PRIORITY);
    }

    @Override
    public void onStop() {
        super.onStop();
        FreemeEventBus.getDefault().unregister(this);
    }
}
