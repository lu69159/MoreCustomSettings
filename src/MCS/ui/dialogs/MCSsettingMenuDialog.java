package MCS.ui.dialogs;

import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import MCS.game.*;

import static arc.Core.*;
import static mindustry.ui.dialogs.SettingsMenuDialog.*;
import static mindustry.Vars.*;
import static MCS.main.*;

public class MCSsettingMenuDialog {
    private BaseDialog blockStringDialog, unitStringDialog, musicImportDialog, musicInGameDialog, planetMusicListDialog, musicListDialog;
    private musicSquareSearchDialog musicSearchDialog;

    public Cons<SettingsTable> settingBuilder = t -> {
        t.pref(new TitleSetting("@settingtitle.music"));

        t.checkPref("instantChangeBossMusic", false);
        t.checkPref("enableCustomMusic", false, b -> {
            if(b){
                musicLoader.loadCustom();
            }
            else{
                musicLoader.reset();
            }
        });
        t.pref(new ButtonSetting("@importMusic", Icon.play, () -> musicImportDialog.show()));
        if(!mobile){
            t.pref(new ButtonSetting("@openMusicFolder", Icon.folder, () -> {
                if (musicLoader.musicFolder == null || !musicLoader.musicFolder.exists()) musicLoader.loadFolder();
                app.openFolder(musicLoader.musicFolder.absolutePath());
            }));
        }
        t.pref(new ButtonSetting("@clearMusic", Icon.trash,
                () -> ui.showConfirm("@clearMusic", "@clearMusic.confirm", () -> musicLoader.delete())
        ));
        t.pref(new ButtonSetting("@musicList", Icon.list, () -> {
            rebuildMusicList();
            musicListDialog.show();
        }));
        t.pref(new ButtonSetting("@musicSquare.search", Icon.zoom, () -> {
            musicSearchDialog.show();
        }));

        t.pref(new TitleSetting("@settingtitle.campaignDifficulty"));

        t.checkPref("enablecustomcampaigndifficulty", false, b -> {
            if(b){
                ui.campaignRules = new CustomCampaignRulesDialog();
                spawner = new CustomWaveSpawner();
            }
            else{
                ui.campaignRules = new CampaignRulesDialog();
                spawner = new WaveSpawner();
            }
        });

        //forceCampaignDifficulty
        t.checkPref("forceCampaignDifficulty", false, b -> {
            if(b){
                for(var p : content.planets()){
                    if(!p.allowCampaignRules){
                        settings.put(p.name + "-forceCD", true);
                        p.allowCampaignRules = true;
                    }
                }
            }
            else{
                for(var p : content.planets()){
                    if(settings.getBool(p.name + "-forceCD")){
                        settings.remove(p.name + "-forceCD");
                        p.allowCampaignRules = false;
                    }
                }
            }
        });

        t.pref(new TitleSetting("@settingtitle.buildAttacked"));

        t.checkPref("enableBuildAttackFrag", false, b -> attacked.blockEnabled = b);
        t.pref(new ButtonSetting("@editAttackedString", Icon.pencil, () -> blockStringDialog.show()));
        t.checkPref("bannedAttackedBlocksWhitelist", false, b -> attacked.blockWhitelist = b);
        t.pref(new ButtonSetting("@bannedAttackedBlocks", Icon.cancel, () -> attacked.bannedAttackBlocksDialog.show(attacked.bannedAttackBlocks)));

        t.pref(new TitleSetting("@settingtitle.unitAttacked"));

        t.checkPref("enableUnitAttackFrag", false, u -> attacked.unitEnabled = u);
        t.sliderPref("unitHealthPercent", 50, 0, 100, 1, i -> i + "%");
        t.pref(new ButtonSetting("@editAttackedString", Icon.pencil, () -> unitStringDialog.show()));
        t.checkPref("bannedAttackedUnitsWhitelist", false, u -> attacked.unitWhitelist = u);
        t.pref(new ButtonSetting("@bannedAttackedUnits", Icon.cancel, () -> attacked.bannedAttackUnitsDialog.show(attacked.bannedAttackUnits)));

        t.pref(new GithubLink("Github"));
    };

