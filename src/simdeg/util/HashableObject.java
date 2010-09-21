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

/**
 * To avoid the random hashing done by JAVA, all objects used in hash objects
 * must inherit from this class.
 */
public class HashableObject implements Cloneable {

	private int hash = count++;

	private static int count = 0;

	@Override
	protected HashableObject clone() {
		try {
			HashableObject result = (HashableObject) super.clone();
			result.hash = hash;
			return result;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int hashCode() {
		return hash;
	}

}