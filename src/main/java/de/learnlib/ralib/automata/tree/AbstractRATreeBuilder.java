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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.UniversalAutomaton;
import net.automatalib.common.util.Pair;
import net.automatalib.common.util.collection.IteratorUtil;
import net.automatalib.common.util.mapping.MapMapping;
import net.automatalib.common.util.mapping.MutableMapping;
import net.automatalib.incremental.dfa.Acceptance;
import net.automatalib.ts.UniversalDTS;
import net.automatalib.util.ts.traversal.TSTraversal;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;

abstract class AbstractRATreeBuilder implements RABuilder {

    public Node root;
    public Alphabet<PSymbolInstance> inputAlphabet;

    AbstractRATreeBuilder(Node root, Alphabet<PSymbolInstance> inputAlphabet) {
        this.root = root;
        this.inputAlphabet = inputAlphabet;
    }

    @Override
    public @Nullable Word<PSymbolInstance> findSeparatingWord(RegisterAutomaton target,
                                                              Collection<? extends PSymbolInstance> inputs,
                                                              boolean omitUndefined) {
        return doFindSeparatingWord(target, inputs, omitUndefined);
    }

    <S> @Nullable Word<PSymbolInstance> doFindSeparatingWord(RegisterAutomaton target,
                                               Collection<? extends PSymbolInstance> inputs,
                                               boolean omitUndefined) {
        RALocation automatonInit = target.getInitialState();

        if (automatonInit == null) {
            return omitUndefined ? null : Word.epsilon();
        }

        if (root.getAcceptance().conflicts(automatonInit.isAccepting())) {
            return Word.epsilon();
        }

        // incomingInput can be null here, because we will always skip the bottom stack element below
        @SuppressWarnings("nullness")
        Record init = new Record(automatonInit, root, null,
                inputs.iterator());

        Deque<Record> dfsStack = new ArrayDeque<>();
        dfsStack.push(init);

        while (!dfsStack.isEmpty()) {
            @SuppressWarnings("nullness") // false positive https://github.com/typetools/checker-framework/issues/399
            @NonNull Record rec = dfsStack.peek();
            if (!rec.inputIt.hasNext()) {
                dfsStack.pop();
                continue;
            }
            PSymbolInstance input = rec.inputIt.next();

            Node succ = rec.treeNode.getChild(input);
            if (succ == null) {
                continue;
            }

            @Nullable RALocation state = rec.automatonState;
            @Nullable RALocation automatonSucc = state == null ? null : target.getTransition(state, input).getDestination();
            if (automatonSucc == null && omitUndefined) {
                continue;
            }

            boolean succAcc = automatonSucc != null && automatonSucc.isAccepting();

            if (succ.getAcceptance().conflicts(succAcc)) {
                WordBuilder<PSymbolInstance> wb = new WordBuilder<>(dfsStack.size());
                wb.append(input);

                dfsStack.pop();
                while (!dfsStack.isEmpty()) {
                    wb.append(rec.incomingInput);
                    rec = dfsStack.pop();
                }
                return wb.reverse().toWord();
            }

            dfsStack.push(new Record(automatonSucc, succ, input, inputs.iterator()));
        }

        return null;
    }

    public Pair<Boolean, Boolean> lookup(Word<? extends PSymbolInstance> inputWord) {
        Node curr = root;

        for (PSymbolInstance sym : inputWord) {
            Node succ = curr.getChild(sym);
            if (succ == null) {
                return Pair.of(false, null);
            }
            curr = succ;
        }
        Boolean out = curr.getAcceptance() == Acceptance.DONT_KNOW ? null : curr.getAcceptance().toBoolean();
        return Pair.of(out != null, out);
    }

    @Override
    public UniversalDTS<?, PSymbolInstance, ?, Acceptance, Void> asTransitionSystem() {
        return new TransitionSystemViewAccept();
    }

    abstract Node createNode();

    abstract Node insertNode(Node parent, PSymbolInstance symIdx, Boolean accept);

    static final class Record {

        public final RALocation automatonState;
        public final Node treeNode;
        public final PSymbolInstance incomingInput;
        public final Iterator<? extends PSymbolInstance> inputIt;

        Record(RALocation automatonState, Node treeNode, PSymbolInstance incomingInput,
               Iterator<? extends PSymbolInstance> inputIt) {
            this.automatonState = automatonState;
            this.treeNode = treeNode;
            this.incomingInput = incomingInput;
            this.inputIt = inputIt;
        }
    }

    class TransitionSystemViewAccept implements UniversalDTS<Node, PSymbolInstance, Node, Acceptance, Void>,
            UniversalAutomaton<Node, PSymbolInstance, Node, Acceptance, Void> {

        @Override
        public Node getSuccessor(Node transition) {
            return transition;
        }

        @Override
        public @Nullable Node getTransition(Node state, PSymbolInstance input) {
            return state.getChild(input);
        }

        @Override
        public Node getInitialState() {
            return root;
        }

        @Override
        public Acceptance getStateProperty(Node state) {
            return state.getAcceptance();
        }

        @Override
        public Void getTransitionProperty(Node transition) {
            return null;
        }

        @Override
        public Collection<Node> getStates() {
            return IteratorUtil.list(TSTraversal.breadthFirstIterator(this, inputAlphabet));
        }

        /*
         * We need to override the default MooreMachine mapping, because its StateIDStaticMapping class requires our
         * nodeIDs, which requires our states, which requires our nodeIDs, which requires ... infinite loop!
         */
        @Override
        public <V> MutableMapping<Node, V> createStaticStateMapping() {
            return new MapMapping<>();
        }
    }

    class TransitionSystemViewBool implements UniversalDTS<Node, PSymbolInstance, Node, Boolean, Void>,
                                            UniversalAutomaton<Node, PSymbolInstance, Node, Boolean, Void> {

        @Override
        public Node getSuccessor(Node transition) {
            return transition;
        }

        @Override
        public @Nullable Node getTransition(Node state, PSymbolInstance input) {
            return state.getChild(input);
        }

        @Override
        public Node getInitialState() {
            return root;
        }

        @Override
        public Boolean getStateProperty(Node state) {
            return state.getAcceptance().toBoolean();
        }

        @Override
        public Void getTransitionProperty(Node transition) {
            return null;
        }

        @Override
        public Collection<Node> getStates() {
            return IteratorUtil.list(TSTraversal.breadthFirstIterator(this, inputAlphabet));
        }

        /*
         * We need to override the default MooreMachine mapping, because its StateIDStaticMapping class requires our
         * nodeIDs, which requires our states, which requires our nodeIDs, which requires ... infinite loop!
         */
        @Override
        public <V> MutableMapping<Node, V> createStaticStateMapping() {
            return new MapMapping<>();
        }
    }
}
