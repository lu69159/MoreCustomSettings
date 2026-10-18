package MCS.musicSquare.sources.musics;

import MCS.customMusic.CustomSoundControl;
import arc.*;
import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import java.net.*;
import java.io.*;

import static MCS.customMusic.MusicTools.*;
import static mindustry.Vars.*;

public abstract class musicBase {
    public String name;
    public String url;

    public musicBase(String name, String url){
        this.name = name;
        this.url = url;
    }

    /**
     * @implNote  {0}:搜索keyWord  {1}:搜索limit
     */
    public String url(String name, int limit){
        try{
            return url.replace("{0}", URLEncoder.encode(name, "UTF-8")).replace("{1}", String.valueOf(limit));
        }catch(UnsupportedEncodingException e){
            return url.replace("{0}", name).replace("{1}", String.valueOf(limit));
        }
    }

    public Seq<Track> search(String name, int limit) {
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
