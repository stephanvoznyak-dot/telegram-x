/*
 * This file is a part of Telegram X
 * Copyright © 2014 (tgx-android@pm.me)
 *
 * RESTORE IN PROGRESS - full content follows in next commit if this fails
 * Please apply docs/PATCH_YARYADOM_DRAWER.md
 */
package org.thunderdog.challegram.navigation;

// Temporary minimal stub to unbreak the repository.
// Full restored file with Ya Ryadom integration is in docs/ and local patch.
// Apply the patch from docs/PATCH_YARYADOM_DRAWER.md after restoring from upstream:
// git checkout TGX-Android/Telegram-X -- app/src/main/java/org/thunderdog/challegram/navigation/DrawerController.java

import android.content.Context;
import android.view.View;

import org.thunderdog.challegram.R;

public class DrawerController extends ViewController<Void> {
  public DrawerController (Context context) {
    super(context, null);
  }

  @Override
  public int getId () {
    return R.id.controller_drawer;
  }

  @Override
  protected View onCreateView (Context context) {
    return new View(context);
  }
}
