package com.csetutorials.fileserver.services;

import com.csetutorials.fileserver.beans.FilesListObj;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
			obj.setSize(getSizeInString(file.length()));
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
		Deque<File> stack = new LinkedList<>(parsePaths(sourceDir, files));
		while (!stack.isEmpty()) {
			File file = stack.pop();
			if (!file.exists()) {
				continue;
			}
			File target = new File(destinationDir.getAbsolutePath() + File.separator + file.getAbsolutePath().substring(sourceDir.getAbsolutePath().length() + 1));
			if (file.isDirectory()) {
				mkdirs(target);
				File[] arr = file.listFiles();
				if (arr != null) {
					Collections.addAll(stack, arr);
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
		Deque<File> queue = new LinkedList<>(parsePaths(sourceDir, files));
		Deque<File> stack = new LinkedList<>();
		while (!queue.isEmpty()) {
			File file = queue.pop();
			if (!file.exists()) {
				continue;
			}
			File target = new File(destinationDir.getAbsolutePath() + File.separator + file.getAbsolutePath().substring(sourceDir.getAbsolutePath().length() + 1));
			if (file.isDirectory()) {
				mkdirs(target);
				File[] arr = file.listFiles();
				if (arr != null) {
					Collections.addAll(queue, arr);
				}
				stack.push(file);
			} else {
				renameTo(file, target);
			}
		}
		while (!stack.isEmpty()) {
			deleteSilently(stack.pop());
		}
	}

	public void delete(File sourceDir, List<String> children) {
		parsePaths(sourceDir, children).forEach(this::delete);
	}

	public void delete(File dir) {
		if (!dir.exists()) {
			return;
		}
		if (dir.isDirectory()) {
			File[] list = dir.listFiles();
			if (list != null) {
				for (File file : list) {
					delete(file);
				}
			}
		}
		deleteSilently(dir);
	}

	public String size(File sourceDir, List<String> files) {
		if (!sourceDir.exists()) {
			return "0 B";
		}
		long length = 0;
		Deque<File> stack = new LinkedList<>(parsePaths(sourceDir, files));
		while (!stack.isEmpty()) {
			File file = stack.pop();
			if (!file.exists()) {
				continue;
			}
			if (file.isDirectory()) {
				File[] arr = file.listFiles();
				if (arr != null) {
					for (File temp : arr) {
						stack.push(temp);
					}
				}
			} else {
				length += file.length();
			}
		}
		return getSizeInString(length);
	}

	public String getSizeInString(long size) {
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

	public void rename(File dir, String oldName, String newName) {
		renameTo(dir.toPath().resolve(oldName).toFile(), dir.toPath().resolve(newName).toFile());
	}

	public void createDir(File file, String newName) {
		mkdirs(file.toPath().resolve(newName).toFile());
	}

	public void remoteUpload(File sourceDir, String url, String newName) throws URISyntaxException {
		URI uri = new URI(url);
		(new Thread(() -> {
			try (BufferedInputStream in = new BufferedInputStream(uri.toURL().openStream());
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

	public void createZip(String parentDirPath, List<String> children, ZipOutputStream zos) throws IOException {
		for (String child : children) {
			Path path = Paths.get(parentDirPath, child);
			if (!Files.exists(path)) {
				continue;
			}

			if (Files.isDirectory(path)) {
				addDirectoryToZip(parentDirPath, path.toString(), zos);
			} else {
				try (FileInputStream fis = new FileInputStream(path.toFile())) {

					ZipEntry zipEntry = new ZipEntry(child);
					zos.putNextEntry(zipEntry);

					byte[] buffer = new byte[1024];
					int length;
					while ((length = fis.read(buffer)) > 0) {
						zos.write(buffer, 0, length);
					}
				}
				zos.closeEntry();
			}
		}
	}

	private void addDirectoryToZip(String rootDir, String currentDir, ZipOutputStream zos) throws IOException {
		File dir = new File(currentDir);
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				addDirectoryToZip(rootDir, file.getAbsolutePath(), zos);
			} else {
				try (FileInputStream fis = new FileInputStream(file)) {
					String entryName = file.getAbsolutePath().replace(rootDir, "");

					ZipEntry zipEntry = new ZipEntry(entryName);
					zos.putNextEntry(zipEntry);

					byte[] buffer = new byte[1024];
					int length;
					while ((length = fis.read(buffer)) > 0) {
						zos.write(buffer, 0, length);
					}
				}
				zos.closeEntry();
			}
		}
	}

	public void saveTextFile(Path path, String content) throws FileNotFoundException {
		mkdirs(path.toFile().getParentFile());
		try (PrintWriter out = new PrintWriter(path.toFile())) {
			out.print(content);
			out.flush();
		}
	}

	public void mkdirs(File dir) {
		if (!dir.exists() && !dir.mkdirs()) {
			log.error("Couldn't create dir {}", dir.getAbsolutePath());
		}
	}

	public void deleteSilently(File file) {
		if (!file.exists()) {
			return;
		}
		try {
			Files.delete(file.toPath());
		} catch (IOException e) {
			log.error("Couldn't delete file {}", file.getAbsolutePath(), e);
		}
	}

	public void renameTo(File src, File target) {
		if (!src.renameTo(target)) {
			log.error("Couldn't rename src {} To target {}", src.getAbsolutePath(), target.getAbsolutePath());
		}
	}


}
