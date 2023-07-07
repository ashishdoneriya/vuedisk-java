package com.csetutorials.fileserver.controllers;

import com.csetutorials.fileserver.beans.FilesListObj;
import com.csetutorials.fileserver.beans.FormParams;
import com.csetutorials.fileserver.services.FileService;
import com.csetutorials.fileserver.services.ThumbnailService;
import com.csetutorials.fileserver.services.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("apis")
public class FileController {

	@Autowired
	FileService fileService;

	@Autowired
	ThumbnailService thumbnailService;

	@Autowired
	UploadService uploadService;

	@GetMapping("list")
	public List<FilesListObj> list(@RequestParam("path") String dirPath) {
		return fileService.list(fileService.parsePath(dirPath));
	}

	@PostMapping("copy")
	public String copy(@RequestBody FormParams params) {
		fileService.copy(fileService.parsePath(params.getSourceDir()),
				fileService.parsePath(params.getDestinationDir()), params.getFiles());
		return "success";
	}

	@PostMapping("cut")
	public String cut(@RequestBody FormParams params) {
		fileService.cut(fileService.parsePath(params.getSourceDir()),
				fileService.parsePath(params.getDestinationDir()), params.getFiles());
		return "success";
	}

	@PostMapping("delete")
	public String delete(@RequestBody FormParams params) {
		fileService.delete(fileService.parsePath(params.getSourceDir()), params.getFiles());
		return "success";
	}

	@PostMapping("size")
	public String size(@RequestBody FormParams params) {
		return fileService.size(fileService.parsePath(params.getSourceDir()), params.getFiles());
	}

	@PostMapping("rename")
	public String rename(@RequestBody FormParams params) {
		fileService.rename(fileService.parsePath(params.getSourceDir()), params.getOldName(), params.getNewName());
		return "success";
	}

	@PostMapping("create-dir")
	public String createDirectory(@RequestBody FormParams params) {
		fileService.createDir(fileService.parsePath(params.getSourceDir()), params.getNewName());
		return "success";
	}

	@PostMapping("remote-upload")
	public String remoteUpload(@RequestBody FormParams params) {
		fileService.remoteUpload(fileService.parsePath(params.getSourceDir()), params.getUrl(), params.getNewName());
		return "success";
	}

	@GetMapping("/download")
	public ResponseEntity<Resource> download(@RequestParam("path") String path) {
		try {
			File file = fileService.parsePath(path);
			if (!file.exists()) {
				return ResponseEntity.notFound().build();
			}
			Resource resource = new UrlResource(file.toPath().toUri());
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.setContentDispositionFormData("attachment", file.getName());
			headers.setContentLength(file.length());
			return ResponseEntity.ok().headers(headers).body(resource);
		} catch (Exception e) {
			return ResponseEntity.notFound().build();
		}
	}

	@GetMapping("/music")
	public ResponseEntity<Resource> music(@RequestParam("path") String path) {
		try {
			File file = fileService.parsePath(path);
			if (!file.exists()) {
				return ResponseEntity.notFound().build();
			}
			Resource resource = new UrlResource(file.toPath().toUri());
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("audio/wav"));
			headers.setContentDispositionFormData("attachment", file.getName());
			headers.setContentLength(file.length());
			return ResponseEntity.ok().headers(headers).body(resource);
		} catch (Exception e) {
			return ResponseEntity.notFound().build();
		}
	}

	@GetMapping("thumbnail")
	public ResponseEntity<Resource> thumbnail(@RequestParam("parent") String sourceDirPath,
											  @RequestParam("type") String thumbnailSize,
											  @RequestParam("name") String fileName) {
		try {
			File file = fileService.parsePath(sourceDirPath + File.separator + fileName);
			if (!file.exists()) {
				return ResponseEntity.notFound().build();
			}
			file = thumbnailService.getThumbnail(file, thumbnailSize);
			Resource resource = new FileSystemResource(file);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("image/jpeg"));
			headers.setContentDispositionFormData("attachment", file.getName());
			headers.setContentLength(file.length());
			return ResponseEntity.ok().headers(headers).body(resource);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.notFound().build();
		}
	}

	@PostMapping("upload")
	public String upload(@RequestPart("upload") MultipartFile file,
						 @RequestParam("fileUniqueId") String fileUniqueId,
						 @RequestParam("num") int chunkNumber,
						 @RequestParam("num_chunks") int totalNumChunks,
						 @RequestParam("parentDir") String parentDir) throws IOException {

		uploadService.upload(file, fileUniqueId, chunkNumber, totalNumChunks, parentDir);
		return "success";
	}

	@PostMapping("read-text-file")
	public String readTextFile(@RequestBody FormParams params) throws IOException {
		return new String(Files.readAllBytes(fileService.parsePath(params.getSourceDir()).toPath().resolve(params.getName())));
	}

	@PostMapping("save-text-file")
	public String saveTextFile(@RequestBody FormParams params) throws IOException {
		Path path = fileService.parsePath(params.getSourceDir()).toPath().resolve(params.getName());
		String content = params.getContent();
		try (PrintWriter out = new PrintWriter(path.toFile())) {
			out.print(content);
			out.flush();
		}
		return fileService.bytesToString(content.getBytes().length);
	}

	@PostMapping("/download-zip")
	public void createAndDownloadZip(@RequestBody FormParams params, HttpServletResponse response) {

		String parentDir =  fileService.parsePath(params.getSourceDir()).getAbsolutePath();

		try {
			// Set the response headers
			response.setContentType("application/zip");
			response.setHeader("Content-Disposition", "attachment; filename=\"output.zip\"");

			// Write the zip file content to the response output stream
			OutputStream outputStream = response.getOutputStream();
			ZipOutputStream zos = new ZipOutputStream(outputStream);
			addToZip(parentDir, params.getFiles(), zos);
			zos.close();
			outputStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void addToZip(String parentDir, List<String> fileAndDirNames, ZipOutputStream zos) throws IOException {
		for (String name : fileAndDirNames) {
			Path path = Paths.get(parentDir, name);
			if (!Files.exists(path)) {
				System.out.println("File or directory not found: " + path);
				continue;
			}

			if (Files.isDirectory(path)) {
				addDirectoryToZip(parentDir, path.toString(), zos);
			} else {
				FileInputStream fis = new FileInputStream(path.toFile());

				ZipEntry zipEntry = new ZipEntry(name);
				zos.putNextEntry(zipEntry);

				byte[] buffer = new byte[1024];
				int length;
				while ((length = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, length);
				}

				fis.close();
				zos.closeEntry();
			}
		}
	}

	private static void addDirectoryToZip(String rootDir, String currentDir, ZipOutputStream zos) throws IOException {
		File dir = new File(currentDir);
		File[] files = dir.listFiles();

		for (File file : files) {
			if (file.isDirectory()) {
				addDirectoryToZip(rootDir, file.getAbsolutePath(), zos);
			} else {
				FileInputStream fis = new FileInputStream(file);
				String entryName = file.getAbsolutePath().replace(rootDir, "");

				ZipEntry zipEntry = new ZipEntry(entryName);
				zos.putNextEntry(zipEntry);

				byte[] buffer = new byte[1024];
				int length;
				while ((length = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, length);
				}

				fis.close();
				zos.closeEntry();
			}
		}
	}


}
