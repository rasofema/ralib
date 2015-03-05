/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package de.learnlib.ralib.automata.guards;

import com.google.common.base.Function;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.VarValuation;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A data expression encodes a relation between parameters and
 * registers as a logic formula using the jConstraints library.
 * 
 * @author falk
 * @param <T> type to which the expression evaluates
 */
public class DataExpression<T extends Object> {

    public static DataExpression<Boolean> TRUE = 
            new DataExpression<>(ExpressionUtil.TRUE, 
                    new HashMap<SymbolicDataValue, Variable>());
    
    public static DataExpression<Boolean> FALSE = 
            new DataExpression<>(ExpressionUtil.FALSE, 
                    new HashMap<SymbolicDataValue, Variable>());

    private static class MapFunction extends HashMap<String, String> implements Function<String, String> {
        @Override
        public String apply(String f) {
            return get(f);
        }        
    }    
    
    private final Expression<T> expression;    
    private final Map<SymbolicDataValue, Variable> mapping;

    public DataExpression(Expression<T> expression, 
            Map<SymbolicDataValue, Variable> mapping) {
        this.expression = expression;
        this.mapping = mapping;
    }
        
    public T evaluate(VarValuation vars, ParValuation pars, Constants consts) {
        
        Valuation val = new Valuation();        
        setVals(val, mapping, vars);
        setVals(val, mapping, pars);
        setVals(val, mapping, consts);
        
        return this.expression.evaluate(val);
    }
    
    private Valuation setVals(Valuation valuation, 
            Map<SymbolicDataValue, Variable> mapping,
            Mapping<? extends SymbolicDataValue, DataValue<?>> vals) {
        
        for (Entry<? extends SymbolicDataValue, DataValue<?>> e : vals) {
            // is entry relevant in this context?
            Variable var = mapping.get(e.getKey());
            if (var == null) {
                continue;
            }
            Object value = e.getValue().getId();
            valuation.setValue(var, value);
        }        
        return valuation;
    }

    @Override
    public String toString() {
        return this.mapping.toString() + "[" + this.expression.toString() + "]";
    }

    /**
     * @return the expression
     */
    public Expression<T> getExpression() {
        return expression;
    }

    /**
     * @return the mapping
     */
    public Map<SymbolicDataValue, Variable> getMapping() {
        return mapping;
    }
    
    
    
    public DataExpression<T> relabel(
            VarMapping<SymbolicDataValue, SymbolicDataValue> renaming) {

        Map<SymbolicDataValue, Variable> newMap = new HashMap<>();
        for (Entry<SymbolicDataValue, Variable> e : mapping.entrySet()) {
            SymbolicDataValue sdv = renaming.get(e.getKey());
            if (sdv == null) {
                sdv = e.getKey();
            }
            newMap.put(sdv, e.getValue());
        }
        return new DataExpression<>(expression, newMap);
    }
    
    public static DataExpression<Boolean> negate(DataExpression<Boolean> neg) {
        return new DataExpression<>(new Negation(neg.expression), neg.mapping);
    } 
    
    public static DataExpression<Boolean> and(DataExpression<Boolean> ... conj) {
        Set<SymbolicDataValue> vals = new HashSet<>();
        for (DataExpression<Boolean> expr : conj) {
            vals.addAll(expr.mapping.keySet());
        }
        return and(vals, conj);
    }
            
    public static DataExpression<Boolean> and(
            Collection<SymbolicDataValue> join, DataExpression<Boolean> ... conj) {
        
        Map<SymbolicDataValue, Variable> map = new HashMap<>();
        Expression<Boolean> and = null;
        
        int eIdx = 0;
        for (DataExpression<Boolean> expr : conj) {
            MapFunction mFkt = new MapFunction();
            for (Entry<SymbolicDataValue, Variable> e : expr.mapping.entrySet()) {
                
                if (join.contains(e.getKey())) {
                    // replace var name by joined name
                    Variable var = map.get(e.getKey());
                    if (var == null) {
                        map.put(e.getKey(), e.getValue());
                    } else {
                        mFkt.put(e.getValue().getName(), var.getName());
                    }
                } else {
                    mFkt.put(e.getValue().getName(), "e" + eIdx + "_" + 
                            e.getValue().getName());
                }
            }

            Expression<Boolean> _temp = ExpressionUtil.renameVars(
                    expr.expression, mFkt);
            
            and = (and == null) ? _temp : ExpressionUtil.and(and, _temp);                    
            eIdx++;
        }
        
        return new DataExpression<>(and, map);
    }

}
