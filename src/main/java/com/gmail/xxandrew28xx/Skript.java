package com.gmail.xxandrew28xx;

import java.io.File;

public class Skript {
	public static void loadScript(File... f){
		ch.njol.skript.ScriptLoader.loadScripts(f);
	}
}
