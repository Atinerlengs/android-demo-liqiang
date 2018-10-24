package com.freeme.safe.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaScannerConnection;
import android.os.StatFs;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Log;

import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.MediaFile;
import com.freeme.filemanager.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES;

public class SafeUtils {
    private static final String TAG = "SafeUtils";
    public static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String DEFAULT_SAFE_FOLDER = ".androidsafe";
    private static final String THUMB_TEMP = "_temp";

    private static final int MAX_ENCRYPT_LEN = 16;

    public static ArrayList<FileInfo> getEncrypFileList() {
        return null;
    }

    public static String getMD5(String source) {
        if (TextUtils.isEmpty(source)) {
            return null;
        }
        byte[] sourceByte = source.getBytes();
        char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sourceByte);
            byte[] tmp = md.digest();
            char[] str = new char[32];
            int k = 0;
            for (int i = 0; i < MAX_ENCRYPT_LEN; i++) {
                byte byte0 = tmp[i];
                int i2 = k + 1;
                str[k] = hexDigits[(byte0 >>> 4) & (MAX_ENCRYPT_LEN - 1)];
                k = i2 + 1;
                str[i2] = hexDigits[byte0 & (MAX_ENCRYPT_LEN - 1)];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressLint("GetInstance")
    public static Cipher initAESCipher(String sKey, int cipherMode) {
        Cipher cipher;
        try {
            SecureRandom random;
            if (SDK_INT > VERSION_CODES.M) {
                random = SecureRandom.getInstance("SHA1PRNG", new CryptoProvider());
            } else {
                random = SecureRandom.getInstance("SHA1PRNG", "Crypto");
            }

            random.setSeed(sKey.getBytes());
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES);
            keyGenerator.init(128, random);
            SecretKeySpec keySpec = new SecretKeySpec(keyGenerator.generateKey().getEncoded(),
                    KeyProperties.KEY_ALGORITHM_AES);

            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES);
            cipher.init(cipherMode, keySpec);
            return cipher;

        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | NoSuchProviderException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getSafeFilePath(String rootPath, String name) {
        if (rootPath == null || name == null) {
            Log.e(TAG, "getModeSavePath param error");
            return null;
        }
        return rootPath + File.separator + DEFAULT_SAFE_FOLDER
                + File.separator + name;
    }

    public static int getLockMode(Context context, String path) {
        SharedPreferences sp = getSafeSharedPreference(context, Context.MODE_PRIVATE);
        int mode = -1;
        if (sp != null) {
            mode = sp.getInt(SafeConstants.SAFE_LOCK_MODE_KEY, -1);
            if (mode == -1) {
                String str = getUnlockParam(path);
                if (str != null) {
                    mode = Integer.valueOf(str);
                }
            }
        } else {
            if (DEBUG) {
                Log.v(TAG, "getLockMode xml file open fail");
            }
        }
        return mode;
    }

    public static String getPasswordWithoutEncryption(Context context) {
        String password = getPassword(context, getSafeFilePath(SafeConstants.SAFE_ROOT_PATH,
                SafeConstants.LOCK_PASSWORD_PATH));
        if (password != null) {
            return dencrypString(password, initAESCipher(SafeConstants.ENCRYPTION_KEY, Cipher.DECRYPT_MODE));
        }
        return null;
    }

    public static String getPassword(Context context, String path) {
        SharedPreferences sp = getSafeSharedPreference(context, Context.MODE_PRIVATE);
        String password = null;
        if (sp != null) {
            password = sp.getString(SafeConstants.SAFE_UNLOCK_PASSWORD, null);
            if (password == null) {
                password = getUnlockParam(path);
            }
        } else {
            if (DEBUG) {
                Log.v(TAG, "getPassword xml file open fail");
            }
        }
        return password;
    }

    private static String getUnlockParam(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        File file = new File(path);
        String str = null;
        if (!file.exists() || file.length() <= 0) {
            return null;
        }
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            str = raf.readUTF();
            raf.close();
            return str;
        } catch (IOException e) {
            e.printStackTrace();
            return str;
        }
    }

    private static SharedPreferences getSafeSharedPreference(Context context, int mode) {
        return context.getSharedPreferences(SafeConstants.SAFE_RECORD_NAME, mode);
    }

