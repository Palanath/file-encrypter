package pala.tools.fenc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import pala.libs.generic.JavaTools;
import pala.libs.generic.parsers.cli.CLIParams;
import pala.libs.generic.strings.StringTools;
import pala.libs.generic.util.Hashing;

public class FileEncrypter {

	private static final String HASH_STRING = "Encrypted by FEnc.";

	/**
	 * <p>
	 * Encrypts files and directories specified by arguments. Each file is processed
	 * separately, even if a directory is specified and files are contained in that
	 * directory.
	 * </p>
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Example program invocation (for my own use, so that I can write the readme):
		// java -jar fenc.jar -k="Some key" --dec -f F:/some/folder/on/f/drive
		// /some/file/on/current/drive.txt some/relative/folder/
		CLIParams flags = new CLIParams(args);
		Options options = new Options(flags);

		List<String> specifiedFilePaths = flags.getUnnamed();
		List<File> files = JavaTools.addAll(specifiedFilePaths, File::new, new ArrayList<>(specifiedFilePaths.size()));
		if (options.isHashMode())
			JavaTools.walktree(a -> {
				if (a.isFile())
					try {
						System.out.println('['
								+ StringTools.toHexString(
										a.length() > options.getBufferSize() ? hashFile(a, options.getBufferSize())
												: Hashing.sha256(Files.readAllBytes(a.toPath())))
								+ "] - " + a.getAbsolutePath());
					} catch (IOException e) {
						System.err.println("[HFL](" + a.getAbsolutePath() + ") Failed to hash: " + a + '.');
					}
			}, files);
		else {
			byte[] hash = Hashing.sha256(options.getKey()),
					header = Hashing.sha256(HASH_STRING + options.getKey() + HASH_STRING);
			for (File f : files)
				try {
					process(f, options.isDecryptionMode(), options.getBufferSize(), options.isSuppressSuccessMessages(),
							header, hash);
				} catch (Exception e) {
					System.err.println("Exception processing: " + f);
					e.printStackTrace();
				}
		}
	}

	public static byte[] hashFile(File f, int bufferSize) throws IOException {
		try (FileInputStream fis = new FileInputStream(f)) {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			byte[] buff = new byte[bufferSize];
			int c;
			while ((c = fis.read(buff)) != -1)
				sha.update(buff, 0, c);
			return sha.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(
					"SHA-256 implementation not supported on this Java system; hashing could not be performed.");
		}
	}

	public static void process(File f, boolean decryptionMode, int bufferSize, boolean suppressSuccessMessages,
			byte[] header, byte... key) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
		if (f.isFile()) {
			processFile(f, decryptionMode, bufferSize, header, key);
			if (!suppressSuccessMessages)
				System.out.println((decryptionMode ? "[DSUC](" + f.getAbsolutePath() + " Decrypted file " + f
						: "[ESUC](" + f.getAbsolutePath() + " Eecrypted file " + f) + ')');
		} else if (f.isDirectory())
			try {
				for (File i : f.listFiles())
					try {
						process(i, decryptionMode, bufferSize, suppressSuccessMessages, header, key);
					} catch (Exception e) {
						System.err
								.println("[FAIL](" + f.getAbsolutePath() + ") Failed to process the file: " + f + '.');
						e.printStackTrace();
					}
			} catch (Exception e) {
				System.err.println("Failed to iterate over files in " + f);
				e.printStackTrace();
			}
	}

	private static void processFile(File f, boolean decryptionMode, int bufferSize, byte[] header, byte... key)
			throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
		// Expects f.isFile() to return true.

		if (f.length() == 0)
			return;

		// Create a temp file as the destination for the encryption/decryption.
		File temp = File.createTempFile("enc", null);

		try {
			if (decryptionMode)
				decryptFile(f, temp, bufferSize, header, key);
			else
				encryptFile(f, temp, bufferSize, header, key);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		Files.copy(temp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
		temp.delete();
		temp.deleteOnExit();
	}

	/**
	 * Reads bytes from the specified {@link File}, <code>f</code>, encrypts them
	 * using the specified key and AES, then outputs the result into the destination
	 * file. The encryption result contains the initialization vector (16 bytes)
	 * followed immediately by the ciphertext.
	 * 
	 * @param f    The source file to read bytes from.
	 * @param dest The destination file to output the encryption result to.
	 * @param key  The key used for the encryption.
	 * @throws NoSuchAlgorithmException           If the underlying {@link Cipher}
	 *                                            object instantiation throws a
	 *                                            {@link NoSuchAlgorithmException}.
	 * @throws NoSuchPaddingException             If the underlying {@link Cipher}
	 *                                            object instantiation throws a
	 *                                            {@link NoSuchPaddingException}.
	 * @throws InvalidKeyException                If the specified key is not valid
	 *                                            for AES.
	 * @throws InvalidAlgorithmParameterException If an error in the underlying
	 *                                            cryptography implementation
	 *                                            occurs, causing it to reject the
	 *                                            initialization vector provided, or
	 *                                            otherwise reject the
	 *                                            {@link Cipher} object
	 *                                            initialization parameterization.
	 * @throws IOException                        If an {@link IOException} occurs
	 *                                            while reading/writing to the
	 *                                            filesystem.
	 */
	private static void encryptFile(File f, File dest, int bufferSize, byte[] header, byte... key)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IOException {
		try (FileInputStream fis = new FileInputStream(f)) {
			byte[] headerbf = new byte[header.length];
			int amt = 0;
			CHECK_ENCRYPTED: {
				while (amt < headerbf.length) {
					int readcnt = fis.read(headerbf, amt, headerbf.length - amt);
					if (readcnt == -1)
						break CHECK_ENCRYPTED;
					else
						amt += readcnt;
				}

				// We get here when enough bytes were read to comprise the "already encrypted"
				// header. Do a comparison.
				if (Arrays.equals(headerbf, header)) {
					// File already encrypted. Throw err:
					throw new IllegalArgumentException("[AENC](" + f.getAbsolutePath() + ") Detected that file " + f
							+ " is already encrypted. Skipping...");
					// Include "[AENC]" followed by the file before the message to make it easy for
					// log scanners to grab the data. AENC is short for "already encrypted."

				} // If the header is not present, we need to encrypt the bytes we read.
			}

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			SecureRandom ran = new SecureRandom();
			byte[] iv = new byte[16];
			ran.nextBytes(iv);
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

			try (FileOutputStream fileOutputStream = new FileOutputStream(dest)) {
				fileOutputStream.write(header);
				fileOutputStream.write(iv);
				try (CipherOutputStream cos = new CipherOutputStream(fileOutputStream, cipher)) {
					// Encrypt already scanned bytes.
					cos.write(headerbf, 0, amt);
					byte buff[] = new byte[bufferSize];
					while ((amt = fis.read(buff)) != -1)
						cos.write(buff, 0, amt);
				}
			}
		}
	}

