package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.combat.ShipwideAIFlags.*;

public class SepCitadelModeAI implements ShipSystemAIScript {
	private ShipAPI ship;
	private ShipSystemAPI system;
	private SepCitadelMode script;
	private static final float distanceSQ = 2000f * 2000f;
	private float averageRange = 0f, timer = 0f, probeTimer = 0f, elapsed = 0f;
	private boolean once = true;
	private float lastHull;
	private boolean afraid = false, damageDanger = false, chasing = false, needMobility = false;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
		this.script = (SepCitadelMode) system.getScript();
		lastHull = ship.getHitpoints();
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (Global.getCombatEngine().isPaused())
			return;

		if (once) {
			doOnce();
			once = false;
		}

		elapsed += amount;
		if (elapsed < 0.1f)
			return;
		elapsed = 0f;

		assessAI(target);

		float fluxLevel = ship.getFluxLevel();
		float hardFlux = ship.getHardFluxLevel();

		if (ship.getAIFlags().hasFlag(AIFlags.SAFE_VENT)) {
			deactivate();
			return;
		}
		if (script.coolantUsed > 32f) {
			deactivate();
			return;
		}
		if (fluxLevel > 0.5f
				&& script.coolantUsed > 25f
				&& afraid) {
			deactivate();
			return;
		}

		float activeMult = 1f;
		if (system.isActive()) {
			activeMult += 0.7f;
			if (fluxLevel < 0.03f || MathUtils.equals(fluxLevel, hardFlux)) {
				timer += amount;
				if (timer > 1.5f) {
					deactivate();
					return;
				}
			} else
				timer = 0f;
		}

		if (ship.getEngineController().isFlamedOut())
			activeMult += 1f;

