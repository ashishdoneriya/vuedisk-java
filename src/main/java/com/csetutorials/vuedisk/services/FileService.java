package com.csetutorials.vuedisk.services;

import com.csetutorials.vuedisk.beans.FilesListObj;
import com.csetutorials.vuedisk.beans.SizeSse;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
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
	private static final Set<String> textFileExtensions = new HashSet<>(Arrays.asList(
			"gnumakefile", "makefile", "ada", "adb", "ads", "ahk", "alg", "as", "ascx", "ashx", "asp", "aspx", "awk",
			"bash", "bat", "c", "cbl", "cc", "cfg", "cfm", "cfml", "clj", "cmf", "cob", "coffee", "config", "cpp",
			"cpy", "cs", "css", "cxx", "d", "dart", "e", "erl", "ex", "exs", "f", "f90", "f95", "fsx", "go",
			"groovy", "h", "hpp", "hrl", "hs", "htm", "html", "inc", "j", "jade", "java", "jl", "js", "json", "kt",
			"liquid", "lisp", "log", "lsp", "lua", "m", "makefile", "md", "ml", "mli", "mm", "nim", "pas", "php", "pl",
			"pp", "prg", "pro", "properties", "ps1", "psm1", "pwn", "py", "r", "rb", "rkt", "rs", "sas", "sass",
			"scala", "scm", "scss", "sh", "sql", "st", "swift", "tcl", "text", "toml", "ts", "v", "vb", "vh", "vhd",
			"vhdl", "vm", "vue", "xml", "xsl", "xstl", "yaml", "zsh"
	));

	private static final Set<String> imageExtentions = new HashSet<>(Arrays.asList(
			"3dv", "ai", "amf", "art", "ase", "awg", "blp", "bmp", "bw", "cd5", "cdr", "cgm", "cit", "cmx", "cpt",
			"cr2", "cur", "cut", "dds", "dib", "djvu", "dxf", "e2d", "ecw", "egt", "emf", "eps", "exif", "gbr",
			"gif", "gpl", "grf", "hdp", "icns", "ico", "iff", "int", "inta", "jfif", "jng", "jp2", "jpeg", "jpg", "jps",
			"jxr", "lbm", "liff", "max", "miff", "mng", "msp", "nitf", "nrrd", "odg", "ota", "pam", "pbm", "pc1", "pc2",
			"pc3", "pcf", "pct", "pcx", "pdd", "pdn", "pgf", "pgm", "pi1", "pi2", "pi3", "pict", "png", "pnm", "pns",
			"ppm", "psb", "psp", "px", "pxm", "pxr", "qfx", "ras", "raw", "rgb", "rgba", "rle", "sct", "sgi",
			"sid", "stl", "sun", "svg", "sxd", "tga", "tif", "tiff", "v2d", "vnd", "vrml", "vtf", "wdp", "webp", "wmf",
			"x3d", "xar", "xbm", "xcf", "xpm"));

	private static final Set<String> audioExtensions = new HashSet<>(Arrays.asList("aac", "mp3", "wav"));

	private static final Set<String> videoExtensions = new HashSet<>(Arrays.asList(
			"avi", "mp4", "mpeg", "mpg", "ogg", "webm"));

	public FileService() {
		this.baseDir = (new File("")).getAbsolutePath();
		this.baseDir = "/home/ashish";
	}

	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

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
				String extension = getExtension(file.getName());
				obj.setText(file.length() <= 5000000 && textFileExtensions.contains(extension));
				obj.setImage(imageExtentions.contains(extension));
				obj.setAudio(audioExtensions.contains(extension));
				obj.setVideo(videoExtensions.contains(extension));
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
				File[] arr = file.listFiles();
				if (arr != null) {
					for (File temp : arr) {
						stack.push(temp);
					}
				}
			} else {
				sizeSse.setSizeInBytes(sizeSse.getSizeInBytes() + file.length());
			}
		}
		sizeSse.setFinished(true);
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

	public String getExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
			return "";
		}
		return fileName.substring(dotIndex + 1).toLowerCase();
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
