package dev.morphia.query.validation;

import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DoubleTypeValidatorTest {
    @Test
    public void shouldAllowDoubleTypeWithDoublePrimitiveValue() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<>();
        // when
        boolean validationApplied = DoubleTypeValidator.getInstance().apply(Double.class, 2.2, validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowDoubleTypeWithDoubleValue() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<>();
        // when
        boolean validationApplied = DoubleTypeValidator.getInstance().apply(Double.class, 2.2, validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowValuesOfIntegerIfTypeIsDouble() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<>();
        // when
        boolean validationApplied = DoubleTypeValidator.getInstance().apply(Double.class, 1, validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowValuesOfIntegerIfTypeIsPrimitiveDouble() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<>();
        // when
        boolean validationApplied = DoubleTypeValidator.getInstance().apply(double.class, 1, validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldAllowValuesOfLongIfTypeIsPrimitiveDouble() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<>();
        // when
        boolean validationApplied = DoubleTypeValidator.getInstance().apply(double.class, 1L, validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldNotApplyValidationIfTypeIsNotDouble() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<>();
        // when
        boolean validationApplied = DoubleTypeValidator.getInstance().apply(String.class, 1L, validationFailures);
        // then
        assertThat(validationApplied, is(false));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldOnlyValuesOfLongIfTypeIsDouble() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<>();
        // when
        boolean validationApplied = DoubleTypeValidator.getInstance().apply(Double.class, 1L, validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(0));
    }

    @Test
    public void shouldRejectValueThatIsNotApplicableToDouble() {
        // given
        ArrayList<ValidationFailure> validationFailures = new ArrayList<>();
        // when
        boolean validationApplied = DoubleTypeValidator.getInstance().apply(Double.class, "something not a double", validationFailures);
        // then
        assertThat(validationApplied, is(true));
        assertThat(validationFailures.size(), is(1));
    }
}
