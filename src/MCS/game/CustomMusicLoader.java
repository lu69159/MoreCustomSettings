package MCS.game;

import arc.*;
import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class CustomMusicLoader{
    public Fi musicFolder;
    public Fi ambient, dark, boss;
    public Seq<Music> ambientMusic = new Seq<>();
    public Seq<Music> darkMusic = new Seq<>();
    public Seq<Music> bossMusic = new Seq<>();

    public void load(){
        loadFolder();
        try{
            for(var fi : ambient.seq()){
                if(isMusic(fi)){
                    ambientMusic.add(new Music(fi));
                }
                else{
                    fi.delete();
                }
            }
            for(var fi : dark.seq()){
                if(isMusic(fi)){
                    darkMusic.add(new Music(fi));
                }
                else{
                    fi.delete();
                }
            }
            for(var fi : musicFolder.seq()){
                if(isMusic(fi)){
                    bossMusic.add(new Music(fi));
                }
                else{
                    fi.delete();
                }
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }

        control.sound.ambientMusic.clear().add(ambientMusic);
        control.sound.darkMusic.clear().add(darkMusic);
        control.sound.bossMusic.clear().add(bossMusic);
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

    public void reset(){
        ambientMusic = Seq.with(Musics.game1, Musics.game3, Musics.game6, Musics.game8, Musics.game9, Musics.fine);
        darkMusic = Seq.with(Musics.game2, Musics.game5, Musics.game7, Musics.game4);
        bossMusic = Seq.with(Musics.boss1, Musics.boss2, Musics.game2, Musics.game5);

        control.sound.ambientMusic.clear().add(ambientMusic);
        control.sound.darkMusic.clear().add(darkMusic);
        control.sound.bossMusic.clear().add(bossMusic);
    }

    public void delete(){
        musicFolder = Core.settings.getDataDirectory().child("MCS-music");
        if(musicFolder.exists()){
            musicFolder.deleteDirectory();
        }
        loadFolder();
    }

    public Runnable importMusic(String musicFi){
        return () -> platform.showMultiFileChooser(fi -> {
            try{
                Fi folder = Core.settings.getDataDirectory().child("MCS-music").child(musicFi);
                if(!folder.exists()) folder.mkdirs();
                fi.copyTo(folder);
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }, "ogg", "mp3");
    }

    public boolean isMusic(Fi fi){
        return fi.extension().equals("ogg") || fi.extension().equals("mp3");
    }
}
