package pala.tools.fenc.logging;

import java.io.File;

/**
 * A logger that is instantiated with either a {@link PeriodicSuccessLogger} or
 * a {@link MessageLogger}. It logs to the appropriate logger depending on what
 * it was instantiated with.
 * 
 * @author Palanath
 *
 */
public class BranchLogger {

	private final PeriodicSuccessLogger periodicSuccessLogger;
	private final MessageLogger messageLogger;

	public BranchLogger(PeriodicSuccessLogger periodicSuccessLogger) {
		this.periodicSuccessLogger = periodicSuccessLogger;
		messageLogger = null;
	}

	public BranchLogger(MessageLogger messageLogger) {
		this.messageLogger = messageLogger;
		periodicSuccessLogger = null;
	}

	public void success(File file) {
		if (periodicSuccessLogger == null)
			messageLogger.success("SUCC", "Successfully processed " + file);
		else
			periodicSuccessLogger.success(file.length());
	}

	public void failure(String prefix, String message) {
		if (periodicSuccessLogger == null)
			messageLogger.failure(prefix, message);
		else
			periodicSuccessLogger.failure(prefix, message);
	}
}
