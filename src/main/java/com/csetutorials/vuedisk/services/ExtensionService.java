package com.csetutorials.vuedisk.services;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class ExtensionService {

	private final Set<String> textFileExtensions = new HashSet<>(Arrays.asList(
			"gnumakefile", "makefile", "ada", "adb", "ads", "ahk", "alg", "as", "ascx", "ashx", "asp", "aspx", "awk",
			"bash", "bat", "c", "cbl", "cc", "cfg", "cfm", "cfml", "clj", "cmf", "cob", "coffee", "config", "cpp",
			"cpy", "cs", "css", "csv", "cxx", "d", "dart", "e", "erl", "ex", "exs", "f", "f90", "f95", "fsx", "go",
			"groovy", "h", "hpp", "hrl", "hs", "htaccess", "htm", "html", "inc", "j", "jade", "java", "jl", "js",
			"json", "jsp", "kt", "liquid", "lisp", "log", "lsp", "lua", "m", "makefile", "md", "ml", "mli", "mm", "nim",
			"pas", "php", "pl", "pp", "prg", "pro", "properties", "ps1", "psm1", "pwn", "py", "r", "rb", "rkt", "rs",
			"rss", "sas", "sass", "scala", "scm", "scss", "sh", "sql", "st", "swift", "tcl", "text", "toml", "ts", "v",
			"vb", "vh", "vhd", "vhdl", "vm", "vue", "xml", "xsl", "xstl", "yaml", "zsh"
	));

	private final Set<String> imageExtentions = new HashSet<>(Arrays.asList(
			"3dv", "ai", "amf", "art", "ase", "awg", "blp", "bmp", "bw", "cd5", "cdr", "cgm", "cit", "cmx", "cpt",
			"cr2", "cur", "cut", "dds", "dib", "djvu", "dxf", "e2d", "ecw", "egt", "emf", "eps", "exif", "gbr",
			"gif", "gpl", "grf", "hdp", "icns", "ico", "iff", "int", "inta", "jfif", "jng", "jp2", "jpeg", "jpg", "jps",
			"jxr", "lbm", "liff", "max", "miff", "mng", "msp", "nitf", "nrrd", "odg", "ota", "pam", "pbm", "pc1", "pc2",
			"pc3", "pcf", "pct", "pcx", "pdd", "pdn", "pgf", "pgm", "pi1", "pi2", "pi3", "pict", "png", "pnm", "pns",
			"ppm", "psb", "psp", "px", "pxm", "pxr", "qfx", "ras", "raw", "rgb", "rgba", "rle", "sct", "sgi",
			"sid", "stl", "sun", "svg", "sxd", "tga", "tif", "tiff", "v2d", "vnd", "vrml", "vtf", "wdp", "webp", "wmf",
			"x3d", "xar", "xbm", "xcf", "xpm"));

	private final Set<String> audioExtensions = new HashSet<>(Arrays.asList("aac", "mp3", "wav"));

	private final Set<String> videoExtensions = new HashSet<>(Arrays.asList(
			"avi", "mp4", "mpeg", "mpg", "ogg", "webm"));

	public String getExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
			return "";
		}
		return fileName.substring(dotIndex + 1).toLowerCase();
	}

	public boolean isText(String fileName) {
		return textFileExtensions.contains(getExtension(fileName));
	}

	public boolean isImage(String fileName) {
		return imageExtentions.contains(getExtension(fileName));
	}

	public boolean isAudio(String fileName) {
		return audioExtensions.contains(getExtension(fileName));
	}

	public boolean isVideo(String fileName) {
		return videoExtensions.contains(getExtension(fileName));
	}

}
