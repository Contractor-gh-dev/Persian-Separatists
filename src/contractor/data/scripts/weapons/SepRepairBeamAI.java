package contractor.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.plugins.SepRepairBeamManager;
import contractor.data.scripts.util.ContractorUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static contractor.data.scripts.util.ContractorStaticVars.REPAIR_BEAM_RATIO;

public class SepRepairBeamAI implements AutofireAIPlugin {
	private final ShipAPI ship;
	private final float weaponRange;
	public final WeaponAPI weapon;
	private ShipAPI target = null;
	private float timer = 0f;
	private boolean ready = false;

	public SepRepairBeamAI(WeaponAPI weapon) {
		this.weapon = weapon;
		this.ship = weapon.getShip();
		this.weaponRange = weapon.getSpec().getMaxRange();
	}

	@Override
	public void advance(float amount) {
		timer += amount;

		if (timer > 0.1f) {

			timer = 0f;
			CombatEngineAPI engine = Global.getCombatEngine();
			if (!engine.getListenerManager().hasListenerOfClass(SepRepairBeamManager.class))
				engine.getListenerManager().addListener(new SepRepairBeamManager());

			SepRepairBeamManager plugin = engine.getListenerManager().getListeners(SepRepairBeamManager.class).get(0);
			target = null;
			List<ShipAPI> targets = ContractorUtils.getAllyShipsWithinRange(weapon.getLocation(), weaponRange * 1.75f, ship.getOwner());

			if (targets.isEmpty()) {
				ready = false;
				return;
			}

			for (ShipAPI s : targets) {
				if (plugin.getMinimumAverageArmor(s) < 0f)
					plugin.updateMinimumAverageArmor(s);
			}

			boolean reeval;
			do {
				reeval = false;
				boolean assigned = false;
				float lowestArmor = 1f;
				for (ShipAPI s : targets) {
					float tmp = ContractorUtils.getAverageArmor(s.getArmorGrid());
					if (tmp >= 0.99f)
						continue;
					if (tmp < lowestArmor) {
						lowestArmor = tmp;
						target = s;
						assigned = true;
					}
				}

				float maxArmorRepair = Math.min(1f, plugin.getMinimumAverageArmor(target) + (REPAIR_BEAM_RATIO - 0.001f));
				if (!targets.isEmpty() && assigned && lowestArmor >= maxArmorRepair) {
					targets.remove(target);
					target = null;
					reeval = true;
				}
			} while (reeval);

			if (target == null) {
				ready = false;
				return;
			}
		}

		if (target != null) {
			if (ship.isFighter() && ship.getShipAI() != null) {
				ship.setShipTarget(target);
				ship.getShipAI().setTargetOverride(target);
			}

			Vector2f startPoint = weapon.getFirePoint(0);
			Vector2f endPoint = target.getLocation();
			List<ShipAPI> ignore = new ArrayList<>(2);
			ignore.add(target);
			ignore.add(ship);

			List<ShipAPI> possibleObs = ContractorUtils.rayCastListShip(startPoint, endPoint, ignore);
			if (!possibleObs.isEmpty()) {
				ready = false;
				return;
			}

			ready = MathUtils.isWithinRange(weapon.getLocation(), endPoint, weaponRange)
					&& Misc.isInArc(weapon.getSlot().computeMidArcAngle(ship), weapon.getArc(), weapon.getLocation(), endPoint)
					&& Misc.isInArc(weapon.getCurrAngle(), 5f, weapon.getLocation(), endPoint);

//            if (ready) {
//                weapon.setForceFireOneFrame(true);
//                //Global.getCombatEngine().maintainStatusForPlayerShip("repairbeam_dev", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Working",  Boolean.toString(ready).toUpperCase(), false);
//            }
		}
	}

	@Override
	public boolean shouldFire() {
		return ready;
	}

	@Override
	public void forceOff() {
		ready = target != null && MathUtils.isWithinRange(weapon.getLocation(), target.getLocation(), weaponRange)
				&& Misc.isInArc(weapon.getSlot().computeMidArcAngle(ship), weapon.getArc(), weapon.getLocation(), target.getLocation())
				&& Misc.isInArc(weapon.getCurrAngle(), 5f, weapon.getLocation(), target.getLocation());
	}

	@Override
	public Vector2f getTarget() {
		if (target == null)
			return null;
		return target.getLocation();
	}

	@Override
	public ShipAPI getTargetShip() {
		if (target == null)
			return null;
		return target;
	}

	@Override
	public WeaponAPI getWeapon() {
		return weapon;
	}

	@Override
	public MissileAPI getTargetMissile() {
		return null;
	}
}
