package pala.tools.fenc.processing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import pala.libs.generic.strings.StringTools;
import pala.libs.generic.util.Hashing;
import pala.tools.fenc.logging.MessageLogger;

public class HashProcessor implements DirectoryProcessor {

	private final MessageLogger logger;
	private final int bufferSize;

	public HashProcessor(MessageLogger logger, int bufferSize) {
		this.logger = logger;
		this.bufferSize = bufferSize;
	}

	@Override
	public void processFile(File a) {
		try {
			logger.success("SUCC", '[' + StringTools.toHexString(
					a.length() > bufferSize ? hashFile(a, bufferSize) : Hashing.sha256(Files.readAllBytes(a.toPath())))
					+ "] - " + a.getAbsolutePath());
		} catch (IOException e) {
			logger.failure("FAIL", "Failure hashing " + a + ". [Err msg: " + e.getLocalizedMessage() + ']');
		} catch (FileProcessingException e) {
			logger.failure("ALGF", e.getLocalizedMessage() + " File: " + a);
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
			throw new FileProcessingException(
					"SHA-256 implementation not supported on this Java system; hashing could not be performed.");
		}
	}

}
