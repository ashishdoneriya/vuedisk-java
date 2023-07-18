package com.csetutorials.vuedisk;

import com.csetutorials.vuedisk.services.CommandLineArgumentsParser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VueDiskApplication {

	public static void main(String[] args) {
		CommandLineArgumentsParser parser = new CommandLineArgumentsParser(args);
		parser.parseArguments();
		SpringApplication app = new SpringApplication(VueDiskApplication.class);
		app.setDefaultProperties(parser.getProperties());
		app.run(args);
	}

}
