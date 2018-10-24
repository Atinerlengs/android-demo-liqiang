/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.freeme.filemanager.controller;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import com.freeme.filemanager.model.EditUtility;
import com.freeme.filemanager.model.FileInfo;

public class FileInfoComparator implements Comparator<FileInfo>, Serializable {

    enum SortBy {
        TYPE, NAME, SIZE, TIME
    }

    private static final long serialVersionUID = 1L;
    private int mSortBy = 0;

    /**
     * The constructor to construct a FileComparator
     * @param sort the sorting mode
     */
    public FileInfoComparator(int sort) {
        mSortBy = sort;
    }

    /**
     * This method compares the files based on the order: category folders->common folders->files
     * @param op the first file
     * @param oq the second file
     * @return a negative integer, zero, or a positive integer as the first file is smaller than,
     *         equal to, or greater than the second file, ignoring case considerations.
     */
    @Override
    public int compare(FileInfo op, FileInfo oq) {
        if (mSortBy == SortBy.TYPE.ordinal()) {
            return sortByType(op, oq);
        } else if (mSortBy == SortBy.NAME.ordinal()) {
            return sortByName(op, oq);
        } else if (mSortBy == SortBy.SIZE.ordinal()) {
            return sortBySize(op, oq);
        } else {
            return sortByTime(op, oq);
        }
    }

    /**
     * This method compares the files based on their type
     * @param op the first file
     * @param oq the second file
     * @return a negative integer, zero, or a positive integer as the first file is smaller than,
     *         equal to, or greater than the second file, ignoring case considerations.
     */
    private int sortByType(FileInfo op, FileInfo oq) {
        if (op.isDirectory() && !oq.isDirectory()) {
            return -1;
        } else if (!op.isDirectory() && oq.isDirectory()) {
            return 1;
        } else if (op.isDirectory() && oq.isDirectory()) {
            return op.getFileDescription().compareToIgnoreCase(oq.getFileDescription());
        } else {
            // get file extension
            String opExt = EditUtility.getFileExtension(op.getFileDescription());
            String oqExt = EditUtility.getFileExtension(oq.getFileDescription());

            if (opExt == null && oqExt != null) {
                return -1;
            } else if (opExt != null && oqExt == null) {
                return 1;
            } else if (opExt == null && oqExt == null) {
                return op.getFileDescription().compareToIgnoreCase(oq.getFileDescription());
            } else {
                if (opExt == null) {
                    return -1;
                } else {
                    if (opExt.equalsIgnoreCase(oqExt)) {
                        return op.getFileDescription().compareToIgnoreCase(oq.getFileDescription());
                    } else {
                        return opExt.compareToIgnoreCase(oqExt);
                    }
                }
            }
        }
    }

    /**
     * This method compares the files based on their names
     * @param op the first file
     * @param oq the second file
     * @return a negative integer, zero, or a positive integer as the first file is smaller than,
     *         equal to, or greater than the second file, ignoring case considerations.
     */
    private int sortByName(FileInfo op, FileInfo oq) {
        return op.getFileDescription().compareToIgnoreCase(oq.getFileDescription());
    }

    /**
     * This method compares the files based on their sizes
     * @param op the first file
     * @param oq the second file
     * @return a negative integer, zero, or a positive integer as the first file is smaller than,
     *         equal to, or greater than the second file, ignoring case considerations.
     */
    private int sortBySize(FileInfo op, FileInfo oq) {
        if (op.isDirectory() && !oq.isDirectory()) {
            return -1;
        } else if (!op.isDirectory() && oq.isDirectory()) {
            return 1;
        } else {
            if (op.getFileSize() < oq.getFileSize()) {
                return 1;
            } else if (op.getFileSize() > oq.getFileSize()) {
                return -1;
            } else {
                return op.getFileDescription().compareToIgnoreCase(oq.getFileDescription());
            }
        }
    }

    /**
     * This method compares the files based on their modified time
     * @param op the first file
     * @param oq the second file
     * @return a negative integer, zero, or a positive integer as the first file is smaller than,
     *         equal to, or greater than the second file, ignoring case considerations.
     */
    private int sortByTime(FileInfo op, FileInfo oq) {
        if (op.getFileLastModifiedTime() > oq.getFileLastModifiedTime()) {
            return -1;
        } else if (op.getFileLastModifiedTime() < oq.getFileLastModifiedTime()) {
            return 1;
        } else {
            return op.getFileDescription().compareToIgnoreCase(oq.getFileDescription());
        }
    }

    /**
     * This method searches the specified array for the specified string using the binary search
     * algorithm.
     * @param array the list to be searched
     * @param key the key to be searched for
     * @return the index of the search key, if it is contained in the list; otherwise, -1.
     */
    public static int binarySearch(String[] array, String key) {
        return binarySearch(array, key, 0, array.length - 1);
    }

    /**
     * This method searches the specified array for the specified string using the binary search
     * algorithm.
     * @param array the list to be searched
     * @param key the key to be searched for
     * @param left the start index to search
     * @param right the end index to search
     * @return the index of the search key, if it is contained in the list; otherwise, -1.
     */
    private static int binarySearch(String[] array, String key, int left, int right) {
        if (left > right) {
            return -1;
        }

        int middle = (left + right) >>> 1;
        if (array[middle].compareToIgnoreCase(key) == 0) {
            return middle;
        } else if (array[middle].compareToIgnoreCase(key) > 0) {
            return binarySearch(array, key, left, middle - 1);
        } else {
            return binarySearch(array, key, middle + 1, right);
        }
    }

    public static int sequenceSearch(List<String> list, String key) {
        if (list == null || key == null) {
            return -1;
        }
        for (int i = 0; i < list.size(); i++) {
            String item = list.get(i);
            if (item.equalsIgnoreCase(key)) {
                return i;
            }
        }
        return -1;
    }
}