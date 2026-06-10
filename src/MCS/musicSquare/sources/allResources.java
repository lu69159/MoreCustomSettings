package MCS.musicSquare.sources;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.*;
import arc.util.serialization.Jval;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

public class allResources{
    private musicBase netBase = new musicBase("https://api.qijieya.cn/meting/?type=search&id=");
    private musicBase kuWo = new musicBase("https://kw-api.cenguigui.cn/?name="){
        @Override
        public Seq<musicBase.Track> search(String name){
            Seq<musicBase.Track> results = new Seq<>();
            String reqUrl = url(name);
            Log.info("kuwo search URL: " + reqUrl);

            String[] get = {""};
            try{
                Http.get(reqUrl).timeout(15000).submit(res -> {
                    get[0] = res.getResultAsString();
                });
            }catch(Exception e){
                return results;
            }
            if(get[0].isEmpty()) return results;

            String body = get[0];
            Log.info("kuwo response: " + (body.length() > 500 ? body.substring(0, 500) + "..." : body));

            var json = Jval.read(body);
            if(json.getInt("code", 0) != 200) return results;
            var data = json.get("data");
            if(data == null || !data.isArray()) return results;
            var arr = data.asArray();

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
    };
    private musicBase qq = new musicBase("https://tang.api.s01s.cn/music_open_api.php?msg="){
        @Override
        public String url(String name) {
            try{
                return url + URLEncoder.encode(name, "UTF-8") + "&type=json&limit=5";
            }catch(UnsupportedEncodingException e){
                return url + name + "&type=json&limit=5";
            }
        }

        @Override
        public Seq<Track> search(String name) {
            Seq<Track> results = new Seq<>();
            String reqUrl = url(name);
            Log.info("qq search URL: " + reqUrl);

            String[] get = {""};
            try {
                Http.get(reqUrl).timeout(15000).submit(res -> {
                    get[0] = res.getResultAsString();
                });
            } catch (Exception e) {
                return results;
            }
            if (get[0].isEmpty()) return results;

            String body = get[0];
            Log.info("qq search response: " + (body.length() > 500 ? body.substring(0, 500) + "..." : body));

            var json = Jval.read(body);
            if(!json.isArray()) return results;
            var arr = json.asArray();

            for (int i = 0; i < arr.size; i++) {
                var item = arr.get(i);
                if(!item.getString("pay").equals("免费")) continue;

                String songid = item.getString("song_mid");
                Track track = new Track();
                track.songid = songid;
                track.artist = item.getString("singer_name");
                track.name = item.getString("song_title");

                try{
                    String detailUrl = "https://tang.api.s01s.cn/music_open_api.php?msg="
                        + URLEncoder.encode(name, "UTF-8")
                        + "&type=json&mid=" + URLEncoder.encode(songid, "UTF-8");
                    Log.info("qq detail URL: " + detailUrl);

                    String[] dh = {""};
                    Http.get(detailUrl).timeout(15000).submit(res -> {
                        dh[0] = res.getResultAsString();
                    });
                    if(!dh[0].isEmpty()){
                        String dBody = dh[0];
                        Log.info("qq detail response: " + (dBody.length() > 500 ? dBody.substring(0, 500) + "..." : dBody));
                        var d = Jval.read(dBody);
                        track.url = d.getString("song_play_url_sq");
                        if(track.url == null || track.url.isEmpty()) track.url = d.getString("song_play_url_hq");
                        if(track.url == null || track.url.isEmpty()) track.url = d.getString("song_play_url");
                        track.pic = d.getString("album_pic");
                    }
                }catch(Exception e){
                    arc.util.Log.err("QQ detail fetch failed", e);
                }

                results.add(track);
            }

            return results;
        }
    };

    Seq<musicBase> all = Seq.with(netBase, kuWo, qq);


    private String keyword;
    private Cons<Seq<musicBase.Track>> onResultsUpdate;

    public void setOnResultsUpdate(Cons<Seq<musicBase.Track>> callback){
        this.onResultsUpdate = callback;
    }

    public void search(String kw, int limit, Seq<String> enabledSources){
        this.keyword = kw;
        String[] sources = {"netease", "kuwo", "qq"};

        new Thread(() -> {
            Seq<musicBase.Track> allResults = new Seq<>();
            for(String src : sources){
                if(!enabledSources.contains(src)) continue;
                musicBase base = src.equals("netease") ? netBase : src.equals("kuwo") ? kuWo : qq;
                Log.info(">> searching " + src + " for: " + kw);
                Seq<musicBase.Track> srcResults = base.search(kw);
                Log.info("<< " + src + " returned " + srcResults.size + " results");

                for(musicBase.Track t : srcResults){
                    t.uid = src + "-" + t.name;
                    t.source = src;
                    t.title = t.name;
                    t.audioUrl = t.url;
                    t.cover = t.pic;
                    allResults.add(t);
                }
            }
            Log.info(">> total results: " + allResults.size);

            Core.app.post(() -> {
                if(onResultsUpdate != null) onResultsUpdate.get(allResults);
            });
        }, "MusicSearch").start();
    }

    public void loadMore(int limit, Seq<String> enabledSources){
        search(keyword, limit, enabledSources);
    }

    public void fetchDetails(musicBase.Track track){
        track.detailsLoaded = true;
    }

    private String sourceName(musicBase base){
        if(base == netBase) return "netease";
        if(base == qq) return "qq";
        if(base == kuWo) return "kuwo";
        return "unknown";
    }
}