    public static String getEncryptionPath(String name) {
        if (name == null) {
            return null;
        }
        return SafeConstants.PRIVATE_FILE_PATH + File.separator + name;
    }

    public static String getEncryptionPath(String root, String name) {
        if (root == null || name == null) {
            return null;
        }
        return root + File.separator + DEFAULT_SAFE_FOLDER
                + File.separator + name;
    }

    private static long getStorageAvailableSize(String path) {
        long blockSize;
        long blockCount;
        try {
            StatFs statFs = new StatFs(path);
            blockSize = statFs.getBlockSizeLong();
            blockCount = statFs.getAvailableBlocksLong();
        } catch (Exception e) {
            e.printStackTrace();
            blockSize = 0;
            blockCount = 0;
        }
        return blockCount * blockSize;
    }

    public static boolean isStorageEnable(int type, long length, String rootPath) {
        if (rootPath != null) {
            long storageSize = getStorageAvailableSize(rootPath);
            boolean isDirectly = isDirectlyEncryp(type);
            if ((isDirectly && storageSize < SafeConstants.THRESHOLD)
                    || (!isDirectly && storageSize < length + SafeConstants.THRESHOLD)) {
                return false;
            }
        }
        return true;
    }


    public static String fetchRootPathByFile(String path) {
        return path.startsWith(Util.MEMORY_DIR) ? Util.MEMORY_DIR : null;
    }

    public static File getThumbFile(File file) {
        return new File(file.getAbsolutePath() + THUMB_TEMP);
    }

