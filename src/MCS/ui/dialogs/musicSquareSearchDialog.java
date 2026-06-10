package MCS.ui.dialogs;

import MCS.musicSquare.sources.*;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class musicSquareSearchDialog extends BaseDialog {
    allResources resource =  new allResources();
    private Table resultTable;
    private TextField searchField;
    private String word = "";

    public musicSquareSearchDialog() {
        super("@musicSquare.search");
        addCloseButton();
        setFillParent(true);
        resource.onSearchBeginning = this::loading;
        resource.onSearchComplete = this::complete;
        shown(this::setup);
    }

    private void setup() {
        cont.table(t -> {
            t.left();
            t.image(Icon.zoom).padRight(8f);
            searchField = t.field("", text -> word = text).growX().get();
            t.button("@searchMusic", Icon.zoom, () -> {
                resource.search(word);
            }).size(100f, 50f);
        }).fillX().padBottom(4).row();

        cont.pane(pane -> {
            pane.top().left();
            resultTable = pane;
        }).grow().row();

        buttons.defaults().size(180f, 50f);
        buttons.button("@musicSquare.loadMore", Icon.download, () -> resource.loadMore(10));
    }

    private void loading(){
        resultTable.clear();
        resultTable.top().left();
        resultTable.add(Core.bundle.get("loading")).pad(20);
    }

    private void complete(){
        resultTable.clear();
        resultTable.top().left();
        for(var t : resource.allResults){
            trackShow(t);
        }
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

            row.button(Icon.play, Styles.clearNonei, () -> {}).size(38f);
            row.button(Icon.download, Styles.clearNonei, () -> {}).size(38f);

            if(t.pic != null && !t.pic.isEmpty()){
                Http.get(t.pic, res -> {
                    try{
                        byte[] data = res.getResult();
                        Core.app.post(() -> {
                            try{
                                Pixmap pix = new Pixmap(data);
                                Texture tex = new Texture(pix);
                                pix.dispose();
                                cover.setDrawable(new TextureRegionDrawable(new TextureRegion(tex)));
                            }catch(Exception ignored){}
                        });
                    }catch(Exception ignored){}
                }, e -> {});
            }
        }).growX().pad(2).row();
    }
}
