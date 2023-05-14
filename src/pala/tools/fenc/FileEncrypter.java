package pala.tools.fenc;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;

import pala.libs.generic.JavaTools;
import pala.libs.generic.parsers.cli.CLIParams;
import pala.tools.fenc.logging.MessageLogger;
import pala.tools.fenc.logging.PeriodicSuccessLogger;
import pala.tools.fenc.processing.CipherProcessor;
import pala.tools.fenc.processing.HashProcessor;

public class FileEncrypter {

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

		if (options.isKeygenMode())
			genkeys(options);
		else
			(options.isHashMode()
					? new HashProcessor(options.isSuppressSuccessMessages() ? MessageLogger.suppressSuccess()
							: MessageLogger.simpleLogger(), options.getBufferSize())
					: options.isNotifyCycleEnabled()
							? CipherProcessor.create(options.isEncryptionMode(), options.getKey(),
									options.getBufferSize(),
									new PeriodicSuccessLogger(MessageLogger.simpleLogger(),
											options.getNotificationCycleTime()))
							: CipherProcessor.create(options.isEncryptionMode(), options.getKey(),
									options.getBufferSize(),
									options.isSuppressSuccessMessages() ? MessageLogger.suppressSuccess()
											: MessageLogger.simpleLogger()))
					.process(JavaTools.addAll(flags.getUnnamed(), File::new,
							new ArrayList<>(flags.getUnnamed().size())));
	}

	public static void genkeys(Options options) {
		SecureRandom sr;
		try {
			sr = SecureRandom.getInstanceStrong();
		} catch (NoSuchAlgorithmException e) {
			System.err.println(
					"This Java implementation is non-compliant and does not have a strong secure random number generator algorithm. Using a plain secure random number generator instead.");
			sr = new SecureRandom();
		}
		char[] x = new char[options.getKeygenSize()];
		for (int i = 0; i < x.length; i++)
			x[i] = options.getKeyCharset().get(sr.nextInt(options.getKeyCharset().size()));
		System.out.println(new String(x));
	}

}
