package id.mas.agent;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.os.*;
import android.print.*;
import android.print.pdf.PrintedPdfDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;

final class VoucherPrinter {
    static void print(Activity activity,String router,JSONArray records) {
        PrintManager manager=(PrintManager)activity.getSystemService(Context.PRINT_SERVICE);
        manager.print("Kartu akses - "+router,new PrintDocumentAdapter(){
            PrintAttributes attributes;int pages;
            @Override public void onLayout(PrintAttributes old,PrintAttributes next,CancellationSignal signal,LayoutResultCallback callback,Bundle extras){attributes=next;pages=(records.length()+9)/10;if(signal.isCanceled()){callback.onLayoutCancelled();return;}callback.onLayoutFinished(new PrintDocumentInfo.Builder("Kartu-Mikrotik.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(pages).build(),true);}
            @Override public void onWrite(PageRange[] requested,ParcelFileDescriptor output,CancellationSignal signal,WriteResultCallback callback){
                PrintedPdfDocument document=new PrintedPdfDocument(activity,attributes);
                try{java.util.List<PageRange> written=new java.util.ArrayList<>();for(int page=0;page<pages;page++){
                    boolean include=false;for(PageRange range:requested)if(page>=range.getStart()&&page<=range.getEnd())include=true;if(!include)continue;
                    if(signal.isCanceled()){callback.onWriteCancelled();return;}PdfDocument.Page p=document.startPage(page);Canvas c=p.getCanvas();Rect area=p.getInfo().getContentRect();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);
                    float w=area.width()/2f,h=area.height()/5f;c.save();c.translate(area.left,area.top);
                    for(int slot=0;slot<10&&page*10+slot<records.length();slot++){JSONObject r=records.optJSONObject(page*10+slot);float x=(slot%2)*w,y=(slot/2)*h;paint.setStyle(Paint.Style.STROKE);c.drawRect(x+3,y+3,x+w-5,y+h-5,paint);paint.setStyle(Paint.Style.FILL);paint.setTextSize(10);
                        String[] lines={router,r.optString("kind","hotspot_users"),"User: "+r.optString("name"),"Pass: "+r.optString("password"),"Profil: "+r.optString("profile"),"Durasi: "+r.optString("uptime"),"Rp "+r.optLong("price_rupiah")};
                        for(int line=0;line<lines.length;line++){String text=lines[line];while(paint.measureText(text)>w-20&&text.length()>1)text=text.substring(0,text.length()-1);c.drawText(text,x+10,y+18+line*14,paint);}
                    }c.restore();document.finishPage(p);written.add(new PageRange(page,page));}
                    try(FileOutputStream out=new FileOutputStream(output.getFileDescriptor())){document.writeTo(out);}callback.onWriteFinished(written.toArray(new PageRange[0]));
                }catch(Exception e){callback.onWriteFailed("Gagal membuat PDF kartu.");}finally{document.close();}
            }
        },new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME).build());
    }
}
