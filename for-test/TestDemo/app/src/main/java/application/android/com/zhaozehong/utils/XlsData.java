package application.android.com.zhaozehong.utils;

public class XlsData {

    private boolean mIsEmpty;

    public XlsData() {
        mIsEmpty = true;
    }

    public XlsData(boolean isEmpty) {
        mIsEmpty = isEmpty;
    }

    public void setEmpty(boolean isEmpty) {
        mIsEmpty = isEmpty;
    }

    public boolean isEmpty() {
        return mIsEmpty;
    }
}
