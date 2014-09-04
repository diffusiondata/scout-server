package com.pushtechnology.scout;

import java.io.File;

import org.kohsuke.args4j.Option;

public class CmdOptions {

	public File getOutputDir() {
		return outputDir;
	}

	public int getPortNumber() {
		return portNumber;
	}

	private static final int SCOUT_PORT_NUMBER = 7934;

	@Option(name="-d", usage="Optional output directory, defaults to current directory")
	private File outputDir = new File(".");
	
	@Option(name="-p", usage="server port number, defaults to " + SCOUT_PORT_NUMBER)
	private int portNumber = SCOUT_PORT_NUMBER;
}
