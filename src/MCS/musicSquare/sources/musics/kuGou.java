package MCS.musicSquare.sources.musics;

import arc.struct.Seq;
import arc.util.Http;
import arc.util.serialization.Jval;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class kuGou extends musicBase{
    private final String searchUrl1 = "http://songsearch.kugou.com/song_search_v2?keyword=";

    public kuGou(){ super(""); }


    public String searchUrl(String name){
        try{
            return searchUrl1 + URLEncoder.encode(name, "UTF-8");
        }catch(UnsupportedEncodingException e){
            return searchUrl1 + name;
        }
    }

    @Override
    public String url(String name) {
        try{
            return url + URLEncoder.encode(name, "UTF-8");
        }catch(UnsupportedEncodingException e){
            return url + name;
        }
    }

    @Override
    public Seq<Track> search(String name) {
        Seq<Track> results = new Seq<>();
        Seq<String> rids = new Seq<>();
        String searchUrl = searchUrl(name);
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
        if(!json.get("error_code").equals("0") || json.get("lists").asArray().size == 0) return results;

        var lists = json.get("lists").asArray();

        for(int i = 0; i < lists.size; i++){
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
