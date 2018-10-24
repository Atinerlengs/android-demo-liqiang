/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2017. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.services.telephony;

import java.util.HashMap;

/**
 * Returned as the Voice codec Type definition.
 */
public enum MtkSpeechCodecType {
    NONE(0),
    QCELP13K(0x0001),
    EVRC(0x0002),
    EVRC_B(0x0003),
    EVRC_WB(0x0004),
    EVRC_NW(0x0005),
    AMR_NB(0x0006),
    AMR_WB(0x0007),
    GSM_EFR(0x0008),
    GSM_FR(0x0009),
    GSM_HR(0x000A);

    private final int mValue;
    private static final HashMap<Integer, MtkSpeechCodecType> sValueToSpeechCodecTypeMap;
    static {
        sValueToSpeechCodecTypeMap = new HashMap<Integer, MtkSpeechCodecType>();
        for (MtkSpeechCodecType sc : values()) {
            sValueToSpeechCodecTypeMap.put(sc.getValue(), sc);
        }
    }

    MtkSpeechCodecType(int value) {
        mValue = value;
    }

    public int getValue() {
        return mValue;
    }

    public boolean isHighDefAudio() {
        return (this == EVRC_WB || this == AMR_WB);
    }


    /**
     *
     * fromInt: Transfers the type to SpeechCodecType Enum.
     *
     * @param value speech codec type
     * @return SpeechCodecType
     */
    public static MtkSpeechCodecType fromInt(int value) {
        MtkSpeechCodecType type = sValueToSpeechCodecTypeMap.get(value);
        if (type == null) {
            type = NONE;
        }
        return type;
    }
}
