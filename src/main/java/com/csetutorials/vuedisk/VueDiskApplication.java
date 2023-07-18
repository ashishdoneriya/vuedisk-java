package com.csetutorials.vuedisk;

import com.csetutorials.vuedisk.services.CommandLineArgumentsParser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;

@SpringBootApplication
public class VueDiskApplication {

	private static CommandLineArgumentsParser parser;

	public static void main(String[] args) {
		parser = new CommandLineArgumentsParser(args);
		parser.parseArguments();
		SpringApplication app = new SpringApplication(VueDiskApplication.class);
		app.setDefaultProperties(Collections.singletonMap("server.port", parser.getPort()));
		app.run(args);
	}

	public static String getBaseDir() {
		return parser.getBaseDir();
	}

}