    public static void setPrivatePathPermition() {
        if (DEBUG) {
            Log.d(TAG, "setPrivatePathPermition");
        }
        try {
            Runtime.getRuntime().exec("chmod 750 " + SafeConstants.PRIVATE_FILE_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setSafePathPermition(String safepath) {
        if (DEBUG) {
            Log.d(TAG, "setSafePathPermition");
        }
        try {
            Runtime.getRuntime().exec("chmod 755 " + safepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void mkSafeFolder() {
        String safeFilePath = SafeConstants.SAFE_ROOT_PATH
                + File.separator
                + DEFAULT_SAFE_FOLDER;
        File file = new File(safeFilePath);
        if (!file.exists()) {
            if (file.mkdirs()) {
                setSafePathPermition(safeFilePath);
            }
        } else if (file.isFile() && file.delete()) {
            if (file.mkdirs()) {
                setSafePathPermition(safeFilePath);
            }
        }
    }

    public static void mkPrivateFolder() {
        File file = new File(SafeConstants.PRIVATE_FILE_PATH);
        if (!file.exists() || file.isFile() && file.delete()) {
            file.mkdirs();
        }
    }

    public static void encryptFile(File source, Cipher cipher) {
        if (source == null || cipher == null || !source.exists()) {
            Log.e(TAG, "encryptFile param error");
            return;
        }
        byte[] encryb;
        byte[] encrya = null;
        boolean isEncryptionAll = false;
        if (source.length() >= SafeConstants.ENCRYPTION_SIZE) {
            encryb = new byte[SafeConstants.ENCRYPTION_SIZE];
        } else {
            encryb = new byte[((int) source.length())];
            isEncryptionAll = true;
        }

        try {
            RandomAccessFile raf = new RandomAccessFile(source, "rw");
            try {
                if (-1 != raf.read(encryb)) {
                    encrya = cipher.doFinal(encryb);
                }

                if (encrya != null) {
                    raf.seek(0);
                    if (isEncryptionAll) {
                        raf.write(encrya);
                    } else {
                        raf.write(encrya, 0, SafeConstants.ENCRYPTION_SIZE);
                        raf.seek((long) ((int) source.length()));
                        raf.write(encrya, SafeConstants.ENCRYPTION_SIZE,
                                encrya.length - SafeConstants.ENCRYPTION_SIZE);
                    }
                    raf.close();
                } else {
                    raf.close();
                }
            } catch (BadPaddingException | IOException | IllegalBlockSizeException e) {
                raf.close();
                e.printStackTrace();
            }
        } catch (IOException ei) {
            ei.printStackTrace();
        }
    }


    public static boolean decryptFile(File source, Cipher cipher) {
        if (source == null || cipher == null || !source.exists()) {
            Log.e(TAG, "encryptFile param error");
            return false;
        }
        byte[] buffer_source;
        byte[] buffer_cipher = null;
        boolean isEncryptionAll = false;

        if (source.length() > SafeConstants.ENCRYPTION_SIZE) {
            buffer_source = new byte[SafeConstants.ENCRYPTION_SIZE + MAX_ENCRYPT_LEN];
        } else {
            isEncryptionAll = true;
            buffer_source = new byte[(int)source.length()];
        }

        RandomAccessFile raf = null;
        long length = source.length();

        try {
            raf = new RandomAccessFile(source, "r");
            if (isEncryptionAll) {
                if (-1 != raf.read(buffer_source)) {
                    buffer_cipher = cipher.doFinal(buffer_source);
                }
                raf.setLength(0);
            }

            if (-1 != raf.read(buffer_source, 0, SafeConstants.ENCRYPTION_SIZE)) {
                raf.seek(length - MAX_ENCRYPT_LEN);
            }
            if (-1 != raf.read(buffer_source, SafeConstants.ENCRYPTION_SIZE, MAX_ENCRYPT_LEN)) {
                buffer_cipher = cipher.doFinal(buffer_source);
            }

            raf.setLength(length - MAX_ENCRYPT_LEN);
            raf.seek(0);
            raf.write(buffer_cipher);
            try {
                raf.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return true;
            }
        } catch (BadPaddingException | IOException | IllegalBlockSizeException e) {
            e.printStackTrace();
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException ei) {
                ei.printStackTrace();
            }
        }
        return false;
    }

    public static String dencrypString(String str, Cipher cipher) {
        if (str == null || cipher == null) {
            return null;
        }
        byte[] result;
        try {
            result = cipher.doFinal(parseHexStr2Byte(str));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            result = null;
        }
        if (result != null) {
            return new String(result);
        }
        return null;
    }

    public static String getOriginalFileName(String path) {
        if (path == null || !path.contains(File.separator)) {
            return null;
        }
        return path.substring(path.lastIndexOf(File.separator) + 1);
    }

    private static String parseByte2HexStr(byte[] buf) {
        StringBuilder sb = new StringBuilder();
        for (byte aBuf : buf) {
            String hex = Integer.toHexString(aBuf & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    private static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        }
        byte[] result = new byte[(hexStr.length() / 2)];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), MAX_ENCRYPT_LEN);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), MAX_ENCRYPT_LEN);
            result[i] = (byte) ((high * MAX_ENCRYPT_LEN) + low);
        }
        return result;
    }

    public static String encrypString(String str, Cipher cipher) {
        if (str == null || cipher == null) {
            return null;
        }
        byte[] result;
        try {
            result = cipher.doFinal(str.getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            result = null;
        }
        if (result != null) {
            return parseByte2HexStr(result);
        }
        return null;
    }

    private static void saveLockMode(Context context, int mode) {
        SharedPreferences sp = getSafeSharedPreference(context,
                Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE | Context.MODE_MULTI_PROCESS);
        if (sp != null) {
            Editor editor = sp.edit();
            editor.putInt(SafeConstants.SAFE_LOCK_MODE_KEY, mode);
            editor.apply();
            return;
        }
        if (DEBUG) {
            Log.v(TAG, "saveLockMode xml file open fail");
        }
    }

    public static void saveLockMode(Context context, int mode, String path) {
        saveLockMode(context, mode);
        saveUnlockParam(String.valueOf(mode), path);
    }

    private static void savePassword(Context context, String password) {
        SharedPreferences sp = getSafeSharedPreference(context,
                Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE | Context.MODE_MULTI_PROCESS);
        if (sp != null) {
            Editor editor = sp.edit();
            editor.putString(SafeConstants.SAFE_UNLOCK_PASSWORD, password);
            editor.apply();
            return;
        }
        if (DEBUG) {
            Log.v(TAG, "savePassword xml file open fail");
        }
    }

    public static void savePassword(Context context, String password, String path) {
        String encryptionPassword = encrypString(password,
                initAESCipher(SafeConstants.ENCRYPTION_KEY, Cipher.ENCRYPT_MODE));
        if (encryptionPassword != null) {
            savePassword(context, encryptionPassword);
            saveUnlockParam(encryptionPassword, path);
        }
    }

    public static void saveLockModeAndPassword(Context context, int mode, String password) {
        SharedPreferences sp = getSafeSharedPreference(context,
                Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE | Context.MODE_MULTI_PROCESS);
        String encryptionPassword = encrypString(password,
                initAESCipher(SafeConstants.ENCRYPTION_KEY, Cipher.ENCRYPT_MODE));
        if (sp != null) {
            Editor editor = sp.edit();
            editor.putInt(SafeConstants.SAFE_LOCK_MODE_KEY, mode);
            editor.putString(SafeConstants.SAFE_UNLOCK_PASSWORD, encryptionPassword);
            editor.apply();
            return;
        }
        if (DEBUG) {
            Log.v(TAG, "savePassword xml file open fail");
        }
    }

    public static void copyFileRandomAccess(File srcFile, File destFile) throws IOException {
        FileChannel srcChannel = null;
        FileChannel destChannel = null;

        if (srcFile.exists() && srcFile.isDirectory()) {
            destFile.mkdirs();
            for (File file : srcFile.listFiles()) {
                copyFileRandomAccess(file, new File(destFile.getAbsolutePath() + File.separator + file.getName()));
            }
        }
        RandomAccessFile destRAF2 = new RandomAccessFile(destFile, "rw");
        RandomAccessFile randomAccessFile;

        try {
            randomAccessFile = new RandomAccessFile(srcFile, "r");

            srcChannel = randomAccessFile.getChannel();
            destChannel = destRAF2.getChannel();
            ByteBuffer buff = ByteBuffer.allocate(MediaFile.FILE_TYPE_WMA);
            while (true) {
                int readLen = srcChannel.read(buff);
                if (-1 == readLen) {
                    break;
                }
                buff.flip();
                destChannel.write(buff);
                buff.clear();
            }
            randomAccessFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (srcChannel != null) {
                srcChannel.close();
            }
            if (destChannel != null) {
                destChannel.close();
            }
        }
    }

    public static void scanAllFile(Context context, String[] files,
                                   MediaScannerConnection.OnScanCompletedListener callback) {
        if (context != null) {
            if (files == null) {
                return;
            }
            for (String path: files) {
                MediaScannerConnection.scanFile(context, new String[]{path}, null, callback);
            }
        }
    }

    private static void saveUnlockParam(String param, String path) {
        mkSafeFolder();
        if (!TextUtils.isEmpty(path)) {
            if (DEBUG) {
                Log.v(TAG, "path = " + path);
            }
            File file = new File(path);

            try {
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        return;
                    }
                }
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                raf.setLength(0);
                raf.writeUTF(param);
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isDirectlyEncryp(int type) {
        return MediaFile.isAudioFileType(type)
                || MediaFile.isVideoFileType(type)
                || MediaFile.isAudioFileType(type)
                || (type == MediaFile.FILE_TYPE_ZIP);
    }

    public static boolean checkLegal(String input, int maxNumber) {
        boolean legal = false;
        if (input != null) {
            int length = input.length();
            if (length >= 4) {
                if (length > maxNumber) {
                    return true;
                }
                int dis = input.charAt(0) - input.charAt(1);
                int i;
                if (dis == 0) {
                    for (i = 1; i <= length - 2; i++) {
                        if (input.charAt(i) != input.charAt(i + 1)) {
                            legal = true;
                            break;
                        }
                    }
                } else if (Math.abs(dis) != 1) {
                    legal = true;
                } else if (input.charAt(0) < '0' || input.charAt(0) > '9' || input.charAt(1) < '0' || input.charAt(1) > '9') {
                    legal = true;
                } else {
                    i = 1;
                    while (i <= length - 2) {
                        if (input.charAt(i + 1) < '0' || input.charAt(i + 1) > '9' || dis != input.charAt(i) - input.charAt(i + 1)) {
                            legal = true;
                            break;
                        }
                        i++;
                    }
                }
            }
        }
        return legal;
    }

    public static boolean isNeededSdk() {
        return SDK_INT >= VERSION_CODES.N;
    }

    private static class CryptoProvider extends Provider {
        /**
         * Creates a Provider and puts parameters
         */
        CryptoProvider() {
            super("Crypto", 1.0, "HARMONY (SHA1 digest; SecureRandom; SHA1withDSA signature)");
            put("SecureRandom.SHA1PRNG",
                    "org.apache.harmony.security.provider.crypto.SHA1PRNG_SecureRandomImpl");
            put("SecureRandom.SHA1PRNG ImplementedIn", "Software");
        }
    }
}