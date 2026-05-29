package com.csetutorials.vuedisk.services;

import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Log4j2
public class ThumbnailService {

	@Autowired
	FileService fileService;

	private static final int SMALL = 320;
	private static final int LARGE = 720;

	public File getThumbnail(File file, String thumbnailSize) throws IOException {
		int height = thumbnailSize.equals("small") ? SMALL : LARGE;
		Path basePath = Paths.get(fileService.getBaseDir()).toAbsolutePath().normalize();
		Path filePath = file.toPath().toAbsolutePath().normalize();
		String relativePath = basePath.relativize(filePath).toString();
		if (relativePath.startsWith(".thumbnails-")) {
			return file;
		}
		File target = basePath.resolve(".thumbnails-height-" + height).resolve(relativePath).toFile();
		if (target.exists()) {
			return target;
		}
		fileService.mkdirs(target.getParentFile());
		try {
			Thumbnails.of(file)
					.height(height)
					.outputFormat("jpg")
					.toFile(target);
		} catch (Exception e) {
			log.error("Problem while creating thumbnail of image {}", file.getAbsolutePath(), e);
			Files.deleteIfExists(target.toPath());
			Files.createSymbolicLink(target.toPath(), file.toPath());
		}
		return target;
	}
}
