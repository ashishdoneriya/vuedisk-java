package com.csetutorials.fileserver.services;

import com.csetutorials.fileserver.beans.FilesListObj;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

@Service
@Log4j2
public class FileService {

	private String baseDir;

	private final DecimalFormat decimalFormat = new DecimalFormat("#.##");

	public FileService() {
		this.baseDir = (new File("")).getAbsolutePath();
	}

	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	public String getBaseDir() {
		return this.baseDir;
	}

	public File parsePath(String path) {
		return new File(this.baseDir + File.separator + path.replace("/", File.separator));
	}

	public List<File> parsePaths(File parentDir, List<String> files) {
		return files.stream().map(file -> parentDir.toPath().resolve(file).toFile()).collect(Collectors.toList());
	}

	public List<FilesListObj> list(File dir) {
		if (!dir.exists()) {
			return Collections.emptyList();
		}
		File[] files = dir.listFiles();
		if (files == null || files.length == 0) {
			return Collections.emptyList();
		}
		List<FilesListObj> list = new ArrayList<>();
		for (File file : files) {
			FilesListObj obj = new FilesListObj();
			obj.setName(file.getName());
			obj.setDir(file.isDirectory());
			obj.setSize(bytesToString(file.length()));
			if (!obj.isDir()) {
				try {
					String mime = Files.probeContentType(file.toPath());
					obj.setMime(mime);
					obj.setTextFile(mime != null && mime.startsWith("text/"));
				} catch (Exception e) {
					log.error("Couldn't fetch mime of file - {}", file.getAbsolutePath(), e);
				}
			}

			list.add(obj);
		}
		return list;
	}

	public void copy(File sourceDir, File destinationDir, List<String> files) {
		if (!destinationDir.exists() && !destinationDir.mkdirs()) {
			log.warn("Couldn't create dir {}", destinationDir.getAbsolutePath());
			return;
		}
		Stack<File> stack = new Stack<>();
		stack.addAll(parsePaths(sourceDir, files));
		while (!stack.isEmpty()) {
			File file = stack.pop();
			if (!file.exists()) {
				continue;
			}
			File target = new File(destinationDir.getAbsolutePath() + File.separator + file.getAbsolutePath().substring(sourceDir.getAbsolutePath().length() + 1));
			if (file.isDirectory()) {
				target.mkdirs();
				File[] arr = file.listFiles();
				if (arr != null && arr.length > 0) {
					for (File temp : arr) {
						stack.add(temp);
					}
				}
			} else {
				try {
					Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					log.error("Problem while copying the file {}", file.getAbsolutePath(), e);
				}
			}
		}
	}

	public void cut(File sourceDir, File destinationDir, List<String> files) {
		if (!destinationDir.exists() && !destinationDir.mkdirs()) {
			log.warn("Couldn't create dir {}", destinationDir.getAbsolutePath());
			return;
		}
		Stack<File> queue = new Stack<>();
		queue.addAll(parsePaths(sourceDir, files));
		Stack<File> stack = new Stack<>();
		while (!queue.isEmpty()) {
			File file = queue.pop();
			if (!file.exists()) {
				continue;
			}
			File target = new File(destinationDir.getAbsolutePath() + File.separator + file.getAbsolutePath().substring(sourceDir.getAbsolutePath().length() + 1));
			if (file.isDirectory()) {
				target.mkdirs();
				File[] arr = file.listFiles();
				if (arr != null && arr.length > 0) {
					for (File temp : arr) {
						queue.add(temp);
					}
				}
				stack.push(file);
			} else {
				file.renameTo(target);
			}
		}
		while (!stack.isEmpty()) {
			stack.pop().delete();
		}
	}

	public void delete(File sourceDir, List<String> files) {
		if (!sourceDir.exists()) {
			return;
		}
		Stack<File> stack = new Stack<>();
		stack.addAll(parsePaths(sourceDir, files));
		while (!stack.isEmpty()) {
			File file = stack.pop();
			if (!file.exists()) {
				continue;
			}
			if (file.isDirectory()) {
				File[] arr = file.listFiles();
				if (arr == null || arr.length == 0) {
					file.delete();
				} else {
					stack.push(file);
					for (File temp : arr) {
						stack.push(temp);
					}
				}
			} else {
				file.delete();
			}
		}
	}

	public String size(File sourceDir, List<String> files) {
		if (!sourceDir.exists()) {
			return "0 B";
		}
		Stack<File> stack = new Stack<>();
		long length = 0;
		stack.addAll(parsePaths(sourceDir, files));
		while (!stack.isEmpty()) {
			File file = stack.pop();
			if (!file.exists()) {
				continue;
			}
			if (file.isDirectory()) {
				File[] arr = file.listFiles();
				if (arr != null && arr.length > 0) {
					for (File temp : arr) {
						stack.push(temp);
					}
				}
			} else {
				length += file.length();
			}
		}
		return bytesToString(length);
	}

	public String bytesToString(long size) {
		if (size > 1000000000) {
			return decimalFormat.format(size / 1000000000.0) + " GB";
		}
		if (size > 1000000) {
			return (size / 1000000) + " MB";
		}
		if (size > 1000) {
			return (size / 1000) + " KB";
		}
		return size + " B";
	}

	public void rename(File file, String oldName, String newName) {
		file.toPath().resolve(oldName).toFile().renameTo(file.toPath().resolve(newName).toFile());
	}

	public void createDir(File file, String newName) {
		file.toPath().resolve(newName).toFile().mkdir();
	}

	public void remoteUpload(File sourceDir, String url, String newName) {
		(new Thread(() -> {
			try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
				 FileOutputStream fileOutputStream = new FileOutputStream(sourceDir.toPath().resolve(newName).toString())) {
				byte[] dataBuffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
					fileOutputStream.write(dataBuffer, 0, bytesRead);
				}
			} catch (Exception e) {
				log.error("Problem while downloading the file", e);
			}
		})).start();
	}
}
