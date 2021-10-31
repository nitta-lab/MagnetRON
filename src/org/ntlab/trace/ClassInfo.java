package org.ntlab.trace;

public class ClassInfo {
	private String name;
	private String path;
	private String loaderPath;
	
	public ClassInfo(String name, String path, String loaderPath) {
		this.name = name;
		this.path = path;
		this.loaderPath = loaderPath;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public String getLoaderPath() {
		return loaderPath;
	}
}
