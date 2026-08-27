package com.sentinel.core.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CosineSimilarityTest {

    @Test
    void identifiesEquivalentDirections() {
        assertThat(CosineSimilarity.calculate(
                new float[]{1.0f, 2.0f},
                new float[]{2.0f, 4.0f}
        )).isCloseTo(1.0, within(0.000001));
    }

    @Test
    void identifiesOrthogonalVectors() {
        assertThat(CosineSimilarity.calculate(
                new float[]{1.0f, 0.0f},
                new float[]{0.0f, 1.0f}
        )).isCloseTo(0.0, within(0.000001));
    }

    @Test
    void handlesZeroVectorsWithoutReturningNaN() {
        assertThat(CosineSimilarity.calculate(
                new float[]{0.0f, 0.0f},
                new float[]{1.0f, 1.0f}
        )).isZero();
    }

    @Test
    void rejectsMismatchedDimensions() {
        assertThatThrownBy(() -> CosineSimilarity.calculate(
                new float[]{1.0f},
                new float[]{1.0f, 2.0f}
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
