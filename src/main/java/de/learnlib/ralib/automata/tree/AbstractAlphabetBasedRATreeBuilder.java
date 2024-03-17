/* Copyright (C) 2013-2024 TU Dortmund University
 * This file is part of AutomataLib, http://www.automatalib.net/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.automata.tree;

import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.concept.InputAlphabetHolder;
import net.automatalib.automaton.graph.TransitionEdge;
import net.automatalib.automaton.graph.UniversalAutomatonGraphView;
import net.automatalib.graph.Graph;
import net.automatalib.incremental.dfa.AbstractVisualizationHelper;
import net.automatalib.incremental.dfa.Acceptance;
import net.automatalib.visualization.VisualizationHelper;

abstract class AbstractAlphabetBasedRATreeBuilder extends AbstractRATreeBuilder
        implements InputAlphabetHolder<PSymbolInstance> {

    private final Alphabet<PSymbolInstance> inputAlphabet;
    private int alphabetSize;

    AbstractAlphabetBasedRATreeBuilder(Alphabet<PSymbolInstance> inputAlphabet) {
        super(new Node(), inputAlphabet);
        this.inputAlphabet = inputAlphabet;
        this.alphabetSize = inputAlphabet.size();
    }

    @Override
    public void addAlphabetSymbol(PSymbolInstance symbol) {
        if (!inputAlphabet.containsSymbol(symbol)) {
            inputAlphabet.asGrowingAlphabetOrThrowException().addSymbol(symbol);
        }

        final int newAlphabetSize = inputAlphabet.size();
        // even if the symbol was already in the alphabet, we need to make sure to be able to store the new symbol
        if (alphabetSize < newAlphabetSize) {
            ensureInputCapacity(root, alphabetSize, newAlphabetSize);
            alphabetSize = newAlphabetSize;
        }
    }

    private void ensureInputCapacity(Node node, int oldAlphabetSize, int newAlphabetSize) {
        node.ensureInputCapacity(newAlphabetSize);
        for (int i = 0; i < oldAlphabetSize; i++) {
            final Node child = node.getChild(i);
            if (child != null) {
                ensureInputCapacity(child, oldAlphabetSize, newAlphabetSize);
            }
        }
    }

    @Override
    Node createNode() {
        return new Node();
    }

    @Override
    Node insertNode(Node parent, PSymbolInstance symIdx, Boolean accept) {
        Node succ = new Node(Acceptance.fromBoolean(accept));
        parent.setChild(inputAlphabet.getSymbolIndex(symIdx), alphabetSize, succ);
        return succ;
    }

    @Override
    public Alphabet<PSymbolInstance> getInputAlphabet() {
        return inputAlphabet;
    }

    public int getInputAlphabetSize() {
        return inputAlphabet.size();
    }

    public int getInputIndex(PSymbolInstance sym) {
        return inputAlphabet.getSymbolIndex(sym);
    }

    @Override
    public Graph<Node, ?> asGraph() {
        return new UniversalAutomatonGraphView<Node, PSymbolInstance, Node, Acceptance, Void, TransitionSystemViewAccept>
                (new TransitionSystemViewAccept(), inputAlphabet) {

            @Override
            public VisualizationHelper<Node, TransitionEdge<PSymbolInstance, Node>> getVisualizationHelper() {
                return new AbstractVisualizationHelper<Node, PSymbolInstance, Node, TransitionSystemViewAccept>(automaton) {

                    @Override
                    public Acceptance getAcceptance(Node node) {
                        return node.getAcceptance();
                    }
                };
            }
        };
    }
}
