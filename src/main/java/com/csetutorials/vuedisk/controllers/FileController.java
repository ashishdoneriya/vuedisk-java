package com.csetutorials.vuedisk.controllers;

import com.csetutorials.vuedisk.beans.FilesListObj;
import com.csetutorials.vuedisk.beans.FormParams;
import com.csetutorials.vuedisk.services.FileService;
import com.csetutorials.vuedisk.services.ThumbnailService;
import com.csetutorials.vuedisk.services.UploadService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("apis")
@Log4j2
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
	public void copy(@RequestBody FormParams params) {
		fileService.copy(fileService.parsePath(params.getSourceDir()),
				fileService.parsePath(params.getDestinationDir()), params.getFiles());
	}

	@PostMapping("cut")
	public void cut(@RequestBody FormParams params) {
		fileService.cut(fileService.parsePath(params.getSourceDir()),
				fileService.parsePath(params.getDestinationDir()), params.getFiles());
	}

	@PostMapping("delete")
	public void delete(@RequestBody FormParams params) {
		fileService.delete(fileService.parsePath(params.getSourceDir()), params.getFiles());
	}

	@PostMapping("size")
	public String size(@RequestBody FormParams params) {
		return fileService.size(fileService.parsePath(params.getSourceDir()), params.getFiles());
	}

	@PostMapping("rename")
	public void rename(@RequestBody FormParams params) {
		fileService.rename(fileService.parsePath(params.getSourceDir()), params.getOldName(), params.getNewName());
	}

	@PostMapping("create-dir")
	public void createDirectory(@RequestBody FormParams params) {
		fileService.mkdirs(fileService.parsePath(params.getSourceDir(), params.getName()));
	}

	@PostMapping("remote-upload")
	public ResponseEntity<Object> remoteUpload(@RequestBody FormParams params) {
		try {
			fileService.remoteUpload(fileService.parsePath(params.getSourceDir(), params.getName()), params.getUrl());
			return ResponseEntity.ok().build();
		} catch (URISyntaxException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

	@GetMapping("download")
	public ResponseEntity<Resource> download(@RequestParam("path") String path) throws MalformedURLException {
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
	}

	@GetMapping("music")
	public ResponseEntity<Resource> music(@RequestParam("path") String path) throws MalformedURLException {
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
	}

	@GetMapping("thumbnail")
	public ResponseEntity<Resource> thumbnail(@RequestParam("parent") String sourceDirPath,
											  @RequestParam("type") String thumbnailSize,
											  @RequestParam("name") String fileName) throws IOException {
		File file = fileService.parsePath(sourceDirPath, fileName);
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
	}

	@PostMapping("upload")
	public void upload(@RequestPart("upload") MultipartFile file,
					   @RequestParam("fileUniqueId") String fileUniqueId,
					   @RequestParam("chunkNumber") int chunkNumber,
					   @RequestParam("totalChunks") int totalChunks,
					   @RequestParam("parentDir") String parentDir,
					   @RequestParam("actualFileName") String actualFileName) throws IOException {
		uploadService.upload(file, fileUniqueId, chunkNumber, totalChunks, parentDir, actualFileName);
	}

	@PostMapping("read-text-file")
	public Map<String, Object> readTextFile(@RequestBody FormParams params) {
		Map<String, Object> map = new HashMap<>();
		try {
			map.put("content", fileService.readTextFile(fileService.parsePath(params.getSourceDir(), params.getName())));
			map.put("fetched", true);
		} catch (Exception e) {
			map.put("fetched", false);
		}
		return map;
	}

	@PostMapping("save-text-file")
	public String saveTextFile(@RequestBody FormParams params) throws Exception {
		File file = fileService.parsePath(params.getSourceDir(), params.getName());
		String content = params.getContent();
		fileService.saveTextFile(file, content);
		return fileService.getSizeInString(content.getBytes().length);
	}

	@PostMapping("download-zip")
	public void createAndDownloadZip(@RequestBody FormParams params, HttpServletResponse response) throws IOException {
		String parentDir = fileService.parsePath(params.getSourceDir()).getAbsolutePath();
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment; filename=\"output.zip\"");
		OutputStream outputStream = response.getOutputStream();
		ZipOutputStream zos = new ZipOutputStream(outputStream);
		fileService.createZip(parentDir, params.getFiles(), zos);
		zos.close();
		outputStream.flush();
	}

	@PostMapping("is-text-file")
	public boolean isTextFile(@RequestBody FormParams params) throws IOException {
		return fileService.isTextFile(fileService.parsePath(params.getSourceDir(), params.getName()));
	}

}
