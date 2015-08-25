package org.nearbytalk;

import org.eclipse.jetty.util.component.LifeCycle;
import org.nearbytalk.android.MessageConstant;
import org.nearbytalk.http.EmbeddedHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class HttpService extends Service {

	EmbeddedHttpServer server;

	private static Logger log = LoggerFactory.getLogger(HttpService.class);

	public class HttpServiceBinder extends Binder {
		public HttpService getService() {
			return HttpService.this;
		}
	}

	private static int state = MessageConstant.ServiceState.STOPPED;

	private Messenger messenger;

	// http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-in-android
	public static int getState() {
		return state;
	}

	/**
	 * jetty server listener is called the same thread as start/stop
	 */
	private LifeCycle.Listener serverListener = new LifeCycle.Listener() {

		void send(int what) {
			state = what;
			Message message = Message.obtain();
			message.what = what;

			try {
				messenger.send(message);
				return;
			} catch (RemoteException e) {
				log.error("send remote message failed:{}", e);
			}

		}

		@Override
		public void lifeCycleStopping(LifeCycle arg0) {
			send(MessageConstant.ServiceState.STOPPING);
		}

		@Override
		public void lifeCycleStopped(LifeCycle arg0) {

			state = MessageConstant.ServiceState.STOPPED;
			send(MessageConstant.ServiceState.STOPPED);
		}

		@Override
		public void lifeCycleStarting(LifeCycle arg0) {

			send(MessageConstant.ServiceState.STARTING);
		}

		@Override
		public void lifeCycleStarted(LifeCycle arg0) {

			send(MessageConstant.ServiceState.STARTED);
		}

		@Override
		public void lifeCycleFailure(LifeCycle arg0, Throwable arg1) {
			log.error("server failed: {}", arg1);

			if (!failureSent) {
				failureSent = true;
				send(MessageConstant.ServiceState.FAILURE);
			}
		}
	};

	/**
	 * atomic value to avoid duplicate failure sent to UI will be reset before
	 * next start
	 */
	private boolean failureSent = false;

	private void processStop() {

		try {
			if (server != null) {

				log.info("stopping server ...");
				server.stop();
			}

		} catch (Exception e) {

			log.error("stop server failed: {}", e);

		}

		stopSelf();
	}

	@Override
	public void onLowMemory() {
		// TODO Auto-generated method stub
		super.onLowMemory();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {

		if (state != MessageConstant.ServiceState.STOPPED
				&& state != MessageConstant.ServiceState.STOPPING) {

			// this will trigger onDestroy second time,but will get over
			// the condition
			processStop();

		}

		super.onDestroy();

	}

	private void processStart() {

		if (server == null) {
			log.debug("creating EmbeddedHttpServer...");
			server = new EmbeddedHttpServer();
			server.getJettyServer().addLifeCycleListener(serverListener);
		}
		
		failureSent=false;

		try {
			log.debug("start http server...");
			server.start();
			return;
		} catch (Exception e) {
			log.error("start http server failed: {}", e);

			Message message = Message.obtain();
			message.what = MessageConstant.ServiceState.FAILURE;
			try {
				messenger.send(message);
			} catch (RemoteException e1) {
				log.error("send error :{}", e1);
			}
		}

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (intent == null) {
			// resumed from killed by system
			processStart();
			return START_STICKY;
		}

		Bundle extras = intent.getExtras();

		int command = intent.getExtras().getInt(MessageConstant.COMMAND_KEY);

			
		messenger = (Messenger) extras.get(MessageConstant.MESSENGER_KEY);
		
		if (command == MessageConstant.Command.START_SERVER) {

			processStart();

		} else if (command == MessageConstant.Command.STOP_SERVER) {
			processStop();
		}

		return START_STICKY;

	}

}
