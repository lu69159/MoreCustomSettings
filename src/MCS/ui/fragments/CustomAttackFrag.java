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
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.gen.*;

import static arc.Core.settings;
import static mindustry.Vars.*;
import static mindustry.game.EventType.*;

public class CustomAttackFrag {
    public Table table;
    public boolean blockEnabled, unitEnabled;
    public boolean blockWhitelist, unitWhitelist;
    public boolean blockChanged = false, unitChanged = false, posted = false;
    public String tmpString;
    public String blockString, unitString;
    public ObjectSet<Block> bannedAttackBlocks = new ObjectSet<>();
    public ObjectSet<UnitType> bannedAttackUnits = new ObjectSet<>();
    public BannedContentDialog<Block> bannedAttackBlocksDialog;
    public BannedContentDialog<UnitType> bannedAttackUnitsDialog;
    float[] attackTime = {0, 0};
    float showTime = 300f;

    private static final Rect rect = new Rect();
    @Nullable private Building attackedBuild;
    @Nullable private Unit attackedUnit;

    //TODO: 残血阈值下的单位受攻击提醒

    public CustomAttackFrag(){
        Events.on(BuildDamageEvent.class, b -> {
            if(blockEnabled && b.build.team == player.team() && bannedAttackBlocks.contains(b.build.block) == blockWhitelist){
                if(attackTime[0] < 60f){
                    attackTime[0] = showTime;
                    attackedBuild = b.build;
                }

                if(attackedBuild == null || attackedBuild.dead()){
                    attackedBuild = b.build;
                }
            }
        });
        Events.on(UnitDamageEvent.class, u -> {
            if(unitEnabled && u.unit.team == player.team() && bannedAttackUnits.contains(u.unit.type) == unitWhitelist && u.unit.health() * 100f / u.unit.maxHealth() <= settings.getInt("unitHealthPercent")){
                if(attackTime[1] < 60f){
                    attackTime[1] = showTime;
                    attackedUnit = u.unit;
                }
                if(attackedUnit == null || attackedUnit.dead()){
                    attackedUnit = u.unit;
                }
            }
        });
        Events.on(WorldLoadEvent.class, e -> {
            if(!posted){
                Core.app.post(() -> {
                    build(ui.hudGroup);
                });
                posted = true;
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void load(){
        table = new Table(Styles.black6);
        String json;

        blockEnabled = settings.getBool("enableBuildAttackFrag", false);
        blockWhitelist = settings.getBool("bannedAttackedBlocksWhitelist", false);
        blockString = settings.getString("blockStringMCS", Core.bundle.get("buildAttacked"));
        json = settings.getString("bannedAttackBlocksMCS", "");
        if(json.isEmpty()){
            bannedAttackBlocks = new ObjectSet<>();
        }
        else{
            bannedAttackBlocks = (ObjectSet<Block>)JsonIO.json.fromJson(ObjectSet.class, Block.class, json);
        }
        bannedAttackBlocksDialog = new BannedContentDialog<>("@bannedAttackedBlocks", ContentType.block, Block::canBeBuilt){{
            hidden(() -> saveBlocks());
        }};

        unitEnabled = settings.getBool("enableUnitAttackFrag", false);
        unitWhitelist = settings.getBool("bannedAttackedUnitsWhitelist", false);
        unitString = settings.getString("unitStringMCS", Core.bundle.get("unitAttacked"));
        json = settings.getString("bannedAttackUnitsMCS", "");
        if(json.isEmpty()){
            bannedAttackUnits = new ObjectSet<>();
        }
        else{
            bannedAttackUnits = (ObjectSet<UnitType>)JsonIO.json.fromJson(ObjectSet.class, UnitType.class, json);
        }
        bannedAttackUnitsDialog = new BannedContentDialog<>("@bannedAttackedUnits", ContentType.unit, u -> !u.isHidden()){{
            hidden(() -> saveUnits());
        }};
    }

    public void saveBlocks(){
        String json = JsonIO.json.toJson(bannedAttackBlocks, ObjectSet.class, Block.class);
        settings.put("bannedAttackBlocksMCS", json);
    }

    public void saveUnits(){
        String json = JsonIO.json.toJson(bannedAttackUnits, ObjectSet.class, UnitType.class);
        settings.put("bannedAttackUnitsMCS", json);
    }

    public void build(Group parent){
        parent.fill(t -> {
            t.y = Core.graphics.getHeight() / 4f;
            t.collapser(top -> top.background(Styles.black3).add(blockString).pad(16f)
            .with(l -> {
                l.tapped(() -> {
                    if(attackedBuild != null){
                        control.input.panCamera(Tmp.v1.set(attackedBuild));
                    }
                });
                l.addListener(new HandCursorListener());
            })
            .update(label -> {
                label.color.set(Color.orange).lerp(Pal.accent.cpy(), Mathf.absin(Time.time, 2f, 1f));
                if(blockChanged){
                    label.setText(blockString);
                    blockChanged = false;
                }
            }), true,
            () -> {
                if (!blockEnabled || state.isPaused()) return false;
                if (state.isMenu()) {
                    attackTime[0] = 0f;
                    return false;
                }
                return (attackTime[0] -= Time.delta) > 0;
            }).touchable(Touchable.disabled).fillX().row();

            t.collapser(top -> top.background(Styles.black3).add(unitString).pad(16f)
                .with(l -> {
                    l.tapped(() -> {
                        if(attackedUnit != null){
                            control.input.panCamera(Tmp.v1.set(attackedUnit));
                        }
                    });
                    l.addListener(new HandCursorListener());
                })
                .update(label -> {
                                label.color.set(Color.orange).lerp(Pal.accent.cpy(), Mathf.absin(Time.time, 2f, 1f));
                                if(unitChanged){
                                    label.setText(unitString);
                                    unitChanged = false;
                                }
                }), true,
                () -> {
                        if (!unitEnabled || state.isPaused()) return false;
                        if (state.isMenu()) {
                            attackTime[1] = 0f;
                            return false;
                }
                return (attackTime[1] -= Time.delta) > 0;
            }).touchable(Touchable.disabled).fillX().row();
        });

        parent.fill((x, y, w, h) -> {
            if(attackedBuild != null){
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
            }
        }).visible(() -> blockEnabled && attackTime[0] > 0 && !attackedBuild.dead());

        parent.fill((x, y, w, h) -> {
            if(attackedUnit != null){
                if(!rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f).setCenter(Core.camera.position.x, Core.camera.position.y).contains(attackedUnit.x, attackedUnit.y)){
                    Vec2 pos1 = Core.scene.screenToStageCoordinates(Tmp.v1.set(Core.camera.project(player.x, player.y).x, Core.camera.project(player.x, player.y).y));
                    Vec2 pos2 = Core.scene.screenToStageCoordinates(Tmp.v2.set(Core.camera.project(attackedUnit.x, attackedUnit.y).x, Core.camera.project(attackedUnit.x, attackedUnit.y).y));

                    float scale = Scl.scl() / renderer.camerascale, size = 5f / scale, len = 25f / scale;
                    float dst = pos1.dst(pos2) / len, dx = (pos2.x - pos1.x) / dst, dy = (pos2.y - pos1.y) / dst;
                    float rx = pos1.x + dx, ry = pos1.y + dy;
                    float angle = Tmp.v1.set(attackedUnit.x, attackedUnit.y).sub(player).angle();

                    Draw.color(Pal.heal);
                    Fill.poly(rx, ry, 3, size, angle);
                    Draw.reset();
                }
            }
        }).visible(() -> unitEnabled && attackTime[1] > 0 && !attackedUnit.dead());
    }
}
