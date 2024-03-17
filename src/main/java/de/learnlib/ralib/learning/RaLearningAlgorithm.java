package de.learnlib.ralib.learning;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;

public interface RaLearningAlgorithm extends LearningAlgorithm<RegisterAutomaton, PSymbolInstance, Boolean> {

	public void setStatisticCounter(QueryStatistics queryStats);

	public QueryStatistics getQueryStatistics();

	public RaLearningAlgorithmName getName();
}
