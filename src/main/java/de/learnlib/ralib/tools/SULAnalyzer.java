package de.learnlib.ralib.tools;

import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.sulanalyzer.SULAnalyzerParser;

public class SULAnalyzer extends GeneralToolTemplate{
	public SULAnalyzer() throws ConfigurationException, ClassNotFoundException {
		super(new SULAnalyzerParser());
	}
	
    @Override
    public String description() {
        return "analyzes Java SUL classes with a pre-defined input and output alphabet";
    }
}
