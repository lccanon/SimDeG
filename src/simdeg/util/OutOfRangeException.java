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

import java.lang.IllegalArgumentException;

/**
 * Exception thrown when the selection of an Result have to be postponed.
 */
public class OutOfRangeException extends IllegalArgumentException {

	private static final long serialVersionUID = 0L;

	private final Object value, min, max;

	public OutOfRangeException(Object value, Object min, Object max) {
		super("Value " + value + " out of range " + "[" + min + ", " + max
				+ "]");
		this.value = value;
		this.min = min;
		this.max = max;
	}

	public Object getValue() {
		return value;
	}

	public Object getMin() {
		return min;
	}

	public Object getMax() {
		return max;
	}

}
