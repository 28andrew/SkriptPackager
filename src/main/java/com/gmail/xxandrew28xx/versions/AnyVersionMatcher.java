package com.gmail.xxandrew28xx.versions;

public class AnyVersionMatcher implements VersionMatcher{

	@Override
	public boolean matches(String version) {
		return true;
	}

}
