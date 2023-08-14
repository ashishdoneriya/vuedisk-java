package com.csetutorials.vuedisk.controllers;

import com.csetutorials.vuedisk.beans.FormParams;
import com.csetutorials.vuedisk.beans.SizeSse;
import com.csetutorials.vuedisk.services.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AsyncMethods {

	@Autowired
	FileService fileService;

	@Async
	public void monitorDirSizeCalculator(SseEmitter emitter, SizeSse sizeSse) {
		try {
			while (!sizeSse.isFinished() && !sizeSse.isStopped()) {
				Thread.sleep(1000);
				SseEmitter.SseEventBuilder event = SseEmitter.event()
						.data("Size : " + fileService.getSizeInString(sizeSse.getSizeInBytes()) + ", Items : " + sizeSse.getItems());
				emitter.send(event);
			}
			if (!sizeSse.isStopped()) {
				SseEmitter.SseEventBuilder event = SseEmitter.event()
						.data("Size : " + fileService.getSizeInString(sizeSse.getSizeInBytes()) + ", Items : " + sizeSse.getItems());
				emitter.send(event);
				emitter.complete();
			}
		} catch (InterruptedException e) {
			sizeSse.setStopped(true);
			emitter.complete();
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			sizeSse.setStopped(true);
			emitter.complete();
		}
	}

}
