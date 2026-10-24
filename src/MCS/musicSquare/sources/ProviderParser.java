package MCS.musicSquare.sources;

import arc.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;

import java.io.*;
import java.net.*;

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

        Seq<String> urls = new Seq<>();
        Seq<JvalHandler> handlers = new Seq<>();
        for(int i = 1; ;i++){
            String u = resource.getString("url" + i);
            if(u.isEmpty()) break;
            var j = resource.get("handler" + i);
            if(j == null){
                Log.err("Each url must have a handler!\n");
                return null;
            }
            JvalHandler jh = new JvalHandler(j);
            if(!jh.prerequisite[0].isEmpty() && jh.prerequisite.length % 2 != 0){
                Log.err("prerequisite must contain an even number of strings!");
                return null;
            }
            if(!jh.replace[0].isEmpty() && jh.replace.length % 2 != 0){
                Log.err("replace must contain an even number of strings!");
                return null;
            }
            if(!jh.replaceName[0].isEmpty() && jh.replaceName.length % 2 != 0){
                Log.err("replaceName must contain an even number of strings!");
                return null;
            }

            urls.add(u);
            handlers.add(jh);
        }
        /*
        String url1 = resource.getString("url1"), url2 = resource.getString("url2");
        JvalHandler handler1 = new JvalHandler(resource.get("handler1"));
        @Nullable JvalHandler handler2 = resource.get("handler2") == null ? null : new JvalHandler(resource.get("handler2"));

         */

        if(urls.isEmpty()){
            Log.err("Url1 can't be empty!");
            return null;
        }
        if(handlers.get(0).name[0].isEmpty()){
            Log.err("Must get music name in handler1!");
            return null;
        }
        if(handlers.get(0).artist[0].isEmpty()){
            Log.err("Must get music artist in handler1!");
            return null;
        }

        return new Provider(name, urls.get(0), isCustom){
            @Override
            public Seq<Track> search(String keyWord, int limit) {
                Seq<Track> results = new Seq<>();
                Seq<TmpTrack> tmp = new Seq<>();
                String[] get = {""};

                for(int i = 0; i < urls.size; i++){
                    if(i == 0){
                        try{
                            Http.get(urls.get(0).replace("{0}", encodeKeyWord(keyWord)).replace("{1}", String.valueOf(limit))).timeout(Core.settings.getInt("musicSquare-timeout", 15) * 1000).block(res -> {
                                get[0] = res.getResultAsString();
                            });
                        }catch(Exception e){
                            return results;
                        }
                        var getArray = handlers.get(i).getArray(Jval.read(get[0]));
                        if(getArray == null || getArray.size == 0) return results;
                        int counter = 0;
                        for(int k = 0; k < getArray.size; k++){
                            Jval j = getArray.get(k);
                            if(handlers.get(0).canAdd(j) && counter < limit){
                                tmp.add(new TmpTrack(j, handlers.get(0)));
                                counter++;
                            }
                        }
                    }else{
                        if(tmp.isEmpty()) return results;
                        for(var t : tmp){
                            if(!t.mids.isEmpty()){
                                String[] get2 = {""};
                                String u = urls.get(i);
                                for(int m=0; m<t.mids.size; m++){
                                    u = u.replace("{" + m + "}", t.mids.get(m));
                                }
                                try{
                                    Http.get(u).timeout(Core.settings.getInt("musicSquare-timeout", 15) * 1000).block(res -> {
                                        get2[0] = res.getResultAsString();
                                    });
                                }catch(Exception e){
                                    return results;
                                }
                                if(!get2[0].isEmpty()){
                                    Jval j = handlers.get(i).url[0].equals("{0}") ? Jval.newObject().put("{0}", get2[0]) : Jval.read(get2[0]);
                                    if(handlers.get(i).canAdd(j)) t.updateTrack(j, handlers.get(i));
                                }
                            }
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

    public static String encodeKeyWord(String kw){
        try{
            return URLEncoder.encode(kw, "UTF-8");
        }catch(UnsupportedEncodingException e){
            return kw;
        }
    }

    public static class TmpTrack{
        public String url = "";
        public String pic = "";
        public String artist = "";
        public String name = "";
        public Seq<String> mids = new Seq<>();

        public TmpTrack(Jval jval1, JvalHandler handler1){
            updateTrack(jval1, handler1);
        }

        public void updateTrack(@Nullable Jval jval, JvalHandler handler){
            if(jval == null) return;
            if(!handler.url[0].isEmpty()){
                url = handler.getUrl(jval);
            }
            if(!handler.getPic(jval).isEmpty()){
                pic = handler.getPic(jval);
            }
            if(!handler.getArtist(jval).isEmpty()){
                artist = handler.getArtist(jval);
            }
            if(!handler.getName(jval).isEmpty()){
                name = handler.getName(jval);
            }
            mids.clear();
            mids = handler.getMids(jval);
        }

        public Track getTrack(){
            if(name.isEmpty() || artist.isEmpty()) return null;
            Track t = new Track();
            t.url = url;
            t.pic = pic;
            t.artist = artist;
            t.name = name;
            return t;
        }
    }

    public static class JvalHandler{
        public String[] url;
        public String[] pic;
        public String[] artist;
        public String[] name;

        public Seq<String[]> mid = new Seq<>();

        public String[] array;
        public String[] replace;
        public String[] replaceName;
        public String[] prerequisite;

        public String picUrl;

        public JvalHandler(Jval init){
            this.url = init.getString("url").split("__");
            this.pic = init.getString("pic").split("__");
            this.artist = init.getString("artist").split("__");
            this.name = init.getString("name").split("__");
            for(int i=0;;i++){
                String[] m = init.getString("mid" + i).split("__");
                if(m[0].isEmpty()) break;
                this.mid.add(m);
            }
            this.array = init.getString("array").split("__");
            this.replace = init.getString("replace").split("__");
            this.replaceName = init.getString("replaceName").split("__");
            this.prerequisite = init.getString("prerequisite").split("__");
            this.picUrl = init.getString("picUrl");
        }

        public String getUrl(Jval jval){
            try{
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
            }catch(Exception e){
                Log.err("Failed to getUrl: " + e.getMessage());
                return "";
            }
        }

        public String getPic(Jval jval){
            try{
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
            }catch(Exception e){
                Log.err("Failed to getPic: " + e.getMessage());
                return "";
            }
        }

        public String getArtist(Jval jval){
            try{
                if(artist.length == 1){
                    return replaceName(jval.getString(artist[0]));
                }else{

                    var j = jval;
                    for(var s : artist){
                        if(j.isArray()) j = j.asArray().first().get(s);
                        else j = j.get(s);
                    }
                    return replaceName(j.asString());
                }
            }catch(Exception e){
                Log.err("Failed to getArtist: " + e.getMessage());
                return "";
            }
        }

        public String getName(Jval jval){
            try{
                if(name.length == 1){
                    return replaceName(jval.getString(name[0]));
                }else{

                    var j = jval;
                    for(var s : name){
                        if(j.isArray()) j = j.asArray().first().get(s);
                        else j = j.get(s);
                    }
                    return replaceName(j.asString());
                }
            }catch(Exception e){
                Log.err("Failed to getName: " + e.getMessage());
                return "";
            }
        }

        public Seq<String> getMids(Jval jval){
            try{
                Seq<String> mids = new Seq<>();
                for(var m : mid){
                    String str = "";
                    if(!m[0].isEmpty()){
                        for(String s : m){
                            if (!jval.getString(s).isEmpty()) {
                                str = jval.getString(s);
                                break;
                            }
                        }
                    }
                    if(str.isEmpty()) Log.warn("There is empty mid.");
                    mids.add(str);
                }
                return mids;
            }catch(Exception e){
                Log.err("Failed to getMids: " + e.getMessage());
                return new Seq<>();
            }
        }

        public Jval.JsonArray getArray(Jval jval){
            try{
                if(jval.isArray()) return jval.asArray();
                if(array[0].isEmpty()){
                    Log.err("Response is an object, but array handler is empty");
                    return null;
                }
                Jval tmp = jval;

                for(String a : array){
                    tmp = tmp.get(a);
                }
                return tmp.asArray();
            }catch(Exception e){
                Log.err("Failed to parse array: " + e.getMessage());
                return null;
            }
        }

        public boolean canAdd(Jval jval){
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

        private String replaceStr(String oldStr){
            if(replace[0].isEmpty() || oldStr.isEmpty()) return oldStr;
            String s = oldStr;
            for(int i = 0; i < replace.length; i+=2){
                s = s.replace(replace[i], replace[i+1]);
            }
            return s;
        }

        private String replaceName(String oldName){
            if(replaceName[0].isEmpty() || oldName.isEmpty()) return oldName;
            String s = oldName;
            for(int i = 0; i < replaceName.length; i+=2){
                s = s.replace(replaceName[i], replaceName[i+1]);
            }
            return s;
        }
    }
}
