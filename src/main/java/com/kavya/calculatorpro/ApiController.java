package com.kavya.calculatorpro;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final ArithmeticService service = new ArithmeticService();

    public record CalculateRequest(String operation, String a, String b, String algorithm) {}
    public record CalculateResponse(String result, String algorithm, long elapsedNanos) {}
    public record BenchmarkRequest(String a, String b) {}
    public record BenchmarkRow(String algorithm, String result, long elapsedNanos, boolean matches) {}

    @PostMapping("/calculate")
    public CalculateResponse calculate(@RequestBody CalculateRequest request) {
        try {
            long start = System.nanoTime();
            String operation = clean(request.operation()).toLowerCase(Locale.ROOT);
            String algorithm = request.algorithm() == null ? "auto" : request.algorithm().toLowerCase(Locale.ROOT);
            String result = service.calculate(operation, request.a(), request.b(), algorithm);
            return new CalculateResponse(result, algorithm, System.nanoTime() - start);
        } catch (IllegalArgumentException | ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/benchmark")
    public List<BenchmarkRow> benchmark(@RequestBody BenchmarkRequest request) {
        try {
            BigInteger a = new BigInteger(clean(request.a()));
            BigInteger b = new BigInteger(clean(request.b()));
            String expected = a.multiply(b).toString();
            List<BenchmarkRow> rows = new ArrayList<>();
            for (String algorithm : List.of("bigint", "karatsuba", "toomcook", "ntt")) {
                long start = System.nanoTime();
                String result = service.multiply(a, b, algorithm).toString();
                long elapsed = System.nanoTime() - start;
                rows.add(new BenchmarkRow(algorithm, result, elapsed, expected.equals(result)));
            }
            return rows;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter valid whole numbers.");
        }
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Please enter the required number(s).");
        return value.trim();
    }
}
