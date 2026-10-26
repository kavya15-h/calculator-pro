package com.kavya.calculatorpro;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class ArithmeticService {
    private static final BigInteger TEN = BigInteger.TEN;

    public String calculate(String op, String rawA, String rawB, String algorithm) {
        if (rawA == null || rawA.isBlank()) throw new IllegalArgumentException("Enter the first number.");
        BigDecimal a = new BigDecimal(rawA.trim());
        String bText = rawB == null ? "" : rawB.trim();

        return switch (op) {
            case "add" -> a.add(decimalRequired(bText)).stripTrailingZeros().toPlainString();
            case "subtract" -> a.subtract(decimalRequired(bText)).stripTrailingZeros().toPlainString();
            case "divide" -> {
                BigDecimal b = decimalRequired(bText);
                if (b.compareTo(BigDecimal.ZERO) == 0) throw new ArithmeticException("Cannot divide by zero.");
                yield a.divide(b, MathContext.DECIMAL64).stripTrailingZeros().toPlainString();
            }
            case "multiply" -> {
                // BigInteger algorithms intentionally operate on integer inputs only.
                BigInteger ia = integerOnly(rawA);
                BigInteger ib = integerOnly(bText);
                yield multiply(ia, ib, algorithm).toString();
            }
            case "power" -> {
                int exponent;
                try { exponent = Integer.parseInt(bText); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Power must be a whole number."); }
                if (exponent < 0 || exponent > 10000) throw new IllegalArgumentException("Power must be between 0 and 10000.");
                yield a.pow(exponent).stripTrailingZeros().toPlainString();
            }
            case "sqrt" -> {
                if (a.signum() < 0) throw new IllegalArgumentException("Square root of a negative number is not supported.");
                yield BigDecimal.valueOf(Math.sqrt(a.doubleValue())).stripTrailingZeros().toPlainString();
            }
            case "gcd" -> integerOnly(rawA).abs().gcd(integerOnly(bText).abs()).toString();
            default -> throw new IllegalArgumentException("Unknown operation.");
        };
    }

    private BigDecimal decimalRequired(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Enter the second number.");
        return new BigDecimal(value);
    }

    private BigInteger integerOnly(String value) {
        try { return new BigInteger(value.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("This operation needs whole numbers (no decimal point)."); }
    }

    public BigInteger multiply(BigInteger a, BigInteger b, String algorithm) {
        String choice = algorithm == null ? "auto" : algorithm.toLowerCase();
        if (choice.equals("auto")) {
            int digits = Math.max(a.abs().toString().length(), b.abs().toString().length());
            choice = digits < 80 ? "bigint" : digits < 700 ? "karatsuba" : digits < 3000 ? "toomcook" : "ntt";
        }
        return switch (choice) {
            case "bigint" -> a.multiply(b);
            case "karatsuba" -> karatsuba(a, b);
            case "toomcook" -> toomCook(a, b);
            case "ntt" -> NttMultiplier.multiply(a, b);
            default -> throw new IllegalArgumentException("Unknown multiplication algorithm.");
        };
    }

    private BigInteger karatsuba(BigInteger x, BigInteger y) {
        if (x.signum() == 0 || y.signum() == 0) return BigInteger.ZERO;
        boolean negative = x.signum() != y.signum();
        x = x.abs(); y = y.abs();
        BigInteger result = karatsubaPositive(x, y);
        return negative ? result.negate() : result;
    }

    private BigInteger karatsubaPositive(BigInteger x, BigInteger y) {
        int n = Math.max(x.toString().length(), y.toString().length());
        if (n <= 32) return x.multiply(y);
        int half = n / 2;
        BigInteger base = TEN.pow(half);
        BigInteger[] xs = x.divideAndRemainder(base);
        BigInteger[] ys = y.divideAndRemainder(base);
        BigInteger z0 = karatsubaPositive(xs[1], ys[1]);
        BigInteger z2 = karatsubaPositive(xs[0], ys[0]);
        BigInteger z1 = karatsubaPositive(xs[0].add(xs[1]), ys[0].add(ys[1])).subtract(z2).subtract(z0);
        return z2.multiply(base.pow(2)).add(z1.multiply(base)).add(z0);
    }

    private BigInteger toomCook(BigInteger x, BigInteger y) {
        if (x.signum() == 0 || y.signum() == 0) return BigInteger.ZERO;
        boolean negative = x.signum() != y.signum();
        BigInteger result = toomPositive(x.abs(), y.abs());
        return negative ? result.negate() : result;
    }

    private BigInteger toomPositive(BigInteger x, BigInteger y) {
        int n = Math.max(x.toString().length(), y.toString().length());
        if (n <= 48) return x.multiply(y);
        int chunk = (n + 2) / 3;
        BigInteger base = TEN.pow(chunk);
        BigInteger base2 = base.multiply(base);
        BigInteger[] X = split3(x, base, base2);
        BigInteger[] Y = split3(y, base, base2);
        BigInteger p0 = X[0].multiply(Y[0]);
        BigInteger p1 = eval(X, BigInteger.ONE).multiply(eval(Y, BigInteger.ONE));
        BigInteger pm1 = eval(X, BigInteger.ONE.negate()).multiply(eval(Y, BigInteger.ONE.negate()));
        BigInteger p2 = eval(X, BigInteger.TWO).multiply(eval(Y, BigInteger.TWO));
        BigInteger pInf = X[2].multiply(Y[2]);

        BigInteger c0 = p0;
        BigInteger c4 = pInf;
        BigInteger s1 = p1.subtract(c0).subtract(c4);
        BigInteger sm1 = pm1.subtract(c0).subtract(c4);
        BigInteger c2 = s1.add(sm1).divide(BigInteger.TWO);
        BigInteger t = s1.subtract(sm1).divide(BigInteger.TWO);
        BigInteger s2 = p2.subtract(c0).subtract(c4.multiply(BigInteger.valueOf(16)));
        BigInteger c3 = s2.subtract(t.multiply(BigInteger.TWO)).subtract(c2.multiply(BigInteger.valueOf(4)))
                .divide(BigInteger.valueOf(6));
        BigInteger c1 = t.subtract(c3);
        return c4.multiply(base.pow(4))
                .add(c3.multiply(base.pow(3)))
                .add(c2.multiply(base2))
                .add(c1.multiply(base))
                .add(c0);
    }

    private BigInteger[] split3(BigInteger x, BigInteger base, BigInteger base2) {
        BigInteger[] r = new BigInteger[3];
        r[0] = x.mod(base);
        r[1] = x.divide(base).mod(base);
        r[2] = x.divide(base2);
        return r;
    }

    private BigInteger eval(BigInteger[] a, BigInteger point) {
        return a[0].add(a[1].multiply(point)).add(a[2].multiply(point.multiply(point)));
    }
}
