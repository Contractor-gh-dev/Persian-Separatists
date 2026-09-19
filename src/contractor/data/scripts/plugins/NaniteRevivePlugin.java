package contractor.data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import contractor.data.hullmods.NaniteHullmod;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static contractor.data.hullmods.NaniteHullmod.SHIP_BEING_REVIVED;

public class NaniteRevivePlugin extends BaseEveryFrameCombatPlugin {
	private final IntervalUtil interval = new IntervalUtil(0.2f, 0.3f);
	private final ShipAPI ship;
	private float elapsed = 0, time;
	private int timesRevived = 1;
	private boolean firstRev = true;
	private static final Color col1 = new Color(187, 207, 146, 90);
	private static final EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams(); static {
		params.segmentLengthMult = 0.9f;
		params.fadeOutDist = 48f;
		params.zigZagReductionFactor = 0.2f;
	}
	private static final Map<ShipAPI.HullSize, Float> repairMap = new HashMap<>(); static {
		repairMap.put(ShipAPI.HullSize.FRIGATE, 0.8f);
		repairMap.put(ShipAPI.HullSize.DESTROYER, 0.7f);
		repairMap.put(ShipAPI.HullSize.CRUISER, 0.6f);
		repairMap.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.5f);
	}
	private static final DamagingExplosionSpec expSpec = new DamagingExplosionSpec(1f, 72f, 38f, 50000f, 5000f, CollisionClass.HITS_SHIPS_ONLY_FF, CollisionClass.NONE,
			12f, 16f, 3f, 48, col1, null); static {
		expSpec.setUseDetailedExplosion(true);
		expSpec.setDamageType(DamageType.HIGH_EXPLOSIVE);
		expSpec.setSoundSetId("hit_hull_heavy");
	}

	public NaniteRevivePlugin(ShipAPI ship, float time) {
		this.ship = ship;
		this.time = (float) (time + ((Math.random() - 0.5) * 2));
		for (ShipAPI curr : Global.getCombatEngine().getShips())
			if (curr.getFleetMember() == ship.getFleetMember())
				curr.addTag(SHIP_BEING_REVIVED);
	}

	public void advance(float amount, List<InputEventAPI> events) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.isInEngine(ship))
			engine.removePlugin(this);
		if (engine.isPaused() || ship.isAlive())
			return;

		List<ShipAPI> pieces = new ArrayList<>();
		for (ShipAPI curr : engine.getShips()) {
			if (curr.getFleetMember() == ship.getFleetMember()) {
				pieces.add(curr);
			}
		}
		if (pieces.size() > 1)
			Global.getCombatEngine().removePlugin(this);

		if (ship.getOriginalOwner() != 0)
			engine.setCombatNotOverForAtLeast(time * 0.5f);

		if (ship.getFluxTracker().isOverloaded())
			elapsed += amount * 0.5f;
		else
			elapsed += amount;
		float timeScale = (elapsed / time) + 0.1f;

		ship.setJitter(this, col1, timeScale + 0.1f, 4, timeScale * 16f);
		ship.setJitterUnder(this, col1, timeScale + 0.1f, 8, timeScale * 20f);

		interval.advance(amount * timeScale);
		if (elapsed > 5f) {
			Global.getSoundPlayer().playLoop("system_temporalshell_loop_low", this, 1f, timeScale, ship.getLocation(), ship.getVelocity());
			if (interval.intervalElapsed()) {
				Vector2f point = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() + 64f);
				EmpArcEntityAPI arc = engine.spawnEmpArcVisual(ship.getLocation(), ship, point, null, 16f, col1, Color.white, params);
				arc.setSingleFlickerMode();
				arc.setFadedOutAtStart(true);
				Global.getSoundPlayer().playSound("energy_lash_fire", 0.7f, 0.45f, ship.getLocation(), ship.getVelocity());
			}
		}

		if (elapsed >= time) {
			if (firstRev || Math.random() > 0.33d * timesRevived) {
				firstRev = false;
				elapsed = 0;
				time = (float) (NaniteHullmod.SPAWN_TIME + ((Math.random() - 0.5) * 2));

				Global.getSoundPlayer().playSound("nanite_revive", 1f, 1.7f, ship.getLocation(), ship.getVelocity());

				ship.setOwner(1);
				ship.setHulk(false);
				float scale = repairMap.get(ship.getHullSize());
				float hp = scale * ship.getMaxHitpoints();
				hp /= timesRevived;
				ship.setHitpoints(hp);
				ship.setShipAI(Global.getSettings().pickShipAIPlugin(ship == null ? null : ship.getFleetMember(), ship));
				for (ShipEngineControllerAPI.ShipEngineAPI shipEngine : ship.getEngineController().getShipEngines())
					shipEngine.repair();
				for (WeaponAPI wep : ship.getAllWeapons())
					wep.repair();
				ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getCurrFlux() * 0.5f);
				ship.getSpriteAPI().setColor(Color.white);
				float max = ship.getArmorGrid().getMaxArmorInCell();
				float[][] grid = ship.getArmorGrid().getGrid();
				for (int i = 0; i < grid.length; i++) {
					for (int j = 0; j < grid[i].length; j++) {
						if (grid[i][j] < scale * max)
							grid[i][j] = scale * max;
					}
				}

				for (ShipAPI child : ship.getChildModulesCopy()) {
					child.setOwner(1);
					child.setHulk(false);
					scale = repairMap.get(child.getHullSize());
					hp = scale * child.getMaxHitpoints();
					hp /= timesRevived;
					child.setHitpoints(hp);
					child.setShipAI(Global.getSettings().pickShipAIPlugin(child == null ? null : child.getFleetMember(), child));
					for (ShipEngineControllerAPI.ShipEngineAPI shipEngine : child.getEngineController().getShipEngines())
						shipEngine.repair();
					for (WeaponAPI wep : child.getAllWeapons())
						wep.repair();
					child.getFluxTracker().setCurrFlux(child.getFluxTracker().getCurrFlux() * 0.5f);
					child.getSpriteAPI().setColor(Color.white);
					max = child.getArmorGrid().getMaxArmorInCell();
					grid = child.getArmorGrid().getGrid();
					for (int i = 0; i < grid.length; i++) {
						for (int j = 0; j < grid[i].length; j++) {
							if (grid[i][j] < scale * max)
								grid[i][j] = scale * max;
						}
					}
				}

				timesRevived++;
			} else {
				engine.spawnDamagingExplosion(expSpec, ship, ship.getLocation(), true);
				engine.removePlugin(this);
			}
			if (timesRevived >= 3)
				engine.removePlugin(this);
		}
	}
}
