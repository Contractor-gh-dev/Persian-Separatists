package contractor.data.scripts.statuseffects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Electric extends BaseStatusEffect {
	private final List<ArcManager> arcManagers = new ArrayList<>(4);
	private final ShipAPI.HullSize size;
	private final float radius;
	private final IntervalUtil choiceInterval = new IntervalUtil(1f, 3f);
	private final boolean hasDFC;
	private final CombatEngineAPI engine;
	private final List<WeaponAPI> targetWeps;
	private static final Map<ShipAPI.HullSize, Float> sizeScale = new HashMap<>(); static {
		sizeScale.put(ShipAPI.HullSize.FIGHTER, 1.3f);
		sizeScale.put(ShipAPI.HullSize.FRIGATE, 1f);
		sizeScale.put(ShipAPI.HullSize.DESTROYER, 1.1f);
		sizeScale.put(ShipAPI.HullSize.CRUISER, 1.4f);
		sizeScale.put(ShipAPI.HullSize.CAPITAL_SHIP, 1.8f);
	}

	private static final Color col1 = new Color(0, 90, 255);
	private static final Color col2 = new Color(0, 160, 255);
	private static final Color phaseCol1 = new Color(120, 70, 255, 75);
	private static final Color phaseCol2 = new Color(120, 130, 255, 75);
	private static final EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams(); static {
		params.segmentLengthMult = 0.9f;
		params.flamesOutMissiles = false;
		params.glowSizeMult = 0.5f;
	}

	private int moveChoice, wepChoice;
	private float timer = 0f;

	private class ArcManager {
		private final int numArcs;
		private int timesArced = 0;
		private double time;
		private Vector2f start;

		ArcManager(int numArcs) {
			this.numArcs = numArcs;
		}

		protected void advance(float amount) {
			time += (Math.random() * 0.2d) + amount;
			if (start == null)
				start = MathUtils.getRandomPointInCircle(target.getLocation(), radius);
			if (time > 0.3f) {
				Vector2f end = MathUtils.getRandomPointInCircle(target.getLocation(), radius);
				EmpArcEntityAPI arc;
				if (target.isPhased())
					arc = engine.spawnEmpArcVisual(start, target, end, target, 16f, phaseCol1, phaseCol2, params);
				else
					arc = engine.spawnEmpArcVisual(start, target, end, target, 16f, col1, col2, params);
				Global.getSoundPlayer().playSound("tachyon_lance_emp_impact", 1.6f, 0.3f, target.getLocation(), target.getVelocity());
				arc.setSingleFlickerMode();
				start = new Vector2f(end);
				timesArced++;
			}
			if (timesArced >= numArcs)
				arcManagers.remove(this);
		}
	}

	public Electric(@NotNull ShipAPI target, float initial) {
		super(target, initial);
		this.type = StatusType.ELECTRIC;
		this.id = target.getId() + StatusType.ELECTRIC;
		this.size = target.getHullSize();
		radius = (target.getSpriteAPI().getWidth() + target.getSpriteAPI().getHeight()) * 0.2f;
		engine = Global.getCombatEngine();
		targetWeps = target.getAllWeapons();
		int j = targetWeps.size();
		for (int i = j - 1; i >= 0; i--)
			if (targetWeps.get(i).isDecorative() || targetWeps.get(i).getSlot().isSystemSlot())
				targetWeps.remove(i);
		moveChoice = (int) Math.round(Math.random() * 5);
		wepChoice = (int) Math.round(Math.random() * (targetWeps.size() - 1));
		hasDFC = target.getVariant().hasHullMod("distributed_fire_control");
	}

	@Override
	public void advance(float amount) {
		if (!target.isAlive()) {
			remove();
			return;
		}
		timer += amount;
		if (timer > 0.2f) {
			timer = 0f;
			int numArcs = (int) (Math.round(Math.random() * 6f) + 1);
			arcManagers.add(new ArcManager(numArcs));
		}
		choiceInterval.advance(amount);
		if (choiceInterval.intervalElapsed()) {
			moveChoice = (int) Math.round(Math.random() * 5);
			wepChoice = (int) Math.round(Math.random() * (targetWeps.size() - 1));
		}

		int j = arcManagers.size();
		for (int i = j - 1; i >= 0; i--)
			if (arcManagers.get(i) != null)
				arcManagers.get(i).advance(amount);

		double random = Math.random();
		if (hasDFC)
			random *= 1.666d;
		if (random < 0.4d)
			forceMovement(moveChoice);
		if (random < 0.2d)
			forceWeapon(wepChoice, amount);
		if (random < 0.01d)
			target.giveCommand(ShipCommand.USE_SYSTEM, target.getMouseTarget(), 0);

		if (target == engine.getPlayerShip())
			engine.maintainStatusForPlayerShip(id, Global.getSettings().getSpriteName("systems", "drg_electric_ui_sprite"), "Electrical Overload", "Helm Malfunctioning", true);

		boolean isPlayer = target == Global.getCombatEngine().getPlayerShip();
		float timeMult;
		if (isPlayer)
			timeMult = 1f;
		else
			timeMult = target.getMutableStats().getTimeMult().modified;
		float scaledAmount = amount * sizeScale.get(size);
		effectLevel -= scaledAmount * timeMult;
		if (effectLevel <= 0f)
			remove();
	}

	private void forceWeapon(int choice, float amount) {
		if (targetWeps.isEmpty())
			return;
		WeaponAPI wep = targetWeps.get(choice);
		float turnRateScaled = wep.getTurnRate() * amount * 1.5f;
		float angle = wep.getCurrAngle();
		angle += turnRateScaled;
		wep.setCurrAngle(angle);
		if (Math.random() < 0.25d) {
			if (target.isPhased()) {
				float cost = wep.getFluxCostToFire() * 0.75f;
				target.getFluxTracker().setCurrFlux(target.getCurrFlux() + cost);
				Global.getSoundPlayer().playSound("disabled_small_crit", 1.0f, 0.3f, target.getLocation(), target.getVelocity());
			} else
				wep.setForceFireOneFrame(true);
		}
	}

	private void forceMovement(int choice) {
		switch (choice) {
			case 0: {
				target.giveCommand(ShipCommand.ACCELERATE, null, 0);
				target.blockCommandForOneFrame(ShipCommand.DECELERATE);
				target.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
			}
			case 1: {
				target.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
				target.blockCommandForOneFrame(ShipCommand.DECELERATE);
				target.blockCommandForOneFrame(ShipCommand.ACCELERATE);
			}
			case 2: {
				target.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
				target.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
				target.blockCommandForOneFrame(ShipCommand.DECELERATE);
				target.blockCommandForOneFrame(ShipCommand.ACCELERATE);
			}
			case 3: {
				target.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
				target.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
				target.blockCommandForOneFrame(ShipCommand.DECELERATE);
				target.blockCommandForOneFrame(ShipCommand.ACCELERATE);
			}
			case 4: {
				target.giveCommand(ShipCommand.TURN_LEFT, null, 0);
				target.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
			}
			case 5: {
				target.giveCommand(ShipCommand.TURN_RIGHT, null, 0);
				target.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
			}
		}
	}
}