	private static void decryptFile(File f, File dest, int bufferSize, byte[] hdr, byte... key) throws IOException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		try (FileInputStream fis = new FileInputStream(f)) {
			byte[] header = new byte[16 + hdr.length];
			int amt = 0;
			while (amt < header.length) {
				int readcnt = fis.read(header, amt, header.length - amt);
				if (readcnt == -1)
					if (amt < header.length)
						throw new IllegalArgumentException("[NENC](" + f.getAbsolutePath()
								+ ") Detected a file that was not encrypted. The file does not have enough bytes (80) to contain a header. Every file encrypted by this program has an 80 byte header (64 bytes containing a unique \"encrypted-by-fenc\" hash string, and 16 containing the initialization vector needed for decryption). This file is not even 80 bytes long and so cannot have been encrypted by this program.");
					else
						break;
				amt += readcnt;
			}

			byte[] iv = Arrays.copyOfRange(header, hdr.length, 16 + hdr.length);
			for (int i = 0; i < hdr.length; i++)
				if (header[i] != hdr[i])
					throw new IllegalArgumentException("[NENC](" + f.getAbsolutePath() + ") Detected a file, " + f
							+ ", that was not encrypted. The file's header does not match the form of the header written to files encrypted with this program. Skipping decryption attempt of this file... ");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
			try (CipherInputStream cis = new CipherInputStream(fis, cipher)) {
				byte[] buff = new byte[bufferSize];
				try (FileOutputStream fos = new FileOutputStream(dest)) {
					while ((amt = cis.read(buff)) != -1)
						fos.write(buff, 0, amt);
				}
			}
		}
	}

}
