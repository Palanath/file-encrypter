package pala.tools.fenc.logging;

import java.math.BigInteger;

/**
 * <p>
 * A logger that prints periodic status messages to the {@link MessageLogger}
 * provided via a separate thread.
 * </p>
 * 
 * @author Palanath
 *
 */
public class PeriodicSuccessLogger {

	private MessageLogger output;

	public PeriodicSuccessLogger(MessageLogger output) {
		this.output = output;
	}

	public void setOutput(MessageLogger output) {
		this.output = output;
	}

	public MessageLogger getOutput() {
		return output;
	}

	private volatile int successes;
	private volatile BigInteger bytesHandled = BigInteger.ZERO;

	private volatile Thread t;

	private synchronized void createThread() {
		if (t == null) {
			t = new Thread(() -> {
				while (successes != 0) {
					int successCount;
					BigInteger bytesHandled;
					synchronized (t) {
						successCount = successes;
						successes = 0;
						bytesHandled = PeriodicSuccessLogger.this.bytesHandled;
						PeriodicSuccessLogger.this.bytesHandled = BigInteger.ZERO;
					}
					output.success("STAT",
							"Encrypted " + successCount + " files and wrote " + bytesHandled + " bytes.");
				}
			});
		}
	}

	public void success(long encryptedBytesOutput) {
		synchronized (t) {
			successes++;
			bytesHandled = bytesHandled.add(BigInteger.valueOf(encryptedBytesOutput));
		}
		if (t == null)
			createThread();
	}

	public void failure(String prefix, String message) {
		output.failure(prefix, message);
	}

}
