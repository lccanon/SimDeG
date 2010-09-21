package simdeg.util;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.lang.Class;
import java.lang.reflect.Method;
import java.lang.NoSuchMethodException;
import java.lang.SecurityException;

/**
 * Utility class equivalent to java.util.Collections with a random features.
 */
public class Collections {

	private Collections() {
	}

	/**
	 * Utilitary function returning the given collection with the element
	 * inside.
	 */
	public static <T, C extends Collection<T>> C addElement(T element,
			C collection) {
		collection.add(element);
		return collection;
	}

	/**
	 * Utilitary function which return a random subset of given size from a set.
	 * Throws an OutOfRangeException if the size is too large.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Set<T> getRandomSubGroup(int size, Set<T> group,
			MersenneTwisterFast random) {
		/* Test for admissibility of parameters */
		if (size > group.size() || size < 0)
			throw new OutOfRangeException(size, 0, group.size());

		/* Optimization */
		if (size == 0)
			return new HashSet<T>();
		if (size == group.size())
			return new HashSet<T>(group);
		if (size == 1) {
			final int value = random.nextInt(group.size());
			Iterator<T> it = group.iterator();
			for (int i = 0; i < value; i++)
				it.next();
			return addElement(it.next(), new HashSet<T>());
		}

		/* Select randomly a subgroup */
		Set<T> subGroup = new HashSet<T>();
		T[] groupArray = group.toArray((T[]) new Object[0]);
		for (int i = 0; i < size; i++) {
			final int value = random.nextInt(groupArray.length
					- subGroup.size());
			T element = groupArray[value];
			subGroup.add(element);
			groupArray[value] = groupArray[groupArray.length - subGroup.size()];
		}
		return subGroup;
	}

	/**
	 * Utilitary function which return a random sublist of given size from a
	 * list. Throws an OutOfRangeException if the size is too large.
	 */
	public static <T> List<T> getRandomSubGroup(int size, List<T> group,
			MersenneTwisterFast random) {
		/* Test for admissibility of parameters */
		if (size > group.size() || size < 0)
			throw new OutOfRangeException(size, 0, group.size());

		/* Optimization */
		if (size == 0)
			return new ArrayList<T>();
		if (size == group.size())
			return new ArrayList<T>(group);
		if (size == 1) {
			final int value = random.nextInt(group.size());
			return addElement(group.get(value), new ArrayList<T>());
		}

		/* Select randomly a subgroup */
		List<T> copyGroup = new ArrayList<T>(group);
		List<T> subGroup = new ArrayList<T>();
		for (int i = 0; i < size; i++) {
			final int value = random.nextInt(copyGroup.size());
			T element = copyGroup.get(value);
			subGroup.add(element);
			copyGroup.add(value, copyGroup.get(copyGroup.size() - 1));
			copyGroup.remove(copyGroup.size() - 1);
		}
		return subGroup;
	}

	/**
	 * Parses a String into a List.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> parseList(Class<T> type, String str) {
		List<T> result = new ArrayList<T>();
		if (type == String.class) {
			Scanner scanner = new Scanner(str.replace(" ", ""));
			scanner.useDelimiter("\\[|,|\\]");
			while (scanner.hasNext())
				result.add((T) scanner.next());
			return result;
		}
		try {
			Method valueOf = type.getDeclaredMethod("valueOf", String.class);
			Scanner scanner = new Scanner(str.replace(" ", ""));
			scanner.useDelimiter("\\[|,|\\]");
			while (scanner.hasNext()) {
				try {
					T elem = (T) valueOf.invoke(null, scanner.next());
					result.add(elem);
				} catch (Exception e) {
				}
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Parses a String into a List of List.
	 */
	public static <T> List<List<T>> parseListOfList(Class<T> type, String str) {
		List<List<T>> result = new ArrayList<List<T>>();
		Scanner scanner = new Scanner(str.replace(" ", ""));
		scanner.useDelimiter("\\(|;|\\)");
		while (scanner.hasNext()) {
			final List<T> list = parseList(type, scanner.next());
			if (!list.isEmpty())
				result.add(list);
		}
		return result;
	}
}
