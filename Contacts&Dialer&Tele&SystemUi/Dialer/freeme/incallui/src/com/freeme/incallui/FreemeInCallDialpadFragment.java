package com.freeme.incallui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.util.ArrayMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.android.contacts.common.compat.PhoneNumberUtilsCompat;
import com.android.dialer.dialpadview.DialpadKeyButton;
import com.android.incallui.DialpadPresenter;
import com.android.incallui.DialpadPresenter.DialpadUi;
import com.android.incallui.InCallActivity;
import com.android.incallui.baseui.BaseFragment;
import com.android.incallui.Log;
import com.android.incallui.R;
import com.freeme.contacts.common.utils.FreemeToast;
import com.freeme.dialer.dialpadview.FreemeDialpadView;

import java.util.Map;

public class FreemeInCallDialpadFragment
        extends BaseFragment<DialpadPresenter, DialpadUi>
        implements DialpadUi, View.OnKeyListener, View.OnClickListener,
        View.OnLongClickListener, DialpadKeyButton.OnPressedListener {

    /**
     * Hash Map to map a view id to a character
     */
    private static final Map<Integer, Character> mDisplayMap = new ArrayMap<>();

    /** Set up the static maps */
    static {
        // Map the buttons to the display characters
        mDisplayMap.put(R.id.one, '1');
        mDisplayMap.put(R.id.two, '2');
        mDisplayMap.put(R.id.three, '3');
        mDisplayMap.put(R.id.four, '4');
        mDisplayMap.put(R.id.five, '5');
        mDisplayMap.put(R.id.six, '6');
        mDisplayMap.put(R.id.seven, '7');
        mDisplayMap.put(R.id.eight, '8');
        mDisplayMap.put(R.id.nine, '9');
        mDisplayMap.put(R.id.zero, '0');
        mDisplayMap.put(R.id.pound, '#');
        mDisplayMap.put(R.id.star, '*');
    }

    private final int[] mButtonIds =
            new int[]{
                    R.id.zero,
                    R.id.one,
                    R.id.two,
                    R.id.three,
                    R.id.four,
                    R.id.five,
                    R.id.six,
                    R.id.seven,
                    R.id.eight,
                    R.id.nine,
                    R.id.star,
                    R.id.pound
            };

    private EditText mDtmfDialerField;
    private ImageButton mDeleteButton;
    // KeyListener used with the "dialpad digits" EditText widget.
    private DTMFKeyListener mDialerKeyListener;
    private FreemeDialpadView mDialpadView;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dialpad_back:
                getActivity().onBackPressed();
                break;
            case R.id.deleteButton:
                deleteDigits(false);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        Log.d(this, "onKey:  keyCode " + keyCode + ", view " + v);

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            int viewId = v.getId();
            if (mDisplayMap.containsKey(viewId)) {
                switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        if (event.getRepeatCount() == 0) {
                            getPresenter().processDtmf(mDisplayMap.get(viewId));
                        }
                        break;
                    case KeyEvent.ACTION_UP:
                        getPresenter().stopDtmf();
                        break;
                }
                // do not return true [handled] here, since we want the
                // press / click animation to be handled by the framework.
            }
        }
        return false;
    }

    @Override
    public DialpadPresenter createPresenter() {
        return new DialpadPresenter();
    }

    @Override
    public DialpadUi getUi() {
        return this;
    }

    // TODO Adds hardware keyboard listener

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View parent = inflater.inflate(R.layout.freeme_incall_dialpad_fragment, container, false);
        mDialpadView = parent.findViewById(R.id.dialpad_view);
        mDialpadView.setCanDigitsBeEdited(false);
        mDialpadView.setNumberIcons(numberIcons);
        mDtmfDialerField = parent.findViewById(R.id.digits);
        if (mDtmfDialerField != null) {
            // remove the long-press context menus that support
            // the edit (copy / paste / select) functions.
            mDtmfDialerField.setLongClickable(false);
            mDtmfDialerField.setElegantTextHeight(false);
            mDtmfDialerField.addTextChangedListener(new NumberTextWatcher());
            configureKeypadListeners();
        }
        mDeleteButton = parent.findViewById(R.id.deleteButton);
        mDeleteButton.setOnClickListener(this);
        mDeleteButton.setOnLongClickListener(this);
        if (savedInstanceState != null) {
            mIsNumberRecord = savedInstanceState.getBoolean(IS_NUMBER_RECORD);
        }
        initDiapladStatus();

        return parent;
    }

    @Override
    public void onDestroyView() {
        mDialerKeyListener = null;
        super.onDestroyView();
    }

    /**
     * Getter for Dialpad text.
     *
     * @return String containing current Dialpad EditText text.
     */
    public String getDtmfText() {
        return mDtmfDialerField.getText().toString();
    }

    /**
     * Sets the Dialpad text field with some text.
     *
     * @param text Text to set Dialpad EditText to.
     */
    public void setDtmfText(String text) {
        mDtmfDialerField.setText(PhoneNumberUtilsCompat.createTtsSpannable(text));
        /// M: set the focus to the end of the text
        mDtmfDialerField.setSelection(mDtmfDialerField.length());
    }

    @Override
    public void setVisible(boolean on) {
        if (on) {
            getView().setVisibility(View.VISIBLE);
        } else {
            getView().setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Starts the slide up animation for the Dialpad keys when the Dialpad is revealed.
     */
    public void animateShowDialpad() {
        final FreemeDialpadView dialpadView = getView().findViewById(R.id.dialpad_view);
        dialpadView.animateShow();
    }

    @Override
    public void appendDigitsToField(char digit) {
        if (mDtmfDialerField != null) {
            // TODO: maybe *don't* manually append this digit if
            // mDialpadDigits is focused and this key came from the HW
            // keyboard, since in that case the EditText field will
            // get the key event directly and automatically appends
            // whetever the user types.
            // (Or, a cleaner fix would be to just make mDialpadDigits
            // *not* handle HW key presses.  That seems to be more
            // complicated than just setting focusable="false" on it,
            // though.)
            mDtmfDialerField.getText().append(digit);
        }
    }

    /**
     * Called externally (from InCallScreen) to play a DTMF Tone.
     */
  /* package */ boolean onDialerKeyDown(KeyEvent event) {
        Log.d(this, "Notifying dtmf key down.");
        if (mDialerKeyListener != null) {
            return mDialerKeyListener.onKeyDown(event);
        } else {
            return false;
        }
    }

    /**
     * Called externally (from InCallScreen) to cancel the last DTMF Tone played.
     */
    public boolean onDialerKeyUp(KeyEvent event) {
        Log.d(this, "Notifying dtmf key up.");
        if (mDialerKeyListener != null) {
            return mDialerKeyListener.onKeyUp(event);
        } else {
            return false;
        }
    }

    private void configureKeypadListeners() {
        DialpadKeyButton dialpadKey;
        for (int i = 0; i < mButtonIds.length; i++) {
            dialpadKey = (DialpadKeyButton) mDialpadView.findViewById(mButtonIds[i]);
            dialpadKey.setOnKeyListener(this);
            dialpadKey.setOnClickListener(this);
            dialpadKey.setOnPressedListener(this);
        }
    }

    @Override
    public void onPressed(View view, boolean pressed) {
        if (pressed && mDisplayMap.containsKey(view.getId())) {
            Log.d(this, "onPressed: " + pressed + " " + mDisplayMap.get(view.getId()));
            getPresenter().processDtmf(mDisplayMap.get(view.getId()));
        }
        if (!pressed) {
            Log.d(this, "onPressed: " + pressed);
            getPresenter().stopDtmf();
        }
    }

    /**
     * Our own key listener, specialized for dealing with DTMF codes. 1. Ignore the backspace since it
     * is irrelevant. 2. Allow ONLY valid DTMF characters to generate a tone and be sent as a DTMF
     * code. 3. All other remaining characters are handled by the superclass.
     * <p>
     * <p>This code is purely here to handle events from the hardware keyboard while the DTMF dialpad
     * is up.
     */
    private class DTMFKeyListener extends DialerKeyListener {

        /**
         * Overrides the characters used in {@link DialerKeyListener#CHARACTERS} These are the valid
         * dtmf characters.
         */
        public final char[] DTMF_CHARACTERS =
                new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '#', '*'};

        private DTMFKeyListener() {
            super();
        }

        /**
         * Overriden to return correct DTMF-dialable characters.
         */
        @Override
        protected char[] getAcceptedChars() {
            return DTMF_CHARACTERS;
        }

        /**
         * special key listener ignores backspace.
         */
        @Override
        public boolean backspace(View view, Editable content, int keyCode, KeyEvent event) {
            return false;
        }

        /**
         * Overriden so that with each valid button press, we start sending a dtmf code and play a local
         * dtmf tone.
         */
        @Override
        public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
            // if (DBG) log("DTMFKeyListener.onKeyDown, keyCode " + keyCode + ", view " + view);

            // find the character
            char c = (char) lookup(event, content);

            // if not a long press, and parent onKeyDown accepts the input
            if (event.getRepeatCount() == 0 && super.onKeyDown(view, content, keyCode, event)) {

                boolean keyOK = ok(getAcceptedChars(), c);

                // if the character is a valid dtmf code, start playing the tone and send the
                // code.
                if (keyOK) {
                    Log.d(this, "DTMFKeyListener reading '" + c + "' from input.");
                    getPresenter().processDtmf(c);
                } else {
                    Log.d(this, "DTMFKeyListener rejecting '" + c + "' from input.");
                }
                return true;
            }
            return false;
        }

        /**
         * Overriden so that with each valid button up, we stop sending a dtmf code and the dtmf tone.
         */
        @Override
        public boolean onKeyUp(View view, Editable content, int keyCode, KeyEvent event) {
            // if (DBG) log("DTMFKeyListener.onKeyUp, keyCode " + keyCode + ", view " + view);

            super.onKeyUp(view, content, keyCode, event);

            // find the character
            char c = (char) lookup(event, content);

            boolean keyOK = ok(getAcceptedChars(), c);

            if (keyOK) {
                Log.d(this, "Stopping the tone for '" + c + "'");
                getPresenter().stopDtmf();
                return true;
            }

            return false;
        }

        /**
         * Handle individual keydown events when we DO NOT have an Editable handy.
         */
        public boolean onKeyDown(KeyEvent event) {
            char c = lookup(event);
            Log.d(this, "DTMFKeyListener.onKeyDown: event '" + c + "'");

            // if not a long press, and parent onKeyDown accepts the input
            if (event.getRepeatCount() == 0 && c != 0) {
                // if the character is a valid dtmf code, start playing the tone and send the
                // code.
                if (ok(getAcceptedChars(), c)) {
                    Log.d(this, "DTMFKeyListener reading '" + c + "' from input.");
                    getPresenter().processDtmf(c);
                    return true;
                } else {
                    Log.d(this, "DTMFKeyListener rejecting '" + c + "' from input.");
                }
            }
            return false;
        }

        /**
         * Handle individual keyup events.
         *
         * @param event is the event we are trying to stop. If this is null, then we just force-stop the
         *              last tone without checking if the event is an acceptable dialer event.
         */
        public boolean onKeyUp(KeyEvent event) {
            if (event == null) {
                //the below piece of code sends stopDTMF event unnecessarily even when a null event
                //is received, hence commenting it.
        /*if (DBG) log("Stopping the last played tone.");
        stopTone();*/
                return true;
            }

            char c = lookup(event);
            Log.d(this, "DTMFKeyListener.onKeyUp: event '" + c + "'");

            // TODO: stopTone does not take in character input, we may want to
            // consider checking for this ourselves.
            if (ok(getAcceptedChars(), c)) {
                Log.d(this, "Stopping the tone for '" + c + "'");
                getPresenter().stopDtmf();
                return true;
            }

            return false;
        }

        /**
         * Find the Dialer Key mapped to this event.
         *
         * @return The char value of the input event, otherwise 0 if no matching character was found.
         */
        private char lookup(KeyEvent event) {
            // This code is similar to {@link DialerKeyListener#lookup(KeyEvent, Spannable) lookup}
            int meta = event.getMetaState();
            int number = event.getNumber();

            if (!((meta & (KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON)) == 0) || (number == 0)) {
                int match = event.getMatch(getAcceptedChars(), meta);
                number = (match != 0) ? match : number;
            }

            return (char) number;
        }

        /**
         * Check to see if the keyEvent is dialable.
         */
        boolean isKeyEventAcceptable(KeyEvent event) {
            return (ok(getAcceptedChars(), lookup(event)));
        }
    }

    private class NumberTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Activity activity = getActivity();
            if (activity instanceof InCallActivity) {
                if (mIsNumberRecord) {
                    ((InCallActivity) activity).setRecordNumber(s.toString());
                } else {
                    ((InCallActivity) activity).setNumber(s.toString());
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private boolean mIsNumberRecord;

    public void setIsNumberRecord(boolean isNumberRecord) {
        this.mIsNumberRecord = isNumberRecord;
    }

    public void resetDialpad(String number) {
        initDiapladStatus();
        mDtmfDialerField.setText(number);
        mDtmfDialerField.setSelection(mDtmfDialerField.length());
        if (mIsNumberRecord) {
            FreemeToast.toast(getActivity(), R.string.toast_recordnumber_show);
        }
    }

    private final int[] numberIcons = new int[]{R.drawable.freeme_incallui_dialpad_num_0,
            R.drawable.freeme_incallui_dialpad_num_1, R.drawable.freeme_incallui_dialpad_num_2,
            R.drawable.freeme_incallui_dialpad_num_3, R.drawable.freeme_incallui_dialpad_num_4,
            R.drawable.freeme_incallui_dialpad_num_5, R.drawable.freeme_incallui_dialpad_num_6,
            R.drawable.freeme_incallui_dialpad_num_7, R.drawable.freeme_incallui_dialpad_num_8,
            R.drawable.freeme_incallui_dialpad_num_9, R.drawable.freeme_incallui_dialpad_num_star,
            R.drawable.freeme_incallui_dialpad_num_pound};

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.deleteButton) {
            deleteDigits(true);
            return true;
        }
        return false;
    }

    private void deleteDigits(boolean clear) {
        if (mDtmfDialerField != null) {
            if (clear) {
                final Editable digits = mDtmfDialerField.getText();
                digits.clear();
            } else {
                int keyCode = KeyEvent.KEYCODE_DEL;
                KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
                mDtmfDialerField.onKeyDown(keyCode, event);
            }
        }
    }

    private static final String IS_NUMBER_RECORD = "is_number_record";
    private void initDiapladStatus() {
        mDeleteButton.setVisibility(!mIsNumberRecord ? View.GONE : View.VISIBLE);
        String hint = mIsNumberRecord ? getString(R.string.onscreenRecordNumberText) : null;
        mDtmfDialerField.setHint(hint);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_NUMBER_RECORD, mIsNumberRecord);
    }

}
