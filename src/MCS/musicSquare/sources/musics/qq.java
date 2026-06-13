package MCS.musicSquare.sources.musics;

import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class qq extends musicBase{
    public qq() {
        super("https://tang.api.s01s.cn/music_open_api.php?msg=");
    }

    @Override
    public String url(String name){
        try{
            return url + URLEncoder.encode(name, "UTF-8") + "&type=json";
        }catch(UnsupportedEncodingException e){
            return url + name + "&type=json";
        }
    }

    @Override
    public Seq<Track> search(String name) {
        Seq<Track> results = new Seq<>();
        Seq<String> mids = new Seq<>();
        String reqUrl = url(name);
        String[] get = {"", ""};

        try {
            Http.get(reqUrl + "&limit=10").timeout(15000).block(res -> {
                get[0] = res.getResultAsString();
            });
        } catch (Exception e) {
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

        for(int i = 0; i < arr.size; i++) {
            var item = arr.get(i);
            if(item.getString("pay").equals("免费")) mids.add(item.getString("song_mid"));
        }

        try {
            for(int i = 0; i < mids.size; i++){
                Http.get(reqUrl + "&mid=" + mids.get(i)).timeout(15000).block(res -> {
                    get[1] = res.getResultAsString();
                });
                String body2 = get[1];
                var json2 = Jval.read(body2);
                Track track = new Track();
                track.url = !json2.getString("song_play_url_sq").isEmpty() ? json2.getString("song_play_url_sq") : !json2.getString("song_play_url_hq").isEmpty() ? json2.getString("song_play_url_hq") :
                        !json2.getString("song_play_url_standard").isEmpty() ? json2.getString("song_play_url_standard") : json2.getString("song_play_url_fq");
                track.pic = json2.getString("album_pic");
                track.artist = json2.getString("singer_name");
                track.name = json2.getString("song_title");
                results.add(track);
            }
        } catch (Exception e) {
            return results;
        }

        return results;
    }
}
