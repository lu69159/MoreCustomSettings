package MCS.customMusic;

import MCS.enumClass.*;
import arc.*;
import arc.audio.*;
import arc.files.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.audio.*;
import mindustry.gen.*;
import mindustry.type.*;

import static arc.Core.settings;
import static mindustry.Vars.*;
import static mindustry.game.EventType.*;
import static MCS.main.*;
import static MCS.customMusic.MusicTools.*;

public class CustomSoundControl extends SoundControl{
    public boolean preview = false;
    public @Nullable Music previewMusic;
    public MusicMode mode;

    public Seq<Music> allInGameMusic = new Seq<>();
    public @Nullable Music menuMusic, editorMusic;
    public ObjectMap<Planet, Music> planetMusicMap = new ObjectMap<>();

    public CustomMusicLoader musicLoader;

    public CustomSoundControl(){
        mode = MusicMode.valueOf(settings.getString("MCS-musicMode", "normal"));
        musicLoader = new CustomMusicLoader(this);
        reloadAllInGameMusic();

        Events.on(MCS_EventType.MusicBarChangeEvent.class, e -> {
            if(e.enabled){
                mode = MusicMode.valueOf(settings.getString("MCS-musicMode", "normal"));
            }else{
                if(state.rules.disableMusic) state.rules.disableMusic = false;
                mode = MusicMode.normal;
            }
        });
        Events.on(MCS_EventType.CustomMusicChangeEvent.class, e -> {
            musicLoader.set(e.enabled);
            if(current != null){
                current.stop();
                current = null;
            }
            reloadAllInGameMusic();
            MCSui.musicBar.rebuild();
        });
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
                if(enabledCustomMusic() && planetMusicMap.get(ui.planet.state.planet) != null){
                    boolean same = isSameMusic(current, planetMusicMap.get(ui.planet.state.planet), true);
                    if(current != null && same){
                        play(current);
                    }else{
                        play(planetMusicMap.get(ui.planet.state.planet));
                    }
                }else{
                    play(ui.planet.state.planet.launchMusic);
                }
            }else if(ui.editor.isShown()){
                if(enabledCustomMusic() && editorMusic != null){
                    play(editorMusic);
                }else{
                    play(Musics.editor);
                }
            }else{
                if(enabledCustomMusic() && menuMusic != null){
                    play(menuMusic);
                }else{
                    play(Musics.menu);
                }
            }
        }else if(state.rules.editor){
            silenced = false;
            if(enabledCustomMusic() && editorMusic != null){
                play(editorMusic);
            }else{
                play(Musics.editor);
            }
        }else{
            //this just fades out the last track to make way for ingame music
            silence();

            if(!state.rules.disableMusic){
                if(settings.getBool("instantChangeBossMusic", false) && state.boss() != null){
                    if(current == null){
                        var bm = getBossMusic().random(lastRandomPlayed);
                        playOnce(bm);
                        if(current == bm) silenced = true;
                    }else if(!getBossMusic().contains(current)){
                        fade = Mathf.clamp(fade - Time.delta / foutTime);
                        current.setVolume(fade * Core.settings.getInt("musicvol") / 100.0f);
                        if(fade <= 0.01f){
                            current.stop();
                            current = null;
                            var bm = getBossMusic().random(lastRandomPlayed);
                            playOnce(bm);
                            if(current == bm) silenced = true;
                        }
                    }
                }else{
                    if(alwaysPlayMusic()){
                        playByMode();
                    }
                    else if(Time.timeSinceMillis(lastPlayed) > 1000 * musicInterval / 60f) {
                        //chance to play it per interval
                        if (Mathf.chance(musicChance)) {
                            lastPlayed = Time.millis();
                            playByMode();
                        }
                    }

                    if(fade < 1f && current != null && allInGameMusic.contains(current)){
                        fade = Mathf.clamp(fade + Time.delta / foutTime);
                        current.setVolume(fade * Core.settings.getInt("musicvol") / 100.0f);
                    }
                }
            }
        }

        updateLoops();
    }

    public void playByMode(){
        if(current != null) return;
        if(mode == MusicMode.seq){
            if(allInGameMusic.indexOf(lastRandomPlayed) < 1){
                playRandom();
            }else{
                int nextIndex = allInGameMusic.indexOf(lastRandomPlayed) + 1 < allInGameMusic.size ? allInGameMusic.indexOf(lastRandomPlayed) + 1 : 0;
                playOnce(allInGameMusic.get(nextIndex));
            }
        }else if(mode == MusicMode.loop){
            if(lastRandomPlayed != null && allInGameMusic.contains(lastRandomPlayed)) playOnce(lastRandomPlayed);
            else playRandom();
        }else if(mode == MusicMode.shuf){
            playOnce(allInGameMusic.random(lastRandomPlayed));
        }else if(mode == MusicMode.normal){
            playRandom();
        }
    }

    @Override
    protected Seq<Music> getAmbientMusic() {
        if(enabledCustomMusic()){
            return ambientMusic;
        }else if(state.rules.ambientMusic != null){
            return state.rules.ambientMusic.map(MusicContainer::get).removeAll(m -> m == null);
        }else{
            return state.getPlanet() != null && state.getPlanet().ambientMusic != null ? state.getPlanet().ambientMusic : ambientMusic;
        }
    }
    @Override
    protected Seq<Music> getDarkMusic() {
        if(enabledCustomMusic()){
            return darkMusic;
        }else if(state.rules.darkMusic != null){
            return state.rules.darkMusic.map(MusicContainer::get).removeAll(m -> m == null);
        }else{
            return state.getPlanet() != null && state.getPlanet().darkMusic != null ? state.getPlanet().darkMusic : darkMusic;
        }
    }
    @Override
    protected Seq<Music> getBossMusic() {
        if(enabledCustomMusic()){
            return bossMusic;
        }else if(state.rules.darkMusic != null){
            return state.rules.darkMusic.map(MusicContainer::get).removeAll(m -> m == null);
        }else{
            return state.getPlanet() != null && state.getPlanet().darkMusic != null ? state.getPlanet().darkMusic : bossMusic;
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

    @Override
    protected void reload(){
        current = null;
        fade = 0f;

        for(var sound : Core.assets.getAll(Sound.class, new Seq<>())){
            var file = Fi.get(Core.assets.getAssetFileName(sound));
            if(file.parent().name().equals("ui")){
                sound.setBus(uiBus);
            }
        }

        Events.fire(new MusicRegisterEvent());
    }

    public void reloadAllInGameMusic(){
        allInGameMusic.clear();
        Seq<Music> tmp = Seq.withArrays(ambientMusic, darkMusic, bossMusic);
        for(var m : tmp){
            if(!allInGameMusic.contains(music -> getFileName(m.file).equals(getFileName(music.file)) && m.file.length() == music.file.length())){
                allInGameMusic.add(m);
            }
        }
        allInGameMusic.sortComparing(music -> getFileName(music.file));
    }
}
