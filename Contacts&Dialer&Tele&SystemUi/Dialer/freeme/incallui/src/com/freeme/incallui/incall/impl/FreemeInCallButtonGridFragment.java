package com.freeme.incallui.incall.impl;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.dialer.common.Assert;
import com.android.dialer.common.FragmentUtils;
import com.android.incallui.incall.impl.ButtonChooser;
import com.android.incallui.incall.impl.ButtonController;
import com.android.incallui.incall.impl.CheckableLabeledButton;
import com.android.incallui.incall.impl.R;
import com.android.incallui.incall.protocol.InCallButtonIds;

import java.util.List;
import java.util.Set;

public class FreemeInCallButtonGridFragment extends Fragment {

    /// M: extend
    private static final int BUTTON_COUNT = 12;
    private static final int BUTTONS_PER_ROW = 3;

    private CheckableLabeledButton[] buttons = new CheckableLabeledButton[BUTTON_COUNT];
    private OnButtonGridCreatedListener buttonGridListener;

    public static Fragment newInstance() {
        return new FreemeInCallButtonGridFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        buttonGridListener = FragmentUtils.getParent(this,
                OnButtonGridCreatedListener.class);
        Assert.isNotNull(buttonGridListener);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle bundle) {
        View view = inflater.inflate(R.layout.freeme_incall_button_grid, parent, false);

        buttons[0] = view.findViewById(R.id.incall_first_button);
        buttons[1] = view.findViewById(R.id.incall_second_button);
        buttons[2] = view.findViewById(R.id.incall_third_button);
        buttons[3] = view.findViewById(R.id.incall_fourth_button);
        buttons[4] = view.findViewById(R.id.incall_fifth_button);
        buttons[5] = view.findViewById(R.id.incall_sixth_button);
        /// M: Extend incall buttons @{
        buttons[6] = view.findViewById(R.id.incall_seventh_button);
        buttons[7] = view.findViewById(R.id.incall_eighth_button);
        buttons[8] = view.findViewById(R.id.incall_ninth_button);
        buttons[9] = view.findViewById(R.id.incall_tenth_button);
        buttons[10] = view.findViewById(R.id.incall_eleventh_button);
        buttons[11] = view.findViewById(R.id.incall_twelveth_button);
        /// @}

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        buttonGridListener.onButtonGridCreated(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        buttonGridListener.onButtonGridDestroyed();
    }

    public void onInCallScreenDialpadVisibilityChange(boolean isShowing) {
        for (CheckableLabeledButton button : buttons) {
            button.setImportantForAccessibility(
                    isShowing
                            ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                            : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        }
    }

    public int updateButtonStates(
            List<ButtonController> buttonControllers,
            @Nullable ButtonChooser buttonChooser,
            int voiceNetworkType,
            int phoneType) {
        Set<Integer> allowedButtons = new ArraySet<>();
        Set<Integer> disabledButtons = new ArraySet<>();
        for (ButtonController controller : buttonControllers) {
            if (controller.isAllowed()) {
                allowedButtons.add(controller.getInCallButtonId());
                if (!controller.isEnabled()) {
                    disabledButtons.add(controller.getInCallButtonId());
                }
            }
        }

        for (ButtonController controller : buttonControllers) {
            if (isExcludeButtons(controller.getInCallButtonId())) {
                // The speaker buttons and other buttons are separate and do not need to be reset
                continue;
            }
            controller.setButton(null);
        }

        if (buttonChooser == null) {
            buttonChooser = FreemeButtonChooserFactory.newButtonChooser(voiceNetworkType,
                    false, phoneType);
        }

        int numVisibleButtons = getResources().getInteger(R.integer.incall_num_rows) * BUTTONS_PER_ROW;
        List<Integer> buttonsToPlace =
                buttonChooser.getButtonPlacement(numVisibleButtons, allowedButtons, disabledButtons);

        for (int i = 0; i < BUTTON_COUNT; ++i) {
            if (i >= buttonsToPlace.size()) {
                /// M: set to GONE
                buttons[i].setVisibility(View.GONE);
                continue;
            }
            @InCallButtonIds int button = buttonsToPlace.get(i);
            if (isExcludeButtons(button)) {
                // The speaker buttons and other buttons are separate and do not need to be reset
                continue;
            }
            buttonGridListener.getButtonController(button).setButton(buttons[i]);
        }

        return numVisibleButtons;
    }

    /**
     * Interface to let the listener know the status of the button grid.
     */
    public interface OnButtonGridCreatedListener {
        void onButtonGridCreated(FreemeInCallButtonGridFragment inCallButtonGridFragment);

        void onButtonGridDestroyed();

        ButtonController getButtonController(@InCallButtonIds int id);
    }

    private boolean isExcludeButtons(@InCallButtonIds int button) {
        switch (button) {
            case InCallButtonIds.BUTTON_AUDIO:
            case InCallButtonIds.BUTTON_DIALPAD:
                return true;
        }
        return false;
    }
}
