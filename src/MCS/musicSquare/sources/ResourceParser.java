package MCS.musicSquare.sources;

import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ResourceParser{

    public static musicBase parse(Jval jval){
        ResourceBase resource = new ResourceBase(jval);
        return new musicBase(resource.name, resource.searchUrl){
            @Override
            public Seq<Track> search(String keyWord, int limit){
                return resource.search(keyWord, limit);
            }
        };
    }

    public static class ResourceBase{
        public String name;
        public String[] prerequisite, replace;
        public String searchUrl, searchUrl2, picUrl;
        public Handler handler, handler2;

        public ResourceBase(Jval resource){
            if(resource == null || !resource.isObject()){
                throw new IllegalArgumentException("Resource configuration must be a JSON object");
            }

            name = resource.getString("name");
            prerequisite = split(resource.getString("prerequisite"));
            replace = split(resource.getString("replace"));
            searchUrl = resource.getString("searchUrl");
            searchUrl2 = resource.getString("searchUrl2");
            picUrl = resource.getString("picUrl");
            handler = new Handler(resource.get("handler"));
            handler2 = new Handler(resource.get("handler2"));

            if(name.isEmpty()) throw new IllegalArgumentException("Resource configuration is missing 'name'");
            if(searchUrl.isEmpty()) throw new IllegalArgumentException("Resource '" + name + "' is missing 'searchUrl'");
        }

        public Seq<Track> search(String keyWord, int limit){
            Seq<Track> results = new Seq<>();

            try{
                String url = searchUrl.replace("{0}", encodeKeyword(keyWord == null ? "" : keyWord))
                    .replace("{1}", String.valueOf(limit));
                String response = requestOnline(url);
                if(response.isEmpty()) return results;

                Jval root = Jval.read(response);
                Jval.JsonArray items = findItems(root);
                if(items == null) return results;

                for(Jval item : items){
                    if(item == null || !item.isObject()) continue;

                    String secondaryUrl = "";
                    Jval detail = null;
                    if(!searchUrl2.isEmpty()){
                        String id = firstString(item, handler.mid);
                        if(id.isEmpty()) continue;

                        String detailUrl = replace(searchUrl2.replace("{0}", encodeKeyword(id)));
                        detail = readResponse(requestOnline(detailUrl));
                        if(detail == null) continue;
                        secondaryUrl = getString(detail, handler2.url);

                        // Some sources return the playable URL as a plain JSON string.
                        if(secondaryUrl.isEmpty() && (detail.isString() || detail.isNumber())){
                            secondaryUrl = detail.asString();
                        }
                    }

                    Track track = new Track();
                    track.name = getString(item, handler.name);
                    track.artist = getString(item, handler.artist);
                    track.pic = getPicture(item, detail);
                    track.url = searchUrl2.isEmpty() ? replace(getString(item, handler.url)) : replace(secondaryUrl);

                    // A track without a playable URL cannot be previewed or downloaded.
                    if(!track.url.isEmpty()) results.add(track);
                }
            }catch(Exception e){
                Log.err("Failed to parse music source '" + name + "'", e);
            }

            return results;
        }

        private Jval.JsonArray findItems(Jval root){
            if(root == null) return null;
            if(root.isArray()) return root.asArray();

            Jval value = root;
            if(handler.arr.length == 0 || handler.arr[0].isEmpty()) return null;
            for(String key : handler.arr){
                if(key.isEmpty()) continue;
                if(value == null || !value.isObject()) return null;
                value = value.get(key);
            }
            return value != null && value.isArray() ? value.asArray() : null;
        }

        private String getPicture(Jval item, Jval detail){
            String picture = getString(item, handler.pic);
            if(!picture.isEmpty()){
                return replace(picUrl.isEmpty() ? picture : picUrl.replace("{0}", picture));
            }

            picture = getString(detail, handler2.pic);
            if(picture.isEmpty()) return "";
            return replace(picUrl.isEmpty() ? picture : picUrl.replace("{0}", picture));
        }

        private Jval readResponse(String response){
            if(response == null || response.trim().isEmpty()) return null;
            try{
                return Jval.read(response);
            }catch(Exception ignored){
                // A few providers return a raw URL rather than JSON; quote it for Jval parsing.
                return Jval.valueOf(response.trim());
            }
        }

        public String requestOnline(String url){
            String[] result = {""};
            try{
                Http.get(url).timeout(15000).block(res -> result[0] = res.getResultAsString());
            }catch(Exception ignored){
            }
            return result[0];
        }

        public String replace(String value){
            if(value == null || value.isEmpty()) return "";
            for(int i = 0; i + 1 < replace.length; i += 2){
                if(!replace[i].isEmpty()) value = value.replace(replace[i], replace[i + 1]);
            }
            return value;
        }

        public String encodeKeyword(String value){
            try{
                return URLEncoder.encode(value, "UTF-8");
            }catch(UnsupportedEncodingException e){
                return value;
            }
        }

        private static String[] split(String value){
            return value == null || value.isEmpty() ? new String[]{""} : value.split("__", -1);
        }

        private static String firstString(Jval value, String[] keys){
            for(String key : keys){
                String result = getString(value, key);
                if(!result.isEmpty()) return result;
            }
            return "";
        }

        private static String getString(Jval object, String key){
            if(object == null || !object.isObject() || key == null || key.isEmpty()) return "";
            Jval value = object.get(key);
            if(value == null || value.isNull()) return "";
            if(value.isString() || value.isNumber()) return value.asString();
            return "";
        }
    }

    public static class Handler{
        public String name;
        public String artist;
        public String pic;
        public String url;
        public String[] mid;
        public String[] arr;

        public Handler(Jval j){
            name = getString(j, "name");
            artist = getString(j, "artist");
            pic = getString(j, "pic");
            url = getString(j, "url");
            mid = ResourceBase.split(getString(j, "mid"));
            arr = ResourceBase.split(getString(j, "array"));
        }

        private static String getString(Jval object, String key){
            if(object == null || !object.isObject()) return "";
            Jval value = object.get(key);
            if(value == null || value.isNull()) return "";
            return value.isString() || value.isNumber() ? value.asString() : "";
        }
    }
}
