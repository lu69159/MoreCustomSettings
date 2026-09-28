package MCS.customMusic;

import arc.*;
import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static MCS.enumClass.MCS_EventType.*;
import static MCS.customMusic.MusicTools.*;
import static MCS.main.*;
import static arc.Core.settings;
import static mindustry.Vars.*;

public class CustomMusicLoader {
    public Fi musicFolder, tmp;
    public MusicSeq menu, editor, ambient, dark, boss;
    public Seq<MusicSeq> planets = new Seq<>();
    final CustomSoundControl sound;

    public CustomMusicLoader(CustomSoundControl sound){
        this.sound = sound;
        musicFolder = Core.settings.getDataDirectory().child("MCS-music");
        init();

        tmp = musicFolder.child("tmp");
        tmp.mkdirs();

        Events.on(ImportMusicEvent.class, e -> {
            var name = e.name;
            if(name.equals("a") || name.equals("d") || name.equals("b")) importMusic(name);
            else importNamedMusic(name);
        });
        Events.on(MoveMusicEvent.class, e -> {
            if(e.isNamed) moveNamedMusic(e.music.file, e.name, e.isCopied);
            else moveMusic(e.music.file, e.name, e.isCopied);
        });
    }

    public void init(){
        menu = new MusicSeq("menu", musicFolder, sound.menuMusic){
            @Override
            public void setMusics(boolean isCustom) {
                sound.menuMusic = getNamedMusic(isCustom);
            }
        };
        editor = new MusicSeq("editor", musicFolder, sound.editorMusic){
            @Override
            public void setMusics(boolean isCustom) {
                sound.editorMusic = getNamedMusic(isCustom);
            }
        };
        ambient = new MusicSeq("ambient", musicFolder.child("a"), sound.ambientMusic, Seq.with(Musics.game1, Musics.game3, Musics.game6, Musics.game8, Musics.game9, Musics.fine)){
            @Override
            public void setMusics(boolean isCustom) {
                sound.ambientMusic = getMusics(isCustom);
            }
        };
        dark = new MusicSeq("dark", musicFolder.child("d"), sound.darkMusic, Seq.with(Musics.game2, Musics.game5, Musics.game7, Musics.game4)){
            @Override
            public void setMusics(boolean isCustom) {
                sound.darkMusic = getMusics(isCustom);
            }
        };
        boss = new MusicSeq("boss", musicFolder.child("b"), sound.bossMusic, Seq.with(Musics.boss1, Musics.boss2, Musics.game2, Musics.game5)){
            @Override
            public void setMusics(boolean isCustom) {
                sound.bossMusic = getMusics(isCustom);
            }
        };

        for(var p : content.planets()){
            planets.add(new MusicSeq(p.name, musicFolder.child("planets"), sound.planetMusicMap.get(p)){
                @Override
                public void setMusics(boolean isCustom) {
                    sound.planetMusicMap.put(content.planet(name), getNamedMusic(isCustom));
                }
            });
        }
    }

    public void reload(String name){
        if(name.equals("planets")){
            for(var m : planets){
                m.loadCustom();
            }
        }else{
            var seq = Seq.with(menu, editor, ambient, dark, boss).find(ms -> ms.name.equals(name));
            if(seq == null){
                ui.showException(new Exception("找不到对应名称的音乐组"));
                return;
            }
            seq.loadCustom();
        }
    }

    public void set(boolean isCustom){
        menu.setMusics(isCustom);
        editor.setMusics(isCustom);
        ambient.setMusics(isCustom);
        dark.setMusics(isCustom);
        boss.setMusics(isCustom);
        for(var m : planets){
            m.setMusics(isCustom);
        }
    }

    public void delete(){
        musicFolder.delete();
        init();
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

    public void renameMusic(Music m, String newName){
        try{
            if(getFileName(m.file).equals("menu")) settings.put("MCSmenuMusicName", newName);
            else if(getFileName(m.file).equals("editor")) settings.put("MCSeditorMusicName", newName);
            else{
                boolean[] isPlanetMusic = { false };
                planets.each(p -> {
                    if(p.namedMusic == m){
                        settings.put("MCSplanetMusicName-" + p.name, newName);
                        isPlanetMusic[0] = true;
                    }
                });
                if(isPlanetMusic[0]) return;

                m.file.moveTo(m.file.parent().child(encodeString(newName) + "__" + m.file.length() + "." + m.file.extension()));
                set(enabledCustomMusic());
            }
        }catch(Exception e){
            ui.showException(e);
        }
    }

    public void moveMusic(Fi from, String folderName, boolean isCopied){
        if(importMusicFromFi(Seq.with(from).toArray(), folderName,false, isCopied)){
            ui.showInfo(isCopied ? "@importMusic.copied" : "@importMusic.moved");
            set(enabledCustomMusic());
            MCSui.musicBar.rebuild();
        }
    }
    public void importMusic(String musicFi){
        FileChooser.open("ogg", "mp3").submitMulti(files -> {
            if(importMusicFromFi(files, musicFi, true, true)){
                ui.showInfo("@importMusic.imported");
                set(enabledCustomMusic());
                MCSui.musicBar.rebuild();
            }
        });
    }
    public boolean importMusicFromFi(Fi[] files, String folderName, boolean isImport, boolean isCopied){
        boolean successImported = false;
        for(var fi : files){
            try{
                Fi folder = Core.settings.getDataDirectory().child("MCS-music").child(folderName);
                if(!folder.exists()) folder.mkdirs();

                Fi to = folder.child(isImport ? encodeFileName(fi) : encodeString(getMusicName(fi)) + "__" + fi.length() + "." + fi.extension());
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
            set(enabledCustomMusic());
        }
    }
    public void importNamedMusic(String inputName){
        FileChooser.open("ogg", "mp3").submitMulti(files -> {
            if(importNamedMusicFromFi(files, inputName, true, true)){
                ui.showInfo("@importMusic.imported");
                set(enabledCustomMusic());
            }
        });
    }
    public boolean importNamedMusicFromFi(Fi[] files, String inputName, boolean isImport, boolean isCopied){
        boolean isPlanets = !inputName.equals("menu") && !inputName.equals("editor"),
                successImported = false;
        Fi folder = isPlanets ? musicFolder.child("planets") : musicFolder;
        if(!folder.exists()) folder.mkdirs();

        var fi = files[0];

        try{
            for(var f : folder.seq()){
                if(!f.isDirectory()){
                    if(getFileName(f).equals(inputName)) f.delete();
                }
            }

            String settingName = inputName.equals("menu") ? "MCSmenuMusicName" : inputName.equals("editor") ? "MCSeditorMusicName" : "MCSplanetMusicName-" + inputName;
            Fi to = folder.child(encodeString(inputName) + "__" + fi.length() + "." + fi.extension());
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

        return successImported;
    }

    public String getMusicName(Fi file){
        if(file == null) return "";
        String name = getFileName(file);
        if(file.parent().equals(musicFolder)) return settings.getString(name.equals("menu") ? "MCSmenuMusicName" : "MCSeditorMusicName", "unknown music");
        else if(file.parent().equals(musicFolder.child("planets"))) return settings.getString("MCSplanetMusicName-" + name, "unknown music");
        else return name;
    }
}
