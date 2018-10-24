package application.android.com.zhaozehong.demoapplication;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import application.android.com.zhaozehong.action.Action;
import application.android.com.zhaozehong.action.DatabaseAction;
import application.android.com.zhaozehong.action.DialAction;
import application.android.com.zhaozehong.action.FlotingViewAction;
import application.android.com.zhaozehong.action.FragmentActivityAction;
import application.android.com.zhaozehong.action.GridLayoutAction;
import application.android.com.zhaozehong.action.HeadsupNotificationAction;
import application.android.com.zhaozehong.action.LanguageAction;
import application.android.com.zhaozehong.action.ModifyGeoCodingDBAction;
import application.android.com.zhaozehong.action.PackagesInfo;
import application.android.com.zhaozehong.action.ParseXlsAction;
import application.android.com.zhaozehong.action.PickContactAction;
import application.android.com.zhaozehong.action.PickMultiContactAction;
import application.android.com.zhaozehong.action.PreferenceActivityAction;
import application.android.com.zhaozehong.action.ShortcutAction;
import application.android.com.zhaozehong.action.TabActivityAction;
import application.android.com.zhaozehong.action.TranslucentActivityAction;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener {

    ArrayList<Action> mList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        setContentView(R.layout.activity_main);

        mList.add(new PickContactAction(this));
        mList.add(new PickMultiContactAction(this));
        mList.add(new GridLayoutAction(this));
        mList.add(new TranslucentActivityAction(this));
        mList.add(new PreferenceActivityAction(this));
        mList.add(new FragmentActivityAction(this));
        mList.add(new FlotingViewAction(this));
        mList.add(new HeadsupNotificationAction(this));
        mList.add(new DialAction(this));
        mList.add(new ParseXlsAction(this));
        mList.add(new ModifyGeoCodingDBAction(this));
        mList.add(new TabActivityAction(this));
        mList.add(new PackagesInfo(this));
        mList.add(new LanguageAction(this));
        mList.add(new ShortcutAction(this));
        mList.add(new DatabaseAction(this));

        ListView listView = findViewById(R.id.testActionList);
        listView.setOnItemClickListener(this);
        listView.setAdapter(new ActionAdapter(this));
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        ((ActionAdapter) adapterView.getAdapter()).getItem(i).onClick();
    }

    class ActionAdapter extends BaseAdapter {

        private Context mContext;

        public ActionAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Action getItem(int i) {
            return mList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder holder = null;
            if (view == null) {
                holder = new ViewHolder();
                view = View.inflate(mContext, R.layout.list_item, null);
                holder.mActionName = view.findViewById(R.id.actionName);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            holder.mActionName.setText(getItem(i).getName());

            return view;
        }
    }

    class ViewHolder {
        TextView mActionName;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        boolean handle = false;
        for (Action action : mList) {
            boolean h = action.onBackPress();
            if (!handle) {
                handle = h;
            }
        }
        if (handle) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Action action : mList) {
            action.onDestroy();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (Action action : mList) {
            action.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
