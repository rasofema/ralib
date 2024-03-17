package de.learnlib.ralib.automata.tree;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.incremental.Construction;
import net.automatalib.incremental.dfa.Acceptance;
import net.automatalib.ts.UniversalDTS;

public interface RABuilder extends Construction<RegisterAutomaton, PSymbolInstance, Boolean> {

    @Override
    UniversalDTS<?, PSymbolInstance, ?, Acceptance, Void> asTransitionSystem();
}
