package dev.morphia.query.validation;

import dev.morphia.mapping.MappedField;

import java.util.List;

import static dev.morphia.query.validation.CollectionTypeValidator.typeIsIterableOrArrayOrMap;
import static java.lang.String.format;

/**
 * Checks if the value can have the {@code dev.morphia.query.FilterOperator.NOT_IN} operator applied to it.
 */
@SuppressWarnings("removal")
public final class NotInOperationValidator extends OperationValidator {
    private static final NotInOperationValidator INSTANCE = new NotInOperationValidator();

    private NotInOperationValidator() {
    }

    /**
     * Get the instance
     *
     * @return the Singleton instance of this validator
     */
    public static NotInOperationValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected dev.morphia.query.FilterOperator getOperator() {
        return dev.morphia.query.FilterOperator.NOT_IN;
    }

    @Override
    protected void validate(final MappedField mappedField, final Object value, final List<ValidationFailure> validationFailures) {
        if (value == null) {
            validationFailures.add(new ValidationFailure(format("For a $nin operation, value cannot be null.")));
        } else if (!typeIsIterableOrArrayOrMap(value.getClass())) {
            validationFailures.add(new ValidationFailure(format("For a $nin operation, value '%s' should be a List or array. Instead it "
                                                                + "was a: %s", value, value.getClass())));
        }
    }
}
