package pala.tools.fenc;

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
	/**
	 * The index of the largest key character in the set. This is the maximum value
	 * that can be provided to {@link #get(int)}.
	 * 
	 * @return
	 */
	int max();

	/**
	 * Returns the character indexed by the specified value or throws an
	 * {@link IllegalArgumentException} if the index is out of bounds.
	 * 
	 * @param val The index of the character to retrieve.
	 * @return
	 * @throws IllegalArgumentException
	 */
	char get(int val) throws IllegalArgumentException;
}
