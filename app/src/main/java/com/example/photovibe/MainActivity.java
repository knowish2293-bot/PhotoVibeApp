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
        TextView sub = tv("사진선택→느낌선택→사이즈선택→저장/공유", 14, false);
        root.addView(title); root.addView(sub);
        Button pick = btn("사진 선택");
        pick.setOnClickListener(v -> pickImage());
        root.addView(pick);
        preview = new ImageView(this);
        preview.setBackgroundColor(Color.rgb(230,230,230));
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));
        HorizontalScrollView ps = new HorizontalScrollView(this);
        LinearLayout pl = new LinearLayout(this);
        pl.setOrientation(LinearLayout.HORIZONTAL);
        for (String p: new String[]{"원본","아이폰","갤럭시","인스타","시네마틱","필름","빈티지","흑백"}) {
            Button b = smallBtn(p); b.setOnClickListener(v -> applyPreset(p)); pl.addView(b);
        }
        ps.addView(pl); root.addView(ps);
        HorizontalScrollView ss = new HorizontalScrollView(this);
        LinearLayout sl = new LinearLayout(this);
        sl.setOrientation(LinearLayout.HORIZONTAL);
        addSize(sl,"원본",0,0); addSize(sl,"인스타1080",1080,1080);
        addSize(sl,"세로1350",1080,1350); addSize(sl,"릴스1920",1080,1920);
        addSize(sl,"웹720",1280,720);
        ss.addView(sl); root.addView(ss);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button save = btn("저장"); save.setOnClickListener(v -> saveToGallery());
        Button kakao = btn("카카오"); kakao.setOnClickListener(v -> shareTo("com.kakao.talk"));
        Button insta = btn("인스타"); insta.setOnClickListener(v -> shareTo("com.instagram.android"));
        actions.addView(save, new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(kakao, new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(insta, new LinearLayout.LayoutParams(0,-2,1));
        root.addView(actions);
        setContentView(root);
    }

    private TextView tv(String s, int sp, boolean bold) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(sp);
        t.setTextColor(Color.rgb(20,20,20));
        if(bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0,4,0,8); return t;
    }
    private Button btn(String s) { Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }
    private Button smallBtn(String s) { Button b=btn(s); b.setTextSize(13); return b; }
    private void addSize(LinearLayout p, String label, int w, int h) {
        Button b=smallBtn(label);
        b.setOnClickListener(v->{ targetW=w; targetH=h; renderExport(); toast(label); });
        p.addView(b);
    }
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
            case "아이폰": cm.setSaturation(1.05f); filteredBitmap=adj(originalBitmap,cm,1.03f,5); break;
            case "갤럭시": cm.setSaturation(1.25f); filteredBitmap=adj(originalBitmap,cm,1.12f,8); break;
            case "인스타": cm.setSaturation(1.35f); filteredBitmap=adj(originalBitmap,cm,1.18f,12); break;
            case "시네마틱": filteredBitmap=cinematic(originalBitmap); break;
            case "필름": filteredBitmap=warmFilm(originalBitmap); break;
            case "빈티지": filteredBitmap=sepia(originalBitmap); break;
            case "흑백": cm.setSaturation(0f); filteredBitmap=adj(originalBitmap,cm,1.05f,0); break;
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
    private Bitmap sepia(Bitmap src) {
        return adj(src,new ColorMatrix(new float[]{0.393f,0.769f,0.189f,0,0,0.349f,0.686f,0.168f,0,0,0.272f,0.534f,0.131f,0,0,0,0,0,1,0}),1.05f,4);
    }
    private Bitmap warmFilm(Bitmap src) {
        Bitmap out=adj(src,new ColorMatrix(),1.06f,6);
        Canvas cv=new Canvas(out); Paint pt=new Paint();
        pt.setColor(Color.argb(24,255,180,80)); cv.drawRect(0,0,out.getWidth(),out.getHeight(),pt); return out;
    }
    private Bitmap cinematic(Bitmap src) {
        Bitmap out=adj(src,new ColorMatrix(),1.15f,-8);
        Canvas cv=new Canvas(out); Paint pt=new Paint();
        pt.setColor(Color.argb(28,0,80,110)); cv.drawRect(0,0,out.getWidth(),out.getHeight(),pt); return out;
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
            lastSavedUri=uri; toast("저장완료");
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
