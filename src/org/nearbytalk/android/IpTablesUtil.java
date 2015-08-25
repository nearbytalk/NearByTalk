package org.nearbytalk.android;

import org.nearbytalk.NearByTalkApplication;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.Global.DNSInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * deal with iptables command
 * 
 * 
 * use per
 * 
 */
public class IpTablesUtil {
	private static Logger log = LoggerFactory.getLogger(IpTablesUtil.class);

	public static long HTTP_DEFAULT_PORT = 80;


	/**
	 * check if iptables forward rule running on port dnsPort if there is
	 * incomplete SHUTDOWN
	 * 
	 * @return true if is already running. false if there is
	 */
	public boolean isDNSForwardRunning(int dnsPort) {
		// TODO
		return false;
	}

	public static String CLEAN_TCP_FORWARD_COMMAND_FORMAT_STRING = "iptables -t nat -D PREROUTING  -p tcp --dport %d -j REDIRECT --to-port %d";
	public static String CLEAN_FORWARD_DROP_COMMAND_FORMAT_STRING = "iptables -P FORWARD ACCEPT";
	public static String CLEAN_UDP_FORWARD_COMMAND_FORMAT_STRING = "iptables -t nat -D PREROUTING  -p udp --dport %d -j REDIRECT --to-port %d";
	public static String FORWARD_DROP_COMMAND_FORMAT_STRING = "iptables -P FORWARD DROP";
	public static String UDP_FORWARD_COMMAND_FORMAT_STRING = "iptables -t nat -I PREROUTING  -p udp --dport %d -j REDIRECT --to-port %d";
	public static String TCP_FORWARD_COMMAND_FORMAT_STRING = "iptables -t nat -I PREROUTING  -p tcp --dport %d -j REDIRECT --to-port %d";

	/**
	 * blocking call
	 * 
	 * @param dnsPort
	 * @param httpPort
	 * @return
	 * @throws RootFailedException
	 */
	public boolean startDNSAndHttpForward(String hostIp, int dnsListenPort,
			int httpPort) throws RootFailedException {

		// assume previous port forward not confused
		stopDNSAndHttpForward();

		startDNSForward(dnsListenPort);

		startHTTPForward(httpPort);

		return true;
	}
	
	public int getState(){

		int dnsForwardPort = NearByTalkApplication.getInstance().getConfig().getDNSForwardPort();

		if(dnsForwardPort!=AndroidConfig.INVALID_PORT){
			//this is not 100% right, but enough for display information
			return MessageConstant.ServiceState.STARTED;
		}else {
			return MessageConstant.ServiceState.STOPPED;
		}

	}

	public void stopDNSAndHttpForward() throws RootFailedException {
		stopDNSForward();
		stopHTTPForward();
	}

	/**
	 * start DNS iptables port forward, and write port value to Preference . do
	 * not consider previous preference record
	 * 
	 * @param dnsListenPort
	 * @return
	 * @throws RootFailedException
	 */
	public void startDNSForward(int dnsListenPort)
			throws RootFailedException {

		// if dns can not listen on standard 53 port
		if (dnsListenPort != DNSInfo.DEFAULT_LISTEN_PORT) {
			// redirect any udp/tcp dns query to self
			// TODO check iptables available ,check device name, only
			// wifi is supported

			// write port value to sharedPreferences

			NearByTalkApplication.getInstance().getConfig().setDNSForwardPort(dnsListenPort);

			RootUtil.executeCommand(String.format(
					UDP_FORWARD_COMMAND_FORMAT_STRING,
					DNSInfo.DEFAULT_LISTEN_PORT, dnsListenPort));

			RootUtil.executeCommand(String.format(
					TCP_FORWARD_COMMAND_FORMAT_STRING,
					DNSInfo.DEFAULT_LISTEN_PORT, dnsListenPort));
		}
	}

	/**
	 * stop DNS forward if any. 
	 * if previous stop action is interrupted, then preference key will be kept
	 * (may already stopped but not writen)
	 * 
	 * we stop it again , to assume no previous port forward exists
	 * 
	 * @throws RootFailedException
	 */
	public void stopDNSForward() throws RootFailedException {

		int dnsForwardPort = NearByTalkApplication.getInstance().getConfig().getDNSForwardPort();

		if (dnsForwardPort != AndroidConfig.INVALID_PORT) {

			log.info("try to clear dns port forward {}", dnsForwardPort);

			RootUtil.executeCommand(String.format(
					CLEAN_TCP_FORWARD_COMMAND_FORMAT_STRING,
					DNSInfo.DEFAULT_LISTEN_PORT, dnsForwardPort));

			RootUtil.executeCommand(String.format(
					CLEAN_UDP_FORWARD_COMMAND_FORMAT_STRING,
					DNSInfo.DEFAULT_LISTEN_PORT, dnsForwardPort));

		}
	}

	public boolean startHTTPForward(int port) throws RootFailedException {
		// redirect any http access to self

		NearByTalkApplication.getInstance().getConfig()
				.setHTTPForwardPort(Global.HttpServerInfo.listenPort);

		RootUtil.executeCommand(String.format(
				TCP_FORWARD_COMMAND_FORMAT_STRING, HTTP_DEFAULT_PORT,
				Global.HttpServerInfo.listenPort));
	
		RootUtil.executeCommand(FORWARD_DROP_COMMAND_FORMAT_STRING);

		return true;
	}


	public void stopHTTPForward() throws RootFailedException {
		int httpForwardPort = NearByTalkApplication.getInstance().getConfig()
				.getHTTPForwardPort();

		if (httpForwardPort != AndroidConfig.INVALID_PORT) {
			log.info("try to stop http port forward ");

			RootUtil.executeCommand(String.format(
					CLEAN_TCP_FORWARD_COMMAND_FORMAT_STRING, HTTP_DEFAULT_PORT,
					httpForwardPort));
			
			RootUtil.executeCommand(CLEAN_FORWARD_DROP_COMMAND_FORMAT_STRING);

			NearByTalkApplication.getInstance().getConfig()
					.setHTTPForwardPort(AndroidConfig.INVALID_PORT);
		}
	}

}
