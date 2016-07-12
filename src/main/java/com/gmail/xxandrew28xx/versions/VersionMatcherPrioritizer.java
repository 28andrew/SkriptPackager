package com.gmail.xxandrew28xx.versions;

import java.util.Comparator;

public class VersionMatcherPrioritizer implements Comparator<VersionMatcher>{

	@Override
	public int compare(VersionMatcher arg0, VersionMatcher arg1) {
		return Integer.valueOf(VersionMatcherPriority.valueOf(arg0.getClass().getName()).getPriority())
				.compareTo(VersionMatcherPriority.valueOf(arg1.getClass().getName()).getPriority());
	}

	public enum VersionMatcherPriority{
		AnyVersionMatcher(1),
		StartsWithVersionMatcher(2),
		FixedVersionMatcher(3);
		int priority;
		VersionMatcherPriority(int priority){
			this.priority = priority;
		}
		public int getPriority(){
			return priority;
		}
		
	}
}
