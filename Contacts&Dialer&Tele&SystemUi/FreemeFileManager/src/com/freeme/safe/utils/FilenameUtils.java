package com.freeme.safe.utils;

public class FilenameUtils {

    private static String getName(String filename) {
        if (filename == null) {
            return null;
        }
        return filename.substring(indexOfLastSeparator(filename) + 1);
    }

    public static String getBaseName(String filename) {
        return removeExtension(getName(filename));
    }

    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int index = indexOfExtension(filename);
        if (index == -1) {
            return "";
        }
        return filename.substring(index + 1);
    }

    private static int indexOfLastSeparator(String filename) {
        if (filename == null) {
            return -1;
        }
        return Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
    }

    private static String removeExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int index = indexOfExtension(filename);
        return index != -1 ? filename.substring(0, index) : filename;
    }

    private static int indexOfExtension(String filename) {
        if (filename == null) {
            return -1;
        }
        int extensionPos = filename.lastIndexOf('.');
        if (indexOfLastSeparator(filename) > extensionPos) {
            extensionPos = -1;
        }
        return extensionPos;
    }
}
