package MCS.musicSquare.sources;

import arc.struct.Seq;
import arc.util.*;
import arc.util.serialization.Jval;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

public class musicBase {
    public String url;

    public musicBase(String url) {
        this.url = url;
    }

    public String url(String name){
        try{
            return url + URLEncoder.encode(name, "UTF-8") + "&limit=5";
        }catch(UnsupportedEncodingException e){
            return url + name + "&limit=5";
        }
    }

    public Seq<Track> search(String name) {
        Seq<Track> results = new Seq<>();
        String reqUrl = url(name);
        Log.info("musicBase search URL: " + reqUrl);

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
        Log.info("musicBase response: " + (body.length() > 500 ? body.substring(0, 500) + "..." : body));

        var json = Jval.read(body);
        if(!json.isArray()) return results;

        var arr = json.asArray();

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

    public static class Track {
        public String url;
        public String pic;
        public String artist;
        public String name;
        public String uid;
        public String source;
        public String title;
        public String qualityLabel;
        public boolean detailsLoaded;
        public String audioUrl;
        public String cover;
        public String songid;
    }
}
