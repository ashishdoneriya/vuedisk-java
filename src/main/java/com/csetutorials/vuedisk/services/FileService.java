package com.csetutorials.vuedisk.services;

import com.csetutorials.vuedisk.VueDiskApplication;
import com.csetutorials.vuedisk.beans.FilesListObj;
import com.csetutorials.vuedisk.beans.SizeSse;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

	@Autowired
	private ExtensionService extensionService;

	@Value("${base.dir}")
	private String baseDir;

	private final DecimalFormat decimalFormat = new DecimalFormat("#.##");

	public String getBaseDir() {
		return this.baseDir;
	}

	public File parsePath(String path) {
		return Paths.get(this.baseDir, path).toFile();
	}

	public File parsePath(String parentPath, String childPath) {
		return Paths.get(this.baseDir, parentPath, childPath).toFile();
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
			obj.setSizeInBytes(file.length());
			if (!file.isDirectory()) {
				obj.setText(file.length() <= 5000000 && extensionService.isText(file.getName()));
				obj.setImage(extensionService.isImage(file.getName()));
				obj.setAudio(extensionService.isAudio(file.getName()));
				obj.setVideo(extensionService.isVideo(file.getName()));
			}
			list.add(obj);
		}
		list.sort((obj1, obj2) -> {
			if ((obj1.isDir() && obj2.isDir()) || (!obj1.isDir() && !obj2.isDir())) {
				return obj1.getName().compareTo(obj2.getName());
			} else if (obj1.isDir()) {
				return -1;
			} else {
				return 1;
			}
		});
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

	public void size(SizeSse sizeSse, File sourceDir, List<String> files) {
		if (!sourceDir.exists()) {
			sizeSse.setFinished(true);
			return;
		}
		Deque<File> stack = new LinkedList<>(parsePaths(sourceDir, files));
		while (!stack.isEmpty()) {
			if (sizeSse.isStopped()) {
				return;
			}
			File file = stack.pop();
			if (!file.exists()) {
				continue;
			}
			sizeSse.setItems(sizeSse.getItems() + 1);
			if (file.isDirectory()) {
				for (File temp : checkNonNull(file.listFiles())) {
					stack.push(temp);
				}
			} else {
				sizeSse.setSizeInBytes(sizeSse.getSizeInBytes() + file.length());
			}
		}
		sizeSse.setFinished(true);
	}

	private File[] checkNonNull(File[] files) {
		return files != null ? files : new File[]{};
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

	public void remoteUpload(File destinationFile, String url) throws URISyntaxException {
		URI uri = new URI(url);
		(new Thread(() -> {
			try (BufferedInputStream in = new BufferedInputStream(uri.toURL().openStream());
				 FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {
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

	public void saveTextFile(File file, String content) throws FileNotFoundException {
		mkdirs(file.getParentFile());
		try (PrintWriter out = new PrintWriter(file)) {
			out.print(content);
			out.flush();
		}
	}

	public String readTextFile(File file) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			StringBuilder sb = null;
			String line;
			while ((line = br.readLine()) != null) {
				if (sb == null) {
					sb = new StringBuilder();
					sb.append(line);
				} else {
					sb.append('\n').append(line);
				}
			}
			if (sb == null) {
				return "";
			} else {
				return sb.toString();
			}

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


	public boolean isTextFile(File file) throws IOException {
		if (!file.exists() || file.length() < 10000000) {
			return false;
		}
		byte[] contentBytes = Files.readAllBytes(file.toPath());
		Tika tika = new Tika();
		try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(contentBytes)) {
			Set<MediaType> mediaTypes = new HashSet<>();
			MediaType mediaType = MediaType.parse(tika.detect(byteArrayInputStream));
			MediaTypeRegistry mediaTypeRegistry = MediaTypeRegistry.getDefaultRegistry();
			while (mediaType != null) {
				mediaTypes.addAll(mediaTypeRegistry.getAliases(mediaType));
				mediaTypes.add(mediaType);
				mediaType = mediaTypeRegistry.getSupertype(mediaType);
			}
			return mediaTypes.stream().anyMatch(mt -> mt.getType().equals("text"));
		} catch (IOException e) {
			return false;
		}
	}
}
