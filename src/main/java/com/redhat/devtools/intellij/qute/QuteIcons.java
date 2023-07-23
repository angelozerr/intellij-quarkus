package com.redhat.devtools.intellij.qute;

import com.intellij.icons.AllIcons;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class QuteIcons {

	private static @NotNull Icon load(@NotNull String path, int cacheKey, int flags) {
		return IconManager.getInstance().loadRasterizedIcon(path, AllIcons.class.getClassLoader(), cacheKey, flags);
	}
}
