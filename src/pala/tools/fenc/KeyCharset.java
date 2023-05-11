package pala.tools.fenc;

import java.util.List;

/**
 * <p>
 * Represents a numbered set of characters.
 * </p>
 * <p>
 * {@link KeyCharset}s are primarily used to sample random characters from. They
 * contain some number of characters in a certain order, (they are essentially
 * an array of characters) and can be indexed.
 * </p>
 * 
 * @author Palanath
 *
 */
public interface KeyCharset {

	@FunctionalInterface
	interface IntToCharFunction {
		char get(int index);
	}

	/**
	 * The number of chars available in this {@link KeyCharset}, that is, the index
	 * of the last available character plus 1.
	 * 
	 * @return The number of chars in the {@link KeyCharset}.
	 */
	int size();

	/**
	 * Returns the character indexed by the specified value or throws an
	 * {@link IllegalArgumentException} if the index is out of bounds.
	 * 
	 * @param val The index of the character to retrieve.
	 * @return The character.
	 * @throws IndexOutOfBoundsException If the provided index is out of bounds.
	 */
	char get(int val) throws IndexOutOfBoundsException;

	static KeyCharset from(char... arr) {
		return from(arr.length, a -> arr[a]);
	}

	static KeyCharset from(int size, IntToCharFunction retriever) {
		return new KeyCharset() {
			@Override
			public int size() {
				return size;
			}

			@Override
			public char get(int val) throws IndexOutOfBoundsException {
				return retriever.get(val);
			}
		};
	}

	static KeyCharset from(List<Character> chars) throws IndexOutOfBoundsException {
		return from(chars.size(), chars::get);
	}
}
