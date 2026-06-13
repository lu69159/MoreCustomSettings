package MCS.musicSquare.sources.musics;

import arc.struct.Seq;
import arc.util.Http;
import arc.util.serialization.Jval;

public class kuWo extends musicBase {
    public kuWo() {
        super("https://kw-api.cenguigui.cn/?name=");
    }

    @Override
    public Seq<Track> search(String name){
        Seq<musicBase.Track> results = new Seq<>();
        String reqUrl = url(name);
        String[] get = {""};

        try{
            Http.get(reqUrl).timeout(15000).block(res -> {
                get[0] = res.getResultAsString();
            });
        }catch(Exception e){
            return results;
        }
        if(get[0].isEmpty()) return results;

        String body = get[0];

        var json = Jval.read(body);
        var data = json.get("data");
        var arr = data.asArray();

        if(arr.size == 0) return results;

        for(int i = 0; i < arr.size; i++){
            var item = arr.get(i);
            musicBase.Track track = new musicBase.Track();
            track.url = item.getString("url");
            track.pic = item.getString("pic");
            track.artist = item.getString("artist");
            track.name = item.getString("name");
            results.add(track);
        }

        return results;
    }
}
