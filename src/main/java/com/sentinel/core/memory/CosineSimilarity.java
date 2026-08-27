package com.sentinel.core.memory;

public final class CosineSimilarity {

    private CosineSimilarity() {
    }

    public static double calculate(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) {
            throw new IllegalArgumentException("vectors must be non-empty and have equal dimensions");
        }

        double dotProduct = 0.0;
        double normLeft = 0.0;
        double normRight = 0.0;
        for (int index = 0; index < left.length; index++) {
            dotProduct += left[index] * right[index];
            normLeft += left[index] * left[index];
            normRight += right[index] * right[index];
        }

        if (normLeft == 0.0 || normRight == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normLeft) * Math.sqrt(normRight));
    }
}
