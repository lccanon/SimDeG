package simdeg.util;

import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;

import flanagan.math.PsRandom;

/*
 * Manager of random generators. Allows to associate a different seed
 * to each need (represented with a string). It would have been cleaner
 * to have a generic class if PsRandom inherited from Random.
 */
public class RandomManager {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(RandomManager.class.getName());

    private static final Map<String,MersenneTwisterFast> randoms
        = new HashMap<String,MersenneTwisterFast>();

    private static final Map<String,PsRandom> psRandoms
        = new HashMap<String,PsRandom>();

    static {
        setSeed("", 3127L);
    }

    public static void setSeed(String key, long seed) {
        if (randoms.get(key) == null)
            randoms.put(key, new MersenneTwisterFast());
        randoms.get(key).setSeed(seed);
        if (psRandoms.get(key) == null)
            psRandoms.put(key, new PsRandom());
        psRandoms.get(key).setSeed(seed);
    }

    public static MersenneTwisterFast getRandom() {
        return randoms.get("");
    }

    public static PsRandom getPsRandom() {
        return psRandoms.get("");
    }

    public static MersenneTwisterFast getRandom(String key) {
        logger.finer("Random requesting for key " + key);
        if (randoms.get(key) == null)
            setSeed(key, 0L);
        return randoms.get(key);
    }

    public static PsRandom getPsRandom(String key) {
        if (psRandoms.get(key) == null)
            setSeed(key, 0L);
        return psRandoms.get(key);
    }

}
