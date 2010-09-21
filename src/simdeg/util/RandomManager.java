/*
 Emapse provides methods for evaluating expressions on random variables.
 Copyright (C) 2010 Canon

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package simdeg.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/*
 * Manager of random generators. Allows to associate a different seed
 * to each need (represented with a string). It would have been cleaner
 * to have a generic class if PsRandom inherited from Random.
 */
public class RandomManager {

	/** Logger */
	private static final Logger logger = Logger.getLogger(RandomManager.class
			.getName());

	private static final Map<String, MersenneTwisterFast> randoms = new HashMap<String, MersenneTwisterFast>();

	static {
		setSeed("", 0L);
	}

	public static void setSeed(String key, long seed) {
		if (!randoms.containsKey(key))
			randoms.put(key, new MersenneTwisterFast());
		randoms.get(key).setSeed(seed);
	}

	public static MersenneTwisterFast getRandom(String... keys) {
		logger.finer("Random requesting for key " + Arrays.toString(keys));
		for (String key : keys) {
			if (randoms.containsKey(key))
				return randoms.get(key);
		}
		return randoms.get("");
	}
}
