package com.gmail.xxandrew28xx.versions;

public class StartsWithVersionMatcher implements VersionMatcher{
	String prefix;
	public StartsWithVersionMatcher(String prefix){
		this.prefix = prefix;
	}
	@Override
	public boolean matches(String version) {
		return version.startsWith(prefix);
	}

}
