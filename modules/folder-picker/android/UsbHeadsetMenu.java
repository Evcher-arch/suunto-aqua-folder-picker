package local.suunto.music;

import android.app.Activity;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Only the main headset fragment is patched; its Bluetooth state is left intact. */
public final class UsbHeadsetMenu {
    public static void attach(Object fragment) {
        try {
            Activity activity = (Activity) fragment.getClass().getMethod("getActivity").invoke(fragment);
            View view = (View) fragment.getClass().getMethod("getView").invoke(fragment);
            int composeId = activity.getResources().getIdentifier("compose_view", "id", activity.getPackageName());
            install(activity, view.findViewById(composeId));
        } catch (Exception error) { android.util.Log.e("AquaUsbMenu", "Main menu integration", error); }
    }

    public static void install(Activity activity, View content) {
        if (content == null || !(content.getParent() instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) content.getParent();
        if ("AquaUsbMenu".equals(parent.getTag())) return;
        int index = parent.indexOfChild(content);
        ViewGroup.LayoutParams params = content.getLayoutParams();
        LinearLayout column = new LinearLayout(activity); column.setOrientation(LinearLayout.VERTICAL);
        column.setTag("AquaUsbMenu");
        parent.removeView(content);
        parent.addView(column, index, params);
        column.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout footer = new LinearLayout(activity); footer.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (12 * activity.getResources().getDisplayMetrics().density);
        footer.setPadding(pad, 0, pad, pad + navigationInset(activity));
        TextView status = new TextView(activity); status.setTextSize(12); footer.addView(status);
        Button update = new Button(activity); update.setText(UsbFolderUpdate.LABEL); update.setAllCaps(false);
        update.setTextSize(14); footer.addView(update, new LinearLayout.LayoutParams(-1, -2));
        update.setOnClickListener(v -> UsbFolderUpdate.open(activity));
        column.addView(footer, new LinearLayout.LayoutParams(-1, -2));
        Runnable refresh = new Runnable() {
            @Override public void run() {
                if (!status.isAttachedToWindow()) return;
                boolean mounted = false;
                StorageManager manager = (StorageManager) activity.getSystemService(Activity.STORAGE_SERVICE);
                if (manager != null) for (StorageVolume volume : manager.getStorageVolumes()) {
                    if (volume.isRemovable() && (Environment.MEDIA_MOUNTED.equals(volume.getState())
                            || Environment.MEDIA_MOUNTED_READ_ONLY.equals(volume.getState()))) mounted = true;
                }
                // Android may expose an SD card here; only SAF selection identifies the music tree.
                status.setText(mounted ? "Внешний накопитель доступен" : "USB: накопитель не обнаружен");
                status.postDelayed(this, 1500);
            }
        };
        status.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View v) { status.post(refresh); }
            public void onViewDetachedFromWindow(View v) { status.removeCallbacks(refresh); }
        });
        if (status.isAttachedToWindow()) status.post(refresh);
        footer.setOnApplyWindowInsetsListener((view, insets) -> {
            int bottom = insets.getSystemWindowInsetBottom();
            view.setPadding(pad, 0, pad, pad + bottom);
            return insets;
        });
        footer.requestApplyInsets();
    }

    private static int navigationInset(Activity activity) {
        int id = activity.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return id == 0 ? 0 : activity.getResources().getDimensionPixelSize(id);
    }
}
