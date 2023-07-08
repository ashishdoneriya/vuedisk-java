package com.csetutorials.fileserver.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

@Service
public class UploadService {

	@Autowired
	FileService fileService;

	public void upload(MultipartFile file, String fileUniqueId, int chunkNumber, int totalChunks, String parentDir,
					   String actualFileName) throws IOException {
		String workspaceDir = fileService.getBaseDir() + File.separator
				+ ".cache" + File.separator + "vuedisk" + File.separator + fileUniqueId;
		File workspaceDirFile = new File(workspaceDir);
		String tempTargetFilePath = workspaceDir + File.separator + fileUniqueId;
		String lockPath = workspaceDir + File.separator + "lock" + File.separator;
		String serialNumFilePath = workspaceDir + File.separator + "serialNo.txt";
		fileService.mkdirs(workspaceDirFile);
		// Moving the uploaded chunk file to our cache
		File temp123 = new File(tempTargetFilePath + chunkNumber);
		fileService.mkdirs(temp123.getParentFile());
		Files.copy(file.getInputStream(), temp123.toPath(), StandardCopyOption.REPLACE_EXISTING);
		// Creating lock path
		File lockFile = new File(lockPath);
		fileService.mkdirs(lockFile);
		// Acquiring lock
		while (!acquireLock(lockPath)) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		// Checking how many number of chunks have been merged serial wise
		int serialNumber = getSerialNumber(serialNumFilePath);
		File destFile = new File(tempTargetFilePath + 0);
		int i = serialNumber + 1;
		for (; i <= totalChunks; i++) {
			File srcFile = new File(tempTargetFilePath + i);
			if (!srcFile.exists()) {
				break;
			}
			if (!destFile.exists()) {
				fileService.renameTo(srcFile, destFile);
			} else {
				byte[] buff = Files.readAllBytes(srcFile.toPath());
				Files.write(destFile.toPath(), buff, StandardOpenOption.APPEND);
				fileService.deleteSilently(srcFile);
			}
		}
		setSerialNumber(serialNumFilePath, i - 1);
		int updatedSerialNumber = i - 1;

		if (updatedSerialNumber == totalChunks) {
			File parentFile = fileService.parsePath(parentDir);
			fileService.mkdirs(parentFile);
			fileService.renameTo(destFile, parentFile.toPath().resolve(actualFileName).toFile());
			fileService.delete(workspaceDirFile);
		}
		releaseLock(lockPath);
	}

	private void releaseLock(String lockPath) {
		fileService.delete(new File(lockPath));
	}

	private void setSerialNumber(String serialNumberFilePath, int serialNumber) throws FileNotFoundException {
		fileService.saveTextFile(Paths.get(serialNumberFilePath), String.valueOf(serialNumber));
	}

	private int getSerialNumber(String serialNumberFilePath) throws IOException {
		File file = new File(serialNumberFilePath);
		if (file.exists()) {
			return Integer.parseInt(new String(Files.readAllBytes(file.toPath())));
		} else {
			return 0;
		}
	}

	private boolean acquireLock(String lockPath) {
		File lockDir = new File(lockPath);
		if (isDirectoryEmpty(lockDir)) {
			long time = System.currentTimeMillis();
			fileService.mkdirs(new File(lockPath + File.separator + time));
			return isLowestTimestamp(lockDir, time);
		}
		return false;
	}

	private boolean isLowestTimestamp(File dir, long millis) {
		File[] list = dir.listFiles();
		if (list == null) {
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
		return list == null || list.length == 0;
	}
}
