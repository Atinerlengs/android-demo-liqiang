/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.list;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.util.ViewUtil;

//*/ freeme.zhaozehong, 20180411. for freemeOS, reset bg color
import android.util.TypedValue;
//*/

/**
 * A custom view for the pinned section header shown at the top of the contact list.
 */
public class ContactListPinnedHeaderView extends TextView {

    public ContactListPinnedHeaderView(Context context, AttributeSet attrs, View parent) {
        super(context, attrs);

        if (R.styleable.ContactListItemView == null) {
            return;
        }
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ContactListItemView);
        int backgroundColor = a.getColor(
                R.styleable.ContactListItemView_list_item_background_color, Color.WHITE);
        int textOffsetTop = a.getDimensionPixelSize(
                R.styleable.ContactListItemView_list_item_text_offset_top, 0);
        /*/ freeme.liqiang. 20180301. for FreemeOS reset padding value
        int paddingStartOffset = a.getDimensionPixelSize(
                R.styleable.ContactListItemView_list_item_padding_left, 0);
        /*/
        int paddingStartOffset = a.getDimensionPixelSize(
                com.android.contacts.common.R.styleable.ContactListItemView_list_item_gap_between_image_and_text, 0);
        //*/
        int textWidth = getResources().getDimensionPixelSize(
                R.dimen.contact_list_section_header_width);
        int widthIncludingPadding = paddingStartOffset + textWidth;
        a.recycle();

        /*/ freeme.zhaozehong, 20180411. for freemeOS, reset color
        setBackgroundColor(backgroundColor);
        /*/
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(
                android.R.attr.colorBackground, outValue, true);
        int resid = outValue.resourceId;
        setBackgroundResource(resid);
        //*/
        setTextAppearance(getContext(), R.style.SectionHeaderStyle);
        /*/ freeme.liqiang. 20180301. for FreemeOS reset padding value
        setLayoutParams(new LayoutParams(widthIncludingPadding, LayoutParams.WRAP_CONTENT));
        /*/
        int height = getResources().getDimensionPixelSize(com.android.contacts.common.R.dimen.freeme_list_item_header_height);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, height));
        //*/
        setLayoutDirection(parent.getLayoutDirection());
        /*/ freeme.liqiang. 20180301. for FreemeOS reset padding value
        setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        /*/
        setGravity(Gravity.CENTER_VERTICAL |
                (ViewUtil.isViewLayoutRtl(this) ? Gravity.RIGHT : Gravity.LEFT));
        //*/

        // Apply text top offset. Multiply by two, because we are implementing this by padding for a
        // vertically centered view, rather than adjusting the position directly via a layout.
        setPaddingRelative(
                getPaddingStart() + paddingStartOffset,
                getPaddingTop() + (textOffsetTop * 2),
                getPaddingEnd(),
                getPaddingBottom());
        //*/ freeme.liqiang. 20180301. for FreemeOS reset padding value
        bottonLineColor = getResources().getColor(com.android.contacts.common.R.color.freeme_list_divider_color);
        lineStartPadding = paddingStartOffset;
        //*/
    }

    /**
     * Sets section header or makes it invisible if the title is null.
     */
    public void setSectionHeaderTitle(String title) {
        if (title != null) {
            setText(title);
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

    //*/ freeme.liqiang. 20180301. for FreemeOS add bottom line
    private int bottonLineColor;
    private int lineStartPadding;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(bottonLineColor);
        paint.setStrokeWidth(2);
        canvas.drawLine(lineStartPadding, getHeight(), getWidth(), getHeight(), paint);
    }
    //*/
}
