package MCS.musicSquare.sources.musics;

import MCS.customMusic.CustomSoundControl;
import arc.*;
import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import java.net.*;
import java.io.*;

import static MCS.customMusic.MusicTools.*;
import static mindustry.Vars.*;

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

    public static boolean isSafeUrl(String url){
        if(url == null || url.isEmpty()) return false;
        try{
            var parsed = new URL(url);
            String protocol = parsed.getProtocol();
            return "http".equals(protocol) || "https".equals(protocol);
        }catch(Exception e){
            return false;
        }
    }

    public static class Track {
        public String url;
        public String pic;
        public String artist;
        public String name;

        public void downloadToTmp(Cons<Fi> done){
            if(!isSafeUrl(url)) ui.showException(new Throwable("Not found usable url."));

            var sound = (CustomSoundControl)control.sound;
            ui.loadfrag.show("[accent]" + Core.bundle.get("musicSquare.downloading"));
            Http.get(url, res -> {
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

                String sanitized = encodeString(artist + " - " + name);
                var tmpMusic = sound.musicLoader.tmp.child(sanitized + "__" + data.length + "." + ext);
                tmpMusic.writeBytes(data);
                done.get(tmpMusic);
            }, error -> Core.app.post(() -> {
                ui.loadfrag.hide();
                ui.showException(new Exception("Download error: " + error));
            }));
        }

        public void download(String name){
            downloadToTmp(fi -> {
                var sound = (CustomSoundControl)control.sound;
                sound.musicLoader.moveMusic(fi, name, false, true);
                Core.app.post(() -> {
                    ui.loadfrag.hide();
                    sound.musicLoader.reload(name);
                    ui.showInfo("@musicSquare.downloaded");
                });
            });
        }
    }
}
