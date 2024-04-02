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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.incremental.AdaptiveConstruction;
import net.automatalib.incremental.CexOrigin;
import net.automatalib.incremental.dfa.Acceptance;
import net.automatalib.util.graph.traversal.GraphTraversal;
import net.automatalib.word.Word;

public class AdaptiveRATreeBuilder extends AbstractAlphabetBasedRATreeBuilder
        implements AdaptiveConstruction<RegisterAutomaton, PSymbolInstance, Boolean>, RABuilder {
    private final Map<Node, Word<PSymbolInstance>> nodeToQuery;

    public AdaptiveRATreeBuilder(Alphabet<PSymbolInstance> inputAlphabet) {
        super(inputAlphabet);
        this.nodeToQuery = new LinkedHashMap<>();
    }

    private boolean insert(Word<? extends PSymbolInstance> word, Boolean acceptance, CexOrigin origin) {
        Node prev = null;
        PSymbolInstance childInput = null;
        Node curr = root;
        boolean hasOverwritten = false;

        for (PSymbolInstance sym : word) {
            Node succ = curr.getChild(sym);
            if (succ == null) {
                succ = new Node();
                curr.setChild(sym, succ);
            }
            prev = curr;
            childInput = sym;
            curr = succ;
        }

        assert curr != null;

        Acceptance acc = curr.getAcceptance();
        Acceptance newWordAcc = Acceptance.fromBoolean(acceptance);
        if (acc == Acceptance.DONT_KNOW) {
            curr.setAcceptance(newWordAcc);
        } else if (acc != newWordAcc) {
//          Only update if from same origin or, otherwise, origin of curr is not user
            if (origin == curr.getOrigin() || curr.getOrigin() != CexOrigin.USER) {
                hasOverwritten = true;
                removeQueries(curr);
                if (prev == null) {
                    assert word.isEmpty();
                    root.setAcceptance(newWordAcc);
                    root.setOrigin(origin);
                } else {
                    prev.setChild(childInput, null);
                    curr = insertNode(prev, childInput, acceptance);
                    curr.setOrigin(origin);
                }
            }
        }

        // Make sure it uses the new ages.
        nodeToQuery.remove(curr);
        nodeToQuery.put(curr, Word.upcast(word));

        return hasOverwritten;
    }

    @Override
    public boolean insert(Word<? extends PSymbolInstance> word, Boolean acceptance) {
        return insert(word, acceptance, CexOrigin.SUL);
    }

    @Override
    public boolean insertFromUser(Word<? extends PSymbolInstance> word, Boolean acceptance) {
        return insert(word, acceptance, CexOrigin.USER);
    }


    private void removeQueries(Node node) {
        GraphTraversal.breadthFirstIterator(this.asGraph(), Collections.singleton(node))
                .forEachRemaining(nodeToQuery::remove);
    }

    @Override
    public @Nullable Word<PSymbolInstance> getOldestInput() {
        final Iterator<Word<PSymbolInstance>> iter = nodeToQuery.values().iterator();
        return iter.hasNext() ? iter.next() : null;
    }
}
