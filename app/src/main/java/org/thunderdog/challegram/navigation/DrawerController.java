/*
 * Telegram X + «Я рядом»
 *
 * ВАЖНО: это временная заглушка.
 * Перед сборкой APK выполните из корня репозитория:
 *
 *   chmod +x docs/RESTORE_AND_PATCH.sh
 *   ./docs/RESTORE_AND_PATCH.sh
 *
 * Скрипт восстановит полный DrawerController из upstream Telegram X
 * и добавит пункт меню «Я рядом».
 *
 * См. docs/BUILD_APK.md и docs/yaryadom_drawer.patch
 */
package org.thunderdog.challegram.navigation;

import android.content.Context;
import android.view.View;

import org.thunderdog.challegram.R;

/**
 * Stub. Run docs/RESTORE_AND_PATCH.sh before building.
 */
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
