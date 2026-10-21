package MCS.musicSquare.sources;

import arc.struct.*;

import java.net.*;
import java.io.*;

public abstract class musicBase {
    public String name;
    public String url;

    public musicBase(String name, String url){
        this.name = name;
        this.url = url;
    }

    public Seq<Track> search(String keyWord, int limit) {
        return new Seq<>();
    }

    public static boolean isSafeUrl(String url){
        if(url == null || url.isEmpty()) return false;
        try{
            var parsed = new URL(url);
            String protocol = parsed.getProtocol();
            return "http".equals(protocol) || "https".equals(protocol);
        }catch(Exception e){
            return false;
        }
    }
}
