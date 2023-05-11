package pala.tools.fenc;

import java.util.Arrays;
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

	@FunctionalInterface
	interface CharToCharFunction {
		char transform(char input);
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

	static KeyCharset from(List<Character> chars) {
		return from(chars.size(), chars::get);
	}

	static KeyCharset combine(KeyCharset first, KeyCharset second) {
		int fs = first.size(), ss = second.size();
		return new KeyCharset() {

			@Override
			public int size() {
				return fs + ss;
			}

			@Override
			public char get(int val) throws IndexOutOfBoundsException {
				return val >= fs ? second.get(val - fs) : first.get(val);
			}
		};
	}

	static KeyCharset combine(KeyCharset... charsets) {
		int total = 0, indices[] = new int[charsets.length];
		for (int i = 0; i < charsets.length; indices[i] = total += charsets[i++].size())
			;
		final int t = total;

		System.out.println(Arrays.toString(indices));

		return new KeyCharset() {

			@Override
			public int size() {
				return t;
			}

			@Override
			public char get(int val) throws IndexOutOfBoundsException {

				for (int i = 0; i < indices.length; i++)
					if (val < indices[i])
						return charsets[i].get(val - (i == 0 ? 0 : indices[i - 1]));
				throw new IndexOutOfBoundsException(
						"Out of bounds for combined KeyCharset. Maximum size is: " + t + ". Provided size is: " + val);
			}
		};
	}

	static KeyCharset transform(KeyCharset other, CharToCharFunction transformer) {
		return new KeyCharset() {
			@Override
			public int size() {
				return other.size();
			}

			@Override
			public char get(int val) throws IndexOutOfBoundsException {
				return transformer.transform(other.get(val));
			}
		};
	}

	KeyCharset LOWERCASE_LETTERS = from('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
			'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'),
			UPPERCASE_LETTERS = transform(LOWERCASE_LETTERS, Character::toUpperCase),
			DIGITS = from('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'),
			LETTERS = combine(LOWERCASE_LETTERS, UPPERCASE_LETTERS),
			LETTERS_AND_NUMBERS = combine(LOWERCASE_LETTERS, UPPERCASE_LETTERS, DIGITS),
			SYMBOLS = from('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_'),
			EXTRA_SYMBOLS = from('+', '=', '[', '{', '}', ']', '\\', '|', '<', '>', '?', '/', ',', '.', ':', ';', '\'',
					'"', '~', '`'),
			EXTENDED_SYMBOL_SET = combine(SYMBOLS, EXTRA_SYMBOLS),
			ALPHANUM_AND_SYMBOLS = combine(LOWERCASE_LETTERS, UPPERCASE_LETTERS, DIGITS, SYMBOLS, EXTRA_SYMBOLS);
}
