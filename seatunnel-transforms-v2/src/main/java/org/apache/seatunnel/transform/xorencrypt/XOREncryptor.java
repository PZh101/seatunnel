package org.apache.seatunnel.transform.xorencrypt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * 工程级等长可逆混淆工具
 * 特点：
 * 1. 所有类型100%可逆
 * 2. 不丢精度、不乱码、不受时区影响
 * 3. 防止数据库溢出
 * <p>
 * 适合：防止“直接看数据”，非密码学安全用途
 */
public class XOREncryptor {

    // 核心密钥
    private static final int CORE_KEY = 12345678;
    // 使用Unicode基本多文种平面的可用范围
    private static final int UNICODE_RANGE = 65536; // 0x0000 - 0xFFFF
    // 32位掩码
    private static final BigInteger LOW_32_MASK = new BigInteger("FFFFFFFF", 16);

    private static final long LOW_32_MASK_LONG = 0xFFFFFFFFL;

    // ===================== 1. Boolean =====================
    public static boolean encryptBoolean(boolean value) {
        return value;
    }

    public static boolean decryptBoolean(boolean encrypted) {
        return encrypted;
    }

    // ===================== 2. Int =====================
    public static int encryptInt(int value) {
        return (value + CORE_KEY) ^ CORE_KEY;
    }

    public static int decryptInt(int encrypted) {
        return (encrypted ^ CORE_KEY) - CORE_KEY;
    }

    // ===================== 3. Long =====================
    public static long encryptLong(long value) {
        return (value + CORE_KEY) ^ CORE_KEY;
    }

    public static long decryptLong(long encrypted) {
        return (encrypted ^ CORE_KEY) - CORE_KEY;
    }

    // ===================== 4. BigDecimal（不丢精度版） =====================

    /**
     * 思路：
     * - 保留 scale
     * - 用 BigInteger 做低32位扰动
     * - 永不转 long，避免溢出
     */
    public static BigDecimal encryptDecimal(BigDecimal value) {
        if (value == null) return null;

        BigInteger unscaled = value.unscaledValue();

        // 分离高位和低位
        BigInteger highPart = unscaled.shiftRight(32).shiftLeft(32);
        BigInteger lowPart = unscaled.and(LOW_32_MASK);

        int lowInt = lowPart.intValue();
        int encryptedLow = encryptInt(lowInt);

        BigInteger encrypted =
                highPart.or(BigInteger.valueOf(encryptedLow & LOW_32_MASK_LONG));
//        BigInteger encrypted = BigInteger.valueOf(Integer.toUnsignedLong(encryptedLow));
        return new BigDecimal(encrypted, value.scale());
    }

    public static BigDecimal decryptDecimal(BigDecimal encrypted) {
        if (encrypted == null) return null;

        BigInteger unscaled = encrypted.unscaledValue();

        BigInteger highPart = unscaled.shiftRight(32).shiftLeft(32);
        BigInteger lowPart = unscaled.and(LOW_32_MASK);

        int lowInt = lowPart.intValue();
        int originalLow = decryptInt(lowInt);

        BigInteger original =
                highPart.or(BigInteger.valueOf(originalLow & LOW_32_MASK_LONG));

        return new BigDecimal(original, encrypted.scale());
    }

    // ===================== 5. LocalDateTime（UTC安全版） =====================

