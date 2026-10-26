package com.kavya.calculatorpro;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Exact convolution multiplication using NTT (number-theoretic transform).
 * Uses base-10 digits and modulus 998244353 (primitive root 3).
 */
public final class NttMultiplier {
    private static final int MOD = 998244353;
    private static final int ROOT = 3;
    private static final int MAX_N = 1 << 23;

    private NttMultiplier() {}

    public static BigInteger multiply(BigInteger a, BigInteger b) {
        if (a.signum() == 0 || b.signum() == 0) return BigInteger.ZERO;
        boolean negative = a.signum() != b.signum();
        String x = a.abs().toString(), y = b.abs().toString();
        int needed = x.length() + y.length();
        int n = 1;
        while (n < needed) n <<= 1;
        // The modulus supports transforms up to 2^23. Avoid unsafe sizes or
        // coefficient wraparound; BigInteger remains the exact fallback.
        if (n > MAX_N || Math.min(x.length(), y.length()) * 81L >= MOD) {
            BigInteger fallback = a.abs().multiply(b.abs());
            return negative ? fallback.negate() : fallback;
        }

        int[] fa = new int[n], fb = new int[n];
        for (int i = 0; i < x.length(); i++) fa[i] = x.charAt(x.length() - 1 - i) - '0';
        for (int i = 0; i < y.length(); i++) fb[i] = y.charAt(y.length() - 1 - i) - '0';
        ntt(fa, false);
        ntt(fb, false);
        for (int i = 0; i < n; i++) fa[i] = (int)((long)fa[i] * fb[i] % MOD);
        ntt(fa, true);

        StringBuilder out = new StringBuilder(needed + 2);
        long carry = 0;
        int limit = x.length() + y.length();
        for (int i = 0; i < limit - 1; i++) {
            long value = fa[i] + carry;
            out.append((char)('0' + (value % 10)));
            carry = value / 10;
        }
        while (carry > 0) {
            out.append((char)('0' + (carry % 10)));
            carry /= 10;
        }
        while (out.length() > 1 && out.charAt(out.length() - 1) == '0') out.setLength(out.length() - 1);
        out.reverse();
        BigInteger result = new BigInteger(out.toString());
        return negative ? result.negate() : result;
    }

    private static void ntt(int[] a, boolean invert) {
        int n = a.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) { int t = a[i]; a[i] = a[j]; a[j] = t; }
        }
        for (int len = 2; len <= n; len <<= 1) {
            long wlen = pow(ROOT, (MOD - 1) / len);
            if (invert) wlen = pow(wlen, MOD - 2);
            for (int i = 0; i < n; i += len) {
                long w = 1;
                for (int j = 0; j < len / 2; j++) {
                    int u = a[i + j];
                    int v = (int)(a[i + j + len / 2] * w % MOD);
                    int sum = u + v;
                    if (sum >= MOD) sum -= MOD;
                    int diff = u - v;
                    if (diff < 0) diff += MOD;
                    a[i + j] = sum;
                    a[i + j + len / 2] = diff;
                    w = w * wlen % MOD;
                }
            }
        }
        if (invert) {
            long invN = pow(n, MOD - 2);
            for (int i = 0; i < n; i++) a[i] = (int)(a[i] * invN % MOD);
        }
    }

    private static long pow(long a, long e) {
        long result = 1;
        while (e > 0) {
            if ((e & 1) != 0) result = result * a % MOD;
            a = a * a % MOD;
            e >>= 1;
        }
        return result;
    }
}
