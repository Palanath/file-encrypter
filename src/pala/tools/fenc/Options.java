package pala.tools.fenc;

import pala.libs.generic.parsers.cli.CLIParams;

public class Options {
	private final String key;
	private final boolean decryptionMode, fastMode;

	public Options(CLIParams params) {
		key = params.readString((String) null, "-k", "--key");
		decryptionMode = params.checkFlag(false, "--dec", "--decrypt", "-d");
		fastMode = params.checkFlag(false, "--fast", "-f");
	}

	/**
	 * Specifies the key to be used for encryption or decryption.
	 * 
	 * @flag -k --key
	 * @return The key to be used for encryption or decryption.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * <code>true</code> if the files specified when the app is launched should be
	 * decrypted rather than encrypted.
	 * 
	 * @flag --deg --decrypt -d
	 * @defaultValue <code>false</code>
	 * @return Whether encryption or decryption is being performed.
	 */
	public boolean isDecryptionMode() {
		return decryptionMode;
	}

	/**
	 * Specifies whether <i>fast/unsafe mode</i> is enabled. Normally, the program
	 * copies each file, (one at a time), to a temporary directory, operates on
	 * (encrypts/decrypts) it in place, and then copies the file back. This prevents
	 * the original files from being damaged in case an error occurs while
	 * encrypting or decrypting. If fast mode is on, the original files are not
	 * copied to a temporary directory and then operated on; the original files are
	 * instead operated on in place.
	 * 
	 * @flag --fast -f
	 * @defaultValue <code>false</code>
	 * @return <code>true</code> if fast mode is enabled, <code>false</code>
	 *         otherwise.
	 */
	public boolean isFastMode() {
		return fastMode;
	}

}
