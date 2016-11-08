package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;

public class IntervalDataValue<T extends Comparable<T>> extends DataValue<T>{
	// the minimum distance from the endpoint of a smaller/bigger interval value.
	private static int OUTSIDE_STEP = 20000;
	private static int INSIDE_STEP = 10;
	
	/**
	 * Constructs interval DVs from left and right ends, by selecting a value
	 * in between. One of left or right can be null, meaning there is no boundary. 
	 * 
	 * Use this function whenever you want an interval between two values and only use the constructor
	 * when a specific value (for example, an old value) is preferred. 
	 */
	public static <T extends Comparable<T>>  IntervalDataValue<T>  instantiateNew(DataValue<T> left, DataValue<T> right) {
		DataType<T> type = left != null ? left.getType() : right.getType();
		Class<T> cls = type.getBase();
		
		T intvVal;
		T leftVal;
		T rightVal;
		
		// in case either is null, we just provide an increment/decrement
		if (left == null && right != null) {
			// we select a value at least 
			intvVal = cls.cast(DataValue.sub(right, new DataValue<>(type, cast(OUTSIDE_STEP, type))).getId());
		} else if (left != null && right == null) {
			intvVal = cls.cast(DataValue.add(left, new DataValue<>(type, cast(OUTSIDE_STEP, type))).getId());
		} else if (left != null && right != null) {
			intvVal = pickInBetweenValue(type.getBase(), left.getId(), right.getId());
		} else {
			throw new RuntimeException("Both ends of the Interval cannot be null");
		}

		return new IntervalDataValue<T>(new DataValue<T>(type, intvVal), left, right);
	}
	
	private static <T extends Comparable<T>> T pickInBetweenValue(Class<T> clz, T leftVal, T rightVal) {
		T betweenVal;
		if (leftVal.compareTo(rightVal) >= 0 ) {
			throw new RuntimeException("Invalid interval, left end bigger or equal to right end \n "
					+ "left: " + leftVal + " right: " + rightVal + " ]");
		}
		
		if (clz.isAssignableFrom(Integer.class)) {
			Integer intVal; 
			if ((((Integer) rightVal) - ((Integer) leftVal)) > INSIDE_STEP) {
				intVal = ((Integer) leftVal) + INSIDE_STEP;
			} else {
				throw new RuntimeException("Cannot instantiate value in int interval \n "
						+ "left: " + leftVal + " right: " + rightVal + " ]");
			}
			betweenVal =  clz.cast( intVal);
		} else {
			if(clz.isAssignableFrom(Double.class)) {
				Double doubleVal;
				if ((((Double) rightVal) - ((Double) leftVal)) > INSIDE_STEP) {
					doubleVal = ((Double) leftVal) + INSIDE_STEP;
				} else {
					doubleVal = (((Double) rightVal) + ((Double) leftVal))/2 ;
				}
				betweenVal = clz.cast(doubleVal);
			} else {
				throw new RuntimeException("Unsupported type " + leftVal.getClass());
			}
		}
		
		return betweenVal;
	}

	private DataValue<T> left;
	private DataValue<T> right;
	
	public IntervalDataValue(DataValue<T> dv, DataValue<T> left, DataValue<T> right) {
		super(dv.getType(), dv.getId());
		this.left = left;
		this.right = right;
		
	}
	
	public String toString() {
		return super.toString() + " ( " + (this.getLeft() != null ? this.getLeft().getId().toString() : "") + ":" +
					(this.getRight() != null ? this.getRight().getId().toString() : "") + ")"; 
	}

	
	public DataValue<T> getLeft() {
		return this.left;
	}
	
	
	public DataValue<T> toRegular() {
		return new DataValue<T> (this.type, this.id);
	}


	public DataValue<T> getRight() {
		return this.right;
	}
}
