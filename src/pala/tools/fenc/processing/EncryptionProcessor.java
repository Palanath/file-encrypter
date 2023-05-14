package pala.tools.fenc.processing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import pala.libs.generic.util.Hashing;
import pala.tools.fenc.logging.BranchLogger;
import pala.tools.fenc.logging.MessageLogger;
import pala.tools.fenc.logging.PeriodicSuccessLogger;

/**
 * <p>
 * A {@link DirectoryProcessor} that encrypts the files its given.
 * 
 * @author Palanath
 *
 */
public class EncryptionProcessor implements DirectoryProcessor {

	public static final String HASH_STRING = "Encrypted by FEnc.";

	private final byte[] keyHash, fileHeader;
	private final int bufferSize;

	private final BranchLogger logger;

	public EncryptionProcessor(String key, int bufferSize, MessageLogger logger) {
		this(key, bufferSize, new BranchLogger(logger));
	}

	public EncryptionProcessor(String key, int bufferSize, PeriodicSuccessLogger logger) {
		this(key, bufferSize, new BranchLogger(logger));
	}

	private EncryptionProcessor(String key, int bufferSize, BranchLogger logger) {
		keyHash = Hashing.sha256(key);
		fileHeader = Hashing.sha256(HASH_STRING + key + HASH_STRING);
		this.bufferSize = bufferSize;
		this.logger = logger;
	}

	@Override
	public void processFile(File f) {

		try {
			if (f.length() == 0)
				return;
			File temp;
			try {
				// Create a temp file as the destination for the encryption/decryption.
				temp = File.createTempFile("enc", null);
			} catch (IOException e) {
				logger.failure("TMPF",
						"Failed to create the temporary file (for intermediary processing) that file, " + f
								+ ", would get encrypted then written to. (The file was NOT encrypted.) [Err msg: "
								+ e.getLocalizedMessage() + ']');
				return;
			}
			temp.deleteOnExit();

			try {
				encryptFile(temp, f, bufferSize, fileHeader, keyHash);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
					| InvalidAlgorithmParameterException e) {
				logger.failure("EFL", "Failed to initialize the encryption algorithm while processing file: " + f
						+ ". [Err msg: " + e.getLocalizedMessage() + ']');
				return;
			} catch (IOException e) {
				logger.failure("IOEX",
						"Encountered a file in-out exception while trying to read or write and encrypt the file " + f
								+ ". [Err msg: " + e.getLocalizedMessage() + ']');
				return;
			} catch (FileProcessingException e) {
				logger.failure("ENEX", "Encountered an encryption failure while trying to encrypt the file " + f
						+ ". [Err msg: " + e.getLocalizedMessage() + ']');
				return;
			}

			try {
				Files.copy(temp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				logger.failure("TMPC", "An encrypted copy of " + f + " was written to a temporary file (" + temp
						+ ") but an issue occurred when trying to copy that temporary file back over to the source file's location. [Err msg: "
						+ e.getLocalizedMessage() + ']');
			}
			temp.delete();

		} catch (Exception e) {
			logger.failure("UNKN", "An unknown failure occurred while processing " + f
					+ ". The file may or may not have been encrypted, but should not be garbage.");
		}
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
	 * @throws FileProcessingException            if the file is already encrypted.
	 */
	private static void encryptFile(File f, File dest, int bufferSize, byte[] header, byte... key)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IOException, FileProcessingException {
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
					throw new FileProcessingException("Detected that file is already encrypted. Skipping...");
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

}
