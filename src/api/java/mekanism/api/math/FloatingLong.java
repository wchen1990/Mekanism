package mekanism.api.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import mekanism.api.NBTConstants;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * A class representing a positive number with an internal value defined by a long, and a floating point number stored in a short
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FloatingLong extends Number implements Comparable<FloatingLong>, INBTSerializable<CompoundNBT> {

    //TODO: Eventually we should define a way of doing a set of operations all at once, and outputting a new value
    // given that way we can internally do all the calculations using primitives rather than spamming a lot of objects
    /**
     * The maximum number of decimal digits we can represent
     */
    private static final int DECIMAL_DIGITS = 4;
    /**
     * The maximum value we can represent as a decimal
     */
    private static final short MAX_DECIMAL = 9_999;
    /**
     * The value which represents 1.0, this is one more than the value of {@link #MAX_DECIMAL}
     */
    private static final short SINGLE_UNIT = MAX_DECIMAL + 1;
    /**
     * A constant holding the value {@code 0}
     */
    public static final FloatingLong ZERO = createConst(0);
    /**
     * A constant holding the value {@code 1}
     */
    public static final FloatingLong ONE = createConst(1);
    /**
     * A constant holding the maximum value for a {@link FloatingLong}
     */
    public static final FloatingLong MAX_VALUE = createConst(Long.MAX_VALUE, MAX_DECIMAL);

    /**
     * Creates a mutable {@link FloatingLong} from a given primitive double.
     *
     * @param value The value to represent as a {@link FloatingLong}
     *
     * @return A mutable {@link FloatingLong} from a given primitive double.
     *
     * @apiNote If this method is called with negative numbers it will be clamped to zero.
     */
    public static FloatingLong create(double value) {
        //TODO: Try to optimize/improve this at the very least it rounds incorrectly, and specify in the docs how it handles the rounding
        long lValue = (long) value;
        short decimal = parseDecimal(Double.toString(value));
        return create(lValue, decimal);
    }

    /**
     * Creates a mutable {@link FloatingLong} from a given primitive long.
     *
     * @param value The value to use for the whole number portion of the {@link FloatingLong}
     *
     * @return A mutable {@link FloatingLong} from a given primitive long.
     *
     * @apiNote If this method is called with negative numbers it will be clamped to zero.
     */
    public static FloatingLong create(long value) {
        return create(value, (short) 0);
    }

    /**
     * Creates a mutable {@link FloatingLong} from a given primitive long.
     *
     * @param value   The value to use for the whole number portion of the {@link FloatingLong}
     * @param decimal The short value to use for the decimal portion of the {@link FloatingLong}
     *
     * @return A mutable {@link FloatingLong} from a given primitive long, and short.
     *
     * @apiNote If this method is called with negative numbers they will be clamped to zero.
     */
    public static FloatingLong create(long value, short decimal) {
        return new FloatingLong(value, decimal, false);
    }

    /**
     * Creates a constant {@link FloatingLong} from a given primitive double.
     *
     * @param value The value to represent as a {@link FloatingLong}
     *
     * @return A constant {@link FloatingLong} from a given primitive double.
     *
     * @apiNote If this method is called with negative numbers it will be clamped to zero.
     */
    public static FloatingLong createConst(double value) {
        //TODO: Try to optimize/improve this at the very least it rounds incorrectly, and specify in the docs how it handles the rounding
        long lValue = (long) value;
        short decimal = parseDecimal(Double.toString(value));
        return create(lValue, decimal);
    }

    /**
     * Creates a constant {@link FloatingLong} from a given primitive long.
     *
     * @param value The value to use for the whole number portion of the {@link FloatingLong}
     *
     * @return A constant {@link FloatingLong} from a given primitive long.
     *
     * @apiNote If this method is called with negative numbers it will be clamped to zero.
     */
    public static FloatingLong createConst(long value) {
        return createConst(value, (short) 0);
    }

    /**
     * Creates a constant {@link FloatingLong} from a given primitive long.
     *
     * @param value   The value to use for the whole number portion of the {@link FloatingLong}
     * @param decimal The short value to use for the decimal portion of the {@link FloatingLong}
     *
     * @return A constant {@link FloatingLong} from a given primitive long, and short.
     *
     * @apiNote If this method is called with negative numbers they will be clamped to zero.
     */
    public static FloatingLong createConst(long value, short decimal) {
        return new FloatingLong(value, decimal, true);
    }

    /**
     * Reads a mutable {@link FloatingLong} from NBT
     *
     * @param nbt The {@link CompoundNBT} to read from
     *
     * @return A mutable {@link FloatingLong}, or {@link #ZERO} if the given nbt is null or empty.
     */
    public static FloatingLong readFromNBT(@Nullable CompoundNBT nbt) {
        if (nbt == null || nbt.isEmpty()) {
            return ZERO;
        }
        return create(nbt.getLong(NBTConstants.VALUE), nbt.getShort(NBTConstants.DECIMAL));
    }

    /**
     * Reads a mutable {@link FloatingLong} from a buffer
     *
     * @param buffer The {@link PacketBuffer} to read from
     *
     * @return A mutable {@link FloatingLong}
     */
    public static FloatingLong readFromBuffer(PacketBuffer buffer) {
        return new FloatingLong(buffer.readVarLong(), buffer.readShort(), false);
    }

    private final boolean isConstant;
    private long value;
    private short decimal;

    private FloatingLong(long value, short decimal, boolean isConstant) {
        setAndClampValues(value, decimal);
        //Set the constant state after we have updated the values
        this.isConstant = isConstant;
    }

    /**
     * @return the long representing the whole number value of this {@link FloatingLong}
     */
    public long getValue() {
        return value;
    }

    /**
     * @return the short representing the decimal value of this {@link FloatingLong}
     */
    public short getDecimal() {
        return decimal;
    }

    /**
     * Sets the internal value and decimal to the given values, clamping them so that {@code value} is not negative, and {@code decimal} is not negative or greater than
     * {@link #MAX_DECIMAL}. If this {@link FloatingLong} is constant, it returns a new object otherwise it returns this {@link FloatingLong} after updating the internal
     * values.
     *
     * @param value   The whole number value to set
     * @param decimal The decimal value to set
     *
     * @return If this {@link FloatingLong} is constant, it returns a new object otherwise it returns this {@link FloatingLong} after updating the internal values.
     */
    private FloatingLong setAndClampValues(long value, short decimal) {
        if (value < 0) {
            //TODO: Remove this clamp for value and allow it to be an unsigned long, and convert string parsing and creation
            value = 0;
        }
        if (decimal < 0) {
            decimal = 0;
        } else if (decimal > MAX_DECIMAL) {
            decimal = MAX_DECIMAL;
        }
        if (isConstant) {
            return create(value, decimal);
        }
        this.value = value;
        this.decimal = decimal;
        return this;
    }

    /**
     * Checks if this {@link FloatingLong} is zero. This includes checks for if somehow the internal values have become negative.
     *
     * @return {@code true} if this {@link FloatingLong} should be treated as zero, {@code false} otherwise.
     */
    public boolean isZero() {
        return value <= 0 && decimal <= 0;
    }

    /**
     * Copies this {@link FloatingLong}, into a mutable {@link FloatingLong}
     */
    public FloatingLong copy() {
        return new FloatingLong(value, decimal, false);
    }

    /**
     * Adds the given {@link FloatingLong} to this {@link FloatingLong}, modifying the current object unless it is a constant in which case it instead returns the result
     * in a new object. This gets clamped at the upper bound of {@link FloatingLong#MAX_VALUE} rather than overflowing.
     *
     * @param toAdd The {@link FloatingLong} to add.
     *
     * @return The {@link FloatingLong} representing the value of adding the given {@link FloatingLong} to this {@link FloatingLong}.
     *
     * @apiNote It is recommended to set this to itself to reduce the chance of accidental calls if calling this on a constant {@link FloatingLong}
     * <br>
     * {@code value = value.plusEqual(toAdd)}
     */
    public FloatingLong plusEqual(FloatingLong toAdd) {
        long newValue;
        short newDecimal;
        try {
            newValue = Math.addExact(value, toAdd.value);
            newDecimal = (short) (decimal + toAdd.decimal);
            if (newDecimal > MAX_DECIMAL) {
                newDecimal -= SINGLE_UNIT;
                newValue = Math.addExact(newValue, 1);
            }
        } catch (ArithmeticException e) {
            //Catch if our long value would overflow and instead cap it
            newValue = MAX_VALUE.value;
            newDecimal = MAX_VALUE.decimal;
        }
        return setAndClampValues(newValue, newDecimal);
    }

    /**
     * Subtracts the given {@link FloatingLong} from this {@link FloatingLong}, modifying the current object unless it is a constant in which case it instead returns the
     * result in a new object. This gets clamped at the lower bound of {@link FloatingLong#ZERO} rather than becoming negative.
     *
     * @param toSubtract The {@link FloatingLong} to subtract.
     *
     * @return The {@link FloatingLong} representing the value of subtracting the given {@link FloatingLong} from this {@link FloatingLong}.
     *
     * @apiNote It is recommended to set this to itself to reduce the chance of accidental calls if calling this on a constant {@link FloatingLong}
     * <br>
     * {@code value = value.minusEqual(toSubtract)}
     */
    public FloatingLong minusEqual(FloatingLong toSubtract) {
        if (toSubtract.greaterThan(this)) {
            //Clamp the result at zero as floating longs cannot become negative
            return setAndClampValues(0, (short) 0);
        }
        long newValue = value - toSubtract.value;
        short newDecimal = (short) (decimal - toSubtract.decimal);
        if (newDecimal < 0) {
            newDecimal += SINGLE_UNIT;
            newValue--;
        }
        return setAndClampValues(newValue, newDecimal);
    }

    /**
     * Multiplies the given {@link FloatingLong} with this {@link FloatingLong}, modifying the current object unless it is a constant in which case it instead returns the
     * result in a new object. This gets clamped at the upper bound of {@link FloatingLong#MAX_VALUE} rather than overflowing.
     *
     * @param toMultiply The {@link FloatingLong} to multiply by.
     *
     * @return The {@link FloatingLong} representing the value of multiplying the given {@link FloatingLong} with this {@link FloatingLong}.
     *
     * @apiNote It is recommended to set this to itself to reduce the chance of accidental calls if calling this on a constant {@link FloatingLong}
     * <br>
     * {@code value = value.timesEqual(toMultiply)}
     */
    public FloatingLong timesEqual(FloatingLong toMultiply) {
        //(a+b)*(c+d) where numbers represent decimal, numbers represent value
        FloatingLong temp = create(multiplyLongs(value, toMultiply.value));//a * c
        temp = temp.plusEqual(multiplyLongAndDecimal(value, toMultiply.decimal));//a * d
        temp = temp.plusEqual(multiplyLongAndDecimal(toMultiply.value, decimal));//b * c
        temp = temp.plusEqual(multiplyDecimals(decimal, toMultiply.decimal));//b * d
        return setAndClampValues(temp.value, temp.decimal);
    }

    /**
     * Divides this {@link FloatingLong} by the given {@link FloatingLong}, modifying the current object unless it is a constant in which case it instead returns the
     * result in a new object. This gets clamped at the upper bound of {@link FloatingLong#MAX_VALUE} rather than overflowing.
     *
     * @param toDivide The {@link FloatingLong} to divide by.
     *
     * @return The {@link FloatingLong} representing the value of dividing this {@link FloatingLong} by the given {@link FloatingLong}.
     *
     * @throws ArithmeticException if {@code toDivide} is zero.
     * @apiNote It is recommended to set this to itself to reduce the chance of accidental calls if calling this on a constant {@link FloatingLong}
     * <br>
     * {@code value = value.divideEquals(toDivide)}
     */
    public FloatingLong divideEquals(FloatingLong toDivide) {
        if (toDivide.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        //TODO: Make a more direct implementation that doesn't go through big decimal
        BigDecimal divide = new BigDecimal(toString()).divide(new BigDecimal(toDivide.toString()), DECIMAL_DIGITS, RoundingMode.HALF_EVEN);
        long value = divide.longValue();
        short decimal = parseDecimal(divide.toString());
        return setAndClampValues(value, decimal);
    }

    /**
     * Adds the given {@link FloatingLong} to this {@link FloatingLong} and returns the result in a new object. This gets clamped at the upper bound of {@link
     * FloatingLong#MAX_VALUE} rather than overflowing.
     *
     * @param toAdd The {@link FloatingLong} to add.
     *
     * @return The {@link FloatingLong} representing the value of adding the given {@link FloatingLong} to this {@link FloatingLong}.
     */
    public FloatingLong add(FloatingLong toAdd) {
        return copy().plusEqual(toAdd);
    }

    /**
     * Helper method to add a long primitive to this {@link FloatingLong} and returns the result in a new object. This gets clamped at the upper bound of {@link
     * FloatingLong#MAX_VALUE} rather than overflowing.
     *
     * @param toAdd The value to add, must be greater than or equal to zero
     *
     * @return The {@link FloatingLong} representing the value of adding the given long to this {@link FloatingLong}.
     *
     * @throws IllegalArgumentException if {@code toAdd} is negative.
     */
    public FloatingLong add(long toAdd) {
        if (toAdd < 0) {
            throw new IllegalArgumentException("Addition called with negative number, this is not supported. FloatingLongs are always positive.");
        }
        return add(FloatingLong.create(toAdd));
    }

    /**
     * Helper method to add a double primitive to this {@link FloatingLong} and returns the result in a new object. This gets clamped at the upper bound of {@link
     * FloatingLong#MAX_VALUE} rather than overflowing.
     *
     * @param toAdd The value to add, must be greater than or equal to zero.
     *
     * @return The {@link FloatingLong} representing the value of adding the given double to this {@link FloatingLong}.
     *
     * @throws IllegalArgumentException if {@code toAdd} is negative.
     */
    public FloatingLong add(double toAdd) {
        if (toAdd < 0) {
            throw new IllegalArgumentException("Addition called with negative number, this is not supported. FloatingLongs are always positive.");
        }
        return add(FloatingLong.create(toAdd));
    }

    /**
     * Subtracts the given {@link FloatingLong} from this {@link FloatingLong} and returns the result in a new object. This gets clamped at the lower bound of {@link
     * FloatingLong#ZERO} rather than becoming negative.
     *
     * @param toSubtract The {@link FloatingLong} to subtract.
     *
     * @return The {@link FloatingLong} representing the value of subtracting the given {@link FloatingLong} from this {@link FloatingLong}.
     */
    public FloatingLong subtract(FloatingLong toSubtract) {
        return copy().minusEqual(toSubtract);
    }

    /**
     * Helper method to subtract a long primitive from this {@link FloatingLong} and returns the result in a new object. This gets clamped at the lower bound of {@link
     * FloatingLong#ZERO} rather than becoming negative.
     *
     * @param toSubtract The value to subtract, must be greater than or equal to zero.
     *
     * @return The {@link FloatingLong} representing the value of subtracting the given long from this {@link FloatingLong}.
     *
     * @throws IllegalArgumentException if {@code toSubtract} is negative.
     */
    public FloatingLong subtract(long toSubtract) {
        if (toSubtract < 0) {
            throw new IllegalArgumentException("Subtraction called with negative number, this is not supported. FloatingLongs are always positive.");
        }
        return subtract(FloatingLong.create(toSubtract));
    }

    /**
     * Helper method to subtract a double primitive from this {@link FloatingLong} and returns the result in a new object. This gets clamped at the lower bound of {@link
     * FloatingLong#ZERO} rather than becoming negative.
     *
     * @param toSubtract The value to subtract, must be greater than or equal to zero.
     *
     * @return The {@link FloatingLong} representing the value of subtracting the given double from this {@link FloatingLong}.
     *
     * @throws IllegalArgumentException if {@code toSubtract} is negative.
     */
    public FloatingLong subtract(double toSubtract) {
        if (toSubtract < 0) {
            throw new IllegalArgumentException("Subtraction called with negative number, this is not supported. FloatingLongs are always positive.");
        }
        return subtract(FloatingLong.create(toSubtract));
    }

    /**
     * Multiplies the given {@link FloatingLong} with this {@link FloatingLong} and returns the result in a new object. This gets clamped at the upper bound of {@link
     * FloatingLong#MAX_VALUE} rather than overflowing.
     *
     * @param toMultiply The {@link FloatingLong} to multiply by.
     *
     * @return The {@link FloatingLong} representing the value of multiplying the given {@link FloatingLong} with this {@link FloatingLong}.
     */
    public FloatingLong multiply(FloatingLong toMultiply) {
        return copy().timesEqual(toMultiply);
    }

    /**
     * Helper method to multiple a long primitive with this {@link FloatingLong} and returns the result in a new object. This gets clamped at the upper bound of {@link
     * FloatingLong#MAX_VALUE} rather than overflowing.
     *
     * @param toMultiply The value to multiply by, must be greater than or equal to zero.
     *
     * @return The {@link FloatingLong} representing the value of multiplying the given long with this {@link FloatingLong}.
     *
     * @throws IllegalArgumentException if {@code toMultiply} is negative.
     */
    public FloatingLong multiply(long toMultiply) {
        if (toMultiply < 0) {
            throw new IllegalArgumentException("Multiply called with negative number, this is not supported. FloatingLongs are always positive.");
        }
        return multiply(FloatingLong.create(toMultiply));
    }

    /**
     * Helper method to multiple a double primitive with this {@link FloatingLong} and returns the result in a new object. This gets clamped at the upper bound of {@link
     * FloatingLong#MAX_VALUE} rather than overflowing.
     *
     * @param toMultiply The value to multiply by, must be greater than or equal to zero.
     *
     * @return The {@link FloatingLong} representing the value of multiplying the given double with this {@link FloatingLong}.
     *
     * @throws IllegalArgumentException if {@code toMultiply} is negative.
     */
    public FloatingLong multiply(double toMultiply) {
        if (toMultiply < 0) {
            throw new IllegalArgumentException("Multiply called with negative number, this is not supported. FloatingLongs are always positive.");
        }
        return multiply(FloatingLong.createConst(toMultiply));
    }

    /**
     * Divides this {@link FloatingLong} by the given {@link FloatingLong} and returns the result in a new object. This gets clamped at the upper bound of {@link
     * FloatingLong#MAX_VALUE} rather than overflowing.
     *
     * @param toDivide The {@link FloatingLong} to divide by.
     *
     * @return The {@link FloatingLong} representing the value of dividing this {@link FloatingLong} by the given {@link FloatingLong}.
     *
     * @throws ArithmeticException if {@code toDivide} is zero.
     */
    public FloatingLong divide(FloatingLong toDivide) {
        return copy().divideEquals(toDivide);
    }

    /**
     * Helper method to divide this {@link FloatingLong} by a long primitive and returns the result in a new object. This gets clamped at the upper bound of {@link
     * FloatingLong#MAX_VALUE} rather than overflowing.
     *
     * @param toDivide The value} to divide by, must be greater than zero.
     *
     * @return The {@link FloatingLong} representing the value of dividing this {@link FloatingLong} by the given long.
     *
     * @throws ArithmeticException      if {@code toDivide} is zero.
     * @throws IllegalArgumentException if {@code toDivide} is negative.
     */
    public FloatingLong divide(long toDivide) {
        if (toDivide < 0) {
            throw new IllegalArgumentException("Division called with negative number, this is not supported. FloatingLongs are always positive.");
        }
        return divide(FloatingLong.create(toDivide));
    }

    /**
     * Helper method to divide this {@link FloatingLong} by a double primitive and returns the result in a new object. This gets clamped at the upper bound of {@link
     * FloatingLong#MAX_VALUE} rather than overflowing.
     *
     * @param toDivide The value} to divide by, must be greater than zero.
     *
     * @return The {@link FloatingLong} representing the value of dividing this {@link FloatingLong} by the given double.
     *
     * @throws ArithmeticException      if {@code toDivide} is zero.
     * @throws IllegalArgumentException if {@code toDivide} is negative.
     */
    public FloatingLong divide(double toDivide) {
        if (toDivide < 0) {
            throw new IllegalArgumentException("Division called with negative number, this is not supported. FloatingLongs are always positive.");
        }
        return divide(FloatingLong.create(toDivide));
    }

    /**
     * Divides this {@link FloatingLong} by the given {@link FloatingLong} and returns the result as a double. This gets clamped at the upper bound of {@link
     * FloatingLong#MAX_VALUE} rather than overflowing. Additionally if the value to divide by is zero, this returns {@code 1}
     *
     * @param toDivide The {@link FloatingLong} to divide by.
     *
     * @return The {@link FloatingLong} representing the value of dividing this {@link FloatingLong} by the given {@link FloatingLong}, or {@code 1} if the given {@link
     * FloatingLong} is {@code 0}.
     */
    public double divideToLevel(FloatingLong toDivide) {
        //TODO: Optimize out creating another object
        return toDivide.isZero() ? 1 : divide(toDivide).doubleValue();
    }

    /**
     * @param other The {@link FloatingLong} to compare to
     *
     * @return this {@link FloatingLong} if it is greater than equal to the given {@link FloatingLong}, otherwise returns the given {@link FloatingLong}
     *
     * @implNote This method does not copy the value that is returned, so it is on the caller to keep track of mutability.
     */
    public FloatingLong max(FloatingLong other) {
        return smallerThan(other) ? other : this;
    }

    /**
     * @param other The {@link FloatingLong} to compare to
     *
     * @return this {@link FloatingLong} if it is smaller than equal to the given {@link FloatingLong}, otherwise returns the given {@link FloatingLong}
     *
     * @implNote This method does not copy the value that is returned, so it is on the caller to keep track of mutability.
     */
    public FloatingLong min(FloatingLong other) {
        return greaterThan(other) ? other : this;
    }

    /**
     * Helper method to check if a given {@link FloatingLong} is smaller than this {@link FloatingLong}
     *
     * @param toCompare The {@link FloatingLong} to compare to
     *
     * @return {@code true} if this {@link FloatingLong} is smaller, {@code false} otherwise.
     */
    public boolean smallerThan(FloatingLong toCompare) {
        return compareTo(toCompare) < 0;
    }

    /**
     * Helper method to check if a given {@link FloatingLong} is greater than this {@link FloatingLong}
     *
     * @param toCompare The {@link FloatingLong} to compare to
     *
     * @return {@code true} if this {@link FloatingLong} is larger, {@code false} otherwise.
     */
    public boolean greaterThan(FloatingLong toCompare) {
        return compareTo(toCompare) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote zero if equal to toCompare
     * <br>
     * less than zero if smaller than toCompare
     * <br>
     * greater than zero if bigger than toCompare
     * @implNote {@code 2} or {@code -2} if the overall value is different
     * <br>
     * {@code 1} or {@code -1} if the value is the same but the decimal is different
     */
    @Override
    public int compareTo(FloatingLong toCompare) {
        if (value < toCompare.value) {
            //If our primary value is smaller than toCompare's value we are always less than
            return -2;
        } else if (value > toCompare.value) {
            //If our primary value is bigger than toCompare's value we are always greater than
            return 2;
        }
        //Primary value is equal, check the decimal
        if (decimal < toCompare.decimal) {
            //If our primary value is equal, but our decimal smaller than toCompare's we are less than
            return -1;
        } else if (decimal > toCompare.decimal) {
            //If our primary value is equal, but our decimal bigger than toCompare's we are greater than
            return 1;
        }
        //Else we are equal
        return 0;
    }

    /**
     * Specialization of {@link #equals(Object)} for comparing two {@link FloatingLong}s
     *
     * @param other The {@link FloatingLong} to compare to
     *
     * @return {@code true} if this {@link FloatingLong} is equal in value to the given {@link FloatingLong}, {@code false} otherwise.
     */
    public boolean equals(FloatingLong other) {
        return value == other.value && decimal == other.decimal;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof FloatingLong && equals((FloatingLong) other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, decimal);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote We clamp the value to MAX_INT rather than having it overflow into the negatives.
     */
    @Override
    public int intValue() {
        return MathUtils.clampToInt(value);
    }

    @Override
    public long longValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote We clamp the int portion to MAX_INT rather than having it overflow into the negatives.
     */
    @Override
    public float floatValue() {
        return intValue() + decimal / (float) SINGLE_UNIT;
    }

    @Override
    public double doubleValue() {
        return longValue() + decimal / (double) SINGLE_UNIT;
    }

    @Override
    public CompoundNBT serializeNBT() {
        //TODO: Do we want to do this in a different form to make sure that it will support unsigned longs
        CompoundNBT nbt = new CompoundNBT();
        nbt.putLong(NBTConstants.VALUE, value);
        nbt.putShort(NBTConstants.DECIMAL, decimal);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        if (isConstant) {
            throw new IllegalStateException("Tried to modify a floating constant long");
        }
        setAndClampValues(nbt.getLong(NBTConstants.VALUE), nbt.getShort(NBTConstants.DECIMAL));
    }

    /**
     * Writes this {@link FloatingLong} to the given buffer
     *
     * @param buffer The {@link PacketBuffer} to write to.
     */
    public void writeToBuffer(PacketBuffer buffer) {
        buffer.writeVarLong(value);
        buffer.writeShort(decimal);
    }

    @Override
    public String toString() {
        return toString(DECIMAL_DIGITS);
    }

    /**
     * Extension of {@link #toString()} that allows for specifying how many decimals digits to show. If the decimal is zero, this is ignored, and this value is capped by
     * the maximum number of decimal digits
     *
     * @param decimalPlaces The number of decimal digits to display
     */
    public String toString(int decimalPlaces) {
        if (decimal == 0) {
            return Long.toString(value);
        }
        if (decimalPlaces > DECIMAL_DIGITS) {
            decimalPlaces = DECIMAL_DIGITS;
        }
        String valueAsString = value + ".";
        String decimalAsString = Short.toString(decimal);
        int numberDigits = decimalAsString.length();
        if (numberDigits > decimalPlaces) {
            //We need to trim it
            decimalAsString = decimalAsString.substring(0, decimalPlaces);
        } else if (numberDigits < decimalPlaces) {
            //We need to prepend some zeros
            decimalAsString = getZeros(decimalPlaces - numberDigits) + decimalAsString;
        }
        return valueAsString + decimalAsString;
    }

    /**
     * Parses the string argument as a signed decimal {@link FloatingLong}. The characters in the string must all be decimal digits, with a decimal point being valid to
     * convey where the decimal starts.
     *
     * @param string a {@code String} containing the {@link FloatingLong} representation to be parsed
     *
     * @return the {@link FloatingLong} represented by the argument in decimal.
     *
     * @throws NumberFormatException if the string does not contain a parsable {@link FloatingLong}.
     */
    public static FloatingLong parseFloatingLong(String string) {
        return parseFloatingLong(string, false);
    }

    /**
     * Parses the string argument as a signed decimal {@link FloatingLong}. The characters in the string must all be decimal digits, with a decimal point being valid to
     * convey where the decimal starts.
     *
     * @param string     a {@code String} containing the {@link FloatingLong} representation to be parsed
     * @param isConstant Specifies if a constant floating long should be returned or a modifiable floating long
     *
     * @return the {@link FloatingLong} represented by the argument in decimal.
     *
     * @throws NumberFormatException if the string does not contain a parsable {@link FloatingLong}.
     */
    public static FloatingLong parseFloatingLong(String string, boolean isConstant) {
        long value;
        int index = string.indexOf(".");
        if (index == -1) {
            value = Long.parseLong(string);
        } else {
            value = Long.parseLong(string.substring(0, index));
        }
        short decimal = parseDecimal(string, index);
        return isConstant ? createConst(value, decimal) : create(value, decimal);
    }

    /**
     * Parses the decimal out of a string argument and gets the representation as a short. The characters in the string must all be decimal digits, with a decimal point
     * being valid to convey where the decimal starts.
     *
     * @param string a {@code String} containing the decimal to be parsed
     *
     * @return the decimal represented as a short.
     *
     * @throws NumberFormatException if the string does not contain a parsable {@link Short}.
     */
    private static short parseDecimal(String string) {
        return parseDecimal(string, string.indexOf("."));
    }

    /**
     * Parses the decimal out of a string argument and gets the representation as a short. The characters in the string must all be decimal digits, the given index is
     * treated as the location of the decimal.
     *
     * @param string a {@code String} containing the decimal to be parsed
     * @param index  The index of the decimal
     *
     * @return the decimal represented as a short.
     *
     * @throws NumberFormatException if the string does not contain a parsable {@link Short}.
     */
    private static short parseDecimal(String string, int index) {
        if (index == -1) {
            return 0;
        }
        String decimalAsString = string.substring(index + 1);
        int numberDigits = decimalAsString.length();
        if (numberDigits < DECIMAL_DIGITS) {
            //We need to pad it on the right with zeros
            decimalAsString += getZeros(DECIMAL_DIGITS - numberDigits);
        } else if (numberDigits > DECIMAL_DIGITS) {
            //We need to trim it to make sure it will be in range of a short
            decimalAsString = decimalAsString.substring(0, DECIMAL_DIGITS);
        }
        return Short.parseShort(decimalAsString);
    }

    /**
     * Helper method to get a given number of repeating zeros as a String
     *
     * @param number The number of zeros to put in the string.
     */
    private static String getZeros(int number) {
        StringBuilder zeros = new StringBuilder();
        for (int i = 0; i < number; i++) {
            zeros.append('0');
        }
        return zeros.toString();
    }

    /**
     * Internal helper to multiply two longs and clamp if they overflow.
     */
    private static long multiplyLongs(long a, long b) {
        try {
            //multiplyExact will throw an exception if we overflow
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Internal helper to multiply a long by a decimal.
     */
    private static FloatingLong multiplyLongAndDecimal(long value, short decimal) {
        //This can't overflow!
        if (value > Long.MAX_VALUE / SINGLE_UNIT) {
            return create((value / SINGLE_UNIT) * decimal, (short) (value % SINGLE_UNIT * decimal));
        }
        return create(value * decimal / SINGLE_UNIT, (short) (value * decimal % SINGLE_UNIT));
    }

    /**
     * Internal helper to multiply two decimals.
     */
    private static FloatingLong multiplyDecimals(short a, short b) {
        //Note: If we instead wanted to round here, just get modulus and add if >= 0.5*SINGLE_UNIT
        long temp = (long) a * (long) b / SINGLE_UNIT;
        return create(0, (short) temp);
    }
}