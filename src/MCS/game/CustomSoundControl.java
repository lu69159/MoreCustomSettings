package MCS.game;

import arc.*;
import arc.audio.*;
import arc.math.Mathf;
import arc.util.*;
import mindustry.Vars;
import mindustry.audio.*;
import mindustry.content.*;
import mindustry.gen.Musics;

import static arc.Core.settings;
import static mindustry.Vars.*;
import static mindustry.game.EventType.*;
import static MCS.main.*;

public class CustomSoundControl extends SoundControl{
    public boolean preview = false;
    public boolean instantChangeBossMusic = settings.getBool("instantChangeBossMusic", false); //TODO
    public @Nullable Music previewMusic;

    public CustomSoundControl(){
        super();
        Events.on(WaveEvent.class, e -> {
            boolean boss = state.rules.spawns.contains((group) -> group.getSpawned(state.wave - 2) > 0 && group.effect == StatusEffects.boss);
            if(instantChangeBossMusic && shouldPlay() && current != null && boss){
                current.setVolume(fade * Core.settings.getInt("musicvol") / 100.0f);
            }
        });
    }

    public boolean enabledCustomMusic(){
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

        if(!(control.sound instanceof CustomSoundControl)){
            menu.loadCustomSoundControl();
        }

        if(keepSilent){
            keepSilent = false;
            stop();
        }else if(preview && previewMusic != null){
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
                if(enabledCustomMusic() && musicLoader.editorMusic != null){
                    play(musicLoader.editorMusic);
                }else{
                    play(Musics.editor);
                }
            }else{
                if(enabledCustomMusic() && musicLoader.menuMusic != null){
                    play(musicLoader.menuMusic);
                }else{
                    play(Musics.menu);
                }
            }
        }else if(state.rules.editor){
            silenced = false;
            if(enabledCustomMusic() && musicLoader.editorMusic != null){
                play(musicLoader.editorMusic);
            }else{
                play(Musics.editor);
            }
        }else {
            //this just fades out the last track to make way for ingame music
            silence();

            /* TODO: 目前不确定这段有没必要
            boolean boss = state.rules.spawns.contains((group) -> group.getSpawned(state.wave - 2) > 0 && group.effect == StatusEffects.boss);
            if(instantChangeBossMusic && boss){
                playOnce(getBossMusic().random(lastRandomPlayed));
            }
             */
            if(!state.rules.disableMusic || enabledCustomMusic()){
                if (alwaysPlayMusic()) {
                    if (current == null) {
                        playRandom();
                    }
                } else if (Time.timeSinceMillis(lastPlayed) > 1000 * musicInterval / 60f) {
                    //chance to play it per interval
                    if (Mathf.chance(musicChance)) {
                        lastPlayed = Time.millis();
                        playRandom();
                    }
                }
            }
        }

        updateLoops();
    }

    @Override
    public void playRandom() {
        if(settings.getBool("enableCustomMusic", false)){
            if(Vars.state.boss() != null){
                playOnce(getBossMusic().random(lastRandomPlayed));
            }else if (isDark()){
                playOnce(getDarkMusic().random(lastRandomPlayed));
            }else{
                playOnce(getAmbientMusic().random(lastRandomPlayed));
            }
        }else{
            if(Vars.state.boss() != null){
                playOnce(bossMusic.random(lastRandomPlayed));
            }else if (isDark()){
                playOnce(darkMusic.random(lastRandomPlayed));
            }else{
                playOnce(ambientMusic.random(lastRandomPlayed));
            }
        }
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
        previewMusic = null;
    }
}
