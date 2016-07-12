package com.gmail.xxandrew28xx.versions;

public class FixedVersionMatcher implements VersionMatcher{
	String version;
	public FixedVersionMatcher(String version){
		this.version = version;
	}
	@Override
	public boolean matches(String version) {
		return this.version.equals(version);
	}

}
