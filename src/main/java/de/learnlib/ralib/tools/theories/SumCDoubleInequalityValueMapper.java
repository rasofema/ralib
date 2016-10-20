package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCDoubleInequalityValueMapper implements ValueMapper<Double>{

	private DataType type;
	private DoubleInequalityTheory theory;
	private List<DataValue<Double>> constants;

	public SumCDoubleInequalityValueMapper(DataType type, DoubleInequalityTheory theory) {
		this(type, theory, Collections.emptyList());
	}
			
	
	public SumCDoubleInequalityValueMapper(DataType type, DoubleInequalityTheory theory, List<DataValue<Double>> sumConstants) {
		this.type = type;
		this.theory = theory;
		this.constants = sumConstants;
		
	}

	/**
	 * We can only canonize to SumC, Equal or Fresh Data Values. 
	 */
	
	public DataValue<Double> canonize(Double value, Map<Double, DataValue<Double>> thisToOtherMap) {
		if (thisToOtherMap.containsKey(value)) {
			DataValue<Double> dv = thisToOtherMap.get(value);
			return new DataValue<>(dv.getType(), dv.getId());
		}
		for (DataValue<Double> constant : this.constants) {
			if (thisToOtherMap.containsKey(constant.getId() + value)) {
				DataValue<Double> operand = canonize(value, thisToOtherMap);
				return new SumCDataValue<Double>(operand, constant);
			}
		}
		
		DataValue<Double> fv = this.theory.getFreshValue(new ArrayList<>(thisToOtherMap.values()));
		return new FreshValue<>(fv.getType(), fv.getId());
	}

	public Double decanonize(DataValue<Double> value, Map<DataValue<Double>, Double> thisToOtherMap) {
		if (thisToOtherMap.containsKey(value)) 
			return thisToOtherMap.get(value);
		if (value instanceof IntervalDataValue) {
			IntervalDataValue<Double> interval = (IntervalDataValue<Double>) value;
			Double left = null;
			Double right = null;
			if (interval.getLeft() != null) 
				left = decanonize(interval.getLeft(), thisToOtherMap);
			if (interval.getLeft() != null) 
				right = decanonize(interval.getRight(), thisToOtherMap);
			if (left != null) 
				if (right != null)
					return (left + right)/2;
				else 
					return left - 1;
			else
				return right + 1;
		}
		
		if (value instanceof SumCDataValue) {
			SumCDataValue<Double> sumCValue = (SumCDataValue<Double>) value;
			Double operand = decanonize(sumCValue.getOperand(), thisToOtherMap);
			return operand + sumCValue.getConstant().getId();
		}
		
		// a second check, in case casting a SumC was used, no SumCDataValue was created (this fallback can only be applied on SumC)
		for (DataValue<Double> constant : this.constants) {
			DataValue<Double> sub = (DataValue<Double>) DataValue.sub(value, constant);
			if (thisToOtherMap.containsKey(sub)) {
				Double otherOperand = thisToOtherMap.get(sub);
				return otherOperand + constant.getId();
			}
		}
		
		DataValue<Double> fv = this.theory.getFreshValue(thisToOtherMap.values().stream().
				map(v -> new DataValue<>(this.type, v)).collect(Collectors.toList()));
		return fv.getId();
	}

}