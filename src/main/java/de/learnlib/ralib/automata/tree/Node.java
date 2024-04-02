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

import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.incremental.CexOrigin;
import net.automatalib.incremental.dfa.Acceptance;
import net.automatalib.incremental.dfa.tree.IncrementalDFATreeBuilder;

/**
 * A node in the tree internally used by {@link IncrementalDFATreeBuilder}.
 */
final class Node {

    private Acceptance acceptance;
    private Map<PSymbolInstance,Node> children;

    private CexOrigin origin;

    /**
     * Constructor. Constructs a new node with no children and an acceptance value of {@link Acceptance#DONT_KNOW}
     */
    Node() {
        this(Acceptance.DONT_KNOW);
        this.origin = CexOrigin.UNKNOWN;
    }

    /**
     * Constructor. Constructs a new node with no children and the specified acceptance value.
     *
     * @param acceptance
     *         the acceptance value for the node
     */
    Node(Acceptance acceptance) {
        this.acceptance = acceptance;
    }

    /**
     * Retrieves the acceptance value of this node.
     *
     * @return the acceptance value of this node
     */
    Acceptance getAcceptance() {
        return acceptance;
    }

    /**
     * Sets the acceptance value for this node.
     *
     * @param acceptance
     *         the new acceptance value for this node
     */
    void setAcceptance(Acceptance acceptance) {
        this.acceptance = acceptance;
    }

    public CexOrigin getOrigin() {
        return this.origin;
    }

    public void setOrigin(CexOrigin origin) {
        this.origin = origin;
    }

    /**
     * Retrieves, for a given index, the respective child of this node.
     *
     * @return the child for the given index, or {@code null} if there is no such child
     */
    @Nullable Node getChild(PSymbolInstance input) {
        if (children == null) {
            return null;
        }
        return children.get(input);
    }

    /**
     * Sets the child for a given index.
     * @param child
     *         the new child
     */
    void setChild(PSymbolInstance input, Node child) {
        if (children == null) {
            children = new HashMap<>();
        }
        children.put(input, child);
    }

    void makeSink() {
        children = null;
        acceptance = Acceptance.FALSE;
    }
}
