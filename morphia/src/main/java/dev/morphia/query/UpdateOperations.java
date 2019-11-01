package dev.morphia.query;


import java.util.List;


/**
 * <p> A nicer interface to the update operations in monogodb. All these operations happen at the server and can cause the server and
 * client
 * version of the Entity to be different </p>
 *
 * @param <T> The Java type used in the updates
 * @deprecated updates should be performed using the {@link Query#update()} instead of this class directly
 */
@Deprecated(since = "2.0", forRemoval = true)
public interface UpdateOperations<T> {

    /**
     * adds the value to an array field if it doesn't already exist in the array
     *
     * @param field the field to update
     * @param value the value to add
     * @return this
     * @mongodb.driver.manual reference/operator/update/addToSet/ $addToSet
     */
    UpdateOperations<T> addToSet(String field, Object value);

    /**
     * adds the values to an array field if they doesn't already exist in the array
     *
     * @param field  the field to update
     * @param values the values to add
     * @return this
     * @mongodb.driver.manual reference/operator/update/addToSet/ $addToSet
     */
    UpdateOperations<T> addToSet(String field, List<?> values);

    /**
     * adds the values to an array field if they doesn't already exist in the array
     *
     * @param field  the field to update
     * @param values the values to add
     * @return this
     * @mongodb.driver.manual reference/operator/update/addToSet/ $addToSet
     */
    UpdateOperations<T> addToSet(String field, Iterable<?> values);

    /**
     * Decrements the numeric field by 1
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/inc/ $inc
     */
    UpdateOperations<T> dec(String field);

    /**
     * Decrements the numeric field by value (must be a positive Double,
     * Float, Long, or Integer).
     *
     * @param field the field to update
     * @param value the value to decrement by
     * @throws IllegalArgumentException of the value is not an instance of
     *         Double, Float,Long, or Integer
     * @return this
     * @mongodb.driver.manual reference/operator/update/inc/ $inc
     */
    UpdateOperations<T> dec(String field, Number value);

    /**
     * Turns off validation (for all calls made after)
     *
     * @return this
     */
    UpdateOperations<T> disableValidation();

    /**
     * Turns on validation (for all calls made after); by default validation is on
     *
     * @return this
     */
    UpdateOperations<T> enableValidation();

    /**
     * Increments the numeric field by 1
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/inc/ $inc
     */
    UpdateOperations<T> inc(String field);

    /**
     * increments the numeric field by value (negatives are allowed)
     *
     * @param field the field to update
     * @param value the value to increment by
     * @return this
     * @mongodb.driver.manual reference/operator/update/inc/ $inc
     */
    UpdateOperations<T> inc(String field, Number value);

    /**
     * Sets the numeric field to value if it is greater than the current value.
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/max/ $max
     */
    UpdateOperations<T> max(String field, Number value);

    /**
     * sets the numeric field to value if it is less than the current value.
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/min/ $min
     */
    UpdateOperations<T> min(String field, Number value);

    /**
     * Adds new values to an array field.
     *
     * @param field the field to updated
     * @param value the value to add
     * @return this
     * @mongodb.driver.manual reference/operator/update/push/ $push
     */
    UpdateOperations<T> push(String field, Object value);

    /**
     * Adds new values to an array field at the given position
     *
     * @param field   the field to updated
     * @param value   the value to add
     * @param options the options to apply to the push
     * @return this
     * @mongodb.driver.manual reference/operator/update/push/ $push
     */
    UpdateOperations<T> push(String field, Object value, PushOptions options);

    /**
     * Adds new values to an array field.
     *
     * @param field  the field to updated
     * @param values the values to add
     * @return this
     * @mongodb.driver.manual reference/operator/update/push/ $push
     */
    UpdateOperations<T> push(String field, List<?> values);

    /**
     * Adds new values to an array field at the given position
     *
     * @param field   the field to updated
     * @param values  the values to add
     * @param options the options to apply to the push
     * @return this
     * @mongodb.driver.manual reference/operator/update/push/ $push
     */
    UpdateOperations<T> push(String field, List<?> values, PushOptions options);

    /**
     * removes the value from the array field
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/pull/ $pull
     */
    UpdateOperations<T> removeAll(String field, Object value);

    /**
     * removes the values from the array field
     *
     * @param field  the field to update
     * @param values the values to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/pullAll/ $pullAll
     */
    UpdateOperations<T> removeAll(String field, List<?> values);

    /**
     * removes the first value from the array
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/pop/ $pop
     */
    UpdateOperations<T> removeFirst(String field);

    /**
     * removes the last value from the array
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/pop/ $pop
     */
    UpdateOperations<T> removeLast(String field);

    /**
     * sets the field value
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/set/ $set
     */
    UpdateOperations<T> set(String field, Object value);

    /**
     * sets the field on insert.
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/setOnInsert/ $setOnInsert
     */
    UpdateOperations<T> setOnInsert(String field, Object value);

    /**
     * removes the field
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/unset/ $unset
     */
    UpdateOperations<T> unset(String field);
}