    public void load(){
        blockStringDialog = new BaseDialog("@settings");
        blockStringDialog.buttons.defaults().size(210, 64);
        blockStringDialog.cont.table(t -> {
            t.field(settings.getString("blockStringMCS", bundle.get("buildAttacked")), s -> attacked.tmpString = s).width(400f).center().padLeft(10f);
            t.button("@confirm", Icon.ok, () -> {
                attacked.blockString = attacked.tmpString;
                attacked.blockChanged = true;
                settings.put("blockStringMCS", attacked.blockString);
                blockStringDialog.hide();
            }).size(105f, 64f).padLeft(10f);
            t.button("@back", Icon.left, blockStringDialog::hide).size(105f, 64f);
        });
        blockStringDialog.addCloseListener();

        unitStringDialog = new BaseDialog("@settings");
        unitStringDialog.buttons.defaults().size(210, 64);
        unitStringDialog.cont.table(t -> {
            t.field(settings.getString("unitStringMCS", bundle.get("unitAttacked")), s -> attacked.tmpString = s).width(400f).center().padLeft(10f);
            t.button("@confirm", Icon.ok, () -> {
                attacked.unitString = attacked.tmpString;
                attacked.unitEnabled = true;
                settings.put("unitStringMCS", attacked.unitString);
                unitStringDialog.hide();
            }).size(105f, 64f).padLeft(10f);
            t.button("@back", Icon.left, unitStringDialog::hide).size(105f, 64f);
        });
        unitStringDialog.addCloseListener();

        musicInGameDialog = new BaseDialog("@importMusic");
        musicInGameDialog.addCloseButton();
        musicInGameDialog.cont.table(Tex.button, t -> {
            t.defaults().size(200f, 60f).left();

            t.button("@importMusic.ambient", Styles.flatt, musicLoader.importMusic("a"));
            t.row();
            t.button("@importMusic.dark", Styles.flatt, musicLoader.importMusic("d"));
            t.row();
            t.button("@importMusic.boss", Styles.flatt, musicLoader.importMusic("b"));
            t.row();
        });

        planetMusicListDialog = new BaseDialog("@importMusic");
        planetMusicListDialog.addCloseButton();
        planetMusicListDialog.cont.pane(t -> {
            t.defaults().size(200f, 60f).left();

            for(var planet : content.planets()){
                if(!planet.accessible) continue;
                t.button(planet.localizedName, Icon.planet.tint(planet.iconColor), musicLoader.importNamedMusic(planet.name));
                t.row();
            }
        });

        musicImportDialog = new BaseDialog("@importMusic");
        musicImportDialog.addCloseButton();
        musicImportDialog.cont.table(Tex.button, t -> {
            t.defaults().size(200f, 60f).left();

            t.button("@importMusic.inGame", Styles.flatt, () -> musicInGameDialog.show());
            t.row();
            t.button("@importMusic.editor", Styles.flatt, musicLoader.importNamedMusic("editor"));
            t.row();
            t.button("@importMusic.menu", Styles.flatt, musicLoader.importNamedMusic("menu"));
            t.row();
            t.button("@importMusic.planet", Styles.flatt, () -> planetMusicListDialog.show());
            t.row();
        });

        musicListDialog = new BaseDialog("@musicList"){{
            onResize(() -> rebuildMusicList());
        }};
        musicListDialog.addCloseButton();

        musicSearchDialog = new musicSquareSearchDialog();
        musicSearchDialog.setup();

        try{
            ui.settings.addCategory(bundle.get("morecustomsettings"), Icon.settings, settingBuilder);
            replaceResetButton();

            Seq<Music> a = new Seq<>(control.sound.ambientMusic), b = new Seq<>(control.sound.bossMusic), d = new Seq<>(control.sound.darkMusic);
            control.sound.ambientMusic.clear();
            control.sound.darkMusic.clear();
            control.sound.bossMusic.clear();
            control.sound = new CustomSoundControl(){{
                ambientMusic = a;
                bossMusic = b;
                darkMusic = d;
            }};
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }

        if(settings.getBool("enablecustomcampaigndifficulty")){
            ui.campaignRules = new CustomCampaignRulesDialog();
            spawner = new CustomWaveSpawner();
        }
    }

