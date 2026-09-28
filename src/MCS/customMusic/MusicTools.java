package MCS.customMusic;

import arc.audio.*;
import arc.files.*;

import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

import static arc.Core.settings;

public class MusicTools {
    private static final Pattern pattern = Pattern.compile("[^-0-9a-zA-Z -)(\\[\\]]");

    public static boolean enabledCustomMusic(){
        return settings.getBool("enableCustomMusic", false);
    }

    public static boolean isMusic(Fi fi){
        return (fi.extension().equals("ogg") || fi.extension().equals("mp3")) && fi.name().lastIndexOf("__") != -1;
    }

    public static boolean isSameMusic(Music current, Music music, boolean replace){
        if(current == null || music == null) return false;
        if(current == music) return true;
        if(replace){
            if(settings.getString("MCSplanetMusicName-" + getFileName(current.file), "unknown music").equals(settings.getString("MCSplanetMusicName-" + getFileName(music.file), "unknown music")) && current.file.length() == music.file.length()){
                music = current;
                return true;
            }
        }else{
            return getFileName(current.file).equals(getFileName(music.file)) && current.file.length() == music.file.length();
        }

        return false;
    }

    public static String encodeString(String nameWithoutExtension){
        if(pattern.matcher(nameWithoutExtension).find()){
            return "encodeName_" + encodeName(nameWithoutExtension);
        }else{
            return nameWithoutExtension;
        }
    }
    public static String encodeFileName(Fi file){
        if(file == null) return "";
        if(pattern.matcher(file.nameWithoutExtension()).find()){
            return "encodeName_" + encodeName(file.nameWithoutExtension()) + "__" + file.length() + "." + file.extension();
        }else{
            return file.nameWithoutExtension() + "__" + file.length() + "." + file.extension();
        }
    }
    public static String getFileName(Fi file){
        if(file == null) return ""; //神秘FOO怎么启动也触发这玩意儿
        String realName = file.nameWithoutExtension();
        int index = realName.lastIndexOf("__");
        if(index < 0) return realName;
        if(!realName.startsWith("encodeName_")) return realName.substring(0, index);

        return decodeName(realName.substring(("encodeName_").length(), index));
    }
    private static String encodeName(String input){
        if(input == null) return null;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
    private static String decodeName(String input){
        if(input == null || input.length() <= 1) return input;
        try{
            return new String(Base64.getUrlDecoder().decode(input), StandardCharsets.UTF_8);
        }catch(IllegalArgumentException ignore){
            return input;
        }
    }
}
