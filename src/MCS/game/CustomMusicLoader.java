package MCS.game;

import arc.Core;
import arc.Events;
import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import arc.util.Nullable;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.type.Planet;
import mindustry.ui.*;

import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

import static MCS.main.*;
import static MCS.game.enumClass.MCSeventType.*;
import static arc.Core.settings;
import static mindustry.Vars.*;

public class CustomMusicLoader{
    public Fi musicFolder;
    public Fi ambient, dark, boss, planets, tmp;
    public Seq<Music> ambientMusic = new Seq<>();
    public Seq<Music> darkMusic = new Seq<>();
    public Seq<Music> bossMusic = new Seq<>();
    public Seq<Music> allInGameMusic = new Seq<>();
    public @Nullable Music menuMusic;
    public @Nullable Music editorMusic;
    public ObjectMap<Planet, Music> planetMusicMap = new ObjectMap<>();

    private boolean replacedFoo = false;
    private final Pattern pattern = Pattern.compile("[^-0-9a-zA-Z -)(\\[\\]]");

    public CustomMusicLoader(){
        loadFolder();
        Events.run(EventType.WorldLoadEvent.class, () -> {
            if(isFoo && !replacedFoo && settings.getBool("enableCustomMusic", false)){
                loadCustom();
                replacedFoo = true;
            }
        });
        Events.on(CustomMusicChangeEvent.class, e -> {
            if(e.enabled){
                loadCustom();
            }else{
                reset();
            }
            MCSui.musicBar.reload();
        });
        Events.on(ImportMusicEvent.class, e -> {
            if(e.music == null){
                importMusic(e.musicFi);
            }else{
                moveMusic(e.music.file, e.musicFi, e.isCopied);
                MCSui.menu.rebuildMusicList();
            }
            if(settings.getBool("enableCustomMusic")) loadCustom();
        });
        Events.on(ImportNamedMusicEvent.class, e -> {
            if(e.music == null){
                importNamedMusic(e.inputName);
            }else{
                moveNamedMusic(e.music.file, e.inputName, e.isCopied);
                MCSui.menu.rebuildMusicList();
            }
            if(settings.getBool("enableCustomMusic")) loadCustom();
        });
    }

    public void load(){
        if(settings.getBool("enableCustomMusic", false)){
            loadCustom();
        }
        else{
            reset();
        }
    }

