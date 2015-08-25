package org.nearbytalk.android;

import java.net.Inet4Address;

public class ServerState

{
	public static enum ScanState {
		SCANNING, FOUND, NOT_FOUND
	}

	public ServerState(ScanState scanState, Inet4Address address) {
		this.scanState = scanState;
		this.address = address;
	}

	public ScanState scanState;

	public Inet4Address address;

	@Override
	public String toString() {
		return address.toString() + ":" + scanState.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerState other = (ServerState) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		return true;
	}

}
