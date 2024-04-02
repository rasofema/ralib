package de.learnlib.ralib.automata.tree;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.word.Word;

@Test
public class AdaptativeRATreeBuilderTest {

    private static final PSymbolInstance PA = new PSymbolInstance(new InputSymbol("a", null));
    private static final PSymbolInstance PB = new PSymbolInstance(new InputSymbol("b", null));
    private static final Alphabet<PSymbolInstance> TEST_ALPHABET = Alphabets.fromArray(PA, PB);

    // Confluence Bug
    private static final Word<PSymbolInstance> WA = Word.fromLetter(PA);
    private static final Word<PSymbolInstance> WB = Word.fromLetter(PB);
    private static final Word<PSymbolInstance> WE = Word.epsilon();

    private final AdaptiveRATreeBuilder adaptiveRA =
            new AdaptiveRATreeBuilder(TEST_ALPHABET);
    @Test
    public void testOverwritingWithOrigin() {
        Assert.assertFalse(adaptiveRA.insert(WE, false));
        Assert.assertFalse(adaptiveRA.insert(WA, true));
        Assert.assertFalse(adaptiveRA.insertFromUser(WB, false));
//      WE:F - WA:T - WB:F
        Assert.assertFalse(adaptiveRA.lookup(WE).getSecond());
        Assert.assertTrue(adaptiveRA.lookup(WA).getSecond());
        Assert.assertFalse(adaptiveRA.lookup(WB).getSecond());


        Assert.assertTrue(adaptiveRA.insert(WB, true));
//      WE:F - WA:T - WB:T
        Assert.assertFalse(adaptiveRA.lookup(WE).getSecond());
        Assert.assertTrue(adaptiveRA.lookup(WA).getSecond());
        Assert.assertTrue(adaptiveRA.lookup(WB).getSecond());


        Assert.assertTrue(adaptiveRA.insertFromUser(WE, true));
//      WE:T - WA:T - WB:T
        Assert.assertTrue(adaptiveRA.lookup(WE).getSecond());
        Assert.assertTrue(adaptiveRA.lookup(WA).getSecond());
        Assert.assertTrue(adaptiveRA.lookup(WB).getSecond());


        Assert.assertFalse(adaptiveRA.insert(WE, false));
//      WE:T - WA:T - WB:T
        Assert.assertTrue(adaptiveRA.lookup(WE).getSecond());
        Assert.assertTrue(adaptiveRA.lookup(WA).getSecond());
        Assert.assertTrue(adaptiveRA.lookup(WB).getSecond());


        Assert.assertTrue(adaptiveRA.insert(WA, false));
//      WE:T - WA:F - WB:T
        Assert.assertTrue(adaptiveRA.lookup(WE).getSecond());
        Assert.assertFalse(adaptiveRA.lookup(WA).getSecond());
        Assert.assertTrue(adaptiveRA.lookup(WB).getSecond());

    }
}