    private void rebuildMusicList(){
        musicLoader.loadFolder();
        musicListDialog.cont.clearChildren();
        musicListDialog.cont.pane(t -> {
            t.add("@importMusic.ambient").color(Pal.accent).padTop(10).left().row();
            boolean found = false;
            for(var f : musicLoader.ambient.seq()){
                if(musicLoader.isMusic(f)){
                    t.table(Styles.grayPanel, mt -> {
                        mt.labelWrap(musicLoader.getName(f)).left().fillX().expandX();
                        mt.button("@delete", Icon.trashSmall, () -> {
                            f.delete();
                            musicLoader.load();
                            rebuildMusicList();
                        }).padLeft(10);
                    }).growX().left().row();
                    found = true;
                }
            }
            if(!found) t.add("@musicList.empty").padLeft(10).left().row();

            t.add("@importMusic.dark").color(Pal.accent).padTop(10).left().row();
            found = false;
            for(var f : musicLoader.dark.seq()){
                if(musicLoader.isMusic(f)){
                    t.table(Styles.grayPanel, mt -> {
                        mt.labelWrap(musicLoader.getName(f)).left().fillX().expandX();
                        mt.button("@delete", Icon.trashSmall, () -> {
                            f.delete();
                            musicLoader.load();
                            rebuildMusicList();
                        }).padLeft(10);
                    }).growX().left().row();
                    found = true;
                }
            }
            if(!found) t.add("@musicList.empty").padLeft(10).left().row();

            t.add("@importMusic.boss").color(Pal.accent).padTop(10).left().row();
            found = false;
            for(var f : musicLoader.boss.seq()){
                if(musicLoader.isMusic(f)){
                    t.table(Styles.grayPanel, mt -> {
                        mt.labelWrap(musicLoader.getName(f)).left().fillX().expandX();
                        mt.button("@delete", Icon.trashSmall, () -> {
                            f.delete();
                            musicLoader.load();
                            rebuildMusicList();
                        }).padLeft(10);
                    }).growX().left().row();
                    found = true;
                }
            }
            if(!found) t.add("@musicList.empty").padLeft(10).left().row();

            //MENU & EDITOR MUSIC
            t.add("@importMusic.menu").color(Pal.accent).padTop(10).left().row();
            found = false;
            for(var f : musicLoader.musicFolder.seq()){
                if(musicLoader.isMusic(f) && f.nameWithoutExtension().split("__", 2)[0].equals("menu")){
                    t.table(Styles.grayPanel, mt -> {
                        mt.labelWrap(settings.getString("MCSmenuMusicName", "unknown music")).left().fillX().expandX();
                        mt.button("@delete", Icon.trashSmall, () -> {
                            f.delete();
                            settings.remove("MCSmenuMusicName");
                            musicLoader.menuMusic = null;
                            musicLoader.load();
                            rebuildMusicList();
                        }).padLeft(10);
                    }).growX().left().row();
                    found = true;
                }
            }
            if(!found) t.add("@musicList.empty").padLeft(10).left().row();

            t.add("@importMusic.editor").color(Pal.accent).padTop(10).left().row();
            found = false;
            for(var f : musicLoader.musicFolder.seq()){
                if(musicLoader.isMusic(f) && f.nameWithoutExtension().split("__", 2)[0].equals("editor")){
                    t.table(Styles.grayPanel, mt -> {
                        mt.labelWrap(settings.getString("MCSeditorMusicName", "unknown music")).left().fillX().expandX();
                        mt.button("@delete", Icon.trashSmall, () -> {
                            f.delete();
                            settings.remove("MCSeditorMusicName");
                            musicLoader.editorMusic = null;
                            musicLoader.load();
                            rebuildMusicList();
                        }).padLeft(10);
                    }).growX().left().row();
                    found = true;
                }
            }
            if(!found) t.add("@musicList.empty").padLeft(10).left().row();

            //planetMusic
            t.add("@importMusic.planet").color(Pal.accent).padTop(10).left().row();
            t.image().color(Pal.accent).height(3).left().fillX().padBottom(3).row();

            for(var p : content.planets()){
                if(!p.accessible) continue;
                t.add("[#" + p.iconColor + "]" + Iconc.planet + p.localizedName).padTop(5).left().row();
                if(musicLoader.planetMusicMap.get(p) != null){
                    t.table(Styles.grayPanel, mt -> {
                        var f = musicLoader.planetMusicMap.get(p).file;
                        mt.labelWrap(settings.getString("MCSplanetMusicName-" + p.name, "unknown music")).left().fillX().expandX();
                        mt.button("@delete", Icon.trashSmall, () -> {
                            f.delete();
                            settings.remove("MCSplanetMusicName-" + p.name);
                            musicLoader.planetMusicMap.remove(p);
                            musicLoader.load();
                            rebuildMusicList();
                        }).padLeft(10);
                    }).growX().left().row();
                }else{
                    t.add("@musicList.empty").padLeft(10).left().row();
                }
            }

        }).width(graphics.getWidth() / Scl.scl() * 0.75f).growY(); //.growX().growY();
    }

