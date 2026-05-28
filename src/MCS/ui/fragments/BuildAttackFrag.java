package MCS.ui.fragments;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import mindustry.ctype.*;
import mindustry.editor.*;
import mindustry.graphics.*;
import mindustry.io.JsonIO;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.gen.*;

import static arc.Core.settings;
import static mindustry.Vars.*;
import static mindustry.game.EventType.*;

public class BuildAttackFrag{
    public Table table;
    public boolean enabled, whitelist;
    public String tmpString;
    public String showString;
    public ObjectSet<Block> bannedAttackBlocks = new ObjectSet<>();
    public BannedContentDialog<Block> bannedAttackBlocksDialog;
    float[] buildAttackTime = {0};
    float showTime = 300f;

    private static final Rect rect = new Rect();
    @Nullable private Building attackedBuild;

    public BuildAttackFrag(){
        Events.on(BuildDamageEvent.class, b -> {
            if(enabled && b.build.team == player.team() && bannedAttackBlocks.contains(b.build.block) == whitelist){
                if(buildAttackTime[0] < 60f){
                    buildAttackTime[0] = showTime;
                    attackedBuild = b.build;
                }

                if(attackedBuild == null || attackedBuild.dead()){
                    attackedBuild = b.build;
                }
            }
        });
        Events.on(WorldLoadEvent.class, e -> {
            Core.app.post(() -> {
                build(ui.hudGroup);
            });

        });
    }

    @SuppressWarnings("unchecked")
    public void load(){
        table = new Table(Styles.black6);
        enabled = settings.getBool("enableBuildAttackFrag", false);
        whitelist = settings.getBool("bannedAttackedBlocksWhitelist", false);
        showString = settings.getString("showStringMCS", Core.bundle.get("buildAttacked"));

        String json = settings.getString("bannedAttackBlocksMCS", "");
        if(json.isEmpty()){
            bannedAttackBlocks = new ObjectSet<>();
        }
        else{
            bannedAttackBlocks = (ObjectSet<Block>)JsonIO.json.fromJson(ObjectSet.class, Block.class, json);
        }

        bannedAttackBlocksDialog = new BannedContentDialog<>("@bannedAttackedBlocks", ContentType.block, Block::canBeBuilt){{
            hidden(() -> save());
        }};
    }

    public void save(){
        String json = JsonIO.json.toJson(bannedAttackBlocks, ObjectSet.class, Block.class);
        settings.put("bannedAttackBlocksMCS", json);
    }

    public void build(Group parent){
        parent.fill(t -> {
            t.y = Core.graphics.getHeight() / 4f;
            t.collapser(top -> top.background(Styles.black3).add(showString).pad(16f)
            .with(l -> {
                l.tapped(() -> {
                    if(attackedBuild != null && !attackedBuild.dead()){
                        control.input.panCamera(Tmp.v1.set(attackedBuild));
                    }
                });
                l.addListener(new HandCursorListener());
            })
            .update(label -> {
                label.color.set(Color.orange).lerp(Pal.accent.cpy(), Mathf.absin(Time.time, 2f, 1f));
            }), true,
            () -> {
                if (!enabled || state.isPaused()) return false;
                if (state.isMenu()) {
                    buildAttackTime[0] = 0f;
                    return false;
                }
                return (buildAttackTime[0] -= Time.delta) > 0;
            }).touchable(Touchable.disabled).fillX().row();
        });

        parent.fill((x, y, w, h) -> {
            if(attackedBuild == null) return;

            if(!rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f).setCenter(Core.camera.position.x, Core.camera.position.y).contains(attackedBuild.x, attackedBuild.y)){
                Vec2 pos1 = Core.scene.screenToStageCoordinates(Tmp.v1.set(Core.camera.project(player.x, player.y).x, Core.camera.project(player.x, player.y).y));
                Vec2 pos2 = Core.scene.screenToStageCoordinates(Tmp.v2.set(Core.camera.project(attackedBuild.x, attackedBuild.y).x, Core.camera.project(attackedBuild.x, attackedBuild.y).y));

                float scale = Scl.scl() / renderer.camerascale, size = 5f / scale, len = 25f / scale;
                float dst = pos1.dst(pos2) / len, dx = (pos2.x - pos1.x) / dst, dy = (pos2.y - pos1.y) / dst;
                float rx = pos1.x + dx, ry = pos1.y + dy;
                float angle = Tmp.v1.set(attackedBuild.x, attackedBuild.y).sub(player).angle();

                Draw.color(Pal.lightOrange);
                Fill.poly(rx, ry, 3, size, angle);
                Draw.reset();
            }
        }).visible(() -> enabled && buildAttackTime[0] > 0 && !attackedBuild.dead());
    }
}
