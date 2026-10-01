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


        private static String extension(byte[] d){ //1
            if(d.length < 12) return "";
            int b0 = d[0] & 0xFF, b1 = d[1] & 0xFF;
<<<<<<< HEAD
            if(b0 == 'I' && b1 == 'D' && d[2] == '3') return "mp3";       // ID3
            if(b0 == 0xFF && (b1 & 0xE0) == 0xE0) return "mp3";            // MPEG
            if(b0 == 'O' && b1 == 'g' && d[2] == 'g' && d[3] == 'S') return "ogg";
            if(b0 == 'f' && b1 == 'L' && d[2] == 'a' && d[3] == 'C') return "flac";
            if(b0 == 'R' && b1 == 'I' && d[2] == 'F' && d[3] == 'F' && d[8] == 'W' && d[9] == 'A' && d[10] == 'V' && d[11] == 'E') return "wav";
            return "";

        }
        public static boolean isUsableExtension(byte[] d){
            return extension(d).equals("mp3") || extension(d).equals("ogg") || extension(d).equals("flac") || extension(d).equals("wav");
=======
            if(b0 == 'I' && b1 == 'D' && d[2] == '3') return "mp3";       // ID3 标签: MP3
            if(b0 == 0xFF && (b1 & 0xE0) == 0xE0) return "mp3";            // MPEG 帧同步: MP3
            if(b0 == 'O' && b1 == 'g' && d[2] == 'g' && d[3] == 'S') return "ogg"; //OGG
            else return "";
        }
        public static boolean isUsableExtension(byte[] d){
            return extension(d).equals("mp3") || extension(d).equals("ogg");
>>>>>>> 3268ea3064d8b73a313afa143f7b7d1a6b57b58f
        }

        public void downloadToTmp(Cons<Fi> done, Cons<Throwable> err, boolean isPreview){
            if(!isSafeUrl(url)) ui.showException(new Throwable("Not found usable url."));

            var sound = (CustomSoundControl)control.sound;
            if(!isPreview) ui.loadfrag.show("[accent]" + Core.bundle.get("musicSquare.downloading"));
            Http.get(url, res -> {
                byte[] data = res.getResult();

                if(!isUsableExtension(data)){
                    if(!isPreview) ui.loadfrag.hide();
                    ui.showInfo("@musicSquare.unusableExt");
                    return;
                }

                String ext = extension(data);

                String sanitized = encodeString(artist + " - " + name);
                var tmpMusic = sound.musicLoader.tmp.child(sanitized + "__" + data.length + "." + ext);
                tmpMusic.writeBytes(data);
                done.get(tmpMusic);
            }, err);
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
            },e -> Core.app.post(() -> {
                ui.loadfrag.hide();
                ui.showException(new Exception("Download error: " + e));
            }), false);
        }
    }
}
