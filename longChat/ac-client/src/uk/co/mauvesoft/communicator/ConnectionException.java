package uk.co.mauvesoft.communicator;

public class ConnectionException extends Exception {
	private static final long serialVersionUID = 3211097437309791276L;

	public ConnectionException(String message) {
		super(message);
	}
	
	public ConnectionException(String message, Throwable t) {
		super(message, t);
	}
};