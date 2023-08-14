package com.csetutorials.vuedisk.services;

import com.mortennobel.imagescaling.ResampleOp;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;

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
		// Create an InputStream object from the File object.
		InputStream inputStream = new FileInputStream(file);

		// Create an Image object by passing the InputStream object to the constructor.
		Image image = new Image(inputStream);
		BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
		try (InputStream compressedStream = compress(bufferedImage, (double) height)) {

			// Preserve image orientation
			TiffOutputSet outputSet = null;
			ImageMetadata metadata = Imaging.getMetadata(file);
			if (metadata instanceof JpegImageMetadata) {
				TiffImageMetadata exif = ((JpegImageMetadata) metadata).getExif();
				if (exif != null) {
					outputSet = exif.getOutputSet();
				}
			}

			// Write the processed image to target file with preserved orientation
			try (OutputStream os = new FileOutputStream(target)) {
				if (outputSet != null) {
					new ExifRewriter().updateExifMetadataLossless(compressedStream, os, outputSet);
				} else {
					byte[] buffer = new byte[1024];
					int length;
					while ((length = compressedStream.read(buffer)) > 0) {
						os.write(buffer, 0, length);
					}
				}
			}
		} catch (Exception e) {
			log.error("Problem while creating thumbnail of image {}", file.getAbsolutePath(), e);
			Files.createSymbolicLink(target.toPath(), file.toPath());
		}
		return target;
	}

	public InputStream compress(BufferedImage bufferedImage, Double targetHeight) throws IOException {
		var outputStream = new ByteArrayOutputStream();

		double originalWidth = bufferedImage.getWidth();
		double originalHeight = bufferedImage.getHeight();
		double targetWidth = (originalWidth * targetHeight) / originalHeight;
		ResampleOp resizeOp = new ResampleOp((int) targetWidth, targetHeight.intValue());
		BufferedImage resizedImage = resizeOp.filter(bufferedImage, null);
		ImageIO.write(resizedImage, "jpg", outputStream);
		return new ByteArrayInputStream(outputStream.toByteArray());
	}
}
