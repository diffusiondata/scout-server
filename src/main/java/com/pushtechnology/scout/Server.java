package com.pushtechnology.scout;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

	static private final Logger LOGGER = LoggerFactory.getLogger(Server.class);

	public static void main(String[] args) {
		// Command linke options
		final CmdOptions cmdOptions = new CmdOptions();
		final CmdLineParser parser = new CmdLineParser(cmdOptions);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("java -jar scoutServer.jar [options...]");
			parser.printUsage(System.err);
			System.exit(1);
		}

		try {

			final ServerSocket serverSocket = new ServerSocket(cmdOptions.getPortNumber());
			final File outDir = cmdOptions.getOutputDir();
			LOGGER.info("Listening on port {}, saving files to \"{}\"",
					serverSocket.getLocalPort(), outDir.getAbsolutePath());
			;

			if (!outDir.exists()) {
				if (!outDir.mkdir()) {
					LOGGER.error("Cannot create directory {}", outDir);
					System.exit(1);
				} else {
					LOGGER.info("Created directory {}", outDir);
				}
			}

			// Advise on client configuration
			LOGGER.info(
					"Remember to set the configuration file on monitored hosts, for example\n{}",
					Server.composeExampleTelemetryConfig(cmdOptions));
			LOGGER.info(
					"See {} for background reading",
					"http://www.adobe.com/devnet/scout/articles/adobe-scout-getting-started.html#articlecontentAdobe_numberedheader_3");

			final ExecutorService executor = Executors.newCachedThreadPool();

			int instanceNumber = 0;
			do {
				Socket socket = serverSocket.accept();
				executor.submit(new FLMStreamLogger(socket, cmdOptions,
						instanceNumber++));
			} while (true);
		} catch (BindException ex) {
			LOGGER.error( "Cannot listen to port {}, presumably someone already is", cmdOptions.getPortNumber() );
		} catch (IOException ex) {
			LOGGER.error("", ex);
		}
	}

	private static CharSequence composeExampleTelemetryConfig(
			CmdOptions cmdOptions) {
		final String ipAddr = getIPAddresses().get(0);

		final StringBuilder result = new StringBuilder();
		result.append(String.format("TelemetryAddress=%s:%d\n", ipAddr,
				cmdOptions.getPortNumber()));
		result.append("SamplerEnabled = true\n");
		result.append("CPUCapture = true\n");
		result.append("DisplayObjectCapture = true\n");
		result.append("Stage3DCapture = true");
		return result;
	}

	/**
	 * getIPAddresses.
	 *
	 * @return a list of all reasonable IPv4 addresses this server should be
	 *         found at.
	 */
	private static List<String> getIPAddresses() {
		final List<String> result = new ArrayList<String>();
		try {
			final Enumeration<NetworkInterface> nics = NetworkInterface
					.getNetworkInterfaces();
			for (NetworkInterface nic : Collections.list(nics)) {
				// Exclude NICs that are loopback or down
				if (nic.isLoopback() || !nic.isUp() || nic.isPointToPoint()
						|| nic.isVirtual()) {
					continue;
				}

				// Get the addresses, and filter out non IPv4 addresses
				// (IPv6 will work one day)
				final Enumeration<InetAddress> addresses = nic
						.getInetAddresses();
				for (InetAddress address : Collections.list(addresses)) {
					if (address instanceof Inet4Address) {
						result.add(address.getHostAddress());
					}
				}
			}
		} catch (SocketException ex) {
			LOGGER.warn("Cannot query NICs", ex);
		}
		return result;
	}

}