    public void loadCustom(){
        if(headless) return;
        loadFolder();
        loadMusic(ambient, ambientMusic);
        loadMusic(dark, darkMusic);
        loadMusic(boss, bossMusic);

        allInGameMusic.clear();
        Seq<Music> tmpAll = Seq.withArrays(ambientMusic, darkMusic, bossMusic);
        for(var m : tmpAll){
            if(!allInGameMusic.contains(music -> isSameMusic(m, music, false))) allInGameMusic.add(m);
        }
        allInGameMusic.sortComparing(music -> getFileName(music.file));

        loadPlanetMusic();

        menuMusic = null;
        editorMusic = null;
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
        planetMusicMap.clear();
        try{
            for(var fi : planets.seq()){
                if(isMusic(fi)){
                    var planet = content.planets().find(p -> p.name.equals(getFileName(fi)));
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

        allInGameMusic = Seq.with(Musics.boss1, Musics.boss2, Musics.fine, Musics.game1, Musics.game2, Musics.game3, Musics.game4, Musics.game5, Musics.game6, Musics.game7, Musics.game8, Musics.game9);

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
            settings.remove("MCSplanetMusicName-" + getFileName(fi));
        }
        musicFolder.deleteDirectory();
        menuMusic = null;
        editorMusic = null;
        settings.remove("MCSmenuMusicName");
        settings.remove("MCSeditorMusicName");
        ui.showInfo("@clearMusic.clear");
        loadFolder();
    }

    public void moveMusic(Fi from, String musicFi, boolean isCopied){
        if(importMusicFromFi(Seq.with(from).toArray(), musicFi,false, isCopied)){
            ui.showInfo(isCopied ? "@importMusic.copied" : "@importMusic.moved");
            load();
            MCSui.musicBar.reload();
        }
    }
    public void importMusic(String musicFi){
        FileChooser.open("ogg", "mp3").submitMulti(files -> {
            if(importMusicFromFi(files, musicFi, true, true)){
                ui.showInfo("@importMusic.imported");
                load();
                MCSui.musicBar.reload();
            }
        });
    }
    public boolean importMusicFromFi(Fi[] files, String musicFi, boolean isImport, boolean isCopied){
        boolean successImported = false;
        for(var fi : files){
            try{
                Fi folder = Core.settings.getDataDirectory().child("MCS-music").child(musicFi);
                if(!folder.exists()) folder.mkdirs();

                Fi to = folder.child(isImport ? realFileName(fi) : realString(getMusicName(fi)) + "__" + fi.length() + "." + fi.extension());
                if(isImport || isCopied) fi.copyTo(to);
                else fi.moveTo(to);
                successImported = true;
            }catch(Exception e){
                ui.showException(e);
            }
        }
        return successImported;
    }

    public void moveNamedMusic(Fi from, String inputName, boolean isCopied){
        if(importNamedMusicFromFi(Seq.with(from).toArray(), inputName, false, isCopied)){
            ui.showInfo(isCopied ? "@importMusic.copied" : "@importMusic.moved");
            load();
        }
    }
    public void importNamedMusic(String inputName){
        FileChooser.open("ogg", "mp3").submitMulti(files -> {
            if(importNamedMusicFromFi(files, inputName, true, true)){
                ui.showInfo("@importMusic.imported");
                load();
            }
        });
    }
    public boolean importNamedMusicFromFi(Fi[] files, String inputName, boolean isImport, boolean isCopied){
        boolean isPlanets = !inputName.equals("menu") && !inputName.equals("editor"),
                successImported = false;
        Fi folder = isPlanets ? planets : musicFolder;
        if(!folder.exists()) folder.mkdirs();

        for(var fi : files){
            try{
                for(var f : folder.seq()){
                    if(!f.isDirectory()){
                        if(getFileName(f).equals(inputName)) f.delete();
                    }
                }

                String settingName = inputName.equals("menu") ? "MCSmenuMusicName" : inputName.equals("editor") ? "MCSeditorMusicName" : "MCSplanetMusicName-" + inputName;
                Fi to = folder.child(realString(inputName) + "__" + fi.length() + "." + fi.extension());
                if(isImport){
                    Core.settings.put(settingName, fi.nameWithoutExtension());
                    fi.copyTo(to);
                }else{
                    Core.settings.put(settingName, getMusicName(fi));
                    if(isCopied) fi.copyTo(to);
                    else fi.moveTo(to);
                }
                successImported = true;
            }catch(Exception e){
                ui.showException(e);
            }
        }
        return successImported;
    }

    public boolean isMusic(Fi fi){
        return (fi.extension().equals("ogg") || fi.extension().equals("mp3")) && fi.name().lastIndexOf("__") != -1;
    }

    public boolean isSameMusic(Music current, Music music, boolean getFromSetting){
        if(current == null || music == null) return false;
        if(current == music) return true;
        if(getFromSetting){
            if(settings.getString("MCSplanetMusicName-" + getFileName(current.file), "unknown music").equals(settings.getString("MCSplanetMusicName-" + getFileName(music.file), "unknown music")) && current.file.length() == music.file.length()){
                music = current;
                return true;
            }
        }else{
            return getFileName(current.file).equals(getFileName(music.file)) && current.file.length() == music.file.length();
        }

        return false;
    }

    public String realString(String nameWithoutExtension){
        if(pattern.matcher(nameWithoutExtension).find()){
            return "encodeName_" + encodeName(nameWithoutExtension);
        }else{
            return nameWithoutExtension;
        }
    }
    public String realFileName(Fi file){
        if(pattern.matcher(file.nameWithoutExtension()).find()){
            return "encodeName_" + encodeName(file.nameWithoutExtension()) + "__" + file.length() + "." + file.extension();
        }else{
            return file.nameWithoutExtension() + "__" + file.length() + "." + file.extension();
        }
    }
    public String getFileName(Fi file){
        String realName = file.nameWithoutExtension();
        int index = realName.lastIndexOf("__");
        if(index < 0) return realName;
        if(!realName.startsWith("encodeName_")) return realName.substring(0, index);

        return decodeName(realName.substring(("encodeName_").length(), index));
    }
    public String getMusicName(Fi file){
        String name = getFileName(file);
        if(file.parent().equals(musicFolder)) return settings.getString(name.equals("menu") ? "MCSmenuMusicName" : "MCSeditorMusicName", "unknown music");
        else if(file.parent().equals(planets)) return settings.getString("MCSplanetMusicName-" + name, "unknown music");
        else return name;
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
