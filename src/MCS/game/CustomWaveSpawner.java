package MCS.game;

import arc.Events;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;
import static MCS.main.*;

public class CustomWaveSpawner extends WaveSpawner{
    private static final float margin = 0f, coreMargin = tilesize * 2f, maxSteps = 30;

    private int tmpCount;
    private Seq<Tile> spawns = new Seq<>(false);
    private boolean spawning = false;
    private boolean any = false;
    private Tile firstSpawn = null;

    public CustomWaveSpawner(){
        super();
    }

    @Override
    public Tile getFirstSpawn(){
        firstSpawn = null;
        eachGroundSpawn((cx, cy) -> {
            firstSpawn = world.tile(cx, cy);
        });
        return firstSpawn;
    }

    @Override
    public int countSpawns(){
        return spawns.size;
    }

    @Override
    public Seq<Tile> getSpawns(){
        return spawns;
    }

    @Override
    public boolean playerNear(){
        return state.hasSpawns() && !player.dead() && spawns.contains(g -> Mathf.dst(g.x * tilesize, g.y * tilesize, player.x, player.y) < state.rules.dropZoneRadius && player.team() != state.rules.waveTeam);
    }

    @Override
    public void spawnEnemies(){
        spawning = true;

        eachGroundSpawn(-1, (spawnX, spawnY, doShockwave) -> {
            if(doShockwave){
                doShockwave(spawnX, spawnY);
            }
        });

        for(SpawnGroup group : state.rules.spawns){
            if(group.type == null) continue;
            int spawned = group.getSpawned(state.wave - 1);
            if(spawned == 0) continue;

            if(state.isCampaign()){
                //1.5+ x 1 boss result in 2 bosses, else
                spawned = Math.max(1, group.effect == StatusEffects.boss ?
                        Mathf.round(spawned * rulesMaps.get(state.getPlanet()).enemySpawnMultiplier / 100f) :
                        //1.n x 1 unit result in 2 units
                        Mathf.ceil(spawned * rulesMaps.get(state.getPlanet()).enemySpawnMultiplier / 100f)
                );
            }
            int spawnedf = spawned;
            if(group.type.flying){
                float spread = margin / 1.5f;

                eachFlyerSpawn(group.spawn, (spawnX, spawnY) -> {
                    for(int i = 0; i < spawnedf; i++){
                        spawnUnit(group, spawnX + Mathf.range(spread), spawnY + Mathf.range(spread));
                    }
                });
            }else{
                float spread = tilesize * 2;

                eachGroundSpawn(group.spawn, (spawnX, spawnY, doShockwave) -> {

                    for(int i = 0; i < spawnedf; i++){
                        Tmp.v1.rnd(spread);

                        spawnUnit(group, spawnX + Tmp.v1.x, spawnY + Tmp.v1.y);
                    }
                });
            }
        }

        Time.run(121f, () -> spawning = false);
    }

    @Override
    public void spawnUnit(SpawnGroup group, float x, float y){
        group.createUnit(group.team == null ? state.rules.waveTeam : group.team, x, y,
                Angles.angle(x, y, world.width()/2f * tilesize, world.height()/2f * tilesize), state.wave - 1, this::spawnEffect);
    }

    //TODO: custom spawnEffect
    @Override
    public void doShockwave(float x, float y){
        Fx.spawnShockwave.at(x, y, state.rules.dropZoneRadius);
        Damage.damage(state.rules.waveTeam, x, y, state.rules.dropZoneRadius, 99999999f, true);
    }

    @Override
    public void eachGroundSpawn(Intc2 cons){
        eachGroundSpawn(-1, (x, y, shock) -> cons.get(World.toTile(x), World.toTile(y)));
    }

