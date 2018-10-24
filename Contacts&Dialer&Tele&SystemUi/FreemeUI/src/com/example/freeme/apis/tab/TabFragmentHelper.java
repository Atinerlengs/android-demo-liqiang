package com.example.freeme.apis.tab;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TabFragmentHelper {

    public static class TabFragment extends Fragment {

        public static TabFragment newInstance(String title) {
            Bundle args = new Bundle();
            args.putString("title", title);
            TabFragment frag = new TabFragment();
            frag.setArguments(args);
            return frag;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            TextView text = new TextView(getContext());
            text.setText(getTitle());
            text.setGravity(Gravity.CENTER);
            return text;
        }

        public String getTitle() {
            return getArguments().getString("title", "default");
        }
    }

    public static PagerAdapter newInstance(FragmentManager fragmentManager, String[] titles) {
        final Fragment[] fragments = new Fragment[titles.length];

        final int N = titles.length;
        for (int i = 0; i < N; i++) {
            fragments[i] = TabFragment.newInstance(titles[i]);
        }

        return new FragmentPagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                return fragments[position];
            }

            @Override
            public int getCount() {
                return fragments.length;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return ((TabFragment) getItem(position)).getTitle();
            }
        };
    }
}
