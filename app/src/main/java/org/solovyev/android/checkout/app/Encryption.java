package org.solovyev.android.checkout.app;

import android.util.Base64;

import javax.annotation.Nonnull;

/**
 * Simple (and stupid) encryption algorithm. Should not be used in real apps.
 */
final class Encryption {

    @Nonnull
    static String decrypt(@Nonnull String message, @Nonnull String salt) {
        return xor(new String(Base64.decode(message, 0)), salt);
    }

    @Nonnull
    static String encrypt(@Nonnull String message, @Nonnull String salt) {
        return new String(Base64.encode(xor(message, salt).getBytes(), 0));
    }

    /**
     * Encrypts or decrypts a base-64 string using a XOR cipher.
     */
    @Nonnull
    private static String xor(@Nonnull String message, @Nonnull String salt) {
        final char[] m = message.toCharArray();
        final int ml = m.length;

        final char[] s = salt.toCharArray();
        final int sl = s.length;

        final char[] res = new char[ml];
        for (int i = 0; i < ml; i++) {
            res[i] = (char) (m[i] ^ s[i % sl]);
        }
        return new String(res);
    }

}
