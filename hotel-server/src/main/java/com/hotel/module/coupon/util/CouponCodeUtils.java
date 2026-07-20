package com.hotel.module.coupon.util;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class CouponCodeUtils {

    private static final String BASE32_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int[] CHAR_INDEXES = new int[128];
    private static final int[][] SIGNATURE_KEYS = {
            {3, 5, 7, 11, 13, 17, 19, 23},
            {5, 7, 11, 13, 17, 19, 23, 29},
            {7, 11, 13, 17, 19, 23, 29, 31},
            {11, 13, 17, 19, 23, 29, 31, 37},
            {13, 17, 19, 23, 29, 31, 37, 41},
            {17, 19, 23, 29, 31, 37, 41, 43},
            {19, 23, 29, 31, 37, 41, 43, 47},
            {23, 29, 31, 37, 41, 43, 47, 53},
            {29, 31, 37, 41, 43, 47, 53, 59},
            {31, 37, 41, 43, 47, 53, 59, 61},
            {37, 41, 43, 47, 53, 59, 61, 67},
            {41, 43, 47, 53, 59, 61, 67, 71},
            {43, 47, 53, 59, 61, 67, 71, 73},
            {47, 53, 59, 61, 67, 71, 73, 79},
            {53, 59, 61, 67, 71, 73, 79, 83},
            {59, 61, 67, 71, 73, 79, 83, 89}
    };
    private static final int SEQUENCE_CHAR_LENGTH = 7;

    static {
        for (int index = 0; index < CHAR_INDEXES.length; index++) {
            CHAR_INDEXES[index] = -1;
        }
        for (int index = 0; index < BASE32_ALPHABET.length(); index++) {
            CHAR_INDEXES[BASE32_ALPHABET.charAt(index)] = index;
        }
    }

    private CouponCodeUtils() {
    }

    public static String generate(long sequence) {
        checkSequence(sequence);
        int selector = ThreadLocalRandom.current().nextInt(16);
        String encodedSequence = encodeFixedLength(sequence, SEQUENCE_CHAR_LENGTH);
        char signature = BASE32_ALPHABET.charAt(calculateSignature(sequence, selector));
        char selectorChar = BASE32_ALPHABET.charAt(selector);
        return new StringBuilder(9)
                .append(signature)
                .append(selectorChar)
                .append(encodedSequence)
                .toString();
    }

    public static boolean isValid(String rawCode) {
        if (rawCode == null) {
            return false;
        }
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        if (code.length() != 9) {
            return false;
        }
        int selector = decodeChar(code.charAt(1));
        if (selector < 0 || selector > 15) {
            return false;
        }
        long sequence = decodeBase32(code.substring(2));
        int signature = calculateSignature(sequence, selector);
        return BASE32_ALPHABET.charAt(signature) == code.charAt(0);
    }

    private static int calculateSignature(long sequence, int selector) {
        int[] keys = SIGNATURE_KEYS[selector];
        int signature = 0;
        for (int index = 0; index < 8; index++) {
            int nibble = (int) ((sequence >>> (index * 4)) & 0xF);
            signature = (signature + nibble * keys[index]) % 32;
        }
        return signature;
    }

    private static String encodeFixedLength(long value, int length) {
        char[] chars = new char[length];
        long current = value;
        for (int index = length - 1; index >= 0; index--) {
            chars[index] = BASE32_ALPHABET.charAt((int) (current & 31));
            current >>>= 5;
        }
        return new String(chars);
    }

    private static long decodeBase32(String encoded) {
        long value = 0;
        for (int index = 0; index < encoded.length(); index++) {
            int decoded = decodeChar(encoded.charAt(index));
            if (decoded < 0) {
                throw new IllegalArgumentException("兑换码包含非法字符");
            }
            value = (value << 5) | decoded;
        }
        return value;
    }

    private static int decodeChar(char ch) {
        return ch < CHAR_INDEXES.length ? CHAR_INDEXES[ch] : -1;
    }

    private static void checkSequence(long sequence) {
        if (sequence <= 0 || sequence > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("序列号超出 32bit 范围");
        }
    }
}
