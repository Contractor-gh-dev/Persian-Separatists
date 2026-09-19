package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.statuseffects.StatusEffect;
import contractor.data.scripts.statuseffects.StatusEffectUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicTargeting.targetSeeking;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.magiclib.util.MagicTargeting.pickShipTarget;

public class DrgEntropicLash extends BaseShipSystemScript {
	public static final float BASE_RANGE = 850f;
	private final Color core = new Color(200, 70, 25, 255);
	private final Color fringe = new Color(255, 125, 125, 175);
	boolean hasFired = false;
	private final EmpArcEntityAPI.EmpArcParams param = new EmpArcEntityAPI.EmpArcParams();

	{
		param.segmentLengthMult = 10f;
		param.zigZagReductionFactor = 0.3f;
		param.fadeOutDist = 400f;
		param.minFadeOutMult = 2.5f;
		param.flickerRateMult = 0.6f;
	}

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null)
			return;

		WeaponAPI deco = null;

		for (WeaponAPI wep : ship.getAllWeapons())
			if (wep.getId().equals("drg_entropic_deco"))
				deco = wep;

		if (state == State.IN || state == State.ACTIVE)
			deco.setForceFireOneFrame(true);

		CombatEngineAPI engine = Global.getCombatEngine();

		if (state == State.ACTIVE) {
			float range = stats.getSystemRangeBonus().computeEffective(BASE_RANGE);
			if (!hasFired) {
				hasFired = true;
				List<Vector2f> firePoints = new ArrayList<>();
				for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy())
					if (slot.isSystemSlot()) {
						Vector2f slotLocation = new Vector2f(slot.computePosition(ship));
						firePoints.add(slotLocation);
					}

				Vector2f targetLoc;
				ShipAPI target = pickShipTarget(ship, targetSeeking.NO_RANDOM, (int) range + 1, 120, 1, 1, 1, 1, 1);
				Global.getSoundPlayer().playSound("drg_sys_entropic_discharge", 1f, 1f, ship.getLocation(), ship.getVelocity());

				if (target != null && !target.getCollisionClass().equals(CollisionClass.NONE)) {
					for (Vector2f point : firePoints) {
						EmpArcEntityAPI arc = engine.spawnEmpArc(ship, point, ship, target, DamageType.ENERGY, 50f, 50f, 900f, "pseudoparticle_jet_hit_heavy",
								(float) (Math.random() * 12f + 16f), fringe, core, param);
						arc.setSingleFlickerMode(true);
						arc.setFadedOutAtStart(true);
						arc.setCoreWidthOverride(16f);
					}
					StatusEffectUtils.addOrMaintainEffect(target, StatusEffect.StatusType.ENTROPIC, 10f, 10f);
				} else {
					targetLoc = ship.getMouseTarget();
					if (MathUtils.getDistanceSquared(ship.getLocation(), targetLoc) > range * range) {
						float min = ship.getFacing() - 30f;
						float max = ship.getFacing() + 30f;
						min = Misc.normalizeAngle(min);
						max = Misc.normalizeAngle(max);
						targetLoc = MathUtils.getRandomPointInCone(ship.getLocation(), 512f, min, max);
					}
					for (Vector2f point : firePoints) {
						float x, y;
						x = (float) (targetLoc.x + ((Math.random() - 0.5) * 128f));
						y = (float) (targetLoc.y + ((Math.random() - 0.5) * 128f));
						Vector2f rand = new Vector2f(x, y);

						engine.spawnEmpArcVisual(point, ship, rand, null, (float) (Math.random() * 12f + 16f), fringe, core);
					}
				}
			}
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		hasFired = false;
	}

	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isCoolingDown())
			return "COOLING";

		ShipAPI target = pickShipTarget(ship, targetSeeking.NO_RANDOM, (int) (ship.getMutableStats().getSystemRangeBonus().computeEffective(BASE_RANGE)) + 1, 120, 1, 1, 1, 1, 1);
		if (target != null) {
			if (MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation()) < BASE_RANGE * BASE_RANGE)
				return "READY";
			else
				return "OUT OF RANGE";
		}
		return "NO TARGET";
	}
}
