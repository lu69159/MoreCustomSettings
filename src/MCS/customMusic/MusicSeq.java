package MCS.customMusic;

import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;

import static MCS.customMusic.MusicTools.*;
import static mindustry.Vars.*;

public abstract class MusicSeq{
    final String name;
    Fi folder;
    Seq<Music> originalMusics;
    boolean isNamed;

    public @Nullable Music namedMusic;
    public Seq<Music> musics = new Seq<>();

    public MusicSeq(String name, Fi folder, Music goalNamedMusic){
        this(name, folder, new Seq<>(), true);
    }
    public MusicSeq(String name, Fi folder, Seq<Music> goalMusics, Seq<Music> originalMusics){
        this(name, folder, originalMusics, false);
    }
    public MusicSeq(String name, Fi folder, Seq<Music>originalMusics, boolean isNamed){
        this.name = name;
        this.folder = folder;
        this.originalMusics = originalMusics;
        this.isNamed = isNamed;

        if(!folder.exists()) folder.mkdirs();
        loadCustom();
    }

    public void loadCustom(){
        try{
            if(isNamed){
                for(Fi file : folder.seq()){
                    if(isMusic(file)){
                        if(getFileName(file).equals(name)){
                            namedMusic = new Music(file){
                                @Override
                                public void setLooping(boolean isLooping){}
                            };
                        }
                    }
                }
            }else{
                musics.clear();
                for(Fi file : folder.seq()){
                    if(isMusic(file)){
                        musics.add(new Music(file){
                            @Override
                            public void setLooping(boolean isLooping){}
                        });
                    }
                }
            }
        }catch(Exception e){
            ui.showException(e);
        }
        setMusics(enabledCustomMusic());
    }

    /**
     * abstract in actually
     */
    public void setMusics(boolean isCustom){}



    protected Seq<Music> getMusics(boolean isCustom){
        return isCustom ? musics : originalMusics;
    }
    protected Music getNamedMusic(boolean isCustom){
        if(isCustom && namedMusic != null) return namedMusic;
        else if(name.equals("menu")) return Musics.menu;
        else if(name.equals("editor")) return Musics.editor;
        else return Musics.launch;
    }
}
