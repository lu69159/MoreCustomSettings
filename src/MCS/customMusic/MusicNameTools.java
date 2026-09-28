package MCS.customMusic;

import arc.files.Fi;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public class MusicNameTools {
    private static final Pattern pattern = Pattern.compile("[^-0-9a-zA-Z -)(\\[\\]]");

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
