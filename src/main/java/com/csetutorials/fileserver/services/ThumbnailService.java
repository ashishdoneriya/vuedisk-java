package com.csetutorials.fileserver.services;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.format.Format;
import com.sksamuel.scrimage.format.FormatDetector;
import com.sksamuel.scrimage.nio.*;
import com.sksamuel.scrimage.webp.WebpWriter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Service
@Log4j2
public class ThumbnailService {

	@Autowired
	FileService fileService;

	private static final int SMALL = 320;
	private static final int LARGE = 720;

	public File getThumbnail(File file, String thumbnailSize) throws IOException {
		int height = thumbnailSize.equals("small") ? SMALL : LARGE;
		String relativePath = file.getAbsolutePath().substring(fileService.getBaseDir().length() + 1);
		if (relativePath.startsWith(".thumbnails-")) {
			return file;
		}
		File target = new File(fileService.getBaseDir() + File.separator + ".thumbnails-height-" + height + File.separator + relativePath);
		if (target.exists()) {
			return target;
		}
		fileService.mkdirs(target.getParentFile());
		try {
			ImmutableImage image = ImmutableImage.loader().fromFile(file);
			if (image.height <= height) {
				Files.createSymbolicLink(target.toPath(), file.toPath());
				return target;
			}
			if (file.getName().endsWith(".bmp") || file.getName().endsWith(".BMP")) {
				image.scaleToHeight(height).output(new BmpWriter(), target);
			} else if (file.getName().endsWith(".pcx") || file.getName().endsWith(".PCX")) {
				image.scaleToHeight(height).output(new PcxWriter(), target);
			}
			Optional<Format> optional = FormatDetector.detect(Files.newInputStream(file.toPath()));

			if (!optional.isPresent()) {
				Files.createSymbolicLink(target.toPath(), file.toPath());
				return target;
			}

			Format format = optional.get();

			switch (format) {
				case GIF:
					image.scaleToHeight(height).output(GifWriter.Progressive, target);
					break;
				case JPEG:
					image.scaleToHeight(height).output(JpegWriter.Default, target);
					break;
				case WEBP:
					image.scaleToHeight(height).output(WebpWriter.DEFAULT, target);
					break;
				case PNG:
					image.scaleToHeight(height).output(PngWriter.MaxCompression, target);
					break;
			}
			return target;
		} catch (Exception e) {
			log.error("Problem while creating thumbnail of image {}", file.getAbsolutePath(), e);
			Files.createSymbolicLink(target.toPath(), file.toPath());
			return target;
		}
	}
}
