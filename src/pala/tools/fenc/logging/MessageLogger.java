package pala.tools.fenc.logging;

/**
 * An API interface that takes string inputs and prints them, formatted, to a
 * certain output. An instance of this type is used as an intermediary between
 * an actual output (like the command line) and another type of logger that
 * formats different data (such as "success objects") into string messages, such
 * as {@link PeriodicSuccessLogger}.
 * 
 * @author Palanath
 *
 */
public interface MessageLogger {
	/**
	 * <p>
	 * Used to print a message on success. The prefix is prepended to the message in
	 * the format:
	 * </p>
	 * 
	 * <pre>
	 * <code>[prefix]: message</code>
	 * </pre>
	 * 
	 * <p>
	 * and the result is usually sent to standard out unless suppressed or
	 * configured otherwise by the caller.
	 * </p>
	 * 
	 * @param prefix  The prefix for the message, used by log-parsing utils to
	 *                easily identify the message.
	 * @param message The message itself.
	 */
	void success(String prefix, String message);

	/**
	 * <p>
	 * Used to print a message on failure. The prefix is prepended to the message in
	 * the format:
	 * </p>
	 * 
	 * <pre>
	 * <code>[prefix]: message</code>
	 * </pre>
	 * 
	 * <p>
	 * and the result is usually sent to standard error, usually immediately even if
	 * suppression is enabled.
	 * </p>
	 * 
	 * @param prefix  The prefix for the message, used by log-parsing utils to
	 *                easily identify the message.
	 * @param message The message itself.
	 */
	void failure(String prefix, String message);

	class SimpleMessageLogger implements MessageLogger {
		@Override
		public void success(String prefix, String message) {
			System.out.println('[' + prefix + "]: " + message);
		}

		@Override
		public void failure(String prefix, String message) {
			System.err.println('[' + prefix + "]: " + message);
		}
	}

	class FailuresOnlyMessageLogger extends SimpleMessageLogger {
		@Override
		public void success(String prefix, String message) {
		}
	}

	static SimpleMessageLogger simpleLogger() {
		return new SimpleMessageLogger();
	}

	static FailuresOnlyMessageLogger suppressSuccess() {
		return new FailuresOnlyMessageLogger();
	}
}
