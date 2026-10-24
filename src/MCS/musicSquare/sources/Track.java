package MCS.musicSquare.sources;

import MCS.customMusic.CustomSoundControl;
import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.util.Http;

import static MCS.customMusic.MusicTools.*;
import static MCS.musicSquare.sources.Provider.*;
import static mindustry.Vars.*;

public class Track{
    public String url;
    public String pic;
    public String artist;
    public String name;

    private String extension(byte[] d){
        if(d.length < 12) return "";
        int b0 = d[0] & 0xFF, b1 = d[1] & 0xFF;
        if(b0 == 'I' && b1 == 'D' && d[2] == '3') return "mp3";       // ID3
        if(b0 == 0xFF && (b1 & 0xE0) == 0xE0) return "mp3";            // MPEG
        if(b0 == 'O' && b1 == 'g' && d[2] == 'g' && d[3] == 'S') return "ogg";
        if(b0 == 'f' && b1 == 'L' && d[2] == 'a' && d[3] == 'C') return "flac";
        if(b0 == 'R' && b1 == 'I' && d[2] == 'F' && d[3] == 'F' && d[8] == 'W' && d[9] == 'A' && d[10] == 'V' && d[11] == 'E') return "wav";
        return "";
    }
    private boolean isUsableExtension(byte[] d){
        return extension(d).equals("mp3") || extension(d).equals("ogg") || extension(d).equals("flac") || extension(d).equals("wav");
    }

    public void downloadToTmp(Cons<Fi> done, Cons<Throwable> err, boolean isPreview){
        if(!isSafeUrl(url)) ui.showException(new Throwable("Not found usable url."));

        var sound = (CustomSoundControl)control.sound;
        if(!isPreview) ui.loadfrag.show("[accent]" + Core.bundle.get("musicSquare.downloading"));

        var existed = sound.musicLoader.tmp.seq().find(fi -> getFileName(fi).equals(artist + " - " + name) && fi.nameWithoutExtension().lastIndexOf("__") >= 0 && fi.nameWithoutExtension().endsWith(String.valueOf(url.hashCode())));
        if(existed != null && existed.length() > 0){
            done.get(existed);
            return;
        }

        Http.get(url).timeout(Core.settings.getInt("musicSquare-timeout", 15) * 1000).error(err).submit(res -> {
            byte[] data = res.getResult();

            if(!isUsableExtension(data)){
                if(!isPreview) ui.loadfrag.hide();
                done.get(null);
                return;
            }

            String ext = extension(data);

            String sanitized = encodeString(artist + " - " + name);
            var tmpMusic = sound.musicLoader.tmp.child(sanitized + "__" + url.hashCode() + "." + ext);
            tmpMusic.writeBytes(data);
            done.get(tmpMusic);
        });
    }

    public void download(String name){
        downloadToTmp(fi -> {
            Core.app.post(() -> {
                if(fi == null){
                    ui.showInfo("@musicSquare.unusableExt");
                    return;
                }
                var sound = (CustomSoundControl)control.sound;
                sound.musicLoader.moveMusic(fi, name, true, true);
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
