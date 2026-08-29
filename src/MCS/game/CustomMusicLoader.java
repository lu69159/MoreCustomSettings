package MCS.game;

import arc.Core;
import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import arc.util.Nullable;
import mindustry.gen.*;
import mindustry.type.Planet;
import mindustry.ui.*;

import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import static arc.Core.settings;
import static mindustry.Vars.*;

public class CustomMusicLoader{
    public Fi musicFolder;
    public Fi ambient, dark, boss, planets, tmp;
    public Seq<Music> ambientMusic = new Seq<>();
    public Seq<Music> darkMusic = new Seq<>();
    public Seq<Music> bossMusic = new Seq<>();
    public @Nullable Music menuMusic;
    public @Nullable Music editorMusic;
    public ObjectMap<Planet, Music> planetMusicMap = new ObjectMap<>();

    private final Pattern pattern = Pattern.compile("[^-0-9a-zA-Z -)(\\[\\]]");

    public void load(){
        if(settings.getBool("enableCustomMusic", false)){
            loadCustom();
        }
        else{
            reset();
        }
    }

    public void loadCustom(){
        loadFolder();
        loadMusic(ambient, ambientMusic);
        loadMusic(dark, darkMusic);
        loadMusic(boss, bossMusic);
        loadPlanetMusic();
        for(var f : musicFolder.seq()){
            if(!f.isDirectory()){
                String n = f.name().split("__", 2)[0];
                if(n.equals("menu")){
                    try{
                        menuMusic = new Music(f){
                            @Override
                            public void setLooping(boolean isLooping){}
                        };
                    }catch (Exception e){
                        ui.showException(e);
                    }
                }
                else if(n.equals("editor")){
                    try{
                        editorMusic = new Music(f){
                            @Override
                            public void setLooping(boolean isLooping){}
                        };
                    }catch (Exception e){
                        ui.showException(e);
                    }
                }
            }
        }

        control.sound.ambientMusic.clear();
        control.sound.darkMusic.clear();
        control.sound.bossMusic.clear();

        control.sound.ambientMusic.add(ambientMusic);
        control.sound.darkMusic.add(darkMusic);
        control.sound.bossMusic.add(bossMusic);
    }

    public void loadFolder(){
        musicFolder = Core.settings.getDataDirectory().child("MCS-music");
        if(!musicFolder.exists()) musicFolder.mkdirs();

        ambient = musicFolder.child("a");
        if(!ambient.exists()) ambient.mkdirs();

        dark = musicFolder.child("d");
        if(!dark.exists()) dark.mkdirs();

        boss = musicFolder.child("b");
        if(!boss.exists()) boss.mkdirs();

        planets = musicFolder.child("planets");
        if(!planets.exists()) planets.mkdirs();

        tmp = musicFolder.child("tmp");
        if(!tmp.exists()) tmp.mkdirs();
    }

    public void loadMusic(Fi folder, Seq<Music> musicSeq){
        musicSeq.clear();
        try {
            for(var fi : folder.seq()) {
                if (isMusic(fi)){
                    musicSeq.add(new Music(fi));
                }
            }
        }catch(Exception e){
            ui.showException(e);
        }
    }

    public void loadPlanetMusic(){
        try{
            for(var fi : planets.seq()){
                if(isMusic(fi)){
                    var planet = content.planets().find(p -> p.name.equals(getName(fi)));
                    if(planet != null && planet.accessible){
                        try{
                            planetMusicMap.put(planet, new Music(fi){
                                @Override
                                public void setLooping(boolean isLooping){}
                            });
                        }catch(Exception e){
                            ui.showException(e);
                        }
                    }
                }
            }
        }catch(Exception e){
            ui.showException(e);
        }
    }

    public void reset(){
        ambientMusic = Seq.with(Musics.game1, Musics.game3, Musics.game6, Musics.game8, Musics.game9, Musics.fine);
        darkMusic = Seq.with(Musics.game2, Musics.game5, Musics.game7, Musics.game4);
        bossMusic = Seq.with(Musics.boss1, Musics.boss2, Musics.game2, Musics.game5);

        control.sound.ambientMusic.clear();
        control.sound.darkMusic.clear();
        control.sound.bossMusic.clear();

        control.sound.ambientMusic.add(ambientMusic);
        control.sound.darkMusic.add(darkMusic);
        control.sound.bossMusic.add(bossMusic);
    }

    public void delete(){
        reset();
        settings.put("enableCustomMusic", false);
        for(var fi : planets.seq()){
            settings.remove("MCSplanetMusicName-" + getName(fi));
        }
        musicFolder.deleteDirectory();
        menuMusic = null;
        editorMusic = null;
        settings.remove("MCSmenuMusicName");
        settings.remove("MCSeditorMusicName");
        ui.showInfo("@clearMusic.clear");
        loadFolder();
    }

