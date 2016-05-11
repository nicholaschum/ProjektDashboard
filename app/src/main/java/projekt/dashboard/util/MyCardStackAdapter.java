package projekt.dashboard.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mutualmobile.cardstack.CardStackAdapter;
import com.tramsun.libs.prefcompat.Pref;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import projekt.dashboard.R;
import projekt.dashboard.colorpicker.ColorPickerDialog;

public class MyCardStackAdapter extends CardStackAdapter implements
        CompoundButton.OnCheckedChangeListener {
    private static int[] bgColorIds;
    private final LayoutInflater mInflater;
    private final Context mContext;
    public Runnable updateSettingsView;
    public SharedPreferences prefs;
    public boolean colorful_icon = true;
    public int folder_directory = 1;

    // ==================================== Framework Tweaks ================================ //
    public int current_selected_system_accent_color = Color.argb(255, 255, 255, 255); // White
    public int current_selected_system_accent_dual_color = Color.argb(255, 119, 119, 119); // Medium Grey
    public int current_selected_system_accent_light_color = Color.argb(255, 119, 119, 119); // Medium grey
    public int current_selected_system_appbg_color = Color.argb(255, 0, 0, 0); // Black
    public int current_selected_system_appbg_light_color = Color.argb(255, 215, 215, 215); // Light grey
    public int current_selected_system_dialog_color = Color.argb(191, 0, 0, 0); // Black with Transparency
    public int current_selected_system_dialog_light_color = Color.argb(191, 238, 238, 238); // Light Grey with Transparency
    public int current_selected_system_notifications_primary_color = Color.argb(255, 255, 255, 255); // White
    public int current_selected_system_notifications_secondary_color = Color.argb(255, 174, 174, 174); // Lighter grey
    public int current_selected_system_ripple_color = Color.argb(74, 119, 119, 119); // Medium Grey with Transparency
    public int current_selected_system_main_color = Color.argb(255, 33, 32, 33); // Main theme color
    // ==================================== Settings Tweaks ================================== //
    public boolean category_title_caps = true;
    public boolean category_title_bold = true;
    public boolean category_title_italics = true;
    public boolean dashboard_dividers = true;
    public boolean dirtytweaks_iconpresence = true;
    public boolean dashboard_rounding = false;
    public int current_selected_settings_icon_color = Color.argb(255, 255, 255, 255);
    public int current_selected_settings_title_color = Color.argb(255, 255, 255, 255);
    public int current_selected_qs_accent_color = Color.argb(255, 255, 255, 255);
    // ==================================== SystemUI Tweaks ================================== //
    public int current_selected_qs_tile_color = Color.argb(255, 255, 255, 255);
    public int current_selected_qs_text_color = Color.argb(255, 255, 255, 255);

    ProgressDialog mProgressDialog;
    private PowerManager.WakeLock mWakeLock;
    private Logger log = new Logger(MyCardStackAdapter.class.getSimpleName());

    public Switch colorful_icon_switch;

    public MyCardStackAdapter(Activity activity) {
        super(activity);
        mContext = activity;
        mInflater = LayoutInflater.from(activity);
        bgColorIds = new int[]{
                R.color.card1_bg, // Framework
                R.color.card2_bg, // Settings
                R.color.card3_bg, // SystemUI
                R.color.card4_bg, // Final Card
        };
    }

    @Override
    public int getCount() {
        return bgColorIds.length;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        log.d("onCheckedChanged() called with: " + "buttonView = [" + buttonView + "], " +
                "isChecked = [" + isChecked + "]");
        Pref.putBoolean(CardStackPrefs.PARALLAX_ENABLED, isChecked);
        Pref.putBoolean(CardStackPrefs.SHOW_INIT_ANIMATION, isChecked);
        updateSettingsView.run();
    }

    @Override
    public View createView(int position, ViewGroup container) {
        if (position == 0) return getFrameworksView(container);
        if (position == 1) return getSettingsView(container);
        if (position == 2) return getSystemUIView(container);
        if (position == 3) return getFinalizedView(container);

        CardView root = (CardView) mInflater.inflate(R.layout.card, container, false);
        root.setCardBackgroundColor(ContextCompat.getColor(mContext, bgColorIds[position]));
        TextView cardTitle = (TextView) root.findViewById(R.id.card_title);
        cardTitle.setText(mContext.getResources().getString(R.string.card_title, position));
        return root;
    }

    public boolean checkCurrentThemeSelection(String packageName) {
        try {
            mContext.getPackageManager().getApplicationInfo(packageName, 0);
            File directory1 = new File("/data/app/" + packageName + "-1/base.apk");
            if (directory1.exists()) {
                folder_directory = 1;
                return true;
            } else {
                File directory2 = new File("/data/app/" + packageName + "-2/base.apk");
                if (directory2.exists()) {
                    folder_directory = 2;
                    return true;
                } else {
                    File directory3 = new File("/data/app/" + packageName + "-3/base.apk");
                    if (directory3.exists()) {
                        folder_directory = 3;
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public int checkCurrentThemeSelectionLocation(String packageName) {
        try {
            mContext.getPackageManager().getApplicationInfo(packageName, 0);
            File directory1 = new File("/data/app/" + packageName + "-1/base.apk");
            if (directory1.exists()) {
                return 1;
            } else {
                File directory2 = new File("/data/app/" + packageName + "-2/base.apk");
                if (directory2.exists()) {
                    return 2;
                } else {
                    File directory3 = new File("/data/app/" + packageName + "-3/base.apk");
                    if (directory3.exists()) {
                        return 3;
                    } else {
                        return 0;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private View getFrameworksView(ViewGroup container) {
        CardView root = (CardView) mInflater.inflate(R.layout.framework_card, container, false);
        root.setCardBackgroundColor(ContextCompat.getColor(mContext, bgColorIds[0]));

        final android.support.v7.widget.Toolbar framework_toolbar =
                (android.support.v7.widget.Toolbar) root.findViewById(R.id.framework_toolbar);
        framework_toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);

        final Switch switch1 = (Switch) root.findViewById(R.id.switch_example);
        final Switch switch2 = (Switch) root.findViewById(R.id.switch_example2);

        final RelativeLayout rl = (RelativeLayout) root.findViewById(R.id.main_relativeLayout);

        // Framework Accent (universal)

        final ImageView accent_universal = (ImageView) root.findViewById(
                R.id.system_accent_colorpicker);
        accent_universal.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_system_accent_color);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_system_accent_color = color;
                        accent_universal.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        ColorStateList csl = new ColorStateList(
                                new int[][]{
                                        new int[]{android.R.attr.state_checked},
                                        new int[]{}
                                },
                                new int[]{
                                        color, color
                                }
                        );
                        switch1.setTrackTintList(csl);
                        switch1.setThumbTintList(csl);
                    }
                });
                cpd.show();
            }
        });

        // Framework Accent (dual)

        final ImageView accent_secondary = (ImageView) root.findViewById(
                R.id.system_accent_dual_colorpicker);
        accent_secondary.setColorFilter(
                current_selected_system_accent_dual_color, PorterDuff.Mode.SRC_ATOP);
        accent_secondary.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_system_accent_dual_color);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_system_accent_dual_color = color;
                        accent_secondary.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    }
                });
                cpd.show();
            }
        });

        // Framework Accent (light)

        final ImageView accent_light = (ImageView) root.findViewById(
                R.id.system_accent_light_colorpicker);
        accent_light.setColorFilter(
                current_selected_system_accent_light_color, PorterDuff.Mode.SRC_ATOP);
        accent_light.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_system_accent_light_color);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_system_accent_light_color = color;
                        accent_light.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        ColorStateList csl = new ColorStateList(
                                new int[][]{
                                        new int[]{android.R.attr.state_checked},
                                        new int[]{}
                                },
                                new int[]{
                                        color, color
                                }
                        );
                        switch2.setTrackTintList(csl);
                        switch2.setThumbTintList(csl);
                    }
                });
                cpd.show();
            }
        });

        // Framework Appbg (dark)

        final ImageView appbg_dark = (ImageView) root.findViewById(
                R.id.system_appbg_colorpicker);
        rl.setBackgroundColor(current_selected_system_appbg_color);
        appbg_dark.setColorFilter(current_selected_system_appbg_color, PorterDuff.Mode.SRC_ATOP);
        appbg_dark.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_system_appbg_color);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_system_appbg_color = color;
                        appbg_dark.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        rl.setBackgroundColor(color);
                    }
                });
                cpd.show();
            }
        });

        // Framework Appbg (light)

        final ImageView appbg_light = (ImageView) root.findViewById(
                R.id.system_appbg_light_colorpicker);
        appbg_light.setColorFilter(
                current_selected_system_appbg_light_color, PorterDuff.Mode.SRC_ATOP);
        switch2.setBackgroundColor(current_selected_system_appbg_light_color);
        appbg_light.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_system_appbg_light_color);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_system_appbg_light_color = color;
                        appbg_light.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        switch2.setBackgroundColor(color);
                    }
                });
                cpd.show();
            }
        });

        // Framework System Dialog Color (dark)

        final ImageView dialog_dark = (ImageView) root.findViewById(
                R.id.system_dialog_colorpicker);
        dialog_dark.setColorFilter(current_selected_system_dialog_color, PorterDuff.Mode.SRC_ATOP);
        dialog_dark.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_system_dialog_color);
                cpd.setAlphaSliderVisible(true);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_system_dialog_color = color;
                        dialog_dark.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    }
                });
                cpd.show();
            }
        });

        // Framework System Dialog Color (light)

        final ImageView dialog_light = (ImageView) root.findViewById(
                R.id.system_dialog_light_colorpicker);
        dialog_light.setColorFilter(
                current_selected_system_dialog_light_color, PorterDuff.Mode.SRC_ATOP);
        dialog_light.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_system_dialog_light_color);
                cpd.setAlphaSliderVisible(true);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_system_dialog_light_color = color;
                        dialog_light.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    }
                });
                cpd.show();
            }
        });

        // Framework System Main Color

        final ImageView main_color = (ImageView) root.findViewById(
                R.id.system_main_colorpicker);
        main_color.setColorFilter(current_selected_system_main_color, PorterDuff.Mode.SRC_ATOP);
        main_color.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_system_main_color);
                cpd.setAlphaSliderVisible(true);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_system_main_color = color;
                        main_color.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        framework_toolbar.setBackgroundColor(color);
                    }
                });
                cpd.show();
            }
        });

        // Framework Notifications Primary Color

        final ImageView notifications_primary = (ImageView) root.findViewById(
                R.id.system_notification_text_1_colorpicker);
        notifications_primary.setColorFilter(
                current_selected_system_notifications_primary_color, PorterDuff.Mode.SRC_ATOP);
        notifications_primary.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_system_notifications_primary_color);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_system_notifications_primary_color = color;
                        notifications_primary.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    }
                });
                cpd.show();
            }
        });

        // Framework Notifications Secondary Color

        final ImageView notifications_secondary = (ImageView) root.findViewById(
                R.id.system_notification_text_2_colorpicker);
        notifications_secondary.setColorFilter(
                current_selected_system_notifications_secondary_color, PorterDuff.Mode.SRC_ATOP);
        notifications_secondary.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_system_notifications_secondary_color);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_system_notifications_secondary_color = color;
                        notifications_secondary.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    }
                });
                cpd.show();
            }
        });

        // Framework Ripple Color

        final ImageView ripples = (ImageView) root.findViewById(
                R.id.system_ripple_colorpicker);
        ripples.setColorFilter(current_selected_system_ripple_color, PorterDuff.Mode.SRC_ATOP);
        ripples.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_system_ripple_color);
                cpd.setAlphaSliderVisible(true);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_system_ripple_color = color;
                        ripples.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    }
                });
                cpd.show();
            }
        });


        return root;
    }

    private View getSettingsView(ViewGroup container) {
        CardView root = (CardView) mInflater.inflate(R.layout.settings_card, container, false);
        root.setCardBackgroundColor(ContextCompat.getColor(mContext, bgColorIds[1]));

        final ImageView wifiIcon = (ImageView) root.findViewById(R.id.wifiIcon);
        final TextView categoryHeader = (TextView) root.findViewById(R.id.categoryHeaderTitle);


        // Colorful DU/PN Tweaks Icon

        colorful_icon_switch = (Switch) root.findViewById(R.id.colorful_icon);
        colorful_icon_switch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            colorful_icon = true;
                            Log.d("Switch Colorful Icon", colorful_icon + "");
                        } else {
                            colorful_icon = false;
                            Log.d("Switch Colorful Icon", colorful_icon + "");
                        }
                    }
                });
        colorful_icon_switch.setVisibility(View.GONE);


        // Dashboard Categories (Rounded)

        final Switch dashboard_round = (Switch) root.findViewById(R.id.dashboard_rounding);
        dashboard_round.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            dashboard_rounding = true;
                            Log.d("Dashboard (Rounded)", dashboard_rounding + "");
                        } else {
                            dashboard_rounding = false;
                            Log.d("Dashboard (Rounded)", dashboard_rounding + "");
                        }
                    }
                });

        // Dashboard Categories Title (All Caps)

        final Switch categories_title_caps = (Switch) root.findViewById(
                R.id.dashboard_title_allcaps);
        categories_title_caps.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            category_title_caps = true;
                            Log.d("Categories Title (Caps)", category_title_caps + "");
                        } else {
                            category_title_caps = false;
                            Log.d("Categories Title (Caps)", category_title_caps + "");
                        }
                    }
                });

        // Dashboard Categories Title (Bold)

        final Switch categories_title_bold = (Switch) root.findViewById(
                R.id.dashboard_title_bold);
        categories_title_bold.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            category_title_bold = true;
                            Log.d("Categories Title (Bold)", category_title_bold + "");
                        } else {
                            category_title_bold = false;
                            Log.d("Categories Title (Bold)", category_title_bold + "");
                        }
                    }
                });

        // Dashboard Categories Title (Italics)

        final Switch categories_title_italics = (Switch) root.findViewById(
                R.id.dashboard_title_italics);
        categories_title_italics.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            category_title_italics = true;
                            Log.d("Categories Title (Ita)", category_title_italics + "");
                        } else {
                            category_title_italics = false;
                            Log.d("Categories Title (Ita)", category_title_italics + "");
                        }
                    }
                });

        // Dashboard Dividers

        final Switch dashboard_divider = (Switch) root.findViewById(R.id.dashboard_dividers);
        dashboard_divider.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            dashboard_dividers = true;
                            Log.d("Dashboard Dividers", dashboard_dividers + "");
                        } else {
                            dashboard_dividers = false;
                            Log.d("Dashboard Dividers", dashboard_dividers + "");
                        }
                    }
                });

        // Dirty Tweaks Icon Presence

        final Switch dutweaks_icons = (Switch) root.findViewById(R.id.dirty_tweaks_icons);
        dutweaks_icons.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            dirtytweaks_iconpresence = true;
                            Log.d("DU Tweaks Icon", dirtytweaks_iconpresence + "");
                        } else {
                            dirtytweaks_iconpresence = false;
                            Log.d("DU Tweaks Icon", dirtytweaks_iconpresence + "");
                        }
                    }
                });

        // Settings Icons Colors

        final ImageView settings_icon_colors = (ImageView) root.findViewById(
                R.id.settings_icon_colorpicker);
        settings_icon_colors.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_settings_icon_color);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_settings_icon_color = color;
                        settings_icon_colors.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        wifiIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    }
                });
                cpd.show();
            }
        });

        // Settings Title Colors

        final ImageView settings_title_colors = (ImageView) root.findViewById(
                R.id.settings_title_colorpicker);
        settings_title_colors.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_settings_title_color);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_settings_title_color = color;
                        settings_title_colors.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        categoryHeader.setTextColor(color);
                    }
                });
                cpd.show();
            }
        });

        return root;
    }

    private View getSystemUIView(ViewGroup container) {
        CardView root = (CardView) mInflater.inflate(R.layout.systemui_card, container, false);
        root.setCardBackgroundColor(ContextCompat.getColor(mContext, bgColorIds[2]));

        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final TextView wifiLabel = (TextView) root.findViewById(R.id.wifiLabel);
        final TextView bluetoothLabel = (TextView) root.findViewById(R.id.bluetoothLabel);
        wifiLabel.setText(prefs.getString("dashboard_username",
                root.getResources().getString(R.string.systemui_preview_default_no_username)) +
                root.getResources().getString(R.string.systemui_preview_label));
        final SeekBar brightness = (SeekBar) root.findViewById(R.id.seekBar);

        // QS Accent Colors

        final ImageView qs_accents = (ImageView) root.findViewById(R.id.qs_accent_colorpicker);
        qs_accents.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_qs_accent_color);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_qs_accent_color = color;
                        qs_accents.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        ColorStateList csl = new ColorStateList(
                                new int[][]{
                                        new int[]{android.R.attr.state_pressed},
                                        new int[]{android.R.attr.state_focused},
                                        new int[]{}
                                },
                                new int[]{
                                        color, color, color
                                }
                        );
                        brightness.setProgressTintList(csl);
                        brightness.setThumbTintList(csl);
                    }
                });
                cpd.show();
            }
        });

        // QS Icon Colors

        final ImageView qs_tile = (ImageView) root.findViewById(R.id.qs_tile_icon_colorpicker);
        qs_tile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_qs_tile_color);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_qs_tile_color = color;
                        qs_tile.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        ColorStateList csl = new ColorStateList(
                                new int[][]{
                                        new int[]{android.R.attr.state_pressed},
                                        new int[]{android.R.attr.state_focused},
                                        new int[]{}
                                },
                                new int[]{
                                        color, color, color
                                }
                        );
                        bluetoothLabel.setCompoundDrawableTintList(csl);
                        wifiLabel.setCompoundDrawableTintList(csl);
                    }
                });
                cpd.show();
            }
        });

        // QS Title Colors

        final ImageView qs_text = (ImageView) root.findViewById(R.id.qs_tile_text_colorpicker);
        qs_text.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final ColorPickerDialog cpd = new ColorPickerDialog(
                        mContext, current_selected_qs_text_color);
                cpd.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        current_selected_qs_text_color = color;
                        qs_text.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        wifiLabel.setTextColor(color);
                        bluetoothLabel.setTextColor(color);
                    }
                });
                cpd.show();
            }
        });

        return root;
    }

    private View getFinalizedView(ViewGroup container) {
        CardView root = (CardView) mInflater.inflate(R.layout.final_card, container, false);
        root.setCardBackgroundColor(ContextCompat.getColor(mContext, bgColorIds[3]));

        int counter = 0;

        final Spinner spinner1 = (Spinner) root.findViewById(R.id.spinner2);
        // Create an ArrayAdapter using the string array and a default spinner layout
        List<String> list = new ArrayList<String>();

        list.add(mContext.getResources().getString(R.string.contextualheaderswapper_select_theme));
        list.add("dark material // akZent");
        list.add("blacked out // blakZent");

        // Now lets add all the located themes found that aren't cdt themes
        File f = new File("/data/resource-cache/");
        File[] files = f.listFiles();
        if (files != null) {
            for (File inFile : files) {
                if (inFile.isDirectory()) {
                    if (!inFile.getAbsolutePath().substring(21).equals(
                            "com.chummy.jezebel.blackedout.donate")) {
                        if (!inFile.getAbsolutePath().substring(21).equals(
                                "com.chummy.jezebel.materialdark.donate")) {
                            if (!inFile.getAbsolutePath().substring(21).equals("projekt.klar")) {
                                list.add(inFile.getAbsolutePath().substring(21));
                                counter += 1;
                            }
                        } else {
                            counter += 1;
                        }
                    } else {
                        counter += 1;
                    }
                }
            }
        }
        if (counter == 0) {
            Toast toast = Toast.makeText(mContext.getApplicationContext(),
                    mContext.getResources().getString(
                            R.string.contextualheaderswapper_toast_cache_empty_reboot_first),
                    Toast.LENGTH_LONG);
            toast.show();
        }
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_spinner_item, list);
        // Specify the layout to use when the list of choices appears
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Set On Item Selected Listener
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int pos, long id) {
                if (pos == 1) {
                    if (!checkCurrentThemeSelection("com.chummy.jezebel.materialdark.donate")) {
                        Toast toast = Toast.makeText(mContext.getApplicationContext(),
                                mContext.getResources().getString(
                                        R.string.akzent_toast_install_before_using),
                                Toast.LENGTH_LONG);
                        toast.show();
                    } else {
                        colorful_icon_switch.setVisibility(View.VISIBLE);
                    }
                }
                if (pos == 2) {
                    if (!checkCurrentThemeSelection("com.chummy.jezebel.blackedout.donate")) {
                        Toast toast = Toast.makeText(mContext.getApplicationContext(),
                                mContext.getResources().getString(
                                        R.string.blakzent_toast_install_before_using),
                                Toast.LENGTH_LONG);
                        toast.show();
                        spinner1.setSelection(0);
                    } else {
                        colorful_icon_switch.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //
            }
        });
        // Apply the adapter to the spinner
        spinner1.setAdapter(adapter1);


        // Begin Creative Mode Functions

        com.github.clans.fab.FloatingActionButton creative_mode_start =
                (com.github.clans.fab.FloatingActionButton) root.findViewById(R.id.begin_action);
        creative_mode_start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // We have to unzip the destination APK first

                mProgressDialog = new ProgressDialog(mContext, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
                mProgressDialog.setTitle(mContext.getResources().getString(
                        R.string.unzipping_assets_dialog_title));
                mProgressDialog.setMessage(
                        mContext.getResources().getString(R.string.unzipping_assets_small));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setCancelable(false);

                // Check that there is SOMETHING changed, let's decide on the theme at least

                if (spinner1.getSelectedItemPosition() != 0) {
                    Phase1_UnzipAssets unzipTask = new Phase1_UnzipAssets();
                    unzipTask.execute(spinner1.getSelectedItem().toString());
                } else {
                    Toast toast = Toast.makeText(mContext.getApplicationContext(),
                            mContext.getResources().getString(
                                    R.string.no_theme_selected),
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
        return root;
    }

    public void startPhase2() {

        // Begin going through all AsyncTasks for Framework (v10)

        Phase2_InjectAndMove accent = new Phase2_InjectAndMove();
        String accent_color = "#" + Integer.toHexString(current_selected_system_accent_color);
        accent.execute("accent_color", accent_color, "theme_color_accent",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v10/");

        Phase2_InjectAndMove accent_secondary = new Phase2_InjectAndMove();
        String accent_secondary_color = "#" + Integer.toHexString(
                current_selected_system_accent_dual_color);
        accent_secondary.execute("dialer_button_bar", accent_secondary_color,
                "theme_color_accent_secondary", mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v10/");

        Phase2_InjectAndMove accent_light = new Phase2_InjectAndMove();
        String accent_light_color = "#" + Integer.toHexString(
                current_selected_system_accent_light_color);
        accent_light.execute("accent_color_light", accent_light_color, "theme_color_accent_light",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v10/");

        Phase2_InjectAndMove app_bg = new Phase2_InjectAndMove();
        String app_bg_color = "#" + Integer.toHexString(current_selected_system_appbg_color);
        app_bg.execute("app_background", app_bg_color, "theme_color_app_background",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v10/");

        Phase2_InjectAndMove app_bg_light = new Phase2_InjectAndMove();
        String app_bg_light_color = "#" + Integer.toHexString(
                current_selected_system_appbg_light_color);
        app_bg_light.execute("light_background", app_bg_light_color,
                "theme_color_light_background",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v10/");

        Phase2_InjectAndMove dialog_dark = new Phase2_InjectAndMove();
        String dialog_dark_color = "#" + Integer.toHexString(current_selected_system_dialog_color);
        dialog_dark.execute("dialog_color_dark", dialog_dark_color, "theme_color_dialog_dark",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v10/");

        Phase2_InjectAndMove dialog_light = new Phase2_InjectAndMove();
        String dialog_light_color = "#" + Integer.toHexString(
                current_selected_system_dialog_light_color);
        dialog_light.execute("dialog_color_light", dialog_light_color,
                "theme_color_dialog_light", mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v10/");

        Phase2_InjectAndMove theme_color = new Phase2_InjectAndMove();
        String theme_color_ = "#" + Integer.toHexString(current_selected_system_main_color);
        theme_color.execute("theme_color", theme_color_, "theme_color",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v10/");

        Phase2_InjectAndMove notification_primary = new Phase2_InjectAndMove();
        String notification_primary_color = "#" + Integer.toHexString(
                current_selected_system_notifications_primary_color);
        notification_primary.execute("notification_primary", notification_primary_color,
                "theme_color_notification_primary", mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v10/");

        Phase2_InjectAndMove notification_secondary = new Phase2_InjectAndMove();
        String notification_secondary_color = "#" + Integer.toHexString(
                current_selected_system_notifications_secondary_color);
        notification_secondary.execute("notification_secondary", notification_secondary_color,
                "theme_color_notification_secondary", mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v10/");

        Phase2_InjectAndMove ripple = new Phase2_InjectAndMove();
        String ripple_color = "#" + Integer.toHexString(current_selected_system_ripple_color);
        ripple.execute("ripple_dark", ripple_color, "theme_color_ripple_color",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v10/");

        // Begin going through all AsyncTasks for Settings (v11)

        Phase2_InjectAndMove settings_icon = new Phase2_InjectAndMove();
        String settings_icon_color = "#" + Integer.toHexString(
                current_selected_settings_icon_color);
        settings_icon.execute("settings_icon_tint", settings_icon_color,
                "theme_color_settings_icon_tint",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/com.android.settings/res/values-v11/");

        Phase2_InjectAndMove settings_title = new Phase2_InjectAndMove();
        String settings_title_color = "#" + Integer.toHexString(
                current_selected_settings_title_color);
        settings_title.execute("theme_accent", settings_title_color,
                "theme_color_settings_title_color",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/com.android.settings/res/values-v11/");

        // Begin going through all AsyncTasks for SystemUI (v12)

        Phase2_InjectAndMove sysui_accent = new Phase2_InjectAndMove();
        String sysui_accent_color = "#" + Integer.toHexString(current_selected_qs_accent_color);
        sysui_accent.execute("system_accent_color", sysui_accent_color,
                "theme_color_systemui_accent_color",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/com.android.systemui/res/values-v12/");

        Phase2_InjectAndMove sysui_qs_tile = new Phase2_InjectAndMove();
        String sysui_qs_tile_color = "#" + Integer.toHexString(current_selected_qs_tile_color);
        sysui_qs_tile.execute("qs_icon_color", sysui_qs_tile_color,
                "theme_color_systemui_qs_icon_color",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v12/");

        Phase2_InjectAndMove sysui_qs_tile_disabled = new Phase2_InjectAndMove();
        String sysui_qs_tile_disabled_color = "#4d" +
                Integer.toHexString(current_selected_qs_tile_color).substring(2);
        sysui_qs_tile_disabled.execute("qs_icon_color_disabled", sysui_qs_tile_disabled_color,
                "theme_color_systemui_qs_icon_disabled_color",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v12/");

        Phase2_InjectAndMove sysui_qs_tile_text = new Phase2_InjectAndMove();
        String sysui_qs_tile_text_color = "#" + Integer.toHexString(current_selected_qs_tile_color);
        sysui_qs_tile_text.execute("qs_tile_text", sysui_qs_tile_text_color,
                "theme_color_systemui_qs_tile_text_color",
                mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v12/");

        // Let's do a while loop file checker for now to wait for v10 to fill up

        Integer processed_items = 0;
        while (processed_items < 11 || processed_items.equals(null)) {
            try {
                Thread.sleep(1000);
                Log.d("WhileLoop", "Processes aren't done, while loop continuing...");
                processed_items = new File(mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/common/res/values-v10/").listFiles().length;
            } catch (InterruptedException e) {
                //
            }
        }
        Phase3_MovePremadeFiles phase3 = new Phase3_MovePremadeFiles();
        phase3.execute();

    }

    private class Phase1_UnzipAssets extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager)
                    mContext.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            mProgressDialog.setTitle("configuring the fun!");
            mProgressDialog.setMessage(mContext.getResources().getString(R.string.
                    unzipping_assets_small));
            startPhase2();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            String package_identifier = sUrl[0];
            try {
                unzip(package_identifier);
            } catch (IOException e) {

            }

            return null;
        }

        public void unzip(String package_identifier) throws IOException {
            // Let's check where it is first

            Boolean is_valid = checkCurrentThemeSelection(package_identifier);

            // After checking package identifier validity, check for exact folder number
            if (is_valid) {
                int folder_abbreviation = checkCurrentThemeSelectionLocation(package_identifier);
                if (folder_abbreviation != 0) {
                    String source = "/data/app/" + package_identifier + "-" +
                            folder_abbreviation + "/base.apk";
                    String destination = mContext.getCacheDir().getAbsolutePath() +
                            "/creative_mode/";

                    File checkFile = new File(source);
                    long fileSize = checkFile.length();
                    if (fileSize > 50000000) { // Picking 50mb to be the threshold of large themes
                        mProgressDialog.setMessage(
                                mContext.getResources().getString(R.string.unzipping_assets_big));
                    }
                    File myDir = new File(mContext.getCacheDir(), "creative_mode");
                    if (!myDir.exists()) {
                        myDir.mkdir();
                    }

                    ZipInputStream inputStream = new ZipInputStream(
                            new BufferedInputStream(new FileInputStream(source)));
                    try {
                        ZipEntry zipEntry;
                        int count;
                        byte[] buffer = new byte[8192];
                        while ((zipEntry = inputStream.getNextEntry()) != null) {
                            File file = new File(destination, zipEntry.getName());
                            File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                            if (!dir.isDirectory() && !dir.mkdirs())
                                throw new FileNotFoundException("Failed to ensure directory: " +
                                        dir.getAbsolutePath());
                            if (zipEntry.isDirectory())
                                continue;
                            FileOutputStream outputStream = new FileOutputStream(file);
                            try {
                                while ((count = inputStream.read(buffer)) != -1)
                                    outputStream.write(buffer, 0, count);
                            } finally {
                                outputStream.close();
                            }
                        }
                    } finally {
                        inputStream.close();
                    }
                } else {
                    Log.d("Unzip",
                            "There is no valid package name under this abbreviated folder count.");
                }
            } else {
                Log.d("Unzip", "Package name chosen is invalid.");
            }
        }
    }

    private class Phase2_InjectAndMove extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager)
                    mContext.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            String color_name = sUrl[0];
            String colorHex = sUrl[1];
            String filename = sUrl[2];
            String theme_destination = sUrl[3];
            createXMLfile(color_name, colorHex, filename, theme_destination);
            return null;
        }

        private void createXMLfile(String color_name, String colorHex, String filename,
                                   String theme_destination) {

            File root = new File(
                    mContext.getCacheDir().getAbsolutePath() + "/" + filename + ".xml");
            try {
                root.createNewFile();
                FileWriter fw = new FileWriter(root);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter pw = new PrintWriter(bw);
                String xmlTags = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "\n");
                String xmlRes1 = ("<resources>" + "\n");
                String xmlRes2 = ("    <color name=\"" + color_name + "\">" + colorHex + "</color>"
                        + "\n");
                String xmlRes3 = ("</resources>");
                pw.write(xmlTags);
                pw.write(xmlRes1);
                pw.write(xmlRes2);
                pw.write(xmlRes3);
                pw.close();
                bw.close();
                fw.close();
            } catch (IOException e) {
                //
            } finally {
                moveXMLfile(
                        mContext.getCacheDir().getAbsolutePath() + "/",
                        filename + ".xml", theme_destination);
            }
        }

        private void moveXMLfile(String current_source, String inputFile,
                                 String theme_destination) {
            InputStream in;
            OutputStream out;
            try {
                //create output directory if it doesn't exist
                File dir = new File(theme_destination);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                in = new FileInputStream(current_source + inputFile);
                out = new FileOutputStream(theme_destination + inputFile);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();

                // write the output file
                out.flush();
                out.close();

                // delete the original file
                new File(current_source + inputFile).delete();

            } catch (FileNotFoundException f) {
                //
            } catch (Exception e) {
                //
            }
        }
    }

    private class Phase3_MovePremadeFiles extends AsyncTask<String, Integer, String> {

        final String TARGET_BASE_PATH = mContext.getCacheDir().getAbsolutePath() + "/";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager)
                    mContext.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {

            super.onPostExecute(result);

            //mWakeLock.release();
            //mProgressDialog.dismiss();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            copyFileOrDir("");


            return null;
        }

        private void copyFileOrDir(String path) {
            AssetManager assetManager = mContext.getAssets();
            String assets[];
            try {
                Log.i("copyFileOrDir", "copyFileOrDir() -> " + path);
                assets = assetManager.list(path);
                if (assets.length == 0) {
                    copyFile(path);
                } else {
                    String fullPath = TARGET_BASE_PATH + path;
                    Log.i("copyFileOrDir", "Path = " + fullPath);
                    File dir = new File(fullPath);
                    if (!dir.exists() && !path.startsWith("images") &&
                            !path.startsWith("sounds") && !path.startsWith("webkit")) {
                        if (!dir.mkdirs()) {
                            Log.i("copyFileOrDir", "Could not create directory " + fullPath);
                        }
                    }
                    for (int i = 0; i < assets.length; ++i) {
                        String p;
                        if (path.equals("")) {
                            p = "";
                        } else {
                            p = path + "/";
                        }
                        if (!path.startsWith("images") &&
                                !path.startsWith("sounds") && !path.startsWith("webkit")) {
                            copyFileOrDir(p + assets[i]);
                        }
                    }
                    MoveWhateverIsActivated();
                }
            } catch (IOException e) {
                //
            }
        }

        private void copyFile(String filename) {
            AssetManager assetManager = mContext.getAssets();

            InputStream in;
            OutputStream out;
            String newFileName = null;
            try {
                Log.i("tag", "copyFile() " + filename);
                in = assetManager.open(filename);
                if (filename.endsWith(".jpg")) // .jpg used to avoid compression on APK file
                    newFileName = TARGET_BASE_PATH + filename.substring(0, filename.length() - 4);
                else
                    newFileName = TARGET_BASE_PATH + filename;
                out = new FileOutputStream(newFileName);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.flush();
                out.close();
            } catch (Exception e) {
                Log.d("tag", "Exception in copyFile() of " + newFileName);
                Log.d("tag", "Exception in copyFile() " + e.toString());
            }
        }

        private void MoveWhateverIsActivated() {
            if (!colorful_icon) {
                String source_colorful_du = mContext.getCacheDir().getAbsolutePath() +
                        "/creative_files/";
                String destination_colorful_du = mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/com.android.settings/res/drawable-v11/";
                String source_colorful_pn = mContext.getCacheDir().getAbsolutePath() +
                        "/creative_files/";
                String destination_colorful_pn = mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/com.android.settings/res/drawable-v11/";

                moveFile(source_colorful_du, "ic_dirtytweaks.xml", destination_colorful_du);
                moveFile(source_colorful_pn, "ic_settings_purenexus.xml", destination_colorful_pn);
            }
            if (category_title_bold || category_title_italics || category_title_caps) {
                createSettingsTitleXML("settings_title_style",
                        mContext.getCacheDir().getAbsolutePath() +
                                "/creative_mode/assets/overlays/com.android.settings/" +
                                "res/values-v11/");
            }

            if (dashboard_dividers) {
                String source = mContext.getCacheDir().getAbsolutePath() +
                        "/creative_files/";
                String destination = mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/com.android.settings/res/values-v11/";
                moveFile(source, "dashboard_dividers.xml", destination);
            }
            if (dirtytweaks_iconpresence) {
                String source = mContext.getCacheDir().getAbsolutePath() +
                        "/creative_files/";
                String destination = mContext.getCacheDir().getAbsolutePath() +
                        "/creative_mode/assets/overlays/com.android.settings/res/values-v11/";
                moveFile(source, "dirty_tweaks_icon_presence.xml", destination);
            }
            mProgressDialog.dismiss();
        }

        private void createSettingsTitleXML(String filename, String theme_destination) {

            File root = new File(
                    mContext.getCacheDir().getAbsolutePath() + "/" + filename + ".xml");

            String parseMe = "";
            String allCaps = ("        <item name=\"android:textAllCaps\">false</item>" + "\n");

            if (category_title_bold) {
                if (parseMe.length() == 0) {
                    parseMe = "bold";
                } else {
                    parseMe += "|bold";
                }
            }
            if (category_title_italics) {
                if (parseMe.length() == 0) {
                    parseMe = "italic";
                } else {
                    parseMe += "|italic";
                }
            }

            String boldItalics = ("        <item name=\"android:textStyle\">" +
                    parseMe + "</item>" + "\n");

            if (!category_title_bold && !category_title_italics) {
                boldItalics = "";
            }
            if (category_title_caps) {
                allCaps = ("        <item name=\"android:textAllCaps\">true</item>" + "\n");
            }

            try {
                root.createNewFile();
                FileWriter fw = new FileWriter(root);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter pw = new PrintWriter(bw);
                String xmlTags = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "\n");
                String xmlRes1 = ("<resources>" + "\n");
                String xmlRes2 = ("    <style name=\"TextAppearance.CategoryTitle\" " +
                        "parent=\"@android:style/TextAppearance.Material.Body2\">" + "\n");
                String xmlRes3 = ("        <item name=\"android:textColor\">" +
                        "?android:attr/colorAccent</item>" + "\n");
                String xmlRes5 = ("    </style>" + "\n");
                String xmlRes6 = ("</resources>");
                pw.write(xmlTags);
                pw.write(xmlRes1);
                pw.write(xmlRes2);
                pw.write(allCaps);
                pw.write(xmlRes3);
                pw.write(boldItalics);
                pw.write(xmlRes5);
                pw.write(xmlRes6);
                pw.close();
                bw.close();
                fw.close();
            } catch (IOException e) {
                //
            } finally {
                moveFile(
                        mContext.getCacheDir().getAbsolutePath() + "/",
                        filename + ".xml", theme_destination);
            }
        }

        private void moveFile(String current_source, String inputFile,
                              String theme_destination) {
            InputStream in;
            OutputStream out;
            try {
                //create output directory if it doesn't exist
                File dir = new File(theme_destination);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                in = new FileInputStream(current_source + inputFile);
                out = new FileOutputStream(theme_destination + inputFile);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();

                // write the output file
                out.flush();
                out.close();

                // delete the original file
                new File(current_source + inputFile).delete();

            } catch (FileNotFoundException f) {
                //
            } catch (Exception e) {
                //
            }
        }
    }
}