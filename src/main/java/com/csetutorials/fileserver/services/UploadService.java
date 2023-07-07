package com.csetutorials.fileserver.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

@Service
public class UploadService {

	@Autowired
	FileService fileService;

	public void cancelUpload() {

	}

	public void upload(MultipartFile file, String fileUniqueId, int chunkNumber, int totalNumChunks, String parentDir)
			throws IOException {
		String workspaceDir = fileService.getBaseDir() + File.separator
				+ ".cache" + File.separator + "vuedisk" + File.separator + fileUniqueId;
		File workspaceDirFile = new File(workspaceDir);
		String tempTargetFilePath = workspaceDir + File.separator + fileUniqueId;
		String lockPath = workspaceDir + File.separator + "lock" + File.separator;
		String serialNumFilePath = workspaceDir + File.separator + "serialNo.txt";
		if (!workspaceDirFile.exists()) {
			workspaceDirFile.mkdirs();
		}
		// Moving the uploaded chunk file to our cache
		try {
			File temp123 = new File(tempTargetFilePath + chunkNumber);
			temp123.getParentFile().mkdirs();
			Files.copy(file.getInputStream(), temp123.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		// Creating lock path
		File lockFile = new File(lockPath);
		if (!lockFile.exists()) {
			lockFile.mkdirs();
		}
		// Acquiring lock
		while (!acquireLock(lockPath)) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		// Checking how many number of chunks have been merged serial wise
		int serialNumber = getSerialNumber(serialNumFilePath);
		File destFile = new File(tempTargetFilePath + 0);
		int i = serialNumber + 1;
		for (; i <= totalNumChunks; i++) {
			File srcFile = new File(tempTargetFilePath + i);
			if (!srcFile.exists()) {
				break;
			}
			if (!destFile.exists()) {
				srcFile.renameTo(destFile);
			} else {
				byte[] buff = Files.readAllBytes(srcFile.toPath());
				Files.write(destFile.toPath(), buff, StandardOpenOption.APPEND);
				srcFile.delete();
			}
		}
		setSerialNumber(serialNumFilePath, i - 1);
		int updatedSerialNumber = i - 1;

		if (updatedSerialNumber == totalNumChunks) {
			File parentFile = fileService.parsePath(parentDir);
			parentFile.mkdirs();
			String originalFileName = file.getOriginalFilename();
			destFile.renameTo(parentFile.toPath().resolve(originalFileName).toFile());
			removeDir(workspaceDirFile);
		}
		releaseLock(lockPath);
	}

	private void releaseLock(String lockPath) {
		removeDir(new File(lockPath));
	}

	private void setSerialNumber(String serialNumberFilePath, int serialNumber) throws FileNotFoundException {
		File file = new File(serialNumberFilePath);
		file.getParentFile().mkdirs();
		PrintWriter out = new PrintWriter(file);
		out.print(serialNumber);
		out.flush();
		out.close();
	}

	private int getSerialNumber(String serialNumberFilePath) throws IOException {
		File file = new File(serialNumberFilePath);
		if (file.exists()) {
			return Integer.parseInt(new String(Files.readAllBytes(file.toPath())));
		} else {
			return 0;
		}
	}

	private void removeDir(File dir) {
		if (dir.isDirectory()) {
			File[] list = dir.listFiles();
			if (list != null && list.length > 0) {
				for (File file : list) {
					removeDir(file);
				}
			}
		}
		dir.delete();
	}

	private boolean acquireLock(String lockPath) {
		File lockDir = new File(lockPath);
		if (isDirectoryEmpty(lockDir)) {
			long time = System.currentTimeMillis();
			(new File(lockPath + File.separator + time)).mkdirs();
			return isLowestTimestamp(lockDir, time);
		}
		return false;
	}

	private boolean isLowestTimestamp(File dir, long millis) {
		File[] list = dir.listFiles();
		if (list == null || list.length == 0) {
			return true;
		}
		for (File file : list) {
			if (Long.parseLong(file.getName()) < millis) {
				return false;
			}
		}
		return true;
	}

	private boolean isDirectoryEmpty(File lockDir) {
		if (!lockDir.exists()) {
			return true;
		}
		File[] list = lockDir.listFiles();
		if (list == null || list.length == 0) {
			return true;
		}
		return false;
	}
}
