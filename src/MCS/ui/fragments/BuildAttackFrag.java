package MCS.ui.fragments;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.*;
import arc.math.geom.Vec2;
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
    public String showString;
    public ObjectSet<Block> bannedAttackBlocks = new ObjectSet<>();
    public BannedContentDialog<Block> bannedAttackBlocksDialog;
    float[] buildAttackTime = {0};
    float showTime = 300f;

    @Nullable private Building attackedBuild;

    public BuildAttackFrag(){
        Events.on(BuildDamageEvent.class, b -> {
            if(enabled && b.build.team == player.team() && bannedAttackBlocks.contains(b.build.block) == whitelist){
                if(buildAttackTime[0] < 60f){
                    buildAttackTime[0] = showTime;
                }

                if(attackedBuild == null || attackedBuild.dead() || buildAttackTime[0] < 180f){
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

    public void top(Building target){
        if(!player.dead() && ui.hudfrag.shown){
            float camX = Core.camera.position.x;
            float camY = Core.camera.position.y;
            float halfW = Core.camera.width * 0.45f;   // 0.9 / 2
            float halfH = Core.camera.height * 0.45f;

            boolean onScreen = target.x >= camX - halfW && target.x <= camX + halfW
                    && target.y >= camY - halfH && target.y <= camY + halfH;

            if(!onScreen){
                // 画指示器，和 OverlayRenderer 一样的逻辑
                Tmp.v1.set(target.x, target.y).sub(player).setLength(14f);
                Lines.stroke(1f, Pal.accent);
                Lines.lineAngle(player.x + Tmp.v1.x, player.y + Tmp.v1.y, Tmp.v1.angle(), 4f);
                Draw.reset();
            }
        }
    }

    public void build(Group parent){
        parent.fill(t -> {
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
            if(buildAttackTime[0] == 0f || attackedBuild == null || !enabled){
                Draw.reset();
                return;
            }

            float camX = Core.camera.position.x;
            float camY = Core.camera.position.y;
            float halfW = Core.camera.width * 0.45f;
            float halfH = Core.camera.height * 0.45f;

            boolean onScreen = attackedBuild.x >= camX - halfW && attackedBuild.x <= camX + halfW
                    && attackedBuild.y >= camY - halfH && attackedBuild.y <= camY + halfH;

            if(!onScreen){
                Tmp.v1.set(attackedBuild.x * tilesize, attackedBuild.y * tilesize).sub(player).setLength(14f);
                Draw.color(Pal.accent);
                Lines.stroke(8f);
                Lines.lineAngle(player.x + Tmp.v1.x, player.y + Tmp.v1.y, Tmp.v1.angle(), 16f);
                Draw.reset();
            }
        }).visible(() -> enabled && buildAttackTime[0] > 0);
    }
}
