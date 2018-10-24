package com.freeme.incallui.incall.protocol;

import android.os.Parcel;
import android.os.Parcelable;

import com.android.dialer.common.LogUtil;
import com.android.incallui.call.DialerCall;

import java.util.Locale;

public class FreemeOnHoldInfo implements Parcelable {
    public String id;
    public String name;
    public String number;
    public boolean nameIsNumber;
    public String label;
    public String lookupKey;
    public boolean primary;

    public long connectTimeMillis;
    public int state;

    public boolean isConference;

    public FreemeOnHoldInfo(String id, String name, String number, boolean nameIsNumber,
                            String label, String lookupKey, boolean isConference) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.nameIsNumber = nameIsNumber;
        this.label = label;
        this.lookupKey = lookupKey;
        this.connectTimeMillis = 0;
        this.state = DialerCall.State.ONHOLD;

        this.isConference= isConference;
    }

    public void copyData(FreemeOnHoldInfo data) {
        this.id = data.id;
        this.name = data.name;
        this.number = data.number;
        this.nameIsNumber = data.nameIsNumber;
        this.label = data.label;
        this.lookupKey = data.lookupKey;
        this.connectTimeMillis = data.connectTimeMillis;
        this.state = data.state;
        this.primary = data.primary;
        this.isConference = data.isConference;
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "FreemeOnHoldInfo, callId: %s, name: %s, number: %s, label: %s, ",
                id,
                LogUtil.sanitizePii(name),
                number,
                label);
    }

    protected FreemeOnHoldInfo(Parcel in) {
        id = in.readString();
        name = in.readString();
        number = in.readString();
        nameIsNumber = in.readByte() != 0;
        label = in.readString();
        lookupKey = in.readString();
        connectTimeMillis = in.readLong();
        state = in.readInt();
        primary = in.readByte() != 0;
        isConference = in.readByte() != 0;
    }

    public static final Creator<FreemeOnHoldInfo> CREATOR =
            new Creator<FreemeOnHoldInfo>() {
                @Override
                public FreemeOnHoldInfo createFromParcel(Parcel in) {
                    return new FreemeOnHoldInfo(in);
                }

                @Override
                public FreemeOnHoldInfo[] newArray(int size) {
                    return new FreemeOnHoldInfo[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(number);
        dest.writeByte((byte) (nameIsNumber ? 1 : 0));
        dest.writeString(label);
        dest.writeString(lookupKey);
        dest.writeLong(connectTimeMillis);
        dest.writeInt(state);
        dest.writeByte((byte) (primary ? 1 : 0));
        dest.writeByte((byte) (isConference ? 1 : 0));
    }
}
