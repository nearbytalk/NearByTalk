package org.nearbytalk.android;

public class MessageConstant {

	public static final String MESSENGER_KEY = "MESSENGER";

	public static final String COMMAND_KEY = "COMMAND";

	public static class ServiceState {
		public static final int STOPPING = 0;
		public static final int STOPPED = 1;
		public static final int STARTING = 2;
		public static final int STARTED = 3;
		public static final int FAILURE = 4;
	}

	public static class Command {
		public static final int START_SERVER = 5;
		public static final int STOP_SERVER = 6;

	}

}
