package net.maxsmr.commonutils.data;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import static net.maxsmr.commonutils.data.text.TextUtilsKt.isEmpty;

public final class ChecksumHelper {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ChecksumHelper.class);

    private ChecksumHelper() {
        throw new AssertionError("no instances.");
    }

    public static final int MD5_HASH_CHARS_COUNT = 32;

    @Nullable
    public static String md5Hash(byte[] encData) {

        if (encData == null || encData.length == 0) {
            return null;
        }

        MessageDigest mdEnc = null;
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.e("a NoSuchAlgorithmException occurred during getInstance()", e);
        }
        if (mdEnc == null) {
            return null;
        }
        mdEnc.update(encData, 0, encData.length);
        String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
        while (md5.length() < MD5_HASH_CHARS_COUNT) {
            md5 = "0" + md5;
        }
        return md5.toUpperCase(Locale.getDefault());
    }

    @Nullable
    public static String md5Hash(String st) {
        return ChecksumHelper.md5Hash(st, Charset.defaultCharset().name());
    }

    @Nullable
    public static String md5Hash(String st, String charsetName) {
        charsetName = isEmpty(charsetName)? "UTF-8" : charsetName;
        try {
            return !isEmpty(st)? md5Hash(st.getBytes(charsetName)) : null;
        } catch (UnsupportedEncodingException e) {
            logger.e("a UnsupportedEncodingException occurred during getBytes()", e);
            return null;
        }
    }

    public static String md5Hash(File file) {
        return FileHelper.isFileValid(file)? md5Hash(FileHelper.readBytesFromFile(file)) : null;
    }
}
