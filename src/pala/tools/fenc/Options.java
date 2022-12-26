package pala.tools.fenc;

import pala.libs.generic.parsers.cli.CLIParams;

public class Options {
	private final String key;
	private final boolean decryptionMode;

	public Options(CLIParams params) {
		key = params.readString((String) null, "-k", "--key");
		decryptionMode = params.checkFlag(false, "--dec", "--decrypt", "-d");
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

}
