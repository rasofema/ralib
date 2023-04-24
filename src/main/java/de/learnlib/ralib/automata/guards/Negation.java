/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
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
package de.learnlib.ralib.automata.guards;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import java.util.Set;

/**
 *
 * @author falk
 */
public class Negation extends GuardExpression {

    private final GuardExpression negated;

    public Negation(GuardExpression negated) {
        this.negated = negated;
    }

    public GuardExpression getNegated() {
    	return negated;
    }

    @Override
    public GuardExpression relabel(VarMapping relabelling) {
        GuardExpression newNegated = negated.relabel(relabelling);
        return new Negation(newNegated);
    }

    @Override
    public boolean isSatisfied(Mapping<SymbolicDataValue, DataValue<?>> val) {
        return !negated.isSatisfied(val);
    }

    @Override
    public String toString() {
        return "(!" + negated + ")";
    }

    @Override
    protected void getSymbolicDataValues(Set<SymbolicDataValue> vals) {
        this.negated.getSymbolicDataValues(vals);
    }

}
