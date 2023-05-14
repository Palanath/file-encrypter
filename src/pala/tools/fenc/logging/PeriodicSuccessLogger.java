package pala.tools.fenc.logging;

import java.math.BigInteger;

/**
 * <h1><code>PeriodicSuccessLogger</code></h1>
 * <p>
 * A logger that prints periodic status messages to the {@link MessageLogger}
 * provided via a separate thread. All {@link #failure(String, String)} messages
 * are simply forwarded to the provided {@link #output} logger.
 * </p>
 * <p>
 * For success messages, each time a file is encrypted, decrypted, hashed, etc.,
 * {@link #success(long)} should be invoked exactly once with the number of
 * bytes written out during the operation. This class keeps track of a number of
 * files processed and increments it each time {@link #success(long)} is
 * invoked. Additionally, this class keeps track of the number of bytes written
 * out during processing (as a sum of all the bytes provided to
 * {@link #success(long)}).
 * </p>
 * <h2>Printing Thread</h2>
 * <p>
 * Whenever {@link #success(long)} is invoked, a printing thread is started. The
 * printing thread completes the following actions in a loop and is synchronized
 * against {@link #success(long)}:
 * </p>
 * <ol>
 * <li>Wait {@link #millisDelay} milliseconds,</li>
 * <li>grab the current number of {@link #successes} and number of
 * {@link #bytesHandled} since the last print and cache the values,</li>
 * <li>set the global number of {@link #successes} and {@link #bytesHandled} to
 * <code>0</code>, and finally,</li>
 * <li>print out a status message (with the prefix "[STAT]: ") denoting how many
 * files and bytes were successfully handled.</li>
 * </ol>
 * <p>
 * When the thread detects that there are no more successes to report, it
 * automatically shuts down. Any call to {@link #success(long)} will start it
 * back up again and any pending {@link #successes} are guaranteed to be handled
 * before it is shut down.
 * </p>
 * 
 * @author Palanath
 *
 */
public class PeriodicSuccessLogger {

	private MessageLogger output;

	public PeriodicSuccessLogger(MessageLogger output, int millisDelay) {
		this.output = output;
		this.millisDelay = millisDelay;
	}

	public PeriodicSuccessLogger(MessageLogger output) {
		this.output = output;
	}

	public void setOutput(MessageLogger output) {
		this.output = output;
	}

	public MessageLogger getOutput() {
		return output;
	}

	public int getMillisDelay() {
		return millisDelay;
	}

	public void setMillisDelay(int millisDelay) {
		this.millisDelay = millisDelay;
	}

	private int millisDelay = 2500;
	private volatile int successes;
	private volatile BigInteger bytesHandled = BigInteger.ZERO;

	private volatile Thread t;

	private final Object tmonitor = new Object();

	private synchronized void createThread() {
		if (t == null) {
			(t = new Thread(() -> {
				while (successes != 0) {
					try {
						Thread.sleep(millisDelay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					int successCount;
					BigInteger bytesHandled;
					synchronized (tmonitor) {
						successCount = successes;
						successes = 0;
						bytesHandled = PeriodicSuccessLogger.this.bytesHandled;
						PeriodicSuccessLogger.this.bytesHandled = BigInteger.ZERO;
					}
					output.success("STAT",
							"Processed " + successCount + " files and wrote " + bytesHandled + " bytes.");
				}
				t = null;
			})).start();
		}
	}

	public void success(long encryptedBytesOutput) {
		synchronized (tmonitor) {
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
