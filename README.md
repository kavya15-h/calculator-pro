# Calculator Pro

A Java + Spring Boot web calculator with a browser UI and large-integer algorithms.

## Features
- Standard arithmetic, powers, square roots, and GCD.
- Arbitrary-size integer multiplication with selectable algorithms:
  - Java `BigInteger` baseline
  - Recursive Karatsuba
  - Recursive Toom-Cook 3-way multiplication
  - Exact NTT (number-theoretic transform), a close transform-based alternative to floating-point FFT
- Auto mode chooses an algorithm based on input size.
- Benchmark panel compares the available integer multiplication methods.
- Browser UI served by Spring Boot.

## Requirements
- Java 17 or newer
- Maven 3.6+ (or use an IDE with Maven support)
- Internet access on first build so Maven can download dependencies

## Run
1. Extract the ZIP.
2. Open a terminal in the extracted `calculator-pro` folder.
3. Run:
   ```bash
   mvn spring-boot:run
   ```
4. Open `http://localhost:8080`.

If `mvn` is not recognized, install Maven or open the project in IntelliJ IDEA and run `CalculatorProApplication`.

## API examples
`POST http://localhost:8080/api/calculate`
```json
{
  "operation": "multiply",
  "a": "12345678901234567890",
  "b": "987654321",
  "algorithm": "karatsuba"
}
```

Supported operations: `add`, `subtract`, `multiply`, `divide`, `power`, `sqrt`, `gcd`.
Multiplication algorithms: `auto`, `bigint`, `karatsuba`, `toomcook`, `ntt`.

`POST http://localhost:8080/api/benchmark`
```json
{
  "a": "123456789012345678901234567890",
  "b": "98765432109876543210"
}
```

## Notes
- Exact large-integer multiplication works with integer strings; it does not convert them to floating point.
- NTT is an exact number-theoretic transform, not a floating-point FFT. It provides the same broad transform-based multiplication idea while avoiding floating-point rounding error.
- Square root accepts non-negative decimal values and returns a double-precision result.
- This is an educational portfolio project, not a replacement for audited numerical libraries.
