package MCS.musicSquare.sources.musics;

import arc.struct.Seq;
import arc.util.Http;
import arc.util.serialization.Jval;

public class qq extends musicBase{
    public qq() {
        super("qq","https://tang.api.s01s.cn/music_open_api.php?{0}&type=json");
    }

    @Override
    public Seq<Track> search(String keyWord, int limit) {
        Seq<Track> results = new Seq<>();
        Seq<String> mids = new Seq<>();
        String[] get = {"", ""};

        try {
            Http.get(url.replace("{0}", "msg=" + encodeKeyword(keyWord))).timeout(15000).block(res -> {
                get[0] = res.getResultAsString();
            });
        } catch (Exception e) {
            return results;
        }
        if (get[0].isEmpty()) return results;

        String body = get[0];

        var arr = Jval.read(body).asArray();

        if(arr.size == 0) return results;

        for(int i = 0; i < arr.size; i++) {
            var item = arr.get(i);
            if(item.getString("pay").equals("免费")) mids.add(item.getString("song_mid"));
        }

        //TODO：快速遍历会被判定为重复请求
        try {
            for(int i = 0; i < mids.size; i++){
                Http.get(url.replace("{0}", "&mid=" + mids.get(i))).timeout(15000).block(res -> {
                    get[1] = res.getResultAsString();
                });
                String body2 = get[1];
                var json2 = Jval.read(body2);
                Track track = new Track();
                track.url = json2.getString("song_play_url");
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
