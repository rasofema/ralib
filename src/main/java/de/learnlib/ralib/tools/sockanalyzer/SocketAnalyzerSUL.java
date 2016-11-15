package de.learnlib.ralib.tools.sockanalyzer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.classanalyzer.SpecialSymbols;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 * SUL for communicating over sockets.
 */
public class SocketAnalyzerSUL extends DataWordSUL {
	private SocketWrapper sock;
	private int depth;
	private int maxDepth;
	private LinkedHashSet<ParameterizedSymbol> inputs;
	private Map<String, ParameterizedSymbol> outputSymbolsMap;

	public SocketAnalyzerSUL(int portNumber, int maxDepth, List<ParameterizedSymbol> inputs,
			List<ParameterizedSymbol> outputs) {
		this("127.0.0.1", portNumber, maxDepth, inputs, outputs);
	}

	public SocketAnalyzerSUL(String ipAddress, int portNumber, int maxDepth, List<ParameterizedSymbol> inputs,
			List<ParameterizedSymbol> outputs) {
		this.depth = 0;
		this.maxDepth = maxDepth;
		this.sock = new SocketWrapper(ipAddress, portNumber);
		this.inputs = new LinkedHashSet<>(inputs);
		this.outputSymbolsMap = new LinkedHashMap<>();
		outputs.forEach(out -> this.outputSymbolsMap.put(out.getName(), out));

	}

	@Override
	public void pre() {
		// System.out.println("----------");
		countResets(1);
		depth = 0;
	}

	@Override
	public void post() {
	}

	@Override
	public PSymbolInstance step(PSymbolInstance input) throws SULException {
		countInputs(1);

		if (depth > maxDepth && (maxDepth > 0)) {
			return new PSymbolInstance(SpecialSymbols.DEPTH);
		}
		depth++;

		String serializedInput = this.serialize(input);
		this.sock.writeInput(serializedInput);
		String serializedOutput = this.sock.readOutput();
		PSymbolInstance output = this.deserialize(serializedOutput, this.outputSymbolsMap);
		return output;
	}

	public void sendReset() {

		// send reset to SUT
		sock.writeInput("reset");
	}

	// serialization uses the toString method for all parameters. This works
	// since our parameter types are primitives/ primitive wrappers
	private String serialize(PSymbolInstance action) {
		assert this.inputs.contains(action.getBaseSymbol());
		String result = action.getBaseSymbol().getName();
		DataValue<?>[] params = action.getParameterValues();

		if (params.length > 0) {
			for (DataValue<?> parameter : params) {
				result += "_" + parameter.getId();
			}
		}
		return result;
	}

	/*
	 * SutSocketWrapper has its own specialized method to deserialize a
	 * serialized output action
	 */
	public PSymbolInstance deserialize(String actionString, Map<String, ParameterizedSymbol> outputSymbolMap) {
		String[] action = actionString.split("_");

		if (action.length < 1) {
			System.out.println("Error deserializing concrete output from string: " + actionString);
			throw new RuntimeException("Error deserializing concrete output from string: " + actionString);
		}

		String methodName = action[0];
		ParameterizedSymbol outputSignature = outputSymbolMap.get(methodName);
		if (outputSignature == null) {
			throw new DecoratedRuntimeException("Output method not registered").addDecoration("output", actionString);
		} else if (action.length != outputSignature.getArity() + 1) {
			throw new DecoratedRuntimeException("Output method registered with wrong arity")
					.addDecoration("output", actionString).addDecoration("expected arity", outputSignature.getArity());
		}

		DataValue[] parameters = new DataValue[outputSignature.getArity()];
		DataType[] paramTypes = outputSignature.getPtypes();
		for (int i = 1; i < action.length; i++) {
			String paramString = action[i];
			DataType paramType = paramTypes[i - 1];
			Object paramValue = valueOf(paramString, paramType);
			parameters[i - 1] = new DataValue(paramType, paramValue);
		}
		// use concrete constructor of OutputAction
		return new PSymbolInstance(outputSignature, parameters);
	}

	public static <T> T valueOf(String serializedParameter, DataType<T> parameterType) {
		Class<T> cls = parameterType.getBase();
		if (cls == Integer.class)
			return cls.cast(Integer.valueOf(serializedParameter));
		else if (cls == Double.class)
			return cls.cast(Double.valueOf(serializedParameter));

		throw new RuntimeException("Deserialization not supported for " + cls);
	}
}