    private void replaceResetButton(){
        var cat = ui.settings.getCategories().find(sc -> sc.name.equals(bundle.get("morecustomsettings")));
        cat.table = new SettingsTable(){
            @Override
            public void rebuild() {
                clearChildren();
                for(Setting setting : list){
                    setting.add(this);
                }
                button(bundle.get("settings.reset", "Reset to Defaults"), () -> {
                    for(Setting setting : list) {
                        if (setting.name != null && setting.title != null) {
                            settings.remove(setting.name);
                        }
                    }

                    musicLoader.reset();
                    ruleMaps.reset();
                    attacked.reset();

                    if(ui.campaignRules instanceof CustomCampaignRulesDialog) ui.campaignRules = new CampaignRulesDialog();
                    if(spawner instanceof CustomWaveSpawner) spawner = new WaveSpawner();

                    for(var p : content.planets()){
                        if(settings.getBool(p.name + "-forceCD")){
                            settings.remove(p.name + "-forceCD");
                            p.allowCampaignRules = false;
                        }
                    }

                    rebuild();
                }).margin(14f).width(240f).pad(6f);
            }
        };
        cat.builder.get(cat.table);
    }

    public static class TitleSetting extends SettingsTable.Setting {
        public TitleSetting(String text) {
            super("");
            this.title = text;
        }

        public void add(SettingsTable table) {
            table.add(this.title).color(Pal.accent).padTop(25.0F).padRight(110.0F).padBottom(-5.0F).left().pad(5.0F);
            table.row();
            table.image().color(Pal.accent).height(3.0F).padRight(110.0F).padBottom(25.0F).left().fillX().padBottom(5.0F);
            table.row();
        }
    }
    public static class ButtonSetting extends SettingsTable.Setting{
        @Nullable Drawable icon;
        @Nullable Runnable onClick;
        public ButtonSetting(String name) {
            super(name);
        }

        public ButtonSetting(String name, Drawable icon, Runnable onClick){
            this(name);
            this.icon = icon;
            this.onClick = onClick;
        }

        @Override
        public void add(SettingsTable table) {
            table.button(name, icon, onClick).marginLeft(5f).growX().row();
        }
    }

    public static class GithubLink extends SettingsTable.Setting{
        public GithubLink(String name) { super(name); }

        @Override
        public void add(SettingsTable table) {
            table.add(new Table(t -> {
                t.button(Icon.github, new ImageButton.ImageButtonStyle(), () -> {
                    String url = "https://github.com/lu69159/MoreCustomSettings";
                    if (!app.openURI(url)) {
                        ui.showInfoFade("@linkfail");
                        app.setClipboardText(url);
                    }
                });
            })).row();
        }
    }
}
