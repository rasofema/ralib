package de.learnlib.ralib.dt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Iterators;

import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.PIVRemappingIterator;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.LocationComponent;
import de.learnlib.ralib.learning.PrefixContainer;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.learning.rattt.DiscriminationTree;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class DTLeaf extends DTNode implements LocationComponent {

    private MappedPrefix access;
    private PrefixSet shortPrefixes;
    private PrefixSet otherPrefixes;

    private final Map<ParameterizedSymbol, Branching> branching = new LinkedHashMap<ParameterizedSymbol, Branching>();
    private final TreeOracle oracle;

    public DTLeaf(TreeOracle oracle) {
        super();
        access = null;
        shortPrefixes = new PrefixSet();
        otherPrefixes = new PrefixSet();
        this.oracle = oracle;
    }

    public DTLeaf(MappedPrefix as, TreeOracle oracle) {
        super();
        access = as;
        shortPrefixes = new PrefixSet();
        otherPrefixes = new PrefixSet();
        this.oracle = oracle;
    }

    public DTLeaf(DTLeaf l) {
        access = new MappedPrefix(l.access);
        shortPrefixes = new PrefixSet(l.shortPrefixes);
        otherPrefixes = new PrefixSet(l.otherPrefixes);
        branching.putAll(l.branching);
        oracle = l.oracle;
    }

    public void addPrefix(Word<PSymbolInstance> p) {
        otherPrefixes.add(new MappedPrefix(p));
    }

    public void addPrefix(MappedPrefix p) {
        assert p.getParsInVars().size() == access.getParsInVars().size();
        otherPrefixes.add(p);
    }

    void setAccessSequence(MappedPrefix mp) {
        access = mp;
    }

    public void addShortPrefix(Word<PSymbolInstance> prefix, PIV registers) {
        if (access == null)
            access = new MappedPrefix(prefix, registers);
        else
            addShortPrefix(new ShortPrefix(prefix, registers));
    }

    public void addShortPrefix(ShortPrefix prefix) {
        if (otherPrefixes.contains(prefix.getPrefix()))
            otherPrefixes.remove(prefix.getPrefix());
        assert access != null;
        shortPrefixes.add(prefix);
    }

    public boolean removeShortPrefix(MappedPrefix p) {
        return shortPrefixes.removeIf((e) -> {
            return e.getPrefix().equals(p.getPrefix());
        });
    }

    public boolean removeShortPrefix(Word<PSymbolInstance> p) {
        return shortPrefixes.remove(p);
    }

    public boolean removePrefix(Word<PSymbolInstance> p) {
        return shortPrefixes.removeIf((e) -> e.getPrefix().equals(p)) || otherPrefixes.removeIf((e) -> e.getPrefix().equals(p));
    }

    /**
     * Clears the short and other prefix sets.
     * The only prefix remaining will be the leaf's access sequence. 
     */
    public void clear() {
        shortPrefixes = new PrefixSet();
        otherPrefixes = new PrefixSet();
    }

    public PrefixSet getShortPrefixes() {
        return shortPrefixes;
    }

    public PrefixSet getPrefixes() {
        return otherPrefixes;
    }

    public boolean isLeaf() {
        return true;
    }

    @Override
    public boolean isAccepting() {
        TreeQueryResult tqr = access.getTQRs().get(RaStar.EMPTY_SUFFIX);
        return tqr.getSdt().isAccepting();
    }

    MappedPrefix getMappedPrefix(Word<PSymbolInstance> p) {
        if (access.getPrefix().equals(p))
            return access;
        MappedPrefix ret = null;
        ret = shortPrefixes.get(p);
        if (ret != null)
            return ret;
        ret = otherPrefixes.get(p);
        return ret;
    }

    @Override
    public Word<PSymbolInstance> getAccessSequence() {
        return access.getPrefix();
    }

    @Override
    public VarMapping getRemapping(PrefixContainer r) {
        if (r.getPrefix().equals(this.getAccessSequence()))
            return null;
        MappedPrefix mp = otherPrefixes.get(r.getPrefix());
        if (mp == null)
            mp = shortPrefixes.get(r.getPrefix());
        PIVRemappingIterator it = new PIVRemappingIterator(mp.getParsInVars(), this.access.getParsInVars());
        return it.next();
    }

    @Override
    public Branching getBranching(ParameterizedSymbol action) {
        return branching.get(action);
    }
    
    public Branching getBranching(ParameterizedSymbol action, Word<PSymbolInstance> src) {
    	if (src.equals(getAccessSequence()))
    		return getBranching(action);
    	ShortPrefix sp = (ShortPrefix)shortPrefixes.get(src);
    	if (sp == null)
    		return null;
    	return sp.getBranching(action);
    }

    @Override
    public MappedPrefix getPrimePrefix() {
        return access;
    }

    Collection<Word<PSymbolInstance>> getAllPrefixes() {
        Collection<Word<PSymbolInstance>> prefs = new ArrayList<Word<PSymbolInstance>>();
        prefs.add(this.getAccessSequence());
        Iterator<MappedPrefix> it = shortPrefixes.iterator();
        while (it.hasNext())
            prefs.add(it.next().getPrefix());
        it = otherPrefixes.iterator();
        while (it.hasNext())
            prefs.add(it.next().getPrefix());
        return prefs;
    }
    
    void getMappedExtendedPrefixes(Collection<MappedPrefix> mps) {
        mps.addAll(shortPrefixes.get());
        mps.addAll(otherPrefixes.get());
    }

    @Override
    public DTInnerNode getParent() {
        return parent;
    }

    public MappedPrefix getPrefix(Word<PSymbolInstance> prefix) {
        return otherPrefixes.get(prefix);
    }
    
    void addTQRs(PIV primePIV, SymbolicSuffix suffix) {
        access.updateMemorable(primePIV);
        Iterator<MappedPrefix> it = Iterators.concat(shortPrefixes.iterator(), otherPrefixes.iterator());
        while (it.hasNext()) {
            MappedPrefix mp = it.next();
            mp.computeTQR(suffix, oracle);
        }
    }

    void addTQRs(PIV primePIV, SymbolicSuffix s, boolean addToAccess) {
        if (addToAccess) {
            access.computeTQR(s, oracle);
        }
        addTQRs(primePIV, s);
    }

    @Override
    public Collection<PrefixContainer> getOtherPrefixes() {
        return Stream.concat(otherPrefixes.get().stream(), shortPrefixes.get().stream()).collect(Collectors.toList());
    }

    /**
     * Elevate a prefix from the set of other prefixes to the set of short prefix,
     * and checks whether this leads to a refinement. The branches of prefix are
     * sifted into the tree, and added to their respective leaves. If a branch of
     * prefix leads to another location than the same branch of the access sequence,
     * returns the diverging words as a Pair, otherwise returns null.
     * 
     * @param dt
     * @param prefix
     * @param oracle
     * @return Pair of diverging words, if such a pair of words exists. Otherwise
     *         null.
     */
    public Pair<Word<PSymbolInstance>, Word<PSymbolInstance>> elevatePrefix(DT dt, Word<PSymbolInstance> prefix,
            DTHyp hyp, SDTLogicOracle logicOracle) {
        MappedPrefix mp = otherPrefixes.get(prefix);
        assert mp != null : "Cannot elevate prefix that is not contained in leaf " + this + " === " + prefix;
        boolean removed = otherPrefixes.remove(mp);

        ShortPrefix sp = new ShortPrefix(mp);
        addShortPrefix(sp);

        return startPrefix(dt, sp, hyp, logicOracle);
    }

    private Pair<Word<PSymbolInstance>, Word<PSymbolInstance>> startPrefix(DT dt, ShortPrefix mp, DTHyp hyp, SDTLogicOracle logicOracle) {
    	
//    	while(!dt.checkDeterminism(hyp));
    	
        Pair<Word<PSymbolInstance>, Word<PSymbolInstance>> divergance = null;
        boolean input = this.isInput(mp.getPrefix().lastSymbol().getBaseSymbol());
        for (ParameterizedSymbol ps : dt.getInputs()) {
//			if (dt.getIoMode() && (input ^ isInput(ps)))
//				continue;

            SymbolicDecisionTree[] sdtsP = this.getSDTsForInitialSymbol(mp, ps);
            Branching prefixBranching = oracle.getInitialBranching(mp.getPrefix(), ps, mp.getParsInVars(), sdtsP);
            mp.putBranching(ps, prefixBranching);

            SymbolicDecisionTree[] sdtsAS = this.getSDTsForInitialSymbol(this.getPrimePrefix(), ps);
            Branching accessBranching = oracle.getInitialBranching(getAccessSequence(), ps, this.access.getParsInVars(),
                    sdtsAS);

            assert prefixBranching.getBranches().size() == accessBranching.getBranches().size();

            for (Word<PSymbolInstance> p : prefixBranching.getBranches().keySet()) {
                if (dt.getIoMode() && ((input ^ !isInput(ps)) || hyp.getLocation(p) == null)) {
                    dt.getSink().addPrefix(p);
                    continue;
                }

                Word<PSymbolInstance> a = null;
                if (hyp == null) {
                    // no hypothesis yet, assume all true guards
                    for (Word<PSymbolInstance> w : accessBranching.getBranches().keySet()) {
                        if (w.lastSymbol().getBaseSymbol().equals(p.lastSymbol().getBaseSymbol())) {
                            a = w;
                            break;
                        }
                    }
                    assert a != null;
                } else {
                	a = branchWithSameGuard(p, prefixBranching, mp.getParsInVars(), accessBranching, access.getParsInVars(), logicOracle);
//                    a = hyp.branchWithSameGuard(p, accessBranching);
                }

                DTLeaf leaf = dt.sift(p, true);

                if (!dt.getLeaf(a).equals(leaf)) {
                    divergance = new ImmutablePair<Word<PSymbolInstance>, Word<PSymbolInstance>>(p, a);
                }
            }
        }
        return divergance;
    }
    
    static public Word<PSymbolInstance> branchWithSameGuard(Word<PSymbolInstance> word, Branching wordBranching, PIV wordPIV, Branching accBranching, PIV accPIV, SDTLogicOracle oracle) {
    	Word<PSymbolInstance> a = null;
    	TransitionGuard g = null;
    	for (Entry<Word<PSymbolInstance>, TransitionGuard> e : wordBranching.getBranches().entrySet()) {
    		if (e.getKey().equals(word)) {
    			g = e.getValue();
    			break;
    		}
    	}
    	
    	for (Entry<Word<PSymbolInstance>, TransitionGuard> e : accBranching.getBranches().entrySet()) {
    		TransitionGuard ag = e.getValue();
    		boolean eq = oracle.areEquivalent(g, accPIV, ag, accPIV, new Mapping<SymbolicDataValue, DataValue<?>>());
    		if (eq) {
    			a = e.getKey();
    			break;
    		}
    	}
    	return a;
    }

    public boolean isRefinemement(DT dt, Word<PSymbolInstance> word) {
        ParameterizedSymbol ps = word.lastSymbol().getBaseSymbol();
        DTLeaf target = dt.getLeaf(word);
        
        Branching b = getBranching(ps);
        boolean refinement = true;
        for (Word<PSymbolInstance> w : b.getBranches().keySet()) {
            if (dt.getLeaf(w) == target)
                refinement = false;
        }
        
        return refinement;        
//        DTLeaf target = dt.sift(word, false);
//
//        Branching b = getBranching(ps);
//        boolean refinement = true;
//        for (Word<PSymbolInstance> w : b.getBranches().keySet()) {
//            if (dt.getLeaf(w) == target)
//                refinement = false;
//        }
//
//        return refinement;
    }

    void start(DT dt, boolean ioMode, ParameterizedSymbol... inputs) {
        boolean input = isInputComponent();
        for (ParameterizedSymbol ps : inputs) {
//			if (ioMode && (input ^ isInput(ps))) {
//	            continue;
//	        }

            SymbolicDecisionTree[] sdts = this.getSDTsForInitialSymbol(ps);
            Branching b = oracle.getInitialBranching(getAccessSequence(), ps, access.getParsInVars(), sdts);
            branching.put(ps, b);
            for (Word<PSymbolInstance> prefix : b.getBranches().keySet()) {
                if (ioMode && (dt.getSink() != null) && (input ^ isInput(ps)))
                    dt.getSink().addPrefix(prefix);
                else
                    dt.sift(prefix, true);
            }
        }
    }

    void start(DT dt, Map<ParameterizedSymbol, Branching> branching) {
        this.branching.putAll(branching);
    }

    boolean updateBranching(DT dt) {
        boolean ret = true;
        for (ParameterizedSymbol p : branching.keySet()) {
            boolean ub = updateBranching(p, dt);
            ret = ret && ub;
        }
        return ret;
    }

    private boolean updateBranching(ParameterizedSymbol ps, DiscriminationTree dt) {
        Branching b = branching.get(ps);
        SymbolicDecisionTree[] sdts = getSDTsForInitialSymbol(ps);
        Branching newB = oracle.updateBranching(access.getPrefix(), ps, b, access.getParsInVars(), sdts);
        boolean ret = true;

        for (Word<PSymbolInstance> prefix : newB.getBranches().keySet()) {
            if (!b.getBranches().containsKey(prefix)) {
                dt.sift(prefix, true);
                ret = false;
            }
        }
        
        for (MappedPrefix sp : shortPrefixes.get()) {
            ret = ret & updateBranching(ps, (ShortPrefix) sp, dt);
        }
        
        branching.put(ps, newB);
        return ret;
    }
    
    private boolean updateBranching(ParameterizedSymbol ps, ShortPrefix sp, DiscriminationTree dt) {
        Branching b = branching.get(ps);
        SymbolicDecisionTree[] sdts = getSDTsForInitialSymbol(sp, ps);
        Branching newB = oracle.updateBranching(sp.getPrefix(), ps, b, sp.getParsInVars(), sdts);
        boolean ret = true;
        
        for (Word<PSymbolInstance> prefix : newB.getBranches().keySet()) {
            if (!b.getBranches().containsKey(prefix)) {
                dt.sift(prefix, true);
                ret = false;
            }
        }
        sp.putBranching(ps, newB);;
        return ret;
    }

    private SymbolicDecisionTree[] getSDTsForInitialSymbol(ParameterizedSymbol p) {
        return getSDTsForInitialSymbol(this.getPrimePrefix(), p);
    }

    private SymbolicDecisionTree[] getSDTsForInitialSymbol(MappedPrefix mp, ParameterizedSymbol p) {
        List<SymbolicDecisionTree> sdts = new ArrayList<>();
        for (Entry<SymbolicSuffix, TreeQueryResult> e : mp.getTQRs().entrySet()) {
            Word<ParameterizedSymbol> acts = e.getKey().getActions();
            if (acts.length() > 0 && acts.firstSymbol().equals(p)) {
                sdts.add(makeConsistent(e.getValue().getSdt(), e.getValue().getPiv(), mp.getParsInVars()));
            }
        }
        return sdts.toArray(new SymbolicDecisionTree[] {});
    }

    private SymbolicDecisionTree makeConsistent(SymbolicDecisionTree sdt, PIV piv, PIV memorable) {
        VarMapping relabeling = new VarMapping();
        for (Entry<Parameter, Register> e : piv.entrySet()) {
            Register r = memorable.get(e.getKey());
            relabeling.put(e.getValue(), r);
        }
        return sdt.relabel(relabeling);
    }

    public boolean checkVariableConsistency(DT dt, Constants consts) {
        if (!checkVariableConsistency(access, dt, consts)) {
            return false;
        }

        Iterator<MappedPrefix> it = otherPrefixes.iterator();
        while (it.hasNext()) {
            if (!checkVariableConsistency(it.next(), dt, consts))
                return false;
        }
        it = shortPrefixes.iterator();
        while (it.hasNext()) {
            if (!checkVariableConsistency(it.next(), dt, consts))
                return false;
        }
        return true;
    }

    private boolean checkVariableConsistency(MappedPrefix mp, DT dt, Constants consts) {
        if (mp.getPrefix().length() < 2)
            return true;

        Word<PSymbolInstance> prefix = mp.getPrefix().prefix(mp.getPrefix().length() - 1);
//        MappedPrefix prefixMapped = dt.getLeaf(prefix).getPrimePrefix();
        DTLeaf prefixLeaf = dt.getLeaf(prefix);
        MappedPrefix prefixMapped = prefixLeaf.getMappedPrefix(prefix);

        PIV memPrefix = prefixMapped.getParsInVars();
        PIV memMP = mp.getParsInVars();

        int max = DataWords.paramLength(DataWords.actsOf(prefix));
        
        for (Parameter p : memMP.keySet()) {
            if (!memPrefix.containsKey(p) && p.getId() <= max) {
                SymbolicSuffix suffix = mp.getSuffixForMemorable(p);
                SymbolicSuffix newSuffix = new SymbolicSuffix(mp.getPrefix(), suffix, consts);

                dt.addSuffix(newSuffix, prefixLeaf);
                return false;
            }
        }
        return true;
    }
    
    public boolean checkParameterAssignment(Word<PSymbolInstance> word, DT dt, Hypothesis hyp, Constants consts) {
    	if (word.length() < 2) 
    		return true;
    	
    	MappedPrefix mp = getMappedPrefix(word);
    	Word<PSymbolInstance> prefix = word.prefix(word.length() - 1);
    	DTLeaf prefixLeaf = dt.getLeaf(prefix);
    	MappedPrefix prefixMapped = prefixLeaf.getMappedPrefix(prefix);
    	
    	PIV memPrefix = prefixMapped.getParsInVars();
    	PIV memMP = mp.getParsInVars();
    	
    	VarMapping<Register, ? extends SymbolicDataValue> transitionMap = hyp.getLastTransitionAssignment(word);
    	
    	for (Parameter p : memMP.keySet()) {
    		if (!memPrefix.containsKey(p) && !hyp.getLastTransitionAssignment(word).containsValue(p)) {
    			SymbolicSuffix suffix = mp.getSuffixForMemorable(p);
    			SymbolicSuffix newSuffix = new SymbolicSuffix(word, suffix, consts);
    			
    			dt.addSuffix(newSuffix, prefixLeaf);
    			return false;
    		}
    	}
    	return true;
    }
    
    public boolean checkDeterminism(DT dt, Hypothesis hyp, Collection<Word<PSymbolInstance>> allPrefs, Constants consts) {
    	if (!checkDeterminism(getAccessSequence(), branching, dt, hyp, allPrefs, consts))
    		return false;
    	Iterator<MappedPrefix> it = shortPrefixes.iterator();
    	while (it.hasNext()) {
    		ShortPrefix sp = (ShortPrefix)it.next();
    		if (!checkDeterminism(sp.getPrefix(), sp.getBranching(), dt, hyp, allPrefs, consts))
    			return false;
    	}
    	return true;
    }
    
    private boolean checkDeterminism(Word<PSymbolInstance> word,
    		                         Map<ParameterizedSymbol, Branching> branching,
    		                         DT dt, Hypothesis hyp,
    		                         Collection<Word<PSymbolInstance>> allPrefs,
    		                         Constants consts) {
//    	Optional<Word<PSymbolInstance>> nonDetPref = allPrefs.stream()
//    			                                             .filter(i -> (i.size() > 1 &&
//    			                                            		       i.prefix(i.size()-1).equals(word) &&
//    	                                                                   !branching.get(i.lastSymbol()
//    	                                                    		                       .getBaseSymbol())
//    	                                                    		                       .getBranches()
//    	                                                    		                 .containsKey(i)))
//    	                                                     .findAny();
//    	
//    	if (nonDetPref.isPresent()) {
//    		Word<PSymbolInstance> dest_id = nonDetPref.get();
//    		DTLeaf dest_c = dt.getLeaf(dest_id);
//    		SymbolicSuffix suff1 = new SymbolicSuffix(word, dest_id.suffix(1));
//    		SymbolicSuffix suff2 = dt.findLCA(this, dest_c).getSuffix();
//    		SymbolicSuffix suffix = suff1.concat(suff2);
//    		dt.addSuffix(suffix, this);
//    		return false;
//    	}
    	
    	Word<PSymbolInstance> prefixNonDet = null;
    	for (Word<PSymbolInstance> w : allPrefs) {
    		if (w.size() > 1 &&
    				w.prefix(w.size()-1).equals(word)) {
    			Branching b = branching.get(w.lastSymbol().getBaseSymbol());
    			if (!b.getBranches().containsKey(w)) {
    				Word<PSymbolInstance> prefixSameGuard = hyp.branchWithSameGuard(w, b);
    				if (dt.getLeaf(prefixSameGuard) != dt.getLeaf(w)) {
    					prefixNonDet = w;
    					break;
    				}
    			}
//    				!branching.get(w.lastSymbol().getBaseSymbol()).getBranches().containsKey(w));
    		}
    	}
    	
    	if (prefixNonDet != null) {
    		DTLeaf dest_c = dt.getLeaf(prefixNonDet);
    		SymbolicSuffix suff1 = new SymbolicSuffix(word, prefixNonDet.suffix(1));
    		SymbolicSuffix suff2 = dt.findLCA(this, dest_c).getSuffix();
    		SymbolicSuffix suffix = suff1.concat(suff2);
    		dt.addSuffix(suffix, this);
    		return false;
    	}
    	
    	return true;
    }

    public boolean isInputComponent() {
        if (this.getAccessSequence().length() == 0)
            return true;

        ParameterizedSymbol ps = this.getAccessSequence().lastSymbol().getBaseSymbol();
        return !isInput(ps);
    }

    public static boolean isInput(ParameterizedSymbol ps) {
        return (ps instanceof InputSymbol);
    }

    public Word<PSymbolInstance> checkIOConsistency() {
        boolean input = isInputComponent();
        for (MappedPrefix mp : otherPrefixes.get()) {
            if (!(input ^ isInput(mp.getPrefix().lastSymbol().getBaseSymbol())))
                return mp.getPrefix();
        }
        return null;
    }

    @Override
    public DTLeaf copy() {
        return new DTLeaf(this);
    }

    public String toString() {
        return getAllPrefixes().toString();
    }
}