    public Runnable importMusic(String musicFi){
        return () -> FileChooser.open("ogg", "mp3").submitMulti(files -> {
            boolean successImported = false;
            for(var fi : files){
                try{
                    Fi folder = Core.settings.getDataDirectory().child("MCS-music").child(musicFi);
                    if(!folder.exists()) folder.mkdirs();

                    fi.copyTo(folder);
                    Path source = Paths.get(folder.path() + "/" + fi.name());
                    Path to = Paths.get(folder.path() + "/" + realName(fi));
                    Files.move(source, to, StandardCopyOption.REPLACE_EXISTING);
                    successImported = true;
                }catch(Exception e){
                    ui.showException(e);
                }
            }
            if(successImported){
                ui.showInfo("@importMusic.imported");
                load();
            }
        });
    }

    public Runnable importNamedMusic(String inputName){
        return () -> FileChooser.open("ogg", "mp3").submitMulti(files -> {
            boolean successImported = false;
            for(var fi : files){
                if(inputName.equals("menu") || inputName.equals("editor")){
                    try{
                        Fi folder = musicFolder;
                        if(!folder.exists()) folder.mkdirs();

                        for(var f : folder.seq()){
                            if(!f.isDirectory()){
                                String n = f.name().split("__", 2)[0];
                                if(n.equals(inputName)) f.delete();
                            }
                        }

                        String musicShowName = fi.nameWithoutExtension();
                        if(inputName.equals("menu")){
                            Core.settings.put("MCSmenuMusicName", musicShowName);
                        }else{
                            Core.settings.put("MCSeditorMusicName", musicShowName);
                        }

                        fi.copyTo(folder);
                        Path source = Paths.get(folder.path() + "/" + fi.name());
                        Path to = Paths.get(folder.path() + "/" + inputName + "__" + fi.length() + "." + fi.extension());
                        Files.move(source, to, StandardCopyOption.REPLACE_EXISTING);
                    }catch(Exception e){
                        ui.showException(e);
                    }
                }else{
                    try{
                        Fi folder = planets;
                        if(!folder.exists()) folder.mkdirs();

                        for(var f : folder.seq()){
                            if(!f.isDirectory()){
                                if(getName(f).equals(inputName)) f.delete();
                            }
                        }

                        Core.settings.put("MCSplanetMusicName-" + inputName, fi.nameWithoutExtension());
                        String name;
                        if(pattern.matcher(inputName).find()){
                            name = "encodeName_" + encodeName(inputName) + "__" + fi.length() + "." + fi.extension();
                        }else{
                            name = inputName + "__" + fi.length() + "." + fi.extension();
                        }

                        fi.copyTo(folder);
                        Path source = Paths.get(folder.path() + "/" + fi.name());
                        Path to = Paths.get(folder.path() + "/" + name);
                        Files.move(source, to, StandardCopyOption.REPLACE_EXISTING);
                        successImported = true;
                    }catch(Exception e){
                        ui.showException(e);
                    }
                }
            }
            if(successImported){
                ui.showInfo("@importMusic.imported");
                load();
            }
        });
    }

    public boolean isMusic(Fi fi){
        return (fi.extension().equals("ogg") || fi.extension().equals("mp3")) && fi.name().lastIndexOf("__") != -1;
    }

    public boolean isSameMusic(Music current, Music music){
        if(current == null || music == null) return false;
        if(current == music) return true;
        if(settings.getString("MCSplanetMusicName-" + getName(current.file), "unknown music").equals(settings.getString("MCSplanetMusicName-" + getName(music.file), "unknown music")) && current.file.length() == music.file.length()){
            music = current;
            return true;
        }
        return false;
    }

    public String realName(Fi file){
        if(pattern.matcher(file.nameWithoutExtension()).find()){
            return "encodeName_" + encodeName(file.nameWithoutExtension()) + "__" + file.length() + "." + file.extension();
        }else{
            return file.nameWithoutExtension() + "__" + file.length() + "." + file.extension();
        }
    }
    public String getName(Fi file){
        String realName = file.nameWithoutExtension();
        int index = realName.lastIndexOf("__");
        if(index < 0) return realName;
        if(!realName.startsWith("encodeName_")) return realName.substring(0, index);

        return decodeName(realName.substring(("encodeName_").length(), index));
    }

    private String encodeName(String input){
        if(input == null) return null;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
    private String decodeName(String input){
        if(input == null || input.length() <= 1) return input;
        try{
            return new String(Base64.getUrlDecoder().decode(input), StandardCharsets.UTF_8);
        }catch(IllegalArgumentException ignore){
            return input;
        }
    }
}
