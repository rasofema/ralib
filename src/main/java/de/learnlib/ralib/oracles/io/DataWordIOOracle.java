package de.learnlib.ralib.oracles.io;

import de.learnlib.ralib.oracles.DataWordOracle;

/**
 * Unfortunately, it's not trivial to combine two interfaces without making a combined interface.
 */
public interface DataWordIOOracle extends DataWordOracle, IOOracle{

}
