package com.pushtechnology.scout;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FLMStreamLogger implements Runnable {
	static private final Logger LOGGER = LoggerFactory.getLogger(FLMStreamLogger.class);

	private static final byte[] FLM_HEADER_BYTES = new byte[] {0x0A, 0x23, 0x0D, 0x2E} ;

	private final Socket socket;
	private final File file;

	public FLMStreamLogger(Socket socket, CmdOptions options, int instanceNumber) throws IOException {
		this.socket = socket;

		// Get the remote IP as dotted-quads
		final String remoteIPStr = getRemoteIP(socket).replace('.', '_');
		
		// Compose the filename, make it mildly less Windows hostile
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HHHH_mm_ss");
		final String timeStampStr = dateFormat.format(new Date());
		this.file = new File(options.getOutputDir(),String.format("%s__%s__%d.flm", remoteIPStr, timeStampStr,instanceNumber));
		
		LOGGER.info("Saving data from {} to {}\n", socket.getRemoteSocketAddress(), file);
	}

	private static String getRemoteIP(Socket socket) {
		final SocketAddress sockAddr = socket.getRemoteSocketAddress();
		if(sockAddr instanceof InetSocketAddress) {
			final InetSocketAddress inetSockAddr = (InetSocketAddress)sockAddr;
			return inetSockAddr.getAddress().toString();
		}
		return "unknown";
	}

	@Override
	public void run() {
		// Consume the data from the wire and save to disk
		OutputStream os = null;
		try {
			final BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
			
			// Check the header is correct
			bis.mark(FLM_HEADER_BYTES.length);
			try {
				if( !checkMagicNumber(bis, FLM_HEADER_BYTES) ) {
					throw new IOException("Non FLM data from "+socket.getRemoteSocketAddress());
				}
			} finally {
				bis.reset();
			}
			
			// Create the file
			os = new FileOutputStream(file);
			IOUtils.copyLarge(bis,os);
		} catch (SocketException ex) {
			LOGGER.error("Connection closed \"{}\". Saved {}", ex.getMessage(), file.getAbsolutePath());
		} catch (IOException ex) {
			LOGGER.error("Error saving data to {}", file.getAbsolutePath(), ex);
		} finally {			
			if(file.exists()) {
				final long fileSize = file.length();
				LOGGER.info("Saved {} bytes to {}", fileSize, file.getAbsolutePath());
			}
			
			IOUtils.closeQuietly(socket);
			IOUtils.closeQuietly(os);
		}
	}

	private boolean checkMagicNumber(InputStream is, byte[] headerBytes) throws IOException {
			byte[] bytes = new byte[headerBytes.length];		
			int n =  IOUtils.read(is,bytes);
			return( n == headerBytes.length && Arrays.equals(headerBytes,bytes));
	}

}
