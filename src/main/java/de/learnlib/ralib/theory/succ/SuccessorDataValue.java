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
package de.learnlib.ralib.theory.succ;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;

/**
 *
 * @author falk
 */
public class SuccessorDataValue extends DataValue<Object> {

    private final SuccessorMinterms[] relations;

    public SuccessorDataValue(DataType type, Object id, SuccessorMinterms ... relations) {
        super(type, id);
        this.relations = relations;
    }
    
    SuccessorMinterms getMinTerms(int i) {
        return relations[i];
    }
        
}
