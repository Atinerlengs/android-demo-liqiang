package com.freeme.filemanager.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Deflater;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class ArchiveHelper {
    private static final String LOG_TAG = "ArchiveHelper";
    private static final int BUFF_SIZE = 1024 * 1024; // 1M Byte
    private static final String ZIP_ARCHIVE_EXTENSION = "zip";
    private static Context mContext;

    public ArchiveHelper(Context context) {
        mContext = context;
    }

    public static boolean checkIfArchive(final String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            int i = filePath.lastIndexOf(".");
            if ((i > 0)&& (ZIP_ARCHIVE_EXTENSION.equals(filePath.substring(i + 1).toLowerCase())))
                return true;
        }
        return false;
    }

    public static void compressZipArchive(ArrayList<FileInfo> selectFileList, String zipPath, String encoding, String comment)
            throws FileNotFoundException, IOException {
        if (zipPath == null || "".equals(zipPath) || !zipPath.endsWith(".zip")) {
            throw new FileNotFoundException("the save path must end with .zip");
        }
        if (encoding == null || "".equals(encoding)) {
            encoding = "GBK";
        }
        File zipFile = new File(zipPath);
        if (!zipFile.getParentFile().exists()) {
            zipFile.getParentFile().mkdirs();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(zipPath);
        } catch (FileNotFoundException e) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e1) {
                }
            }
        }
        CheckedOutputStream csum = new CheckedOutputStream(fos, new CRC32());
        ZipOutputStream zos = new ZipOutputStream(csum);
        zos.setEncoding(encoding);
        zos.setComment(comment);
        zos.setMethod(ZipOutputStream.DEFLATED);
        zos.setLevel(Deflater.BEST_COMPRESSION);
        BufferedOutputStream bout = null;
        try {
            bout = new BufferedOutputStream(zos);
            for(FileInfo fileInfo : selectFileList) {
                File sourceFile = new File(fileInfo.filePath);
                compressZipRecursive(zos, bout, sourceFile, sourceFile.getParent());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if (bout != null) {
                try {
                    bout.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private static void compressZipRecursive(ZipOutputStream zos, BufferedOutputStream bout, File sourceFile, String prefixDir)
            throws IOException, FileNotFoundException {
        if(FileOperationHelper.mCancelfileoperation){
            return;
        }
        if (sourceFile.isDirectory()) {
            File[] srcFiles = sourceFile.listFiles();
            for (int i = 0; i < srcFiles.length; i++) {
                compressZipRecursive(zos, bout, srcFiles[i], prefixDir);
            }
        } else {
            String entryName = sourceFile.getAbsolutePath().substring(prefixDir.length());
            ZipEntry zipEntry = new ZipEntry(entryName);
            zos.putNextEntry(zipEntry);
            BufferedInputStream bin = null;
            try {
                bin = new BufferedInputStream(new FileInputStream(sourceFile));
                byte[] buffer = new byte[1024];
                int readCount = -1;
                while ((readCount = bin.read(buffer)) != -1) {
                    bout.write(buffer, 0, readCount);
                }
                bout.flush();
                zos.closeEntry();
            } finally {
                if (bin != null) {
                    try {
                        bin.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    public static int decompressZipArchive(String zipfile, String destDir) throws IOException {
        int unzipState = GlobalConsts.DECOMPRESS_ZIP_STATE_SUCCESS;
        BufferedInputStream bi;
        ZipFile zf = new ZipFile(zipfile, "GBK");
        Enumeration e = zf.getEntries();
        Log.i(LOG_TAG, "decompress state is " + e.hasMoreElements());
        if(!e.hasMoreElements()){
            unzipState = GlobalConsts.DECOMPRESS_ZIP_STATE_NO_FILE;
        }
        while (e.hasMoreElements()) {
            if(FileOperationHelper.mCancelfileoperation){
                unzipState = GlobalConsts.DECOMPRESS_ZIP_STATE_CANCEL;
                return unzipState;
            }
            ZipEntry ze2 = (ZipEntry) e.nextElement();
            String entryName = ze2.getName();
            String path = destDir + "/" + entryName;
            if (ze2.isDirectory()) {
                File decompressDirFile = new File(path);
                if (!decompressDirFile.exists())
                {
                    decompressDirFile.mkdirs();
                }
            } else {
                String fileDir = path.substring(0, path.lastIndexOf("/"));
                File fileDirFile = new File(fileDir);
                if (!fileDirFile.exists()) {
                    fileDirFile.mkdirs();
                }
                File file = new File(destDir + "/" + entryName);
                if(file.exists()){
                    unzipState = GlobalConsts.DECOMPRESS_ZIP_STATE_FILE_EXISTS;
                    zf.close();
                    return unzipState;
                }
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destDir + "/" + entryName));
                bi = new BufferedInputStream(zf.getInputStream(ze2));
                byte[] readContent = new byte[1024];
                int readCount = bi.read(readContent);
                while (readCount != -1) {
                    bos.write(readContent, 0, readCount);
                    readCount = bi.read(readContent);
                }
                bos.close();
                Util.scanAllFile(mContext, new String[]{path});
            }
        }
        zf.close();
        return unzipState;
    }

}
