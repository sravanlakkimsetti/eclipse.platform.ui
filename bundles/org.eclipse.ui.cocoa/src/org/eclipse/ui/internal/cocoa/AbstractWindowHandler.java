/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.cocoa;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.swt.internal.cocoa.NSWindow;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * 
 * @since 3.7 
 *
 */

public abstract class AbstractWindowHandler extends AbstractHandler {

	public boolean isEnabled() {
		boolean enabled = false;
		Shell activeShell = Display.getDefault().getActiveShell();
		if (activeShell != null && activeShell.view != null) {
			NSWindow window = activeShell.view.window();
			if(window!=null)
				enabled = !window.isMiniaturized();
		}
		return enabled;
	}
}