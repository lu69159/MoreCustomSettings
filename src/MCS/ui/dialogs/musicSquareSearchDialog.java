package MCS.ui.dialogs;

import MCS.customMusic.CustomSoundControl;
import MCS.musicSquare.sources.*;
import MCS.musicSquare.sources.musics.*;
import arc.*;
import arc.audio.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class musicSquareSearchDialog extends BaseDialog {
    allResources resource =  new allResources();
    private Table resultTable;
    private TextField searchField;
    private String word = "";
    String previewingUrl = "";
    Image loadingSpinner;
    private final Seq<Texture> coverTextures = new Seq<>();
    private int coverGeneration;

    public musicSquareSearchDialog() {
        super("@musicSquare.search");
        addCloseButton();
        setFillParent(true);
        resource.onSearchBeginning = this::loading;
        resource.onSearchComplete = this::complete;
        hidden(() -> {
            stopPreviewListening();
            searchField.setText("");
            word = "";
            resource.allResults.clear();
            resultTable.clear();
            clearCoverTextures();
            var sound = (CustomSoundControl)control.sound;
            sound.musicLoader.tmp.deleteDirectory();
            sound.musicLoader.tmp.mkdirs();
        });
    }

    public void setup() {
        cont.table(t -> {
            t.left();
            t.image(Icon.zoom).padRight(8f);
            searchField = t.field("", text -> word = text).growX().get();
            if(!mobile){
                searchField.keyDown(KeyCode.enter, () -> {
                    if(!word.isEmpty()){
                        resource.search(word);
                    }
                });
            }
            t.button("@searchMusic", Icon.zoom, () -> {
                if(!word.isEmpty()) resource.search(word);
            }).size(100f, 50f);
        }).fillX().padBottom(4).row();

        cont.pane(pane -> {
            pane.top().left();
            resultTable = pane;
        }).grow().row();

        buttons.defaults().size(210f,64f);
        //buttons.button("@musicSquare.loadMore", Icon.download, () -> resource.loadMore(10)); //TODO
    }

    private void loading(){
        clearCoverTextures();
        resultTable.clear();
        resultTable.top().left();
        resultTable.add(Core.bundle.get("loading")).pad(20);
        loadingSpinner = resultTable.image(Icon.settings, Pal.accent).update(i -> {
            i.rotateBy(Time.delta * 360f / 20f);
        }).size(16f).padLeft(4f).get();
    }

    private void complete(){
        loadingSpinner = null;
        resultTable.clear();
        resultTable.top().left();
        if(resource.allResults.size == 0){
            resultTable.add(Core.bundle.get("musicList.empty")).pad(20);
        }else{
            for(var t : resource.allResults){
                trackShow(t);
            }
        }
    }

    private void clearCoverTextures(){
        coverGeneration++;
        for(Texture texture : coverTextures){
            texture.dispose();
        }
        coverTextures.clear();
    }

    private void trackShow(musicBase.Track t){
        resultTable.table(Styles.grayPanel, row -> {
            row.defaults().pad(4).left();

            Image cover = new Image();
            cover.setScaling(Scaling.fit);
            row.add(cover).size(48f);

            row.table(info -> {
                info.defaults().left();
                info.add(t.name != null ? t.name : "@unknown")
                    .color(Pal.accent).growX().wrap();
                info.row();
                info.add(t.artist != null ? t.artist : "")
                    .color(Color.lightGray).growX().wrap();
            }).width(300f).growX().padLeft(8f);

            if(t.url != null && !t.url.isEmpty() && musicBase.isSafeUrl(t.url)){
                row.button(Icon.play, new ImageButton.ImageButtonStyle(Styles.clearNonei){{
                    imageUp = Icon.play;
                    imageChecked = Icon.pause;
                }}, () -> {
                    if(t.url.equals(previewingUrl)){
                        stopPreviewListening();
                    }else{
                        trackPreviewListening(t);
                    }
                }).checked(b -> t.url.equals(previewingUrl)).size(38f); //TODO：加载中按钮
                row.button(Icon.download, Styles.clearNonei, () -> {
                    trackDownload(t);
                }).size(38f);
            }else{
                row.add(Core.bundle.get("musicSquare.noSource"))
                    .color(Color.lightGray).padLeft(8f);
            }

            if(t.pic != null && !t.pic.isEmpty() && musicBase.isSafeUrl(t.pic)){
                int generation = coverGeneration;

                Http.get(t.pic, res -> {
                    try{
                        byte[] data = res.getResult();

                        Core.app.post(() -> {
                            if(generation != coverGeneration) return;

                            Pixmap pix = null;
                            Texture tex = null;
                            try{
                                pix = new Pixmap(data);
                                tex = new Texture(pix);
                                cover.setDrawable(new TextureRegionDrawable(new TextureRegion(tex)));
                                coverTextures.add(tex);
                                tex = null;
                            }catch(Exception e){
                                cover.setDrawable(Core.atlas.find("error"));
                            }finally{
                                if(pix != null) pix.dispose();
                                if(tex != null) tex.dispose();
                            }
                        });
                    }catch(Exception e){
                        Core.app.post(() -> cover.setDrawable(Core.atlas.find("error")));
                    }
                }, e -> {});
            }
        }).growX().pad(2).row();
    }

    private void trackPreviewListening(musicBase.Track t){
        if(!musicBase.isSafeUrl(t.url)) return;
        previewingUrl = t.url;

        var sound = (CustomSoundControl)control.sound;

        t.downloadToTmp(fi -> {
            Core.app.post(() -> {
                try{
                    Music music = new Music(fi){
                        @Override
                        public void setLooping(boolean isLooping){}
                    };
                    sound.playPreView(music);
                }catch(Exception ex){
                    ui.showException(ex);
                }
            });
        }, e -> {
            previewingUrl = "";
            ui.showInfo("@musicSquare.noAudio");
        }, true);
    }

    private void stopPreviewListening(){
        ((CustomSoundControl)control.sound).stopPreView();
        previewingUrl = "";
    }

    private void trackDownload(musicBase.Track t){
        BaseDialog dialog = new BaseDialog("@musicSquare.selectCategory");
        dialog.addCloseButton();
        dialog.cont.table(Tex.button, bt -> {
            bt.defaults().size(200f, 60f).left();
            bt.button("@importMusic.ambient", Styles.flatt, () -> {
                dialog.hide();
                t.download("ambient");
            });
            bt.row();
            bt.button("@importMusic.dark", Styles.flatt, () -> {
                dialog.hide();
                t.download("dark");
            });
            bt.row();
            bt.button("@importMusic.boss", Styles.flatt, () -> {
                dialog.hide();
                t.download("boss");
            });
            bt.row();
            bt.button("@importMusic.menu", Styles.flatt, () -> {
                dialog.hide();
                t.download("menu");
            });
            bt.row();
            bt.button("@importMusic.editor", Styles.flatt, () -> {
                dialog.hide();
                t.download("editor");
            });
            bt.row();
            bt.button("@importMusic.planet", Styles.flatt, () -> {
                var planets = new BaseDialog("@importMusic.planet");
                planets.addCloseButton();
                planets.cont.pane(table -> {
                    table.defaults().size(200f, 60f).left();
                    for(var planet : content.planets()){
                        if(!planet.accessible) continue;
                        table.button(planet.localizedName, Icon.planet.tint(planet.iconColor), () -> {
                            planets.hide();
                            dialog.hide();
                            t.download(planet.name);
                        });
                        table.row();
                    }
                });
                planets.show();
            });
        });
        dialog.show();
    }
}
