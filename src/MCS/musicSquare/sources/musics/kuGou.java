package MCS.musicSquare.sources.musics;

import arc.struct.Seq;
import arc.util.Http;
import arc.util.serialization.Jval;

public class kuGou extends musicBase{
    public kuGou(){ super("kugou","https://songsearch.kugou.com/song_search_v2?keyword={0}&pagesize={1}"); }

    @Override
    public Seq<Track> search(String name, int limit) {
        Seq<Track> results = new Seq<>();
        String searchUrl = url(name, limit);
        String[] get = {"", ""};

        try {
            Http.get(searchUrl + "&pagesize=5").timeout(15000).block(res -> {
                get[0] = res.getResultAsString();
            });
        } catch (Exception e) {
            return results;
        }
        if (get[0].isEmpty()) return results;

        String body = get[0];

        var json = Jval.read(body);
        if(json.getInt("error_code", -1) != 0 || json.get("data").getInt("total", 0) == 0) return results;

        var lists = json.get("data").get("lists").asArray();
        int total = Math.min(10, json.get("data").getInt("total", 0));

        for(int i = 0; i < total; i++){
            var item = lists.get(i);
            Track track = new Track();
            track.url = firstFreeUrl(item);
            track.pic = item.getString("Image").replace("{size}", "64");
            track.artist = item.getString("SingerName");
            track.name = item.getString("SongName");
            results.add(track);
        }

        return results;
    }

    private String firstFreeUrl(Jval item){
        String sq = item.getString("SQFileHash", "");
        if(!sq.isEmpty() && item.getInt("SQPayType", 0) == 0 && item.getInt("SQPrivilege", 0) == 0 && item.getInt("ASQPrivilege", 0) == 0){
            String u = fetchPlayUrl(sq);
            if(!u.isEmpty()) return u;
        }
        String hq = item.getString("HQFileHash", "");
        if(!hq.isEmpty() && item.getInt("HQPayType", 0) == 0 && item.getInt("HQPrivilege", 0) == 0){
            String u = fetchPlayUrl(hq);
            if(!u.isEmpty()) return u;
        }
        String lo = item.getString("FileHash", "");
        if(!lo.isEmpty() && item.getInt("PayType", 0) == 0 && item.getInt("Privilege", 0) == 0){
            String u = fetchPlayUrl(lo);
            if(!u.isEmpty()) return u;
        }
        return "";
    }

    private String fetchPlayUrl(String hash){
        String[] get = {""};
        try{
            Http.get("http://m.kugou.com/app/i/getSongInfo.php?cmd=playInfo&hash=" + hash)
                .timeout(15000).block(res -> get[0] = res.getResultAsString());
        }catch(Exception e){
            return "";
        }
        if(get[0].isEmpty()) return "";
        try{
            var j = Jval.read(get[0]);
            if(!j.has("errcode") || j.getInt("errcode", -1) != 0) return "";
            return j.getString("url", "");
        }catch(Exception e){
            return "";
        }
    }
}
