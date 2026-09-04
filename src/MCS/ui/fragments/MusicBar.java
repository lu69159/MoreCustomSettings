package MCS.ui.fragments;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import MCS.game.CustomSoundControl;

import static arc.Core.*;
import static mindustry.Vars.*;
import static MCS.main.*;

public class MusicBar{
    public boolean posted = false;
    public boolean openList = false;
    private float barX = -1f, barY = -1f;
    private Table bar, list;

    public MusicBar(){
        Events.on(EventType.WorldLoadEvent.class, e -> {
            if(!posted){
                app.post(() -> {
                    build(ui.hudGroup);
                });
                posted = true;
            }
        });
    }

    public void build(Group parent){
        ImageButton moveButton = new ImageButton(Icon.move, Styles.clearNonei);
        moveButton.touchable = Touchable.enabled;

        float barScl = settings.getInt("musicBarScl", 100)/100f;
        Table musicBarTable = new Table(){{
            setWidth(barScl * Scl.scl(480f));
            setHeight(barScl * Scl.scl(100f));
            x = barX < 0 ? graphics.getWidth() / 4f : barX;
            y = barY < 0 ? graphics.getHeight() * 7/8f : barY;
            background(Styles.black3);
            labelWrap(() -> control.sound.getCurrent() == null ? ((CustomSoundControl)control.sound).getLastRandomPlayed() == null ?
                    "@empty" : musicLoader.getName(((CustomSoundControl)control.sound).getLastRandomPlayed().file) : musicLoader.getName(control.sound.getCurrent().file)).growX().left().row();
            table(buttons -> {
                buttons.defaults().size(barScl * 60f);
                buttons.button(Icon.leftOpen, Styles.clearNonei, () -> {
                    var sound = (CustomSoundControl)control.sound;
                    var m = sound.getCurrent() == null ? sound.getLastRandomPlayed() == null ? null : sound.getLastRandomPlayed() : sound.getCurrent();

                    if(state.rules.disableMusic) state.rules.disableMusic = false;
                    if(m == null){
                        sound.playRandom();
                    }else{
                        int index = musicLoader.allInGameMusic.indexOf(m) + 1;
                        if(index < 1){
                            sound.playMusic(musicLoader.allInGameMusic.random(),true);
                        }else{
                            int nextIndex = index < musicLoader.allInGameMusic.size ? index : 0;
                            sound.playMusic(musicLoader.allInGameMusic.get(nextIndex), true);
                        }
                    }
                }).disabled(dis -> settings.getBool("instantChangeBossMusic", false) && state.boss() != null).left().padRight(20f);

                var style = new ImageButton.ImageButtonStyle(Styles.clearNonei);
                style.imageUp = Icon.play;
                style.imageChecked = Icon.pause;
                buttons.button(Icon.play, style, () -> {
                    if(state.rules.disableMusic){
                        state.rules.disableMusic = false;
                        if(control.sound.getCurrent() == null){
                            var sound = (CustomSoundControl)control.sound;
                            if((sound.getLastRandomPlayed() == null)) sound.playRandom();
                            else sound.playMusic(sound.getLastRandomPlayed(),true);
                        }
                    }else if(control.sound.getCurrent() == null){
                        control.sound.playRandom();
                    }else{
                        state.rules.disableMusic = true;
                        control.sound.getCurrent().stop();
                    }

                }).checked(chk -> !state.rules.disableMusic && control.sound.getCurrent() != null).left().padRight(20f);

                buttons.button(Icon.rightOpen, Styles.clearNonei, () -> {
                    var sound = (CustomSoundControl)control.sound;
                    var m = sound.getCurrent() == null ? sound.getLastRandomPlayed() == null ? null : sound.getLastRandomPlayed() : sound.getCurrent();

                    if(state.rules.disableMusic) state.rules.disableMusic = false;
                    if(m == null){
                        sound.playRandom();
                    }else{
                        int index = musicLoader.allInGameMusic.indexOf(m) - 1;
                        if(index < -1){
                            sound.playMusic(musicLoader.allInGameMusic.random(),true);
                        }else{
                            int nextIndex = index == -1 ? musicLoader.allInGameMusic.size - 1 : index;
                            sound.playMusic(musicLoader.allInGameMusic.get(nextIndex), true);
                        }
                    }
                }).disabled(dis -> settings.getBool("instantChangeBossMusic", false) && state.boss() != null).left().padRight(20f);

                buttons.button(Icon.menu, Styles.clearNonei, () -> {
                    openList = !openList;
                }).checked(chk -> openList).left().padRight(20f);

                buttons.add(moveButton).tooltip("@dragToMove").left();
            }).width(barScl * (5*60f + 4*20f)).fillY().center();
            visible(() -> settings.getBool("enableMusicBar", false) && state.isGame());
        }};

        Table musicListTable = new Table(){{
            setWidth(barScl * Scl.scl(480f));
            float h = Math.min(musicLoader.allInGameMusic.size * Scl.scl(32f), graphics.getHeight() / 4f);
            setHeight(h);
            x = musicBarTable.x;
            y = musicBarTable.y - h;
            pane(list -> {
                list.background(Styles.black5);
                boolean found = false;
                for (var music : musicLoader.allInGameMusic) {
                    String name = musicLoader.getName(music.file);
                    list.table(Styles.none, mt -> {
                        mt.labelWrap(name).left().fillX().expandX();
                        mt.button(Icon.play, Styles.clearNonei, () -> {
                            control.sound.playMusic(music, true);
                        }).height(32f).disabled(dis -> control.sound.getCurrent() == music || (settings.getBool("instantChangeBossMusic", false) && state.boss() != null)).padLeft(10);
                    }).growX().left().row();
                    found = true;
                }
                if (!found) list.add("@musicList.empty").padLeft(10).left().row();
            }).visible(() -> musicBarTable.visible && openList).grow();
        }};

        moveButton.addListener((new ElementGestureListener(){
            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                musicBarTable.moveBy(deltaX, deltaY);

                if(musicBarTable.y > parent.getHeight() - musicBarTable.getHeight()){
                    musicBarTable.y = parent.getHeight() - musicBarTable.getHeight();
                }else if(musicBarTable.y < 0){
                    musicBarTable.y = 0;
                }
                if(musicBarTable.x > parent.getWidth() - musicBarTable.getWidth()){
                    musicBarTable.x = parent.getWidth() - musicBarTable.getWidth();
                }else if(musicBarTable.x < 0){
                    musicBarTable.x = 0;
                }

                barX = musicBarTable.x;
                barY = musicBarTable.y;

                if(musicBarTable.y > parent.getHeight() / 2f){
                    musicListTable.y = musicBarTable.y - musicListTable.getHeight();
                }else{
                    musicListTable.y = musicBarTable.y + musicBarTable.getHeight();
                }
                if(musicListTable.x != musicBarTable.x) musicListTable.x = musicBarTable.x;
            }


        }));

        bar = musicBarTable;
        list = musicListTable;

        parent.addChild(musicBarTable);
        parent.addChild(musicListTable);
    }

    public void reload(){
        if(posted){
            app.post(() -> {
                ui.hudGroup.removeChild(bar);
                ui.hudGroup.removeChild(list);
                build(ui.hudGroup);
            });
        }
    }
}
