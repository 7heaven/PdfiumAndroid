package com.sevenheaven.pdfdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;

import com.qozix.tileview.TileView;
import com.qozix.tileview.graphics.BitmapProvider;
import com.qozix.tileview.tiles.Tile;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    ParcelFileDescriptor mFileDescriptor;

    PdfiumCore pdfiumCore;
    PdfDocument pdfDocument;
    int pageIndex = -1;

    private String PDF_FILE_NAME = "test.pdf";

    private int pageWidth;
    private int pageHeight;

    TileView tileView;

    int[] pageHeights;

    Rect mTmpTextBound = new Rect();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        ImagePage imagePage = new ImagePage(this);
////        imagePage.setPrimaryPageImage(BitmapFactory.decodeResource(getResources(), R.drawable.page_1));
////        imagePage.setSecondaryPageImage(BitmapFactory.decodeResource(getResources(), R.drawable.page_2));
//        imagePage.setBackgroundResource(R.mipmap.ic_launcher);
//
//        setContentView(imagePage);
//
//
        tileView = new TileView(this);
        tileView.setScaleLimits(0, 4);
        tileView.setScale(0);
        tileView.defineBounds(0, 0, 1, 1);

        tileView.addDetailLevel(1.0f, "");
        tileView.addDetailLevel(1.5f, "");
        tileView.addDetailLevel(2.0f, "");
        tileView.addDetailLevel(3.0f, "");
        tileView.addDetailLevel(4.0f, "");

        DisplayMetrics dm = getResources().getDisplayMetrics();
        pageWidth = dm.widthPixels;
        pageHeight = dm.heightPixels;
        tileView.setSize(dm.widthPixels, dm.heightPixels);
        tileView.setShouldRenderWhilePanning(true);
        tileView.setShouldRecycleBitmaps(true);

        setContentView(tileView);


        try{
            File file = new File(getCacheDir(), PDF_FILE_NAME);
            copy(getAssets().open(PDF_FILE_NAME), file);

            mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
            pdfiumCore = new PdfiumCore(this);
            pdfDocument = pdfiumCore.newDocument(mFileDescriptor);
            final int count = pdfiumCore.getPageCount(pdfDocument);
            int maxWidth = 0;
            int totalHeight = 0;

            pageHeights = new int[count];
            for(int i = 0; i < count; i++){
                pdfiumCore.openPage(pdfDocument, i);

                int width = pdfiumCore.getPageWidthPoint(pdfDocument, i);
                int height = pdfiumCore.getPageHeightPoint(pdfDocument, i);

                float scale = (float) dm.widthPixels / width;

                int pageHeight = (int) (height * scale);
                totalHeight += pageHeight;
                pageHeights[i] = pageHeight;
            }

            tileView.setSize(dm.widthPixels, totalHeight);

//            imagePage.setPrimaryPageImage(getPage(0));
//            imagePage.setSecondaryPageImage(getPage(1));

        }catch(IOException e){
            e.printStackTrace();
        }

        tileView.setBitmapProvider(new BitmapProvider() {
            @Override
            public Bitmap getBitmap(Tile tile, Context context) {
                return getPageRegion(0, tile.getRelativeRect(), tile.getDetailLevel().getScale());
            }
        });
    }

    private Bitmap getPageRegion(int index, Rect region, float scale){

        synchronized (this){
            int realLeft = region.left;
            int realTop = region.top;

            while(realTop > pageHeights[index]){
                realTop -= pageHeights[index];

                index++;
            }

            Bitmap bitmap = Bitmap.createBitmap((int) (region.width() * scale), (int) (region.height() * scale), Bitmap.Config.ARGB_8888);

            if(pdfiumCore != null){
                if(pdfiumCore.getPageCount(pdfDocument) <= index){
                    return null;
                }

                if(pdfDocument == null || pageIndex != index){
                    pdfiumCore.openPage(pdfDocument, index);
                    pageIndex = index;
                }

                int width = pdfiumCore.getPageWidthPoint(pdfDocument, index);
                int height = pdfiumCore.getPageHeightPoint(pdfDocument, index);

                float ratio = (float) pageWidth / width;
                width *= ratio;
                height *= ratio;

//                float pageScale = (float) pageWidth / width;

//                Paint paint = new Paint();
//                paint.setColor(0xFFFFFFF);
//                Canvas canvas = new Canvas(bitmap);
//                canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
//
//                String text = "格上理财";

//                paint.setTextSize(bitmap.getHeight() * 0.2F);
//                paint.getTextBounds(text, 0, text.length(), mTmpTextBound);
//                paint.setColor(0xFFEEEEEE);
//                int textX = (bitmap.getWidth() - mTmpTextBound.width()) / 2;
//                int textY = (bitmap.getHeight() - mTmpTextBound.height()) / 2;
//                canvas.rotate(30, textX, textY);
//                canvas.drawText(text, textX, textY, paint);


                pdfiumCore.renderPageBitmap(pdfDocument, bitmap, index, (int) (-realLeft * scale), (int) (-realTop * scale), (int) (width * scale), (int) (height * scale));

            }

            return bitmap;
        }
    }

    public static void copy(InputStream inputStream, File output) throws IOException {
        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(output);
            boolean read = false;
            byte[] bytes = new byte[1024];

            int read1;
            while((read1 = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read1);
            }
        } finally {
            try {
                if(inputStream != null) {
                    inputStream.close();
                }
            } finally {
                if(outputStream != null) {
                    outputStream.close();

                }

            }

        }

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

//        if(mCurrentPage != null){
//            mCurrentPage.close();
//        }
//
//        if(mPdfRenderer != null){
//            mPdfRenderer.close();
//        }

        if(pdfiumCore != null){
            if(pdfDocument != null){
                pdfiumCore.closeDocument(pdfDocument);
            }
        }

        tileView.destroy();
    }
}
