package MCS.game;

import arc.*;
import arc.audio.*;
import arc.audio.Filters.*;
import arc.math.Mathf;
import arc.util.*;
import mindustry.audio.SoundControl;
import mindustry.gen.Musics;

import static arc.Core.settings;
import static mindustry.Vars.*;
import static MCS.main.*;

public class CustomSoundControl extends SoundControl{
    public boolean preview = false;
    public @Nullable Music previewMusic;

    public boolean custom(){
        return settings.getBool("enableCustomMusic", false);
    }

    @Override
    public void update(){
        boolean paused = state.isGame() && Core.scene.hasDialog();
        boolean playing = state.isGame();
        //check if current track is finished
        if(current != null && !current.isPlaying()){
            current = null;
            fade = 0f;
        }

        if(timer.get(1, 30f)){
            Core.audio.soundBus.fadeFilterParam(0, Filters.paramWet, paused ? 1f : 0f, 0.4f);
        }

        //play/stop ordinary effects
        if(playing != wasPlaying){
            wasPlaying = playing;

            if(playing){
                Core.audio.soundBus.play();
                setupFilters();
            }else{
                //stopping a single audio bus stops everything else, yay!
                Core.audio.soundBus.stop();
                //play music bus again, as it was stopped above
                Core.audio.musicBus.play();

                Core.audio.soundBus.play();
            }
        }

        Core.audio.setPaused(Core.audio.soundBus.id, state.isPaused());

        if(preview && previewMusic != null){
            if(current != previewMusic){
                if(current != null) current.stop();
                current = previewMusic;
                current.setVolume(Core.settings.getInt("musicvol") / 100f);
                current.setLooping(false);
                current.play();
            }
        }else if(state.isMenu()){
            silenced = false;
            if(ui.planet.isShown()){
                play(ui.planet.state.planet.launchMusic);
            }else if(ui.editor.isShown()){
                if(custom() && musicLoader.editorMusic != null){
                    play(musicLoader.editorMusic);
                }else{
                    play(Musics.editor);
                }
            }else{
                if(custom() && musicLoader.menuMusic != null){
                    play(musicLoader.menuMusic);
                }else{
                    play(Musics.menu);
                }
            }
        }else if(state.rules.editor){
            silenced = false;
            if(custom() && musicLoader.editorMusic != null){
                play(musicLoader.editorMusic);
            }else{
                play(Musics.editor);
            }
        }else{
            //this just fades out the last track to make way for ingame music
            silence();

            if(Core.settings.getBool("alwaysmusic")){
                if(current == null){
                    playRandom();
                }
            }else if(Time.timeSinceMillis(lastPlayed) > 1000 * musicInterval / 60f){
                //chance to play it per interval
                if(Mathf.chance(musicChance)){
                    lastPlayed = Time.millis();
                    playRandom();
                }
            }
        }

        updateLoops();
    }

    public void playPreView(Music music){
        if(music == null) return;
        preview = true;
        previewMusic = music;
    }
    public void stopPreView(){
        preview = false;
        if(current != null) current.stop();
        current = null;
    }
}