		float rangeMult = 1f;
		if (target != null && !target.isFighter())
			rangeMult = (float) Math.log(distanceSQ / MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation()));
		rangeMult = Math.max(0.45f, Math.min(4f, rangeMult));

		float fluxDiff = (fluxLevel * 8 - hardFlux * 8) + 2.5f;
		fluxDiff = (float) Math.log(fluxDiff);

		float fluxScaled = fluxLevel + 0.3f;
		fluxScaled = fluxScaled * fluxScaled;

		float shieldMod = 1f;
		if (ship.getShield() != null && ship.getShield().isOff() && !needMobility && !chasing)
			shieldMod += 0.5f;

		float desire = fluxScaled * activeMult * rangeMult * fluxDiff * shieldMod;

		if (afraid)
			desire *= 0.6f;
		if (needMobility || chasing)
			desire *= 0.8f;
		if (damageDanger)
			desire *= 0.75f;

		if (fluxLevel > 0.1f && desire > 0.5f * Math.min(1.5f, Math.max(1f, script.coolantUsed / 15f)))
			activate();
		else {
			deactivate();
			return;
		}

		if (system.isActive()) {
			ship.getAIFlags().setFlag(AIFlags.DO_NOT_VENT, 0.5f);
			if (fluxLevel > 0.98f) {
				ship.getAIFlags().setFlag(AIFlags.DO_NOT_USE_SHIELDS, 0.5f);
			} else if (hardFlux + 0.04f > fluxLevel) {
				float averageArmor = 1f;
				if (target != null && !target.isFighter()) {
					probeTimer += amount;
					if (probeTimer > 0.5f) {
						probeTimer = 0f;
						averageArmor = ship.getAverageArmorInSlice(Misc.getAngleInDegrees(ship.getLocation(), target.getLocation()), 120f);
						averageArmor = averageArmor / ship.getArmorGrid().getMaxArmorInCell();

						if (averageArmor < 0.6f && damageDanger)
							ship.getAIFlags().unsetFlag(AIFlags.DO_NOT_USE_SHIELDS);
						else
							ship.getAIFlags().setFlag(AIFlags.DO_NOT_USE_SHIELDS, 0.6f);

					} else if (damageDanger)
						ship.getAIFlags().unsetFlag(AIFlags.DO_NOT_USE_SHIELDS);
					else
						ship.getAIFlags().setFlag(AIFlags.DO_NOT_USE_SHIELDS, 0.6f);

				} else {
					if (damageDanger)
						ship.getAIFlags().unsetFlag(AIFlags.DO_NOT_USE_SHIELDS);
					else
						ship.getAIFlags().setFlag(AIFlags.DO_NOT_USE_SHIELDS, 0.6f);
				}
			}
		}
	}

	private void doOnce() {
		int activeWeps = 0;
		for (WeaponAPI wep : ship.getAllWeapons()) {
			if (!wep.getOriginalSpec().getAIHints().contains(WeaponAPI.AIHints.PD)) {
				averageRange += wep.getRange();
				activeWeps++;
			}
		}
		averageRange = (averageRange / activeWeps) * 0.95f;
	}

	private void assessAI(ShipAPI target) {
		if (lastHull > ship.getHitpoints() * 0.85f)
			afraid = true;
		else
			afraid = ship.getAIFlags().hasFlag(AIFlags.RUN_QUICKLY) || ship.getAIFlags().hasFlag(AIFlags.BACKING_OFF) || ship.getAIFlags().hasFlag(AIFlags.NEEDS_HELP);
		lastHull = ship.getHitpoints();

		damageDanger = ship.getAIFlags().hasFlag(AIFlags.HAS_INCOMING_DAMAGE) || ship.getAIFlags().hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER);

		chasing = ship.getAIFlags().hasFlag(AIFlags.PURSUING) || ship.getAIFlags().hasFlag(AIFlags.HARASS_MOVE_IN);

		boolean far = false;
		if (ship.getAIFlags().hasFlag(AIFlags.MOVEMENT_DEST)) {
			Object o = ship.getAIFlags().getCustom(AIFlags.MOVEMENT_DEST);
			if (o instanceof Vector2f point) {
				far = MathUtils.getDistanceSquared(ship.getLocation(), point) > averageRange * averageRange * 2f;
			} else if (o instanceof ShipAPI s) {
				far = MathUtils.getDistanceSquared(ship.getLocation(), s.getLocation()) > averageRange * averageRange * 2f;
			}
		}
		boolean needTurn = false;
		if (target != null && !target.isFighter()) {
			if (Math.abs(MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), target.getLocation()))) > 150f)
				needTurn = true;
		}
		needMobility = ship.getAIFlags().hasFlag(AIFlags.ESCORT_OTHER_SHIP) || far || needTurn;
	}

	private float probeArmor(ShipAPI toTest, ShipAPI from) {
		Vector2f start = new Vector2f(from.getLocation());
		Vector2f end = new Vector2f(toTest.getLocation());
		List<Vector2f> segment = new ArrayList<>();
		segment.add(start);
		for (float i = 0.75f; i < 1; i += 0.01f) {
			float x = (start.x - (i * start.x)) + (i * end.x);
			float y = (start.y - (i * start.y)) + (i * end.y);
			Vector2f temp = new Vector2f(x, y);
			segment.add(temp);
		}
		segment.add(end);

		for (Vector2f test : segment) {
			ArmorGridAPI gridEntity = toTest.getArmorGrid();
			if (gridEntity.getCellAtLocation(test) != null) {
				float[][] fullGrid = gridEntity.getGrid();
				int[] gridArray = gridEntity.getCellAtLocation(test);
				float averageArmorFrac = 0f;
				int averageNum = 0;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++) {
						if (gridArray[0] + x < 0 || gridArray[0] + x > fullGrid.length)
							continue;
						if (gridArray[1] + y < 0 || gridArray[1] + y > fullGrid[gridArray[0]].length)
							continue;

						averageArmorFrac += gridEntity.getArmorFraction(gridArray[0] + x, gridArray[1] + y);
						averageNum++;
					}
				}
				averageArmorFrac = averageArmorFrac / averageNum;

				return averageArmorFrac;
			}
		}
		return 1f;
	}

	private void activate() {
		if (!system.isActive())
			ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
	}

	private void deactivate() {
		timer = 0f;
		if (system.isActive())
			ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
	}
}
