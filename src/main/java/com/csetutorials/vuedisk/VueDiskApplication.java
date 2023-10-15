package com.csetutorials.vuedisk;

import com.csetutorials.vuedisk.services.CommandLineArgumentsParser;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

@SpringBootApplication
@Log4j2
@EnableAsync
public class VueDiskApplication {

	public static void main(String[] args) {
		CommandLineArgumentsParser parser = new CommandLineArgumentsParser(args);
		parser.parseArguments();
		SpringApplication app = new SpringApplication(VueDiskApplication.class);
		app.setDefaultProperties(parser.getProperties());
		app.run(args);
	}

}
