/*
s * Copyright (C) 2014-2015 The LearnLib Contributors
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
package de.learnlib.ralib.learning;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibLearningTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 *
 * @author falk
 */
public class LearnPalindromeIOTest extends RaLibLearningTestSuite {

    @Test
    public void learnLoginExampleIO() {
        
        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/palindrome.xml");
        
        RegisterAutomaton model = loader.getRegisterAutomaton();
        logger.log(Level.FINE, "SYS: {0}", model);
        
        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});
        
        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            TypedTheory<Integer> theory = new IntegerEqualityTheory(t);
            theory.setUseSuffixOpt(true);            
            teachers.put(t, theory);
        });

        ConstraintSolver solver = new SimpleConstraintSolver();
        
        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        
        super.setHypValidator((hyp) -> {
        	Assert.assertEquals(hyp.getStates().size(), 5);
            Assert.assertEquals(hyp.getTransitions().size(), 16);
        });

        super.runIOLearningExperiments(sul, teachers, consts, false, solver, actions, ERROR);
    }
}
