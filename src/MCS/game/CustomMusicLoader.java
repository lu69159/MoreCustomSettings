package MCS.game;

import arc.Core;
import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import mindustry.game.*;
import mindustry.gen.*;

import java.nio.file.*;

import static arc.Core.settings;
import static mindustry.Vars.*;

public class CustomMusicLoader{
    public Fi musicFolder;
    public Fi ambient, dark, boss;
    public Seq<Music> ambientMusic = new Seq<>();
    public Seq<Music> darkMusic = new Seq<>();
    public Seq<Music> bossMusic = new Seq<>();

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
        for(var f : musicFolder.seq()){
            if(!f.isDirectory()){
                String n = f.name().split("__", 2)[0];
                if(n.equals("menu")){
                    try{
                        Musics.menu = new Music(f){
                            @Override
                            public void setLooping(boolean isLooping){}
                        };
                    }catch (Exception e){
                        ui.showException(e);
                    }
                }
                else if(n.equals("editor")){
                    try{
                        Musics.editor = new Music(f){
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
    }

    public void loadMusic(Fi folder, Seq<Music> musicSeq){
        musicSeq.clear();
        try {
            for(var fi : folder.seq()) {
                if (isMusic(fi)){
                    musicSeq.add(new Music(fi));
                }
            }
        }catch (Exception e){
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

        // still had problem: can't play vanilla Musics.xxx when reset
        Musics.menu = tree.loadMusic("menu");
        Musics.editor = tree.loadMusic("editor");
    }

    public void delete(){
        reset();
        settings.put("enableCustomMusic", false);
        musicFolder = Core.settings.getDataDirectory().child("MCS-music");
        if(musicFolder.exists()){
            musicFolder.deleteDirectory();
        }
        ui.showInfo("@clearMusic.clear");
        loadFolder();
    }

    public Runnable importMusic(String musicFi){
        return () -> platform.showMultiFileChooser(fi -> {
            try{
                Fi folder = Core.settings.getDataDirectory().child("MCS-music").child(musicFi);
                if(!folder.exists()) folder.mkdirs();

                fi.copyTo(folder);
                Path source = Paths.get(folder.path() + "/" + fi.name());
                Path to = Paths.get(folder.path() + "/" + realName(fi));
                Files.move(source, to, StandardCopyOption.REPLACE_EXISTING);

                ui.showInfo("@importMusic.imported");
                load();
            }catch(Exception e){
                ui.showException(e);
            }
        }, "ogg", "mp3");
    }

    public Runnable importMenuMusic(String name){
        return () -> platform.showMultiFileChooser(fi -> {
            try{
                Fi folder = Core.settings.getDataDirectory().child("MCS-music");
                if(!folder.exists()) folder.mkdirs();

                for(var f : folder.seq()){
                    if(!f.isDirectory()){
                        String n = f.name().split("__", 2)[0];
                        if(n.equals(name)) f.delete();
                    }
                }

                fi.copyTo(folder);
                Path source = Paths.get(folder.path() + "/" + fi.name());
                Path to = Paths.get(folder.path() + "/" + name + "__" + fi.length() + "." + fi.extension());
                Files.move(source, to, StandardCopyOption.REPLACE_EXISTING);

                ui.showInfo("@importMusic.imported");
                load();
            }catch(Exception e){
                ui.showException(e);
            }
        }, "ogg", "mp3");
    }

    public String realName(Fi file){
        int dotIndex = file.name().lastIndexOf("__");
        if(dotIndex != -1) return file.name();

        String name = file.nameWithoutExtension().replaceAll("[^-0-9a-zA-Z]", "");
        return name + "__" + file.length() + "." + file.extension();
    }

    public boolean isMusic(Fi fi){
        return (fi.extension().equals("ogg") || fi.extension().equals("mp3")) && fi.name().lastIndexOf("__") != -1;
    }
}
