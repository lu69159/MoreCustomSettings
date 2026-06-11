package MCS.musicSquare.sources.musics;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.Http;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

import static mindustry.Vars.*;
import static MCS.main.*;

public abstract class musicBase {
    public String url;

    public musicBase(String url){
        this.url = url;
    }

    public String url(String name){
        try{
            return url + URLEncoder.encode(name, "UTF-8") + "&limit=10";
        }catch(UnsupportedEncodingException e){
            return url + name + "&limit=10";
        }
    }

    public Seq<Track> search(String name) {
        return new Seq<>();
    }

    public static class Track {
        public String url;
        public String pic;
        public String artist;
        public String name;

        public void download(Fi dir){
            if(url == null) return;

            Core.app.post(() -> ui.loadfrag.show(Core.bundle.get("musicSquare.downloading")));

            Http.get(url, res -> {
                try{
                    byte[] data = res.getResult();

                    String ext = "mp3";
                    String base = url.split("\\?")[0];
                    int dot = base.lastIndexOf('.');
                    if(dot > 0){
                        String e = base.substring(dot + 1).toLowerCase();
                        if(e.equals("ogg") || e.equals("mp3") || e.equals("flac") || e.equals("wav")){
                            ext = e;
                        }
                    }

                    String sanitized = (artist + "-" + name).replaceAll("[^-0-9a-zA-Z]", "");
                    if(sanitized.isEmpty()) sanitized = "track_" + System.nanoTime();

                    if(!dir.exists()) musicLoader.loadFolder();
                    dir.child(sanitized + "__" + data.length + "." + ext).writeBytes(data);

                    Core.app.post(() -> {
                        ui.loadfrag.hide();
                        musicLoader.load();
                        ui.showInfo("@musicSquare.downloaded");
                    });
                }catch(Throwable e){
                    Core.app.post(() -> {
                        ui.loadfrag.hide();
                        ui.showException(new Exception("Download failed", e));
                    });
                }
            }, error -> Core.app.post(() -> {
                ui.loadfrag.hide();
                ui.showException(new Exception("Download error: " + error));
            }));
        }
    }
}
