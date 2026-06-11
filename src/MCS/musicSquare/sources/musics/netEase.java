package MCS.musicSquare.sources.musics;

import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;

import static mindustry.Vars.ui;

public class netEase extends musicBase{
    public netEase() {
        super("https://api.qijieya.cn/meting/?type=search&id=");
    }

    public Seq<Track> search(String name) {
        Seq<Track> results = new Seq<>();
        String reqUrl = url(name);
        String[] get = {""};

        try {
            Http.get(reqUrl).timeout(15000).block(res -> {
                get[0] = res.getResultAsString();
            });
        } catch (Exception e) {
            ui.showException(e);
            return results;
        }
        if (get[0].isEmpty()) return results;

        String body = get[0];

        var json = Jval.read(body);
        if(!json.isArray()){
            Log.info("musicBase json array expected");
            return results;
        }
        var arr = json.asArray();

        if(arr.size == 0) return results;

        for (int i = 0; i < arr.size; i++) {
            var item = arr.get(i);
            Track track = new Track();
            track.url = item.getString("url");
            track.pic = item.getString("pic");
            track.artist = item.getString("artist");
            track.name = item.getString("name");
            results.add(track);
        }

        return results;
    }
}