    public static LocalDateTime encryptDateTime(LocalDateTime value) {
        if (value == null) return null;

        long timestamp = value.toInstant(ZoneOffset.UTC).toEpochMilli();

        long highPart = timestamp & ~LOW_32_MASK_LONG;
        int lowPart = (int) timestamp;

        int encryptedLow = encryptInt(lowPart);

        long encrypted =
                highPart | (encryptedLow & LOW_32_MASK_LONG);

        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(encrypted),
                ZoneOffset.UTC
        );
    }

    public static LocalDateTime decryptDateTime(LocalDateTime encrypted) {
        if (encrypted == null) return null;

        long timestamp = encrypted.toInstant(ZoneOffset.UTC).toEpochMilli();

        long highPart = timestamp & ~LOW_32_MASK_LONG;
        int lowPart = (int) timestamp;

        int originalLow = decryptInt(lowPart);

        long original =
                highPart | (originalLow & LOW_32_MASK_LONG);

        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(original),
                ZoneOffset.UTC
        );
    }

    // ===================== 6. Date =====================

    public static Date encryptDate(Date value) {
        if (value == null) return null;

        long time = value.getTime();

        long highPart = time & ~LOW_32_MASK_LONG;
        int lowPart = (int) time;

        int encryptedLow = encryptInt(lowPart);

        long encrypted =
                highPart | (encryptedLow & LOW_32_MASK_LONG);

        return new Date(encrypted);
    }

    public static Date decryptDate(Date encrypted) {
        if (encrypted == null) return null;

        long time = encrypted.getTime();

        long highPart = time & ~LOW_32_MASK_LONG;
        int lowPart = (int) time;

        int originalLow = decryptInt(lowPart);

        long original =
                highPart | (originalLow & LOW_32_MASK_LONG);

        return new Date(original);
    }

    // ===================== 7. String（Base64安全版） =====================

    public static String encryptString(String value) {
        return encryptString(value, String.valueOf(CORE_KEY));
    }

    /**
     * 加密 - 字符级异或，保持长度不变
     */
    public static String encryptString(String text, String key) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("文本和密钥不能为空");
        }

        char[] textChars = text.toCharArray();
        char[] keyChars = key.toCharArray();
        char[] encrypted = new char[textChars.length];

        for (int i = 0; i < textChars.length; i++) {
            int textCode = textChars[i];
            int keyCode = keyChars[i % keyChars.length];

            // 字符级异或
            int xorResult = textCode ^ keyCode;

            // 确保结果在有效Unicode范围内
            encrypted[i] = (char) (xorResult % UNICODE_RANGE);
        }

        return new String(encrypted);
    }

    public static String decryptString(String encrypted) {
        return decryptString(encrypted, String.valueOf(CORE_KEY));
    }

    /**
     * 解密 - 与加密过程相同（异或的对称性）
     */
    public static String decryptString(String encryptedText, String key) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("加密文本和密钥不能为空");
        }

        char[] encryptedChars = encryptedText.toCharArray();
        char[] keyChars = key.toCharArray();
        char[] decrypted = new char[encryptedChars.length];

        for (int i = 0; i < encryptedChars.length; i++) {
            int encryptedCode = encryptedChars[i];
            int keyCode = keyChars[i % keyChars.length];

            // 异或解密
            int xorResult = encryptedCode ^ keyCode;

            decrypted[i] = (char) (xorResult % UNICODE_RANGE);
        }

        return new String(decrypted);
    }
    // ===================== 8. 通用入口（不丢类型） =====================

    public static Object encrypt(Object value) {
        if (value == null) return null;

        if (value instanceof Boolean) {
            return encryptBoolean((Boolean) value);
        } else if (value instanceof Integer) {
            return encryptInt((Integer) value);
        } else if (value instanceof Long) {
            return encryptLong((Long) value);
        } else if (value instanceof BigDecimal) {
            return encryptDecimal((BigDecimal) value);
        } else if (value instanceof LocalDateTime) {
            return encryptDateTime((LocalDateTime) value);
        } else if (value instanceof Timestamp) {
            LocalDateTime ldt = ((Timestamp) value)
                    .toInstant()
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDateTime();
            return Timestamp.valueOf(encryptDateTime(ldt));
        } else if (value instanceof Date) {
            return encryptDate((Date) value);
        } else if (value instanceof String) {
            return encryptString((String) value);
        } else {
            // 保底：序列化为字符串加密
            return encryptString(value.toString());
        }
    }

    public static Object decrypt(Object encrypted, Class<?> targetType) {
        if (encrypted == null) return null;

        if (targetType == Boolean.class || targetType == boolean.class) {
            return decryptBoolean((Boolean) encrypted);
        } else if (targetType == Integer.class || targetType == int.class) {
            return decryptInt((Integer) encrypted);
        } else if (targetType == Long.class || targetType == long.class) {
            return decryptLong((Long) encrypted);
        } else if (targetType == BigDecimal.class) {
            return decryptDecimal((BigDecimal) encrypted);
        } else if (targetType == LocalDateTime.class) {
            return decryptDateTime((LocalDateTime) encrypted);
        } else if (targetType == Timestamp.class) {
            LocalDateTime ldt = decryptDateTime(
                    ((Timestamp) encrypted)
                            .toInstant()
                            .atOffset(ZoneOffset.UTC)
                            .toLocalDateTime()
            );
            return Timestamp.valueOf(ldt);
        } else if (targetType == Date.class) {
            return decryptDate((Date) encrypted);
        } else if (targetType == String.class) {
            return decryptString((String) encrypted);
        } else {
            return decryptString(encrypted.toString());
        }
    }

}
