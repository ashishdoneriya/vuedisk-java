package com.csetutorials.vuedisk.services;

import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.regex.Pattern;

public class CommandLineArgumentsParser {

	private final String[] args;
	private String baseDir = "";
	private int port = 8989;
	private Options options;
	private CommandLine commands;

	public CommandLineArgumentsParser(String[] args) {
		this.args = args;
	}

	public void parseArguments() {
		if (args.length == 0) {
			return;
		}
		buildOptions();
		CommandLineParser parser = new DefaultParser();
		try {
			commands = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println("Invalid Option(s)");
			printHelp(1);
		}
		if (commands.getOptions().length == 0) {
			System.out.println("Invalid Option(s)");
			printHelp(1);
		}
		if (commands.hasOption("help")) {
			printHelp(0);
		}
		checkBaseDir();
		checkApplicationPort();
	}

	public String getBaseDir() {
		return this.baseDir;
	}

	public int getPort() {
		return this.port;
	}

	private void printHelp(int exitCode) {
		HelpFormatter helper = new HelpFormatter();
		helper.printHelp("java -jar vuedisk-0.0.1-SNAPSHOT.jar [options]", options);
		System.exit(exitCode);
	}

	private void checkBaseDir() {
		if (commands.hasOption("b")) {
			String dirPath = commands.getOptionValue("b");
			File dir = new File(dirPath);
			if (!dir.exists()) {
				System.out.println("Directory doesn't exist");
				System.exit(1);
			}
			if (!dir.isDirectory()) {
				System.out.println(dirPath + " is not a directory");
				System.exit(1);
			}
			this.baseDir = dirPath;
		}
	}

	private void checkApplicationPort() {
		if (commands.hasOption("p")) {
			String sPort = commands.getOptionValue("p");
			if (!Pattern.matches("\\d{2,5}", sPort)) {
				System.out.println("Invalid port number specified");
				System.exit(1);
			}
			int portNumber = Integer.parseInt(sPort);
			if (!isPortAvailable(portNumber)) {
				System.out.println("Port already occupied");
				System.exit(1);
			}
			this.port = portNumber;
		}
	}

	private boolean isPortAvailable(int port) {
		try (ServerSocket ignored = new ServerSocket(port)) {
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private Options buildOptions() {
		options = new Options();
		Option option = Option.builder("b")
				.argName("basedir")
				.longOpt("base-dir")
				.hasArg(true)
				.numberOfArgs(1)
				.required(false)
				.desc("Base directory path. Default base directory is the current directory")
				.build();
		options.addOption(option);
		option = Option.builder("p")
				.argName("port")
				.longOpt("port")
				.hasArg(true)
				.numberOfArgs(1)
				.required(false)
				.desc("Start the application at a different port. Default port is 8989")
				.build();
		options.addOption(option);
		option = Option.builder("h")
				.argName("help")
				.longOpt("help")
				.hasArg(false)
				.required(false)
				.desc("Print this help")
				.build();
		options.addOption(option);
		return options;
	}

}
