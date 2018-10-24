package com.freeme.incallui.answer.impl;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.dialer.common.FragmentUtils;
import com.android.dialer.common.LogUtil;
import com.android.incallui.R;
import com.android.incallui.answer.impl.SmsBottomSheetFragment;

import java.util.ArrayList;

public class FreemeAnswerBootSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_DOWNGRADEAUDIO = "isSupportDownGradeAudio";
    private static final String ARG_ANSWERANDRELEASE = "isSupportAnswerAndRelease";
    private final static String TAG_FRAGMENT = "FRAGMENT_ANSWER_MENU";

    public static FreemeAnswerBootSheetFragment newInstance(boolean isSupportDownGradeAudio,
                                                            boolean isSupportAnswerAndRelease) {
        FreemeAnswerBootSheetFragment fragment = new FreemeAnswerBootSheetFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_DOWNGRADEAUDIO, isSupportDownGradeAudio);
        args.putBoolean(ARG_ANSWERANDRELEASE, isSupportAnswerAndRelease);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup,
                             @Nullable Bundle bundle) {
        Bundle args = getArguments();
        boolean isSupportDownGradeAudio = args.getBoolean(ARG_DOWNGRADEAUDIO);
        boolean isSupportAnswerAndRelease = args.getBoolean(ARG_ANSWERANDRELEASE);
        ArrayList<FreemeAnswerFragment.SecondaryBehavior> list = new ArrayList<>();
        if (isSupportDownGradeAudio) {
            list.add(FreemeAnswerFragment.SecondaryBehavior.ANSWER_VIDEO_CALL);
            list.add(FreemeAnswerFragment.SecondaryBehavior.ANSWER_VIDEO_AS_AUDIO);
        } else if (isSupportAnswerAndRelease) {
            list.add(FreemeAnswerFragment.SecondaryBehavior.ANSWER_AUDIO);
            list.add(FreemeAnswerFragment.SecondaryBehavior.ANSWER_AND_RELEASE);
        }
        list.add(FreemeAnswerFragment.SecondaryBehavior.CANCEL);

        Context context = getContext();

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.freeme_content_bg_default);

        int childHeight = getResources().getDimensionPixelSize(
                R.dimen.freeme_list_item_height);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, childHeight);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        int lineColor = context.getColor(R.color.freeme_list_divider_color);
        for (int i = 0, size = list.size(); i < size; i++) {
            View child = newTextViewItem(context, params, list.get(i));
            layout.addView(child);
            if (i == 0) {
                child.setBackgroundResource(R.drawable.freeme_content_top_bg_selector);
                layout.addView(newLineViewItem(context, lineParams, lineColor));
            } else if (i == size - 1) {
                child.setBackgroundResource(R.drawable.freeme_content_bottom_bg_selector);
            } else {
                child.setBackgroundResource(R.drawable.freeme_content_center_bg_selector);
                layout.addView(newLineViewItem(context, lineParams, lineColor));
            }
        }

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        int margin = getResources().getDimensionPixelSize(
                R.dimen.freeme_incall_answer_menu_margin);
        layoutParams.bottomMargin = margin;
        layoutParams.leftMargin = margin;
        layoutParams.rightMargin = margin;
        layout.setLayoutParams(layoutParams);

        return layout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentUtils.checkParent(this, FreemeAnswerBootSheetFragment.AnswerMenuSheetHolder.class);
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LogUtil.i("SmsBottomSheetFragment.onCreateDialog", null);
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        return dialog;
    }

    private TextView newTextViewItem(Context context, LinearLayout.LayoutParams params,
                                     final FreemeAnswerFragment.SecondaryBehavior behavior) {
        TextView textView = new TextView(context);
        textView.setTextAppearance(R.style.Dialer_Incall_Answer_Menu_Text);
        textView.setText(behavior.contentDescription);
        textView.setLayoutParams(params);
        textView.setGravity(Gravity.CENTER);

        textView.setOnClickListener((View v) -> {
            if (behavior != null && getParentFragment() instanceof FreemeAnswerFragment) {
                behavior.performAction((FreemeAnswerFragment) getParentFragment());
            }
            dismiss();
        });
        return textView;
    }

    private ImageView newLineViewItem(Context context, LinearLayout.LayoutParams params,
                                      int color) {
        ImageView img = new ImageView(context);
        img.setBackgroundColor(color);
        img.setLayoutParams(params);
        return img;
    }

    @Override
    public void show(FragmentManager fm, String Tag) {
        super.show(fm, TAG_FRAGMENT);
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialog;
    }


    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        FragmentUtils.getParentUnsafe(this, AnswerMenuSheetHolder.class).onAnswerMenuDismissed();
    }

    public interface AnswerMenuSheetHolder {

        void onAnswerMenuDismissed();
    }
}
