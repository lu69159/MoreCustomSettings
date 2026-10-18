package MCS.musicSquare.sources.musics;

import arc.struct.Seq;
import arc.util.*;
import arc.util.serialization.*;

import static mindustry.Vars.ui;

public class kuWo extends musicBase{
    public kuWo(){ super("kuwo", "https://search.kuwo.cn/r.s?all={0}&ft=music&pn=0&rn={1}&rformat=json&encoding=utf8"); }
    String getDownloadUrl = "https://antiserver.kuwo.cn/anti.s?type=convert_url&rid={0}&format=mp3&response=url", picUrl = "https://img2.kuwo.cn/star/albumcover/{0}";

    @Override
    public Seq<Track> search(String name, int limit) {
        Seq<Track> results = new Seq<>();
        String reqUrl = url(name, limit);
        String[] get = {""};

        try{
            Http.get(reqUrl).timeout(15000).block(res -> {
                get[0] = res.getResultAsString();
            });
        }catch(Exception e){
            ui.showException(e);
            return results;
        }
        if (get[0].isEmpty()) return results;

        var arr = Jval.read(get[0]).get("abslist").asArray();
        if(arr.isEmpty()) return results;

        for (int i = 0; i < arr.size; i++){
            var item = arr.get(i);
            Track track = new Track();
            try{
                Http.get(getDownloadUrl.replace("{0}", item.getString("MUSICRID"))).timeout(15000).block(res -> {
                    track.url = res.getResultAsString();
                });
            }catch(Exception e){
                track.url = "";
            }
            
            track.pic = item.getString("web_albumpic_short").isEmpty() ? "" : picUrl.replace("{0}", item.getString("web_albumpic_short"));
            track.artist = item.getString("ARTIST");
            track.name = item.getString("NAME");
            results.add(track);
        }
        return results;
    }
}
