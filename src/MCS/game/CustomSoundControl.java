package MCS.game;

import MCS.game.enumClass.*;
import arc.*;
import arc.audio.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.audio.*;
import mindustry.gen.Musics;

import static arc.Core.settings;
import static mindustry.Vars.*;
import static MCS.main.*;

public class CustomSoundControl extends SoundControl{
    public boolean preview = false;
    public @Nullable Music previewMusic;
    public MusicMode mode;

    public CustomSoundControl(){
        mode = MusicMode.valueOf(settings.getString("MCS-musicMode", "normal"));
        Events.on(MCSeventType.MusicBarChangeEvent.class, e -> {
            if(e.enabled){
                mode = MusicMode.valueOf(settings.getString("MCS-musicMode", "normal"));
            }else{
                if(state.rules.disableMusic) state.rules.disableMusic = false;
                mode = MusicMode.normal;
            }
        });
        Events.run(MCSeventType.CustomMusicChangeEvent.class, () -> {
            if(current != null){
                current.stop();
                current = null;
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
                if(enabledCustomMusic() && musicLoader.planetMusicMap.get(ui.planet.state.planet) != null){
                    boolean same = musicLoader.isSameMusic(current, musicLoader.planetMusicMap.get(ui.planet.state.planet), true);
                    if(current != null && same){
                        play(current);
                    }else{
                        play(musicLoader.planetMusicMap.get(ui.planet.state.planet));
                    }
                }else{
                    play(ui.planet.state.planet.launchMusic);
                }
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
        }else{
            //this just fades out the last track to make way for ingame music
            silence();

            if(!state.rules.disableMusic){
                if(settings.getBool("instantChangeBossMusic", false) && state.boss() != null){
                    var m = enabledCustomMusic() ? bossMusic : getBossMusic();

                    if(current == null){
                        var bm = m.random(lastRandomPlayed);
                        playOnce(bm);
                        if(current == bm) silenced = true;
                    }else if(!m.contains(current)){
                        fade = Mathf.clamp(fade - Time.delta / foutTime);
                        current.setVolume(fade * Core.settings.getInt("musicvol") / 100.0f);
                        if(fade <= 0.01f){
                            current.stop();
                            current = null;
                            var bm = m.random(lastRandomPlayed);
                            playOnce(bm);
                            if(current == bm) silenced = true;
                        }
                    }
                }else{
                    if(alwaysPlayMusic()){
                        if(current == null) playByMode();
                    }
                    else if(Time.timeSinceMillis(lastPlayed) > 1000 * musicInterval / 60f) {
                        //chance to play it per interval
                        if (Mathf.chance(musicChance)) {
                            lastPlayed = Time.millis();
                            playByMode();
                        }
                    }

                    if(fade < 1f && current != null && musicLoader.allInGameMusic.contains(current)){
                        fade = Mathf.clamp(fade + Time.delta / foutTime);
                        current.setVolume(fade * Core.settings.getInt("musicvol") / 100.0f);
                    }
                }
            }
        }

        updateLoops();
    }

    public void playByMode(){
        if(mode == MusicMode.seq){
            if(musicLoader.allInGameMusic.indexOf(lastRandomPlayed) < 1){
                playRandom();
            }else{
                int nextIndex = musicLoader.allInGameMusic.indexOf(lastRandomPlayed) + 1 < musicLoader.allInGameMusic.size ? musicLoader.allInGameMusic.indexOf(lastRandomPlayed) + 1 : 0;
                playOnce(musicLoader.allInGameMusic.get(nextIndex));
            }
        }else if(mode == MusicMode.loop){
            if(lastRandomPlayed != null && musicLoader.allInGameMusic.contains(lastRandomPlayed)) playOnce(lastRandomPlayed);
            else playRandom();
        }else if(mode == MusicMode.shuf){
            playOnce(musicLoader.allInGameMusic.random(lastRandomPlayed));
        }else if(mode == MusicMode.normal){
            playRandom();
        }
    }

    @Override
    public void playRandom(){
        Seq<Music> bossSeq, darkSeq, ambientSeq;
        if(settings.getBool("enableCustomMusic", false)){
            bossSeq = bossMusic;
            darkSeq = darkMusic;
            ambientSeq = ambientMusic;
        }else{
            bossSeq = getBossMusic();
            darkSeq = getDarkMusic();
            ambientSeq = getAmbientMusic();
        }
        if(state.boss() != null){
            playOnce(bossSeq.random(lastRandomPlayed));
        }else if (isDark()){
            playOnce(darkSeq.random(lastRandomPlayed));
        }else{
            playOnce(ambientSeq.random(lastRandomPlayed));
        }
    }
    @Nullable
    public Music getLastRandomPlayed(){
        return lastRandomPlayed;
    }

    public void playPreView(Music music){
        if(music == null) return;
        preview = true;
        previewMusic = music;
    }
    public void stopPreView(){
        if(preview){
            preview = false;
            if(current != null) current.stop();
            current = null;
            previewMusic = null;
        }
    }
}
