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
    private String currentPreset = "원본";

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
        TextView title = tv("PhotoVibe", 26, true);
        title.setTextColor(Color.WHITE);
        root.addView(title);
        Button pick = btn("사진 선택");
        pick.setBackgroundColor(Color.rgb(255,100,100));
        pick.setTextColor(Color.WHITE);
        pick.setOnClickListener(v -> pickImage());
        root.addView(pick);
        preview = new ImageView(this);
        preview.setBackgroundColor(Color.rgb(40,40,40));
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(preview, new LinearLayout.LayoutParams(-1, 600));
        Button compare = btn("전후 비교 (길게 누르기)");
        compare.setBackgroundColor(Color.rgb(70,70,70));
        compare.setTextColor(Color.WHITE);
        compare.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) { if (originalBitmap != null) preview.setImageBitmap(originalBitmap); }
            else if (e.getAction() == MotionEvent.ACTION_UP) { if (exportBitmap != null) preview.setImageBitmap(exportBitmap); }
            return true;
        });
        root.addView(compare);
        root.addView(sectionTitle("회전"));
        LinearLayout rotRow = new LinearLayout(this);
        rotRow.setOrientation(LinearLayout.HORIZONTAL);
        Button rl = smallBtn("왼쪽 90"); rl.setOnClickListener(v -> rotate(-90)); rotRow.addView(rl, lp());
        Button rr = smallBtn("오른쪽 90"); rr.setOnClickListener(v -> rotate(90)); rotRow.addView(rr, lp());
        Button rf = smallBtn("좌우반전"); rf.setOnClickListener(v -> flipH()); rotRow.addView(rf, lp());
        root.addView(rotRow);
        root.addView(sectionTitle("자르기"));
        LinearLayout cropRow = new LinearLayout(this);
        cropRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] cl = {"1:1","4:3","16:9","3:4","9:16"};
        float[] cw = {1,4,16,3,9}, ch = {1,3,9,4,16};
        for (int i=0;i<cl.length;i++) { final float w=cw[i],h=ch[i]; Button b=smallBtn(cl[i]); b.setOnClickListener(v->cropR(w,h)); cropRow.addView(b,lp()); }
        root.addView(cropRow);
        root.addView(sectionTitle("프리셋"));
        String[][] presets = {
            {"원본","아이폰","갤럭시","인스타"},
            {"시네마틱","따뜻한필름","빈티지","흑백"},
            {"Kodak Gold","Kodak Portra","Fuji Super","Fuji Pro"},
            {"페이디드","모리걸","도쿄블루","시티팝"},
            {"홍콩느와르","Y2K","오펜하이머","듄"},
            {"매트릭스","라라랜드","황금시간","새벽감성"},
            {"VSCO M5","VSCO F2","VSCO G3","VSCO C1"},
            {"소니SLog","소니비비드","풍경","야경"}
        };
        for (String[] row : presets) {
            LinearLayout rl2 = new LinearLayout(this); rl2.setOrientation(LinearLayout.HORIZONTAL);
            for (String p : row) {
                Button b = smallBtn(p); b.setBackgroundColor(Color.rgb(50,50,50)); b.setTextColor(Color.WHITE);
                b.setOnClickListener(v -> { if(selectedPresetBtn!=null) selectedPresetBtn.setBackgroundColor(Color.rgb(50,50,50)); b.setBackgroundColor(Color.rgb(255,100,100)); selectedPresetBtn=b; currentPreset=p; applyPreset(p); });
                rl2.addView(b, lp());
            }
            root.addView(rl2);
        }
        root.addView(sectionTitle("사이즈"));
        int[][] dims = {{0,0},{1080,1080},{1080,1350},{1080,1920},{1280,720},{1200,675}};
        String[] sl = {"원본","인스타","세로1350","릴스","웹720","트위터"};
        LinearLayout sr1 = new LinearLayout(this); sr1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout sr2 = new LinearLayout(this); sr2.setOrientation(LinearLayout.HORIZONTAL);
        for (int i=0;i<sl.length;i++) { final int w=dims[i][0],h=dims[i][1]; Button b=smallBtn(sl[i]); b.setBackgroundColor(Color.rgb(50,50,50)); b.setTextColor(Color.WHITE); b.setOnClickListener(v->{ if(selectedSizeBtn!=null) selectedSizeBtn.setBackgroundColor(Color.rgb(50,50,50)); b.setBackgroundColor(Color.rgb(100,200,255)); selectedSizeBtn=b; targetW=w; targetH=h; renderExport(); }); if(i<3) sr1.addView(b,lp()); else sr2.addView(b,lp()); }
        root.addView(sr1); root.addView(sr2);
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL); actions.setPadding(0,16,0,0);
        Button save=btn("저장"); save.setBackgroundColor(Color.rgb(50,200,100)); save.setTextColor(Color.WHITE); save.setOnClickListener(v->saveToGallery());
        Button kakao=btn("카카오"); kakao.setBackgroundColor(Color.rgb(255,220,0)); kakao.setTextColor(Color.BLACK); kakao.setOnClickListener(v->shareTo("com.kakao.talk"));
        Button insta=btn("인스타"); insta.setBackgroundColor(Color.rgb(200,50,150)); insta.setTextColor(Color.WHITE); insta.setOnClickListener(v->shareTo("com.instagram.android"));
        actions.addView(save,lp()); actions.addView(kakao,lp()); actions.addView(insta,lp());
        root.addView(actions);
        scroll.addView(root); setContentView(scroll);
    }

    private TextView sectionTitle(String s) { TextView t=tv(s,14,true); t.setTextColor(Color.rgb(255,180,100)); t.setPadding(0,12,0,4); return t; }
    private LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(0,-2,1); }
    private TextView tv(String s,int sp,boolean bold) { TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.WHITE); if(bold) t.setTypeface(Typeface.DEFAULT_BOLD); t.setPadding(0,4,0,4); return t; }
    private Button btn(String s) { Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }
    private Button smallBtn(String s) { Button b=btn(s); b.setTextSize(11); b.setBackgroundColor(Color.rgb(60,60,60)); b.setTextColor(Color.WHITE); return b; }

    private void rotate(int deg) {
        if(originalBitmap==null){toast("사진 먼저 선택");return;}
        Matrix m=new Matrix(); m.postRotate(deg);
        originalBitmap=Bitmap.createBitmap(originalBitmap,0,0,originalBitmap.getWidth(),originalBitmap.getHeight(),m,true);
        applyPreset(currentPreset);
    }
    private void flipH() {
        if(originalBitmap==null){toast("사진 먼저 선택");return;}
        Matrix m=new Matrix(); m.preScale(-1,1);
        originalBitmap=Bitmap.createBitmap(originalBitmap,0,0,originalBitmap.getWidth(),originalBitmap.getHeight(),m,false);
        applyPreset(currentPreset);
    }
    private void cropR(float rw,float rh) {
        if(originalBitmap==null){toast("사진 먼저 선택");return;}
        int sw=originalBitmap.getWidth(),sh=originalBitmap.getHeight(),cw,ch;
        if(sw/rw>sh/rh){ch=sh;cw=(int)(sh*rw/rh);}else{cw=sw;ch=(int)(sw*rh/rw);}
        originalBitmap=Bitmap.createBitmap(originalBitmap,(sw-cw)/2,(sh-ch)/2,cw,ch);
        applyPreset(currentPreset); toast((int)rw+":"+(int)rh+" 완료");
    }
    private void pickImage() { Intent i=new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI); i.setType("image/*"); startActivityForResult(i,PICK_IMAGE); }
    @Override protected void onActivityResult(int req,int res,Intent data) {
        super.onActivityResult(req,res,data);
        if(req==PICK_IMAGE&&res==RESULT_OK&&data!=null) {
            try { originalBitmap=MediaStore.Images.Media.getBitmap(getContentResolver(),data.getData()); filteredBitmap=null; exportBitmap=null; lastSavedUri=null; if(selectedPresetBtn!=null) selectedPresetBtn.setBackgroundColor(Color.rgb(50,50,50)); selectedPresetBtn=null; currentPreset="원본"; applyPreset("원본"); }
            catch(Exception e){toast("오류:"+e.getMessage());}
        }
    }
    private void applyPreset(String p) {
        if(originalBitmap==null){toast("사진 먼저 선택");return;}
        ColorMatrix cm=new ColorMatrix();
        switch(p) {
            case "아이폰": cm.setSaturation(1.05f); filteredBitmap=adj(originalBitmap,cm,1.03f,5); break;
            case "갤럭시": cm.setSaturation(1.25f); filteredBitmap=adj(originalBitmap,cm,1.12f,8); break;
            case "인스타": cm.setSaturation(1.35f); filteredBitmap=adj(originalBitmap,cm,1.18f,12); break;
            case "시네마틱": filteredBitmap=overlay(adj(originalBitmap,new ColorMatrix(),1.15f,-8),Color.rgb(0,80,110),28); break;
            case "따뜻한필름": filteredBitmap=overlay(adj(originalBitmap,new ColorMatrix(),1.06f,6),Color.rgb(255,180,80),24); break;
            case "빈티지": filteredBitmap=adj(originalBitmap,new ColorMatrix(new float[]{0.393f,0.769f,0.189f,0,0,0.349f,0.686f,0.168f,0,0,0.272f,0.534f,0.131f,0,0,0,0,0,1,0}),1.05f,4); break;
            case "흑백": cm.setSaturation(0f); filteredBitmap=adj(originalBitmap,cm,1.05f,0); break;
            case "Kodak Gold": filteredBitmap=overlay(adj(originalBitmap,new ColorMatrix(),1.08f,10),Color.rgb(255,200,50),30); break;
            case "Kodak Portra": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(0.9f); filteredBitmap=overlay(adj(originalBitmap,c2,1.05f,8),Color.rgb(255,220,180),20); break; }
            case "Fuji Super": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(1.1f); filteredBitmap=overlay(adj(originalBitmap,c2,1.05f,5),Color.rgb(100,180,100),18); break; }
            case "Fuji Pro": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(0.85f); filteredBitmap=overlay(adj(originalBitmap,c2,0.95f,15),Color.rgb(200,230,255),22); break; }
            case "페이디드": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(0.7f); filteredBitmap=adj(originalBitmap,c2,0.85f,25); break; }
            case "모리걸": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(0.8f); filteredBitmap=overlay(adj(originalBitmap,c2,0.9f,30),Color.rgb(255,255,240),35); break; }
            case "도쿄블루": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(0.9f); filteredBitmap=overlay(adj(originalBitmap,c2,1.1f,-5),Color.rgb(50,80,180),30); break; }
            case "시티팝": filteredBitmap=overlay(adj(originalBitmap,new ColorMatrix(),1.1f,5),Color.rgb(180,80,200),28); break;
            case "홍콩느와르": filteredBitmap=overlay(adj(originalBitmap,new ColorMatrix(),1.2f,-15),Color.rgb(180,30,30),25); break;
            case "Y2K": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(1.4f); filteredBitmap=adj(originalBitmap,c2,1.15f,15); break; }
            case "오펜하이머": filteredBitmap=overlay(overlay(adj(originalBitmap,new ColorMatrix(),1.3f,-10),Color.rgb(255,120,0),30),Color.rgb(0,100,120),20); break;
            case "듄": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(0.8f); filteredBitmap=overlay(adj(originalBitmap,c2,1.1f,8),Color.rgb(220,180,100),35); break; }
            case "매트릭스": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(0.3f); filteredBitmap=overlay(adj(originalBitmap,c2,1.1f,-10),Color.rgb(0,180,50),35); break; }
            case "라라랜드": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(1.1f); filteredBitmap=overlay(adj(originalBitmap,c2,1.0f,10),Color.rgb(255,180,220),25); break; }
            case "황금시간": filteredBitmap=overlay(adj(originalBitmap,new ColorMatrix(),1.1f,12),Color.rgb(255,160,30),40); break;
            case "새벽감성": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(0.8f); filteredBitmap=overlay(adj(originalBitmap,c2,0.95f,-5),Color.rgb(100,130,200),35); break; }
            case "VSCO M5": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(0.85f); filteredBitmap=overlay(adj(originalBitmap,c2,1.05f,12),Color.rgb(200,150,100),22); break; }
            case "VSCO F2": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(0.75f); filteredBitmap=overlay(adj(originalBitmap,c2,0.9f,20),Color.rgb(255,240,220),30); break; }
            case "VSCO G3": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(1.05f); filteredBitmap=overlay(adj(originalBitmap,c2,1.02f,5),Color.rgb(120,160,100),15); break; }
            case "VSCO C1": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(0.9f); filteredBitmap=overlay(adj(originalBitmap,c2,1.05f,8),Color.rgb(180,200,220),18); break; }
            case "소니SLog": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(0.7f); filteredBitmap=adj(originalBitmap,c2,0.85f,30); break; }
            case "소니비비드": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(1.4f); filteredBitmap=adj(originalBitmap,c2,1.15f,5); break; }
            case "풍경": { ColorMatrix c2=new ColorMatrix(); c2.setSaturation(1.3f); filteredBitmap=overlay(adj(originalBitmap,c2,1.1f,3),Color.rgb(50,120,200),15); break; }
            case "야경": filteredBitmap=overlay(adj(originalBitmap,new ColorMatrix(),1.2f,-20),Color.rgb(80,0,120),25); break;
            default: filteredBitmap=originalBitmap.copy(Bitmap.Config.ARGB_8888,true);
        }
        renderExport();
    }
    private Bitmap adj(Bitmap src,ColorMatrix cm,float c,float b) {
        Bitmap out=Bitmap.createBitmap(src.getWidth(),src.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas cv=new Canvas(out); Paint pt=new Paint(Paint.ANTI_ALIAS_FLAG);
        ColorMatrix cb=new ColorMatrix(new float[]{c,0,0,0,b,0,c,0,0,b,0,0,c,0,b,0,0,0,1,0});
        cm.postConcat(cb); pt.setColorFilter(new ColorMatrixColorFilter(cm)); cv.drawBitmap(src,0,0,pt); return out;
    }
    private Bitmap overlay(Bitmap src,int color,int alpha) {
        Bitmap out=src.copy(Bitmap.Config.ARGB_8888,true); Canvas cv=new Canvas(out); Paint pt=new Paint();
        pt.setColor(Color.argb(alpha,Color.red(color),Color.green(color),Color.blue(color)));
        cv.drawRect(0,0,out.getWidth(),out.getHeight(),pt); return out;
    }
    private void renderExport() {
        if(filteredBitmap==null) return;
        exportBitmap=(targetW<=0)?filteredBitmap:cropBitmap(filteredBitmap,targetW,targetH);
        preview.setImageBitmap(exportBitmap);
    }
    private Bitmap cropBitmap(Bitmap src,int w,int h) {
        Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); Canvas cv=new Canvas(out); cv.drawColor(Color.WHITE);
        float sc=Math.max(w/(float)src.getWidth(),h/(float)src.getHeight());
        RectF dst=new RectF((w-src.getWidth()*sc)/2f,(h-src.getHeight()*sc)/2f,(w+src.getWidth()*sc)/2f,(h+src.getHeight()*sc)/2f);
        cv.drawBitmap(src,null,dst,new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG)); return out;
    }
    private void saveToGallery() {
        if(exportBitmap==null){toast("저장할 사진 없음");return;}
        try {
            String name="PhotoVibe_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.KOREA).format(new Date())+".jpg";
            ContentValues cv=new ContentValues();
            cv.put(MediaStore.Images.Media.DISPLAY_NAME,name);
            cv.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
            if(Build.VERSION.SDK_INT>=29) cv.put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/PhotoVibe");
            Uri uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);
            OutputStream os=getContentResolver().openOutputStream(uri);
            exportBitmap.compress(Bitmap.CompressFormat.JPEG,95,os); os.close();
            lastSavedUri=uri; toast("저장완료!");
        } catch(Exception e){toast("저장실패:"+e.getMessage());}
    }
    private void shareTo(String pkg) {
        if(exportBitmap==null){toast("공유할 사진 없음");return;}
        lastSavedUri=null; saveToGallery();
        if(lastSavedUri==null){toast("저장 후 다시 시도");return;}
        Intent send=new Intent(Intent.ACTION_SEND);
        send.setType("image/jpeg"); send.putExtra(Intent.EXTRA_STREAM,lastSavedUri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); send.setPackage(pkg);
        try{startActivity(send);}catch(Exception e){send.setPackage(null);startActivity(Intent.createChooser(send,"공유"));}
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
                                               }
