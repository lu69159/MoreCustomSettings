package MCS.musicSquare.sources;

import arc.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;

import java.io.*;
import java.net.*;

import static MCS.main.saveFolder;

public class ProviderParser{

    public static Provider parse(String name, Jval resource, boolean isCustom){
        if(name.isEmpty()){
            Log.err("Provider's name is empty!");
            return null;
        }
        if(!resource.isObject()){
            Log.err("Wrong provider format!\n");
            return null;
        }

        String url1 = resource.getString("url1"), url2 = resource.getString("url2");
        String[] prerequisite = resource.getString("prerequisite").split("__");
        JvalHandler handler1 = new JvalHandler(resource.get("handler1"));
        @Nullable JvalHandler handler2 = resource.get("handler2") == null ? null : new JvalHandler(resource.get("handler2"));

        if(url1.isEmpty()){
            Log.err(name + "'s url is empty!");
            return null;
        }
        if(handler1.name.isEmpty()){
            Log.err("Must get music name in handler1!");
            return null;
        }
        if(handler1.artist[0].isEmpty()){
            Log.err("Must get music artist in handler1!");
            return null;
        }

        return new Provider(name, url1, isCustom){
            @Override
            public Seq<Track> search(String keyWord, int limit) {
                Seq<Track> results = new Seq<>();
                Seq<TmpTrack> tmp = new Seq<>();
                String[] get = {"",""};

                try{
                    Http.get(url1.replace("{0}", encodeKeyWord(keyWord)).replace("{1}", String.valueOf(limit))).timeout(15000).block(res -> {
                        get[0] = res.getResultAsString();
                    });
                }catch(Exception e){
                    Log.err(name + " failed to get info from url1");
                    return results;
                }

                Jval.JsonArray res1 = handler1.getArray(Jval.read(get[0]));
                if(res1 == null || res1.size == 0) return results;

                int limitCounter = 0;
                if(url2.isEmpty()){
                    for(var j1 : res1){
                        if(shouldAdd(j1, prerequisite) && limitCounter < limit){
                            tmp.add(new TmpTrack(j1, null, handler1, handler2));
                            limitCounter++;
                        }
                    }
                }else{
                    for(var j1 : res1){
                        if(shouldAdd(j1, prerequisite) && limitCounter < limit){
                            String mid = handler1.getMid(j1);
                            if(mid.isEmpty()) continue;

                            Jval j2 = null;
                            try{
                                Http.get(url2.replace("{0}", mid)).timeout(15000).block(res -> {
                                    get[1] = res.getResultAsString();
                                });
                                if(!get[1].isEmpty()){
                                    if(get[1].startsWith("http")) j2 = Jval.newObject().put("{0}", get[1]);
                                    else j2 = Jval.read(get[1]);
                                }
                            }catch(Exception e){
                                Log.err(name + " failed to get info from url2");
                            }
                            tmp.add(new TmpTrack(j1, j2, handler1, handler2));
                            limitCounter++;
                        }
                    }
                }

                for(var t : tmp){
                    var track = t.getTrack();
                    if(track != null) results.add(track);
                }
                return results;
            }
        };
    }

    public static boolean shouldAdd(Jval jval, String[] prerequisite){
        if(prerequisite[0].isEmpty()) return true;
        boolean b = true;
        for(int i = 0; i < prerequisite.length; i+=2){
            if(!jval.get(prerequisite[i]).asString().equals(prerequisite[i+1])){
                b = false;
                break;
            }
        }
        return b;
    }

    public static String encodeKeyWord(String kw){
        try{
            return URLEncoder.encode(kw, "UTF-8");
        }catch(UnsupportedEncodingException e){
            return kw;
        }
    }

    public static class TmpTrack{
        public Jval j1;
        public @Nullable Jval j2;
        public JvalHandler handler1;
        public @Nullable JvalHandler handler2;

        public TmpTrack(Jval j1, Jval j2, JvalHandler handler1, JvalHandler handler2){
            this.j1 = j1;
            this.j2 = j2;
            this.handler1 = handler1;
            this.handler2 = handler2;
        }

        public Track getTrack(){
            var t = new Track();
            t.name = j1.getString(handler1.name);
            t.artist = handler1.getArtist(j1);
            t.url = !handler1.url[0].isEmpty() ? handler1.getUrl(j1) : handler2.getUrl(j2);
            t.pic = !handler1.pic[0].isEmpty() ? handler1.getPic(j1) : handler2.getPic(j2);
            return t;
        }
    }

    public static class JvalHandler{
        public String[] url;
        public String[] pic;
        public String[] artist;
        public String name;
        public String[] mid;
        public String[] array;

        public String[] replace;
        public String picUrl;

        public JvalHandler(Jval init){
            this.url = init.getString("url").split("__");
            this.pic = init.getString("pic").split("__");
            this.artist = init.getString("artist").split("__");
            this.name = init.getString("name");
            this.mid = init.getString("mid").split("__");
            this.array = init.getString("array").split("__");
            this.replace = init.getString("replace").split("__");
            this.picUrl = init.getString("picUrl");
        }

        public String getUrl(Jval jval){
            if(url.length == 1){
                return replaceStr(jval.getString(url[0]));
            }else{
                var j = jval;
                for(var s : url){
                    if(j.isArray()) j = j.asArray().first().get(s);
                    else j = j.get(s);
                }
                return replaceStr(j.asString());
            }
        }

        public String getPic(Jval jval){
            if(pic.length == 1){
                String u = jval.getString(pic[0]);
                if(!u.isEmpty() && !picUrl.isEmpty()){
                    return picUrl.replace("{0}", replaceStr(u));
                }else{
                    return replaceStr(u);
                }
            }else{
                var j = jval;
                for(var s : pic){
                    if(j.isArray()) j = j.asArray().first().get(s);
                    else j = j.get(s);
                }
                return replaceStr(j.asString());
            }
        }

        public String getArtist(Jval jval){
            if(artist.length == 1){
                return jval.getString(artist[0]);
            }else{
                var j = jval;
                for(var s : artist){
                    if(j.isArray()) j = j.asArray().first().get(s);
                    else j = j.get(s);
                }
                return j.asString();
            }
        }

        public String getMid(Jval jval){
            if(mid[0].isEmpty()) return "";
            for(String s : mid){
                if (!jval.getString(s).isEmpty()) {
                    return jval.getString(s);
                }
            }
            return "";
        }

        public Jval.JsonArray getArray(Jval jval){
            if(jval.isArray() || array[0].isEmpty()) return jval.asArray();
            Jval tmp = jval;
            try{
                for(String a : array){
                    tmp = tmp.get(a);
                }
                return tmp.asArray();
            }catch(Exception e){
                Log.err("Failed to parse array: " + e.getMessage());
                return null;
            }
        }

        private String replaceStr(String oldStr){
            if(replace[0].isEmpty() || oldStr.isEmpty()) return oldStr;
            String s = oldStr;
            for(int i = 0; i < replace.length; i+=2){
                s = s.replace(replace[i], replace[i+1]);
            }
            return s;
        }
    }
}
