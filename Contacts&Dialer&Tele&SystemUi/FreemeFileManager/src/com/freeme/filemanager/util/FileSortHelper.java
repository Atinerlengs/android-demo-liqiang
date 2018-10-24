/*
 * This file is part of FileManager.
 * FileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * TYD Inc. (C) 2012. All rights reserved.
 */
package com.freeme.filemanager.util;

import java.util.Comparator;
import java.util.HashMap;

import com.freeme.filemanager.model.FileInfo;

public class FileSortHelper {

    public enum SortMethod {
        name, size, date, type
    }

    public SortMethod mSort;

    private boolean mFileFirst;

    private HashMap<SortMethod, Comparator> mComparatorList = new HashMap<SortMethod, Comparator>();

    public FileSortHelper(SortMethod sort) {
        mSort = (sort != null ? sort:SortMethod.name);
        mComparatorList.put(SortMethod.name, cmpName);
        mComparatorList.put(SortMethod.size, cmpSize);
        mComparatorList.put(SortMethod.date, cmpDate);
        mComparatorList.put(SortMethod.type, cmpType);
    }

    public void setSortMethod(SortMethod s) {
        mSort = s;
    }

    public SortMethod getSortMethod() {
        return mSort;
    }

    public int getIntSortMethod() {
        if (mSort == SortMethod.name) {
            return 0;
        } else if (mSort == SortMethod.size) {
            return 1;
        } else if (mSort == SortMethod.date) {
            return 2;
        } else if (mSort == SortMethod.type) {
            return 3;
        } else {
            return 0;
        }
    }

    public void setFileFirst(boolean f) {
        mFileFirst = f;
    }

    public Comparator getComparator() {
        return mComparatorList.get(mSort);
    }

    private abstract class FileComparator implements Comparator<FileInfo> {

        @Override
        public int compare(FileInfo object1, FileInfo object2) {
            if ((object1.IsDir && object2.IsDir) || (!object1.IsDir && !object2.IsDir) ) {
                return doCompare(object1, object2);
            }

            if (mFileFirst) {
                // the files are listed before the dirs
                return (object1.IsDir ? 1 : -1);
            } else {
                // the dir-s are listed before the files
                return object1.IsDir ? -1 : 1;
            }
        }

        protected abstract int doCompare(FileInfo object1, FileInfo object2);
    }

    private Comparator cmpName = new FileComparator() {
        @Override
        public int doCompare(FileInfo object1, FileInfo object2) {
            return object1.fileName.compareToIgnoreCase(object2.fileName);
        }
    };

    private Comparator cmpSize = new FileComparator() {
        @Override
        public int doCompare(FileInfo op, FileInfo oq) {
            //sort from big to small
            if (!op.IsDir && !oq.IsDir) {
                long opSize = op.fileSize;
                long oqSize = oq.fileSize;
                if (opSize != oqSize) {
                    return opSize > oqSize ? -1 : 1;
                }
            } else if (op.IsDir && oq.IsDir) {
                int opCnt = op.Count;
                int oqCnt = oq.Count;
                if (opCnt != oqCnt) {
                    return opCnt > oqCnt ? -1 : 1;
                }
            }
            return op.fileName.compareToIgnoreCase(oq.fileName);
        }
    };

    private Comparator cmpDate = new FileComparator() {
        @Override
        public int doCompare(FileInfo op, FileInfo oq) {
            long opTime = op.ModifiedDate;
            long oqTime = oq.ModifiedDate;
            if (opTime != oqTime) {
                return opTime > oqTime ? -1 : 1;
            }
            return op.fileName.compareToIgnoreCase(oq.fileName);
        }
    };

    private int longToCompareInt(long result) {
        return result > 0 ? 1 : (result < 0 ? -1 : 0);
    }

    private Comparator cmpType = new FileComparator() {
        @Override
        public int doCompare(FileInfo op, FileInfo oq) {
            boolean isOpDirectory = op.IsDir;
            boolean isOqDirectory = oq.IsDir;
            if (!isOpDirectory && !isOqDirectory) {
                // both are not directory
                String opExtension = Util.getExtFromFilename(op.fileName);
                String oqExtension = Util.getExtFromFilename(oq.fileName);
                if (opExtension == null && oqExtension != null) {
                    return -1;
                } else if (opExtension != null && oqExtension == null) {
                    return 1;
                } else if (opExtension != null && oqExtension != null) {
                    if (!opExtension.equalsIgnoreCase(oqExtension)) {
                        return opExtension.compareToIgnoreCase(oqExtension);
                    }
                }
            }
            return op.fileName.compareToIgnoreCase(oq.fileName);
        }
    };
}