    private void eachGroundSpawn(int filterPos, SpawnConsumer cons){
        if(state.hasSpawns()){
            for(Tile spawn : spawns){
                if(filterPos != -1 && filterPos != spawn.pos()) continue;

                cons.accept(spawn.worldx(), spawn.worldy(), true);
            }
        }

        if(state.rules.wavesSpawnAtCores && state.rules.attackMode && state.teams.isActive(state.rules.waveTeam) && !state.teams.playerCores().isEmpty()){
            Building firstCore = state.teams.playerCores().first();
            for(CoreBlock.CoreBuild core : state.rules.waveTeam.cores()){
                if(filterPos != -1 && filterPos != core.pos()) continue;

                if(core.commandPos != null){
                    cons.accept(core.commandPos.x, core.commandPos.y, false);
                }else{
                    boolean valid = false;

                    Tmp.v1.set(firstCore).sub(core).limit(coreMargin + core.block.size * tilesize /2f * Mathf.sqrt2);

                    int steps = 0;

                    //keep moving forward until the max step amount is reached
                    while(steps++ < maxSteps){
                        int tx = World.toTile(core.x + Tmp.v1.x), ty = World.toTile(core.y + Tmp.v1.y);
                        any = false;
                        Geometry.circle(tx, ty, world.width(), world.height(), 3, (x, y) -> {
                            if(world.solid(x, y)){
                                any = true;
                            }
                        });

                        //nothing is in the way, spawn it
                        if(!any){
                            valid = true;
                            break;
                        }else{
                            //make the vector longer
                            Tmp.v1.setLength(Tmp.v1.len() + tilesize*1.1f);
                        }
                    }

                    if(valid){
                        cons.accept(core.x + Tmp.v1.x, core.y + Tmp.v1.y, false);
                    }
                }
            }
        }
    }

    private void eachFlyerSpawn(int filterPos, Floatc2 cons){

        for(Tile tile : spawns){
            if(filterPos != -1 && filterPos != tile.pos()) continue;

            if(!state.rules.airUseSpawns){
                float angle = Angles.angle(world.width() / 2f, world.height() / 2f, tile.x, tile.y);
                float trns = Math.max(world.width(), world.height()) * Mathf.sqrt2 * tilesize;
                float spawnX = Mathf.clamp(world.width() * tilesize / 2f + Angles.trnsx(angle, trns), -margin, world.width() * tilesize + margin);
                float spawnY = Mathf.clamp(world.height() * tilesize / 2f + Angles.trnsy(angle, trns), -margin, world.height() * tilesize + margin);
                cons.get(spawnX, spawnY);
            }else{
                cons.get(tile.worldx(), tile.worldy());
            }
        }

        if(state.rules.wavesSpawnAtCores && state.rules.attackMode && state.teams.isActive(state.rules.waveTeam)){
            for(Building core : state.rules.waveTeam.data().cores){
                if(filterPos != -1 && filterPos != core.pos()) continue;

                cons.get(core.x, core.y);
            }
        }
    }

    @Override
    public int countGroundSpawns(){
        tmpCount = 0;
        eachGroundSpawn((x, y) -> tmpCount ++);
        return tmpCount;
    }

    @Override
    public int countFlyerSpawns(){
        tmpCount = 0;
        eachFlyerSpawn(-1, (x, y) -> tmpCount ++);
        return tmpCount;
    }

    @Override
    public boolean isSpawning(){
        return spawning && !net.client();
    }

    @Override
    public void reset(){
        spawning = false;
        spawns.clear();

        for(Tile tile : world.tiles){
            if(tile.overlay() == Blocks.spawn){
                spawns.add(tile);
            }
        }
    }

    @Override
    public void spawnEffect(Unit unit){
        unit.apply(StatusEffects.unmoving, 30f);
        unit.apply(StatusEffects.invincible, 60f);
        unit.unloaded();

        Events.fire(new EventType.UnitSpawnEvent(unit));
        Call.spawnEffect(unit.x, unit.y, unit.rotation, unit.type);
    }

    private interface SpawnConsumer{
        void accept(float x, float y, boolean shockwave);
    }
}
