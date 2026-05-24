package com.example.photovibe;

import android.app.Activity;
import android.os.*;
import android.provider.MediaStore;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.content.ContentValues;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1001;
    private ImageView preview;
    private Bitmap originalBitmap, filteredBitmap, exportBitmap;
    private int targetW = 0, targetH = 0;
    private Uri lastSavedUri;
    private Button selectedPresetBtn, selectedSizeBtn;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 40, 24, 24);
        root.setBackgroundColor(Color.rgb(18,18,18));

        TextView title = tv("📸 PhotoVibe", 26, true);
        title.setTextColor(Color.WHITE);
        TextView sub = tv("사진선택 → 프리셋 → 사이즈 → 저장", 13, false);
        sub.setTextColor(Color.rgb(180,180,180));
        root.addView(title); root.addView(sub);

        Button pick = btn("🖼 사진 선택");
        pick.setBackgroundColor(Color.rgb(255,100,100));
        pick.setTextColor(Color.WHITE);
        pick.setOnClickListener(v -> pickImage());
        root.addView(pick);

        preview = new ImageView(this);
        preview.setBackgroundColor(Color.rgb(40,40,40));
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(preview, new LinearLayout.LayoutParams(-1, 700));

        // 프리셋 섹션
        TextView presetTitle = tv("🎨 프리셋", 15, true);
        presetTitle.setTextColor(Color.rgb(255,180,100));
        root.addView(presetTitle);

        String[][] presets = {
            {"원본","아이폰 자연톤","갤럭시 선명톤","인스타 쨍한톤"},
            {"시네마틱","따뜻한 필름","빈티지","흑백"},
            {"Kodak Gold","Kodak Portra","Fuji Superia","Fuji Pro 400H"},
            {"페이디드","모리걸","도쿄 블루","시티팝 80s"},
            {"홍콩 느와르","Y2K","오펜하이머","듄"},
            {"매트릭스","라라랜드","황금시간대","새벽 감성"}
        };

        for (String[] row : presets) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (String p : row) {
                Button b = smallBtn(p);
                b.setBackgroundColor(Color.rgb(50,50,50));
                b.setTextColor(Color.WHITE);
                b.setOnClickListener(v -> {
                    if (selectedPresetBtn != null) selectedPresetBtn.setBackgroundColor(Color.rgb(50,50,50));
                    b.setBackgroundColor(Color.rgb(255,100,100));
                    selectedPresetBtn = b;
                    applyPreset(p);
                });
                rowLayout.addView(b, new LinearLayout.LayoutParams(0,-2,1));
            }
            root.addView(rowLayout);
        }

        // 사이즈 섹션
        TextView sizeTitle = tv("📐 사이즈", 15, true);
        sizeTitle.setTextColor(Color.rgb(100,200,255));
        root.addView(sizeTitle);

        String[][] sizes = {
            {"원본","인스타 1080","세로 1350"},
            {"릴스 1920","웹 720p","트위터 1200"}
        };
        int[][] dims = {{0,0},{1080,1080},{1080,1350},{1080,1920},{1280,720},{1200,675}};
        String[] sizeLabels = {"원본","인스타 1080","세로 1350","릴스 1920","웹 720p","트위터 1200"};

        LinearLayout sizeRow1 = new LinearLayout(this); sizeRow1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout sizeRow2 = new LinearLayout(this); sizeRow2.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < sizeLabels.length; i++) {
            final int w = dims[i][0], h = dims[i][1];
            final String label = sizeLabels[i];
            Button b = smallBtn(label);
            b.setBackgroundColor(Color.rgb(50,50,50));
            b.setTextColor(Color.WHITE);
            b.setOnClickListener(v -> {
                if (selectedSizeBtn != null) selectedSizeBtn.setBackgroundColor(Color.rgb(50,50,50));
                b.setBackgroundColor(Color.rgb(100,200,255));
                selectedSizeBtn = b;
                targetW=w; targetH=h; renderExport();
            });
            if (i < 3) sizeRow1.addView(b, new LinearLayout.LayoutParams(0,-2,1));
            else sizeRow2.addView(b, new LinearLayout.LayoutParams(0,-2,1));
        }
        root.addView(sizeRow1); root.addView(sizeRow2);

        // 저장/공유 버튼
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0,16,0,0);
        Button save = btn("💾 저장"); save.setBackgroundColor(Color.rgb(50,200,100)); save.setTextColor(Color.WHITE); save.setOnClickListener(v -> saveToGallery());
        Button kakao = btn("💬 카카오"); kakao.setBackgroundColor(Color.rgb(255,220,0)); kakao.setTextColor(Color.BLACK); kakao.setOnClickListener(v -> shareTo("com.kakao.talk"));
        Button insta = btn("📷 인스타"); insta.setBackgroundColor(Color.rgb(200,50,150)); insta.setTextColor(Color.WHITE); insta.setOnClickListener(v -> shareTo("com.instagram.android"));
        actions.addView(save, new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(kakao, new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(insta, new LinearLayout.LayoutParams(0,-2,1));
        root.addView(actions);

        scroll.addView(root);
        setContentView(scroll);
    }

    private TextView tv(String s, int sp, boolean bold) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(sp);
        t.setTextColor(Color.WHITE);
        if(bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0,8,0,4); return t;
    }
    private Button btn(String s) { Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setPadding(8,8,8,8); return b; }
    private Button smallBtn(String s) { Button b=btn(s); b.setTextSize(11); return b; }

    private void pickImage() {
        Intent i=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*"); startActivityForResult(i, PICK_IMAGE);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req,res,data);
        if(req==PICK_IMAGE&&res==RESULT_OK&&data!=null) {
            try { originalBitmap=MediaStore.Images.Media.getBitmap(getContentResolver(),data.getData()); applyPreset("원본"); }
            catch(Exception e) { toast("오류: "+e.getMessage()); }
        }
    }

    private void applyPreset(String p) {
        if(originalBitmap==null){ toast("사진을 먼저 선택하세요"); return; }
        ColorMatrix cm=new ColorMatrix();
        switch(p) {
            case "아이폰 자연톤": cm.setSaturation(1.05f); filteredBitmap=adj(originalBitmap,cm,1.03f,5); break;
            case "갤럭시 선명톤": cm.setSaturation(1.25f); filteredBitmap=adj(originalBitmap,cm,1.12f,8); break;
            case "인스타 쨍한톤": cm.setSaturation(1.35f); filteredBitmap=adj(originalBitmap,cm,1.18f,12); break;
            case "시네마틱": filteredBitmap=cinematic(originalBitmap); break;
            case "따뜻한 필름": filteredBitmap=warmFilm(originalBitmap); break;
            case "빈티지": filteredBitmap=sepia(originalBitmap); break;
            case "흑백": cm.setSaturation(0f); filteredBitmap=adj(originalBitmap,cm,1.05f,0); break;
            case "Kodak Gold": filteredBitmap=kodakGold(originalBitmap); break;
            case "Kodak Portra": filteredBitmap=kodakPortra(originalBitmap); break;
            case "Fuji Superia": filteredBitmap=fujiSuperia(originalBitmap); break;
            case "Fuji Pro 400H": filteredBitmap=fujiPro(originalBitmap); break;
            case "페이디드": filteredBitmap=faded(originalBitmap); break;
            case "모리걸": filteredBitmap=mori(originalBitmap); break;
            case "도쿄 블루": filteredBitmap=tokyoBlue(originalBitmap); break;
            case "시티팝 80s": filteredBitmap=cityPop(originalBitmap); break;
            case "홍콩 느와르": filteredBitmap=hongkong(originalBitmap); break;
            case "Y2K": filteredBitmap=y2k(originalBitmap); break;
            case "오펜하이머": filteredBitmap=oppenheimer(originalBitmap); break;
            case "듄": filteredBitmap=dune(originalBitmap); break;
            case "매트릭스": filteredBitmap=matrix(originalBitmap); break;
            case "라라랜드": filteredBitmap=lalaland(originalBitmap); break;
            case "황금시간대": filteredBitmap=goldenHour(originalBitmap); break;
            case "새벽 감성": filteredBitmap=dawn(originalBitmap); break;
            default: filteredBitmap=originalBitmap.copy(Bitmap.Config.ARGB_8888,true);
        }
        renderExport();
    }

    private Bitmap adj(Bitmap src, ColorMatrix cm, float c, float b) {
        Bitmap out=Bitmap.createBitmap(src.getWidth(),src.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas cv=new Canvas(out); Paint pt=new Paint(Paint.ANTI_ALIAS_FLAG);
        ColorMatrix cb=new ColorMatrix(new float[]{c,0,0,0,b,0,c,0,0,b,0,0,c,0,b,0,0,0,1,0});
        cm.postConcat(cb); pt.setColorFilter(new ColorMatrixColorFilter(cm)); cv.drawBitmap(src,0,0,pt); return out;
    }
    private Bitmap overlay(Bitmap src, int color, int alpha) {
        Bitmap out=src.copy(Bitmap.Config.ARGB_8888,true);
        Canvas cv=new Canvas(out); Paint pt=new Paint();
        pt.setColor(Color.argb(alpha,Color.red(color),Color.green(color),Color.blue(color)));
        cv.drawRect(0,0,out.getWidth(),out.getHeight(),pt); return out;
    }
    private Bitmap sepia(Bitmap src) {
        return adj(src,new ColorMatrix(new float[]{0.393f,0.769f,0.189f,0,0,0.349f,0.686f,0.168f,0,0,0.272f,0.534f,0.131f,0,0,0,0,0,1,0}),1.05f,4);
    }
    private Bitmap warmFilm(Bitmap src) {
        Bitmap b=adj(src,new ColorMatrix(),1.06f,6);
        return overlay(b,Color.rgb(255,180,80),24);
    }
    private Bitmap cinematic(Bitmap src) {
        Bitmap b=adj(src,new ColorMatrix(),1.15f,-8);
        return overlay(b,Color.rgb(0,80,110),28);
    }
    private Bitmap kodakGold(Bitmap src) {
        Bitmap b=adj(src,new ColorMatrix(),1.08f,10);
        return overlay(b,Color.rgb(255,200,50),30);
    }
    private Bitmap kodakPortra(Bitmap src) {
        ColorMatrix cm=new ColorMatrix(); cm.setSaturation(0.9f);
        Bitmap b=adj(src,cm,1.05f,8);
        return overlay(b,Color.rgb(255,220,180),20);
    }
    private Bitmap fujiSuperia(Bitmap src) {
        ColorMatrix cm=new ColorMatrix(); cm.setSaturation(1.1f);
        Bitmap b=adj(src,cm,1.05f,5);
        return overlay(b,Color.rgb(100,180,100),18);
    }
    private Bitmap fujiPro(Bitmap src) {
        ColorMatrix cm=new ColorMatrix(); cm.setSaturation(0.85f);
        Bitmap b=adj(src,cm,0.95f,15);
        return overlay(b,Color.rgb(200,230,255),22);
    }
    private Bitmap faded(Bitmap src) {
        ColorMatrix cm=new ColorMatrix(); cm.setSaturation(0.7f);
        return adj(src,cm,0.85f,25);
    }
    private Bitmap mori(Bitmap src) {
        ColorMatrix cm=new ColorMatrix(); cm.setSaturation(0.8f);
        Bitmap b=adj(src,cm,0.9f,30);
        return overlay(b,Color.rgb(255,255,240),35);
    }
    private Bitmap tokyoBlue(Bitmap src) {
        ColorMatrix cm=new ColorMatrix(); cm.setSaturation(0.9f);
        Bitmap b=adj(src,cm,1.1f,-5);
        return overlay(b,Color.rgb(50,80,180),30);
    }
    private Bitmap cityPop(Bitmap src) {
        Bitmap b=adj(src,new ColorMatrix(),1.1f,5);
        return overlay(b,Color.rgb(180,80,200),28);
    }
    private Bitmap hongkong(Bitmap src) {
        Bitmap b=adj(src,new ColorMatrix(),1.2f,-15);
        return overlay(b,Color.rgb(180,30,30),25);
    }
    private Bitmap y2k(Bitmap src) {
        ColorMatrix cm=new ColorMatrix(); cm.setSaturation(1.4f);
        return adj(src,cm,1.15f,15);
    }
    private Bitmap oppenheimer(Bitmap src) {
        Bitmap b=adj(src,new ColorMatrix(),1.3f,-10);
        b=overlay(b,Color.rgb(255,120,0),30);
        return overlay(b,Color.rgb(0,100,120),20);
    }
    private Bitmap dune(Bitmap src) {
        ColorMatrix cm=new ColorMatrix(); cm.setSaturation(0.8f);
        Bitmap b=adj(src,cm,1.1f,8);
        return overlay(b,Color.rgb(220,180,100),35);
    }
    private Bitmap matrix(Bitmap src) {
        ColorMatrix cm=new ColorMatrix(); cm.setSaturation(0.3f);
        Bitmap b=adj(src,cm,1.1f,-10);
        return overlay(b,Color.rgb(0,180,50),35);
    }
    private Bitmap lalaland(Bitmap src) {
        ColorMatrix cm=new ColorMatrix(); cm.setSaturation(1.1f);
        Bitmap b=adj(src,cm,1.0f,10);
        return overlay(b,Color.rgb(255,180,220),25);
    }
    private Bitmap goldenHour(Bitmap src) {
        Bitmap b=adj(src,new ColorMatrix(),1.1f,12);
        return overlay(b,Color.rgb(255,160,30),40);
    }
    private Bitmap dawn(Bitmap src) {
        ColorMatrix cm=new ColorMatrix(); cm.setSaturation(0.8f);
        Bitmap b=adj(src,cm,0.95f,-5);
        return overlay(b,Color.rgb(100,130,200),35);
    }

    private void renderExport() {
        if(filteredBitmap==null) return;
        exportBitmap=(targetW<=0)?filteredBitmap:crop(filteredBitmap,targetW,targetH);
        preview.setImageBitmap(exportBitmap);
    }
    private Bitmap crop(Bitmap src, int w, int h) {
        Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas cv=new Canvas(out); cv.drawColor(Color.WHITE);
        float sc=Math.max(w/(float)src.getWidth(),h/(float)src.getHeight());
        RectF dst=new RectF((w-src.getWidth()*sc)/2f,(h-src.getHeight()*sc)/2f,(w+src.getWidth()*sc)/2f,(h+src.getHeight()*sc)/2f);
        cv.drawBitmap(src,null,dst,new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG)); return out;
    }
    private void saveToGallery() {
        if(exportBitmap==null){ toast("저장할 사진 없음"); return; }
        try {
            String name="PhotoVibe_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.KOREA).format(new Date())+".jpg";
            ContentValues cv=new ContentValues();
            cv.put(MediaStore.Images.Media.DISPLAY_NAME,name);
            cv.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
            if(Build.VERSION.SDK_INT>=29) cv.put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/PhotoVibe");
            Uri uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);
            OutputStream os=getContentResolver().openOutputStream(uri);
            exportBitmap.compress(Bitmap.CompressFormat.JPEG,95,os); os.close();
            lastSavedUri=uri; toast("✅ 저장완료!");
        } catch(Exception e){ toast("저장실패: "+e.getMessage()); }
    }
    private void shareTo(String pkg) {
        if(exportBitmap==null){ toast("공유할 사진 없음"); return; }
        if(lastSavedUri==null) saveToGallery();
        Intent send=new Intent(Intent.ACTION_SEND);
        send.setType("image/jpeg"); send.putExtra(Intent.EXTRA_STREAM,lastSavedUri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setPackage(pkg);
        try{ startActivity(send); }
        catch(Exception e){ send.setPackage(null); startActivity(Intent.createChooser(send,"공유")); }
    }
    private void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }
}
