package com.example.photovibe;

import android.app.*;
import android.os.*;
import android.provider.MediaStore;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.database.Cursor;
import android.content.ContentValues;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1001;
    private ImageView preview;
    private Bitmap originalBitmap, filteredBitmap, exportBitmap;
    private String currentPreset = "원본";
    private int targetW = 0, targetH = 0;
    private Uri lastSavedUri;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 36, 28, 24);
        root.setBackgroundColor(Color.rgb(248,248,248));
        TextView title = tv("PhotoVibe", 28, true);
        TextView sub = tv("사진 선택 → 느낌 선택 → 사이즈 선택 → 저장/공유", 14, false);
        root.addView(title); root.addView(sub);
        Button pick = btn("사진 선택");
        pick.setOnClickListener(v -> pickImage());
        root.addView(pick);
        preview = new ImageView(this);
        preview.setBackgroundColor(Color.rgb(230,230,230));
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));
        HorizontalScrollView presetScroll = new HorizontalScrollView(this);
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        String[] presetNames = {"원본","아이폰 자연톤","갤럭시 선명톤","인스타 쨍한톤","시네마틱","따뜻한 필름","빈티지","흑백"};
        for (String p: presetNames) { Button b = smallBtn(p); b.setOnClickListener(v -> applyPreset(p)); presets.addView(b); }
        presetScroll.addView(presets); root.addView(presetScroll);
        HorizontalScrollView sizeScroll = new HorizontalScrollView(this);
        LinearLayout sizes = new LinearLayout(this);
        sizes.setOrientation(LinearLayout.HORIZONTAL);
        addSizeButton(sizes,"원본비율",0,0);
        addSizeButton(sizes,"인스타 정사각 1080",1080,1080);
        addSizeButton(sizes,"인스타 세로 1080x1350",1080,1350);
