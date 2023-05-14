package pala.tools.fenc.processing;

import static pala.tools.fenc.processing.EncryptionProcessor.HASH_STRING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import pala.libs.generic.util.Hashing;
import pala.tools.fenc.logging.BranchLogger;
import pala.tools.fenc.logging.MessageLogger;
import pala.tools.fenc.logging.PeriodicSuccessLogger;

public class CipherProcessor implements DirectoryProcessor {

	protected interface Operator {
		void operate(File f, File dest, int bufferSize, byte[] hdr, byte... key)
				throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
				InvalidAlgorithmParameterException, FileProcessingException;
	}

	private final byte[] keyHash, fileHeader;
	private final int bufferSize;
	private final BranchLogger logger;

	protected CipherProcessor(String operation, Operator operator, String key, int bufferSize, MessageLogger logger) {
		this(operation, operator, key, bufferSize, new BranchLogger(logger));
	}

	protected CipherProcessor(String operation, Operator operator, String key, int bufferSize,
			PeriodicSuccessLogger logger) {
		this(operation, operator, key, bufferSize, new BranchLogger(logger));
	}

	protected CipherProcessor(String operation, Operator operator, String key, int bufferSize, BranchLogger logger) {
		keyHash = Hashing.sha256(key);
		fileHeader = Hashing.sha256(HASH_STRING + key + HASH_STRING);
		this.bufferSize = bufferSize;
		this.logger = logger;
		this.operation = operation;
		this.operator = operator;
	}

	/**
	 * Simple string used to print "encrypt" or "decrypt" in output messages.
	 */
	private final String operation;
	private final Operator operator;

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
								+ ", would get " + operation + "ed then written to. (The file was NOT " + operation
								+ "ed.) [Err msg: " + e.getLocalizedMessage() + ']');
				return;
			}
			temp.deleteOnExit();

			try {
				operator.operate(temp, f, bufferSize, fileHeader, keyHash);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
					| InvalidAlgorithmParameterException e) {
				logger.failure("EFL", "Failed to initialize the " + operation + "ion algorithm while processing file: "
						+ f + ". [Err msg: " + e.getLocalizedMessage() + ']');
				return;
			} catch (IOException e) {
				logger.failure("IOEX", "Encountered a file in-out exception while trying to read or write and "
						+ operation + " the file " + f + ". [Err msg: " + e.getLocalizedMessage() + ']');
				return;
			} catch (FileProcessingException e) {
				logger.failure("ENEX",
						"Encountered a" + (operation.startsWith("e") ? "n" : "") + ' ' + operation
								+ "ion failure while trying to " + operation + " the file " + f + ". [Err msg: "
								+ e.getLocalizedMessage() + ']');
				return;
			}

			try {
				Files.copy(temp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				logger.failure("TMPC", "A" + (operation.startsWith("e") ? "n" : "") + ' ' + operation + "ed copy of "
						+ f + " was written to a temporary file (" + temp
						+ ") but an issue occurred when trying to copy that temporary file back over to the source file's location. [Err msg: "
						+ e.getLocalizedMessage() + ']');
			}
			temp.delete();

		} catch (Exception e) {
			logger.failure("UNKN", "An unknown failure occurred while processing " + f
					+ ". The file may or may not have been " + operation + "ed, but should not be garbage.");
			return;
		}
		logger.success(f);
	}
}
