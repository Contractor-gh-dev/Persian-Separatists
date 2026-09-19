package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.combat.ShipwideAIFlags.*;

public class SepCitadelModeAIOld implements ShipSystemAIScript {
	private ShipAPI ship;
	private ShipSystemAPI system;
	private SepCitadelMode script;
	private final float distanceSQ = 2000f * 2000f;
	private float averageRange = 0f, timer = 0f, probeTimer = 0f;
	private boolean once = true;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
		this.script = (SepCitadelMode) system.getScript();
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		float fluxLevel = ship.getFluxLevel();
		float hardFlux = ship.getHardFluxLevel();

		if (fluxLevel > 0.5f
				&& script.coolantUsed > 25f
				&& ship.getAIFlags().hasFlag(AIFlags.RUN_QUICKLY)) {
			deactivate();
			return;
		}
		if (script.coolantUsed > 32f) {
			deactivate();
			return;
		}
		if (script.coolantUsed < 15f && !ship.getAIFlags().hasFlag(AIFlags.PURSUING))
			ship.getAIFlags().setFlag(AIFlags.DO_NOT_VENT, 1f);

		if (once) {
			doOnce();
			once = false;
		}

//        if (target != null && MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation()) > averageRange * averageRange
//                && (ship.getAIFlags().hasFlag(AIFlags.PURSUING) || !ship.getAIFlags().hasFlag(AIFlags.MAINTAINING_STRIKE_RANGE))) {
//            deactivate();
//            return;
//        }
		float activeMult = 1f;
		if (system.isActive()) {
			activeMult = 1.25f;

			if (fluxLevel < 0.05f || fluxLevel == hardFlux) {
				timer += amount;
				if (timer > 2f) {
					deactivate();
					return;
				}
			} else
				timer = 0f;
		}
		if (ship.getEngineController().isFlamedOut())
			activeMult += 0.5f;

		float rangeMult = 1f;
		if (target != null && !target.isFighter())
			rangeMult = (float) Math.log(distanceSQ / MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation()));
		if (rangeMult < 0.45f)
			rangeMult = 0.45f;

		float fluxDiff = (fluxLevel * 6 - hardFlux * 6) + 1f;
		fluxDiff = (float) Math.log(fluxDiff);
		float fluxScaled = fluxLevel + 0.3f;
		fluxScaled = fluxScaled * fluxScaled;

		if (fluxLevel > 0.2f
				&& fluxScaled * activeMult * rangeMult * fluxDiff > 0.25f * Math.max(1f, script.coolantUsed / 12f))
			activate();

		if (system.isActive()) {
			if (fluxLevel > 0.98f) {
				ship.getAIFlags().setFlag(AIFlags.DO_NOT_USE_SHIELDS, 1f);
			} else if (hardFlux + 0.03f > fluxLevel) {
				float averageArmor = 1f;
				if (target != null) {
					probeTimer += amount;
					if (probeTimer > 1f) {
						probeTimer = 0f;
						averageArmor = probeArmor(ship, target);
					}
					if (averageArmor < 0.5f && ship.getAIFlags().hasFlag(AIFlags.HAS_INCOMING_DAMAGE))
						ship.getAIFlags().unsetFlag(AIFlags.DO_NOT_USE_SHIELDS);
					else
						ship.getAIFlags().setFlag(AIFlags.DO_NOT_USE_SHIELDS, 1f);
				} else {
					if (ship.getAIFlags().hasFlag(AIFlags.HAS_INCOMING_DAMAGE))
						ship.getAIFlags().unsetFlag(AIFlags.DO_NOT_USE_SHIELDS);
					else
						ship.getAIFlags().setFlag(AIFlags.DO_NOT_USE_SHIELDS, 1f);
				}
			}
		}
	}

	private void doOnce() {
		int activeWeps = 0;
		for (WeaponAPI wep : ship.getAllWeapons()) {
			if (!wep.hasAIHint(WeaponAPI.AIHints.PD)) {
				averageRange += wep.getRange();
				activeWeps++;
			}
		}
		averageRange = (averageRange / activeWeps) * 0.95f;
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
