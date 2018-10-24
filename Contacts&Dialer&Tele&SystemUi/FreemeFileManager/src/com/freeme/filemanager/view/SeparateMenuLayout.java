package com.freeme.filemanager.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.freeme.filemanager.R;

public class SeparateMenuLayout extends LinearLayout {
    private LinearLayout storageLayout;
    private TextView menuTitle;
    private Context mContext;
    PopupWindow popupWindow;

    public SeparateMenuLayout(Context context) {
        super(context);
        mContext = context;
    }

    public SeparateMenuLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.storageLayout = (LinearLayout) this.findViewById(R.id.separate_menu_layout);
        this.menuTitle = (TextView) this.findViewById(R.id.separate_menu_name);
        this.storageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopWindow(storageLayout, menuTitle.getText().toString());
            }
        });
    }

    private void showPopWindow(View view, String title) {
        if (getMenuId(title) == -1) {
            return;
        }

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View popView = inflater.inflate(getMenuId(title), null);
        popView.measure(MeasureSpec.UNSPECIFIED,MeasureSpec.UNSPECIFIED);

        popupWindow = new PopupWindow(popView,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                true);

        int popViewTop = getResources().getDimensionPixelSize(R.dimen.separate_menu_margin_bottom);
        int popViewLeft = view.getMeasuredWidth()/2
                              - popupWindow.getContentView().getMeasuredWidth()/2;
        popupWindow.showAsDropDown(view, popViewLeft, popViewTop);

        addMenuItemClick(popView);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
            }
        });
    }

    private void addMenuItemClick(View popView) {
        TextView sortByName = (TextView) popView.findViewById(R.id.sort_by_name);
        sortByName.setOnClickListener(menuItemClick);
        TextView sortBySize = (TextView) popView.findViewById(R.id.sort_by_size);
        sortBySize.setOnClickListener(menuItemClick);
        TextView sortByDate = (TextView) popView.findViewById(R.id.sort_by_date);
        sortByDate.setOnClickListener(menuItemClick);
    }

    private OnClickListener menuItemClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            mPopMenuItemClick.onPopupMenuClick(id);
            popupWindow.dismiss();
        }
    };

    public void setMenuTitle(String title) {
        menuTitle.setText(title);
    }

    public void setMenuTitle(int stringId, int itemId) {
        String sortName = ((TextView) popupWindow.getContentView()
                .findViewById(itemId)).getText().toString();

        String title = getResources().getString(stringId, sortName);
        menuTitle.setText(title);
    }

    public int getMenuId(String menutitle) {
        String sort_by = this.getContext().getString(R.string.menu_item_sort);
        if (menutitle.contains(sort_by)) { //sort menu
            return R.layout.sort_popwindow_layout;
        } else { //file format menu
            return -1;
        }
    }

    public interface IpopupMenuItemClick {
        void onPopupMenuClick(int itemId);
    }

    IpopupMenuItemClick mPopMenuItemClick = null;

    public void setOnMenuItemClickListener(IpopupMenuItemClick itemClick){
        mPopMenuItemClick = itemClick;
    }
}
