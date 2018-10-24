package com.freeme.safe.model;

import com.freeme.filemanager.model.FileInfo;

public class EncryptionFileInfo extends FileInfo {
    private String mEncryptionName;
    private String mOriginalPath;
    private String mRootPath;

    private boolean mSelected;
    private long mOriginalSize;
    private int mMediaType;
    private int mOriginalCount;
    private int mType;

    public EncryptionFileInfo(String originalPath, String encryptionName, String rootPath,
                              long originalSize, int type, int originalCount, int mediaType) {
        mOriginalPath = originalPath;
        mEncryptionName = encryptionName;
        mOriginalSize = originalSize;
        mRootPath = rootPath;
        mType = type;
        mOriginalCount = originalCount;
        mMediaType = mediaType;
    }

    public void setOriginalPath(String path) {
        mOriginalPath = path;
    }

    public String getOriginalPath() {
        return mOriginalPath;
    }

    public void setEncryptionName(String name) {
        mEncryptionName = name;
    }

    public String getEncryptionName() {
        return mEncryptionName;
    }

    public void setOriginalFileSize(long size) {
        mOriginalSize = size;
    }

    public long getOriginalFileSize() {
        return mOriginalSize;
    }

    public void setOriginalFileType(int type) {
        mType = type;
    }

    public int getOriginalFileType() {
        return mType;
    }

    public void setOriginalCount(int originalCount) {
        mOriginalCount = originalCount;
    }

    public int getOriginalCount() {
        return mOriginalCount;
    }

    public void setMediaType(int mediaType) {
        mMediaType = mediaType;
    }

    public int getMediaType() {
        return mMediaType;
    }

    public void setRootPath(String rootPath) {
        mRootPath = rootPath;
    }

    public String getRootPath() {
        return mRootPath;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }
    public boolean getSelected() {
        return mSelected;
    }

    public void clear() {
        mOriginalPath = null;
        mEncryptionName = null;
        mOriginalSize = 0;
        mRootPath = null;
        mType = 0;
    }

    public String toString() {
        if (mEncryptionName != null) {
            return mEncryptionName;
        }
        return "";
    }

    public int hashCode() {
        String temp = toString();
        return temp == null ? 0 : temp.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        EncryptionFileInfo other = (EncryptionFileInfo) obj;
        String str = toString();
        String otherStr = other.toString();
        return str != null && str.equals(otherStr) || otherStr == null;
    }
}