package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class RetrogradeThrustersAI implements ShipSystemAIScript {
	private ShipAPI ship;
	private float timer = 0f;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		//this.engine = engine;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (!AIUtils.canUseSystemThisFrame(ship))
			return;
		timer += amount;
		if (timer < 0.1f)
			return;
		timer = 0f;

		CombatEntityAPI manuvTarget = (CombatEntityAPI) ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);

		if (manuvTarget != null && Misc.isInArc(MathUtils.clampAngle(ship.getFacing() + 180), 30f, ship.getLocation(), manuvTarget.getLocation()) && Misc.getDistance(ship.getLocation(), manuvTarget.getLocation()) >= 900f) {
			ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
			return;
		}

		if (ship.getShipAI().getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.RUN_QUICKLY) || ship.getShipAI().getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF)) {
			ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
			return;
		}

		float damage = 0f;
		if (missileDangerDir != null && Misc.getAngleDiff(ship.getFacing(), Misc.getAngleInDegrees(missileDangerDir)) <= 180f) {
			for (MissileAPI m : AIUtils.getNearbyEnemyMissiles(ship, 400f)) {
				damage += m.getDamage().getDamage();
			}
			if (damage >= ship.getFluxTracker().getMaxFlux() / 2 || ship.getFluxTracker().getMaxFlux() - ship.getFluxTracker().getCurrFlux() <= damage || damage >= ship.getHitpoints() / 2)
				ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
		}
	}
}
